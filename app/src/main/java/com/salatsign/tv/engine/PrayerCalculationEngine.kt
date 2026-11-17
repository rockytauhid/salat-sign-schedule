package com.salatsign.tv.engine

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerEvent
import com.salatsign.tv.data.model.PrayerReminderEvent
import com.salatsign.tv.data.model.PrayerTimeItem
import com.salatsign.tv.data.model.PrayerTimesDay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date

object PrayerCalculationEngine {

    fun getCalculationParameters(methodName: String): CalculationParameters {
        val method = runCatching {
            CalculationMethod.valueOf(methodName.uppercase())
        }.getOrDefault(CalculationMethod.SINGAPORE)
        val params = method.parameters
        params.madhab = Madhab.SHAFI
        return params
    }

    /**
     * Offline calculation of Islamic prayer times for a specified date and location coordinates.
     */
    fun calculatePrayerTimesDay(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        calculationMethodName: String = "SINGAPORE",
        hijriOffsetDays: Int = 0,
        referenceTime: LocalTime = LocalTime.now(),
        referenceDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): PrayerTimesDay {
        val coordinates = Coordinates(latitude, longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val parameters = getCalculationParameters(calculationMethodName)

        val prayerTimes = PrayerTimes(coordinates, dateComponents, parameters)

        val subuhTime = dateToLocalTime(prayerTimes.fajr, zoneId)
        val sunriseTime = dateToLocalTime(prayerTimes.sunrise, zoneId)
        val zuhurTime = dateToLocalTime(prayerTimes.dhuhr, zoneId)
        val ashrTime = dateToLocalTime(prayerTimes.asr, zoneId)
        val magribTime = dateToLocalTime(prayerTimes.maghrib, zoneId)
        val isyaTime = dateToLocalTime(prayerTimes.isha, zoneId)

        val rawList = listOf(
            Prayer.SUBUH to subuhTime,
            Prayer.ZUHUR to zuhurTime,
            Prayer.ASHR to ashrTime,
            Prayer.MAGRIB to magribTime,
            Prayer.ISYA to isyaTime
        )

        val isToday = (date == referenceDate)

        var identifiedNextPrayer: Prayer? = null
        var identifiedActivePrayer: Prayer? = null
        var secondsUntilNext: Long = 0

        if (isToday) {
            // Find upcoming prayer today
            val upcoming = rawList.firstOrNull { (_, time) -> time.isAfter(referenceTime) }
            if (upcoming != null) {
                identifiedNextPrayer = upcoming.first
                secondsUntilNext = ChronoUnit.SECONDS.between(referenceTime, upcoming.second).coerceAtLeast(0)
            } else {
                // If all prayers today passed, next is tomorrow's Subuh
                identifiedNextPrayer = Prayer.SUBUH
                val tomorrowComponents = DateComponents(date.plusDays(1).year, date.plusDays(1).monthValue, date.plusDays(1).dayOfMonth)
                val tomorrowTimes = PrayerTimes(coordinates, tomorrowComponents, parameters)
                val tomorrowSubuh = dateToLocalTime(tomorrowTimes.fajr, zoneId)
                val secondsUntilMidnight = ChronoUnit.SECONDS.between(referenceTime, LocalTime.MAX) + 1
                val secondsFromMidnight = tomorrowSubuh.toSecondOfDay().toLong()
                secondsUntilNext = secondsUntilMidnight + secondsFromMidnight
            }

            // Find currently active prayer:
            // A prayer is active if referenceTime is >= prayerTime and < nextPrayerTime (or within 20 mins after prayer starts)
            for (i in rawList.indices) {
                val (prayer, time) = rawList[i]
                val nextTime = if (i < rawList.lastIndex) rawList[i + 1].second else LocalTime.MAX
                if (!referenceTime.isBefore(time) && referenceTime.isBefore(nextTime)) {
                    // Check if within 20 minutes for active pulse or current prayer window
                    identifiedActivePrayer = prayer
                    break
                }
            }
        }

        val prayerItems = rawList.map { (prayer, time) ->
            PrayerTimeItem(
                prayer = prayer,
                time = time,
                isNext = isToday && (prayer == identifiedNextPrayer),
                isActive = isToday && (prayer == identifiedActivePrayer)
            )
        }

        val hijriFormatted = HijriCalendarEngine.formatHijriDate(date, hijriOffsetDays)
        val gregorianFormatted = HijriCalendarEngine.formatGregorianDate(date)

        val nextItem = prayerItems.firstOrNull { it.prayer == identifiedNextPrayer }
        val activeItem = prayerItems.firstOrNull { it.prayer == identifiedActivePrayer }

        return PrayerTimesDay(
            date = date,
            hijriDate = hijriFormatted,
            gregorianDate = gregorianFormatted,
            prayers = prayerItems,
            sunrise = sunriseTime,
            nextPrayer = nextItem,
            activePrayer = activeItem,
            secondsUntilNext = secondsUntilNext
        )
    }

    /**
     * Checks if referenceTime is currently within the exact strike window (0 to 5 minutes after prayer)
     * of any prayer today. Used by Mosque Mode to trigger silent slideshow interruption.
     */
    fun getPrayerAtExactTimeStrike(
        todayPrayerTimes: PrayerTimesDay,
        currentTime: LocalTime = LocalTime.now(),
        windowMinutes: Long = 5
    ): Prayer? {
        for (item in todayPrayerTimes.prayers) {
            val prayerTime = item.time
            val diffSeconds = ChronoUnit.SECONDS.between(prayerTime, currentTime)
            if (diffSeconds in 0..(windowMinutes * 60)) {
                return item.prayer
            }
        }
        return null
    }

    /**
     * Calculates all 5 prayer events for a given calendar date as ZonedDateTime instances.
     */
    fun getDayPrayerEvents(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        calculationMethodName: String = "SINGAPORE",
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<PrayerEvent> {
        val coordinates = Coordinates(latitude, longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val parameters = getCalculationParameters(calculationMethodName)

        val prayerTimes = PrayerTimes(coordinates, dateComponents, parameters)

        return listOf(
            PrayerEvent(Prayer.SUBUH, dateToZonedDateTime(prayerTimes.fajr, date, zoneId)),
            PrayerEvent(Prayer.ZUHUR, dateToZonedDateTime(prayerTimes.dhuhr, date, zoneId)),
            PrayerEvent(Prayer.ASHR, dateToZonedDateTime(prayerTimes.asr, date, zoneId)),
            PrayerEvent(Prayer.MAGRIB, dateToZonedDateTime(prayerTimes.maghrib, date, zoneId)),
            PrayerEvent(Prayer.ISYA, dateToZonedDateTime(prayerTimes.isha, date, zoneId))
        )
    }

    /**
     * Determines the earliest prayer event strictly after [now], evaluating both today's
     * and tomorrow's prayer times so transitions across midnight/after Isya are seamless.
     */
    fun nextPrayerEvent(
        now: ZonedDateTime,
        latitude: Double,
        longitude: Double,
        calculationMethodName: String = "SINGAPORE"
    ): PrayerEvent {
        val zoneId = now.zone
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)

        val todayEvents = getDayPrayerEvents(today, latitude, longitude, calculationMethodName, zoneId)
        val tomorrowEvents = getDayPrayerEvents(tomorrow, latitude, longitude, calculationMethodName, zoneId)

        val allEvents = todayEvents + tomorrowEvents
        return allEvents.firstOrNull { it.at.isAfter(now) }
            ?: tomorrowEvents.first()
    }

    fun nextPrayerEvent(
        now: ZonedDateTime,
        settings: AppSettings
    ): PrayerEvent = nextPrayerEvent(
        now = now,
        latitude = settings.latitude,
        longitude = settings.longitude,
        calculationMethodName = settings.calculationMethodName
    )

    /**
     * Determines the earliest 15-minute pre-prayer reminder strictly after [now].
     */
    fun nextReminderEvent(
        now: ZonedDateTime,
        latitude: Double,
        longitude: Double,
        calculationMethodName: String = "SINGAPORE"
    ): PrayerReminderEvent {
        val zoneId = now.zone
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)

        val todayEvents = getDayPrayerEvents(today, latitude, longitude, calculationMethodName, zoneId)
        val tomorrowEvents = getDayPrayerEvents(tomorrow, latitude, longitude, calculationMethodName, zoneId)

        val allReminders = (todayEvents + tomorrowEvents).map {
            PrayerReminderEvent(
                prayer = it.prayer,
                prayerAt = it.at,
                reminderAt = it.at.minusMinutes(15)
            )
        }

        return allReminders.firstOrNull { it.reminderAt.isAfter(now) }
            ?: allReminders.first().copy(
                prayerAt = allReminders.first().prayerAt.plusDays(1),
                reminderAt = allReminders.first().reminderAt.plusDays(1)
            )
    }

    fun nextReminderEvent(
        now: ZonedDateTime,
        settings: AppSettings
    ): PrayerReminderEvent = nextReminderEvent(
        now = now,
        latitude = settings.latitude,
        longitude = settings.longitude,
        calculationMethodName = settings.calculationMethodName
    )

    /**
     * Checks if an alarm event fired more than [thresholdMillis] (default: 5 minutes) late.
     */
    fun isAlarmStale(
        scheduledAtEpochMillis: Long,
        nowEpochMillis: Long = System.currentTimeMillis(),
        thresholdMillis: Long = 5 * 60 * 1000L
    ): Boolean {
        if (scheduledAtEpochMillis <= 0L) return false
        return (nowEpochMillis - scheduledAtEpochMillis) > thresholdMillis
    }

    private fun dateToZonedDateTime(date: Date?, targetDate: LocalDate, zoneId: ZoneId): ZonedDateTime {
        val time = dateToLocalTime(date, zoneId)
        return ZonedDateTime.of(targetDate, time, zoneId)
    }

    private fun dateToLocalTime(date: Date?, zoneId: ZoneId): LocalTime {
        if (date == null) return LocalTime.of(12, 0)
        return Instant.ofEpochMilli(date.time).atZone(zoneId).toLocalTime().truncatedTo(ChronoUnit.MINUTES)
    }
}
