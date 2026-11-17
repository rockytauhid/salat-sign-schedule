package com.salatsign.tv.engine

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerAdjustments
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerConfig
import com.salatsign.tv.data.model.PrayerEvent
import com.salatsign.tv.data.model.PrayerReminderEvent
import com.salatsign.tv.data.model.PrayerTimeItem
import com.salatsign.tv.data.model.PrayerTimesDay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date

object PrayerCalculationEngine {

    fun getCalculationParameters(
        methodName: String,
        madhab: String = "SHAFI",
        highLatitudeRule: String? = null,
        offsetsMinutes: Map<Prayer, Int> = emptyMap(),
        sunriseOffsetMinutes: Int = 0
    ): CalculationParameters {
        val method = runCatching {
            CalculationMethod.valueOf(methodName.uppercase())
        }.getOrDefault(CalculationMethod.SINGAPORE)
        val params = method.parameters

        params.madhab = when (madhab.uppercase()) {
            "HANAFI" -> Madhab.HANAFI
            else -> Madhab.SHAFI
        }

        when (highLatitudeRule?.uppercase()) {
            "MIDDLE_OF_THE_NIGHT" -> params.highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT
            "SEVENTH_OF_THE_NIGHT" -> params.highLatitudeRule = HighLatitudeRule.SEVENTH_OF_THE_NIGHT
            "TWILIGHT_ANGLE" -> params.highLatitudeRule = HighLatitudeRule.TWILIGHT_ANGLE
            else -> { /* retain default HighLatitudeRule from calculation parameters */ }
        }

        // Apply per-prayer offsets (−30..+30 mins)
        val fajrAdj = offsetsMinutes[Prayer.SUBUH] ?: 0
        val sunriseAdj = sunriseOffsetMinutes
        val dhuhrAdj = offsetsMinutes[Prayer.ZUHUR] ?: 0
        val asrAdj = offsetsMinutes[Prayer.ASHR] ?: 0
        val maghribAdj = offsetsMinutes[Prayer.MAGRIB] ?: 0
        val ishaAdj = offsetsMinutes[Prayer.ISYA] ?: 0

        params.adjustments = PrayerAdjustments(
            fajrAdj.coerceIn(-30, 30),
            sunriseAdj.coerceIn(-30, 30),
            dhuhrAdj.coerceIn(-30, 30),
            asrAdj.coerceIn(-30, 30),
            maghribAdj.coerceIn(-30, 30),
            ishaAdj.coerceIn(-30, 30)
        )

        return params
    }

    fun getCalculationParameters(config: PrayerConfig): CalculationParameters {
        return getCalculationParameters(
            methodName = config.methodName,
            madhab = config.madhab,
            highLatitudeRule = config.highLatitudeRule,
            offsetsMinutes = config.offsetsMinutes,
            sunriseOffsetMinutes = config.sunriseOffsetMinutes
        )
    }

    /**
     * Offline calculation of Islamic prayer times for a specified date and configuration.
     */
    fun calculatePrayerTimesDay(
        date: LocalDate,
        config: PrayerConfig,
        referenceZoned: ZonedDateTime = ZonedDateTime.now(config.zoneId),
        hijriOffsetDays: Int = 0,
        hijriChangeAtMidnight: Boolean = false,
        iqamahOffsets: Map<Prayer, Int> = emptyMap()
    ): PrayerTimesDay {
        val coordinates = Coordinates(config.latitude, config.longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val parameters = getCalculationParameters(config)

        val prayerTimes = PrayerTimes(coordinates, dateComponents, parameters)

        val subuhTime = dateToLocalTime(prayerTimes.fajr, config.zoneId)
        val sunriseTime = dateToLocalTime(prayerTimes.sunrise, config.zoneId)
        val zuhurTime = dateToLocalTime(prayerTimes.dhuhr, config.zoneId)
        val ashrTime = dateToLocalTime(prayerTimes.asr, config.zoneId)
        val magribTime = dateToLocalTime(prayerTimes.maghrib, config.zoneId)
        val isyaTime = dateToLocalTime(prayerTimes.isha, config.zoneId)

        val rawList = listOf(
            Prayer.SUBUH to subuhTime,
            Prayer.ZUHUR to zuhurTime,
            Prayer.ASHR to ashrTime,
            Prayer.MAGRIB to magribTime,
            Prayer.ISYA to isyaTime
        )

        val isFriday = (date.dayOfWeek == DayOfWeek.FRIDAY)
        val referenceDate = referenceZoned.toLocalDate()
        val referenceTime = referenceZoned.toLocalTime()
        val isToday = (date == referenceDate)

        var identifiedNextPrayer: Prayer? = null
        var identifiedActivePrayer: Prayer? = null
        var secondsUntilNext: Long = 0

        if (isToday) {
            val nonNullPrayers = rawList.filter { it.second != null }

            // 1. Next Prayer Today
            val upcoming = nonNullPrayers.firstOrNull { (_, time) -> time!!.isAfter(referenceTime) }
            if (upcoming != null) {
                identifiedNextPrayer = upcoming.first
                secondsUntilNext = ChronoUnit.SECONDS.between(referenceTime, upcoming.second).coerceAtLeast(0)
            } else {
                // If all valid prayers today have passed (or after Isya), next is tomorrow's first available prayer (Subuh)
                val tomorrow = date.plusDays(1)
                val tomorrowComponents = DateComponents(tomorrow.year, tomorrow.monthValue, tomorrow.dayOfMonth)
                val tomorrowTimes = PrayerTimes(coordinates, tomorrowComponents, parameters)
                val tomorrowSubuh = dateToLocalTime(tomorrowTimes.fajr, config.zoneId)
                if (tomorrowSubuh != null) {
                    identifiedNextPrayer = Prayer.SUBUH
                    val secondsUntilMidnight = ChronoUnit.SECONDS.between(referenceTime, LocalTime.MAX) + 1
                    val secondsFromMidnight = tomorrowSubuh.toSecondOfDay().toLong()
                    secondsUntilNext = (secondsUntilMidnight + secondsFromMidnight).coerceAtLeast(0)
                }
            }

            // 2. Active Prayer Semantics (P4.3):
            // A prayer is active from its time until the next prayer's time.
            // Isya stays active until Subuh, including after midnight.
            if (subuhTime != null && referenceTime.isBefore(subuhTime)) {
                // Between midnight and Subuh: yesterday's Isya is active
                identifiedActivePrayer = Prayer.ISYA
            } else if (isyaTime != null && !referenceTime.isBefore(isyaTime)) {
                // From Isya until midnight: Isya is active
                identifiedActivePrayer = Prayer.ISYA
            } else {
                // Daytime prayers: find the active prayer whose time <= referenceTime
                for (i in 0 until nonNullPrayers.lastIndex) {
                    val current = nonNullPrayers[i]
                    val next = nonNullPrayers[i + 1]
                    if (!referenceTime.isBefore(current.second) && referenceTime.isBefore(next.second)) {
                        identifiedActivePrayer = current.first
                        break
                    }
                }
            }
        }

        val prayerItems = rawList.map { (prayer, time) ->
            PrayerTimeItem(
                prayer = prayer,
                time = time,
                isNext = isToday && (prayer == identifiedNextPrayer),
                isActive = isToday && (prayer == identifiedActivePrayer),
                isFriday = isFriday,
                iqamahMinutesAfter = iqamahOffsets[prayer] ?: 0
            )
        }

        val maghribZoned = if (magribTime != null) ZonedDateTime.of(date, magribTime, config.zoneId) else null
        val hijriFormatted = HijriCalendarEngine.formatHijriDate(
            date = date,
            offsetDays = hijriOffsetDays,
            now = referenceZoned,
            maghribTime = maghribZoned,
            changeAtMidnight = hijriChangeAtMidnight
        )
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
     * Backward-compatible overload for calculatePrayerTimesDay
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
        val config = PrayerConfig(
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            methodName = calculationMethodName
        )
        val referenceZoned = ZonedDateTime.of(referenceDate, referenceTime, zoneId)
        return calculatePrayerTimesDay(
            date = date,
            config = config,
            referenceZoned = referenceZoned,
            hijriOffsetDays = hijriOffsetDays
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
            val prayerTime = item.time ?: continue
            val diffSeconds = ChronoUnit.SECONDS.between(prayerTime, currentTime)
            if (diffSeconds in 0..(windowMinutes * 60)) {
                return item.prayer
            }
        }
        return null
    }

    /**
     * Calculates all valid prayer events for a given calendar date as ZonedDateTime instances.
     * Null times are omitted (P4.2).
     */
    fun getDayPrayerEvents(
        date: LocalDate,
        config: PrayerConfig
    ): List<PrayerEvent> {
        val coordinates = Coordinates(config.latitude, config.longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val parameters = getCalculationParameters(config)

        val prayerTimes = PrayerTimes(coordinates, dateComponents, parameters)

        val events = mutableListOf<PrayerEvent>()
        dateToZonedDateTime(prayerTimes.fajr, date, config.zoneId)?.let { events.add(PrayerEvent(Prayer.SUBUH, it)) }
        dateToZonedDateTime(prayerTimes.dhuhr, date, config.zoneId)?.let { events.add(PrayerEvent(Prayer.ZUHUR, it)) }
        dateToZonedDateTime(prayerTimes.asr, date, config.zoneId)?.let { events.add(PrayerEvent(Prayer.ASHR, it)) }
        dateToZonedDateTime(prayerTimes.maghrib, date, config.zoneId)?.let { events.add(PrayerEvent(Prayer.MAGRIB, it)) }
        dateToZonedDateTime(prayerTimes.isha, date, config.zoneId)?.let { events.add(PrayerEvent(Prayer.ISYA, it)) }
        return events
    }

    fun getDayPrayerEvents(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        calculationMethodName: String = "SINGAPORE",
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<PrayerEvent> {
        val config = PrayerConfig(
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            methodName = calculationMethodName
        )
        return getDayPrayerEvents(date, config)
    }

    /**
     * Determines the earliest prayer event strictly after [now], evaluating both today's
     * and tomorrow's prayer times so transitions across midnight/after Isya are seamless.
     */
    fun nextPrayerEvent(
        now: ZonedDateTime,
        config: PrayerConfig
    ): PrayerEvent? {
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)

        val todayEvents = getDayPrayerEvents(today, config)
        val tomorrowEvents = getDayPrayerEvents(tomorrow, config)

        val allEvents = todayEvents + tomorrowEvents
        return allEvents.firstOrNull { it.at.isAfter(now) }
    }

    fun nextPrayerEvent(
        now: ZonedDateTime,
        settings: AppSettings
    ): PrayerEvent? = nextPrayerEvent(now, PrayerConfig.fromAppSettings(settings))

    fun nextPrayerEvent(
        now: ZonedDateTime,
        latitude: Double,
        longitude: Double,
        calculationMethodName: String = "SINGAPORE"
    ): PrayerEvent? {
        val config = PrayerConfig(
            latitude = latitude,
            longitude = longitude,
            zoneId = now.zone,
            methodName = calculationMethodName
        )
        return nextPrayerEvent(now, config)
    }

    /**
     * Determines the earliest 15-minute pre-prayer reminder strictly after [now].
     */
    fun nextReminderEvent(
        now: ZonedDateTime,
        config: PrayerConfig
    ): PrayerReminderEvent? {
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)

        val todayEvents = getDayPrayerEvents(today, config)
        val tomorrowEvents = getDayPrayerEvents(tomorrow, config)

        val allReminders = (todayEvents + tomorrowEvents).map {
            PrayerReminderEvent(
                prayer = it.prayer,
                prayerAt = it.at,
                reminderAt = it.at.minusMinutes(15)
            )
        }

        return allReminders.firstOrNull { it.reminderAt.isAfter(now) }
    }

    fun nextReminderEvent(
        now: ZonedDateTime,
        settings: AppSettings
    ): PrayerReminderEvent? = nextReminderEvent(now, PrayerConfig.fromAppSettings(settings))

    fun nextReminderEvent(
        now: ZonedDateTime,
        latitude: Double,
        longitude: Double,
        calculationMethodName: String = "SINGAPORE"
    ): PrayerReminderEvent? {
        val config = PrayerConfig(
            latitude = latitude,
            longitude = longitude,
            zoneId = now.zone,
            methodName = calculationMethodName
        )
        return nextReminderEvent(now, config)
    }

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

    private fun dateToZonedDateTime(date: Date?, targetDate: LocalDate, zoneId: ZoneId): ZonedDateTime? {
        val time = dateToLocalTime(date, zoneId) ?: return null
        return ZonedDateTime.of(targetDate, time, zoneId)
    }

    private fun dateToLocalTime(date: Date?, zoneId: ZoneId): LocalTime? {
        if (date == null) return null
        return Instant.ofEpochMilli(date.time).atZone(zoneId).toLocalTime().truncatedTo(ChronoUnit.MINUTES)
    }
}
