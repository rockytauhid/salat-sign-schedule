// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

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
import com.salatsign.tv.data.model.JumuahMode
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
        iqamahOffsets: Map<Prayer, Int> = emptyMap(),
        isMosqueSignage: Boolean = false,
        jumuahMode: JumuahMode = JumuahMode.FIXED,
        jumuahTimes: List<String> = emptyList(),
        is24HourFormat: Boolean = false
    ): PrayerTimesDay {
        val key = DailyPrayerCacheKey(
            date = date,
            config = config,
            hijriOffsetDays = hijriOffsetDays,
            hijriChangeAtMidnight = hijriChangeAtMidnight,
            iqamahOffsets = iqamahOffsets,
            isMosqueSignage = isMosqueSignage,
            jumuahMode = jumuahMode,
            jumuahTimes = jumuahTimes,
            is24HourFormat = is24HourFormat
        )
        val cached = computeCachedDayTimes(key)
        return evaluateForReferenceTime(cached, key, referenceZoned)
    }

    /**
     * Computes the expensive daily astronomical prayer times, tomorrow's Subuh, Jumu'ah sittings,
     * and formatted Hijri/Gregorian dates once per (date + config) key (F10).
     */
    fun computeCachedDayTimes(key: DailyPrayerCacheKey): CachedDayTimes {
        val date = key.date
        val config = key.config
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

        val tomorrow = date.plusDays(1)
        val tomorrowComponents = DateComponents(tomorrow.year, tomorrow.monthValue, tomorrow.dayOfMonth)
        val tomorrowTimes = PrayerTimes(coordinates, tomorrowComponents, parameters)
        val tomorrowSubuh = dateToLocalTime(tomorrowTimes.fajr, config.zoneId)

        val isFriday = (date.dayOfWeek == DayOfWeek.FRIDAY)

        // Jumu'ah resolution for Friday in SalatSign (D16)
        val jumuahSittings: List<LocalTime>
        val effectiveZuhurTime: LocalTime?
        val secondaryJumuahNote: String?

        if (key.isMosqueSignage && isFriday && zuhurTime != null) {
            val zuhurZoned = ZonedDateTime.of(date, zuhurTime, config.zoneId)
            val resolvedZonedSittings = JumuahCalculator.resolveSittings(date, config, key.jumuahMode, key.jumuahTimes, zuhurZoned)
            jumuahSittings = resolvedZonedSittings.map { it.toLocalTime() }
            effectiveZuhurTime = jumuahSittings.first()
            if (key.jumuahTimes.isNotEmpty()) {
                val formattedZuhr = PrayerTimeItem.formatLocalTime(zuhurTime, key.is24HourFormat)
                secondaryJumuahNote = "Zuhr begins $formattedZuhr"
            } else {
                secondaryJumuahNote = null
            }
        } else {
            jumuahSittings = emptyList()
            effectiveZuhurTime = zuhurTime
            secondaryJumuahNote = null
        }

        val maghribZoned = if (magribTime != null) ZonedDateTime.of(date, magribTime, config.zoneId) else null
        val hijriBeforeMaghrib = HijriCalendarEngine.formatHijriDate(
            date = date,
            offsetDays = key.hijriOffsetDays,
            changeAtMidnight = true,
            monthNames = key.hijriMonthNames
        )
        val hijriAfterMaghrib = HijriCalendarEngine.formatHijriDate(
            date = date.plusDays(1),
            offsetDays = key.hijriOffsetDays,
            changeAtMidnight = true,
            monthNames = key.hijriMonthNames
        )
        val gregorianFormatted = HijriCalendarEngine.formatGregorianDate(date)

        return CachedDayTimes(
            date = date,
            isFriday = isFriday,
            subuhTime = subuhTime,
            sunriseTime = sunriseTime,
            effectiveZuhurTime = effectiveZuhurTime,
            ashrTime = ashrTime,
            magribTime = magribTime,
            isyaTime = isyaTime,
            tomorrowSubuh = tomorrowSubuh,
            jumuahSittings = jumuahSittings,
            secondaryJumuahNote = secondaryJumuahNote,
            maghribZoned = maghribZoned,
            hijriBeforeMaghrib = hijriBeforeMaghrib,
            hijriAfterMaghrib = hijriAfterMaghrib,
            gregorianFormatted = gregorianFormatted
        )
    }

    /**
     * Cheap per-second evaluation of next prayer, active prayer, countdown, and Maghrib Hijri rollover
     * using precomputed [CachedDayTimes] (F10).
     */
    fun evaluateForReferenceTime(
        cached: CachedDayTimes,
        key: DailyPrayerCacheKey,
        referenceZoned: ZonedDateTime
    ): PrayerTimesDay {
        val date = cached.date
        val isFriday = cached.isFriday
        val subuhTime = cached.subuhTime
        val effectiveZuhurTime = cached.effectiveZuhurTime
        val ashrTime = cached.ashrTime
        val magribTime = cached.magribTime
        val isyaTime = cached.isyaTime
        val jumuahSittings = cached.jumuahSittings

        val rawList = listOf(
            Prayer.SUBUH to subuhTime,
            Prayer.ZUHUR to effectiveZuhurTime,
            Prayer.ASHR to ashrTime,
            Prayer.MAGRIB to magribTime,
            Prayer.ISYA to isyaTime
        )

        val referenceDate = referenceZoned.toLocalDate()
        val referenceTime = referenceZoned.toLocalTime()
        val isToday = (date == referenceDate)

        var identifiedNextPrayer: Prayer? = null
        var identifiedActivePrayer: Prayer? = null
        var secondsUntilNext: Long = 0

        if (isToday) {
            val nonNullPrayers = rawList.filter { it.second != null }

            // 1. Next Prayer Today (computed via ZonedDateTime durations for DST accuracy, F17)
            if (key.isMosqueSignage && isFriday && jumuahSittings.isNotEmpty()) {
                if (effectiveZuhurTime != null && referenceTime.isBefore(effectiveZuhurTime)) {
                    val upcomingBefore = nonNullPrayers.firstOrNull { (_, time) -> time!!.isAfter(referenceTime) }
                    if (upcomingBefore != null) {
                        identifiedNextPrayer = upcomingBefore.first
                        val targetZoned = ZonedDateTime.of(date, upcomingBefore.second!!, referenceZoned.zone)
                        secondsUntilNext = ChronoUnit.SECONDS.between(referenceZoned, targetZoned).coerceAtLeast(0)
                    }
                } else if (jumuahSittings.size > 1 && referenceTime.isBefore(jumuahSittings.last())) {
                    val nextSitting = jumuahSittings.firstOrNull { it.isAfter(referenceTime) }
                    if (nextSitting != null) {
                        identifiedNextPrayer = Prayer.ZUHUR
                        val targetZoned = ZonedDateTime.of(date, nextSitting, referenceZoned.zone)
                        secondsUntilNext = ChronoUnit.SECONDS.between(referenceZoned, targetZoned).coerceAtLeast(0)
                    }
                } else {
                    val afterJumuahPrayers = listOf(
                        Prayer.ASHR to ashrTime,
                        Prayer.MAGRIB to magribTime,
                        Prayer.ISYA to isyaTime
                    ).filter { it.second != null }

                    val upcomingAfter = afterJumuahPrayers.firstOrNull { (_, time) -> time!!.isAfter(referenceTime) }
                    if (upcomingAfter != null) {
                        identifiedNextPrayer = upcomingAfter.first
                        val targetZoned = ZonedDateTime.of(date, upcomingAfter.second!!, referenceZoned.zone)
                        secondsUntilNext = ChronoUnit.SECONDS.between(referenceZoned, targetZoned).coerceAtLeast(0)
                    } else if (cached.tomorrowSubuh != null) {
                        identifiedNextPrayer = Prayer.SUBUH
                        val tomorrowSubuhZoned = ZonedDateTime.of(date.plusDays(1), cached.tomorrowSubuh, referenceZoned.zone)
                        secondsUntilNext = ChronoUnit.SECONDS.between(referenceZoned, tomorrowSubuhZoned).coerceAtLeast(0)
                    }
                }
            } else {
                val upcoming = nonNullPrayers.firstOrNull { (_, time) -> time!!.isAfter(referenceTime) }
                if (upcoming != null) {
                    identifiedNextPrayer = upcoming.first
                    val targetZoned = ZonedDateTime.of(date, upcoming.second!!, referenceZoned.zone)
                    secondsUntilNext = ChronoUnit.SECONDS.between(referenceZoned, targetZoned).coerceAtLeast(0)
                } else if (cached.tomorrowSubuh != null) {
                    // If all valid prayers today have passed (or after Isya), next is tomorrow's first available prayer (Subuh)
                    identifiedNextPrayer = Prayer.SUBUH
                    val tomorrowSubuhZoned = ZonedDateTime.of(date.plusDays(1), cached.tomorrowSubuh, referenceZoned.zone)
                    secondsUntilNext = ChronoUnit.SECONDS.between(referenceZoned, tomorrowSubuhZoned).coerceAtLeast(0)
                }
            }

            // 2. Active Prayer Semantics (P4.3):
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
                // Fajr (Subuh) ends strictly at Sunrise; between Sunrise and Dhuhr no obligatory prayer is active
                if (identifiedActivePrayer == Prayer.SUBUH &&
                    cached.sunriseTime != null &&
                    !referenceTime.isBefore(cached.sunriseTime)
                ) {
                    identifiedActivePrayer = null
                }
            }
        }

        val prayerItems = rawList.map { (prayer, time) ->
            val isZuhurOnFridayMosque = key.isMosqueSignage && isFriday && (prayer == Prayer.ZUHUR)
            PrayerTimeItem(
                prayer = prayer,
                time = time,
                isNext = isToday && (prayer == identifiedNextPrayer),
                isActive = isToday && (prayer == identifiedActivePrayer),
                isFriday = isFriday,
                iqamahMinutesAfter = if (isZuhurOnFridayMosque) 0 else (key.iqamahOffsets[prayer] ?: 0),
                jumuahSittings = if (isZuhurOnFridayMosque) jumuahSittings else emptyList(),
                secondaryTimeNote = if (isZuhurOnFridayMosque) cached.secondaryJumuahNote else null
            )
        }

        val useAfterMaghrib = !key.hijriChangeAtMidnight &&
            cached.maghribZoned != null &&
            !referenceZoned.isBefore(cached.maghribZoned)
        val hijriFormatted = if (useAfterMaghrib) cached.hijriAfterMaghrib else cached.hijriBeforeMaghrib

        val nextItem = prayerItems.firstOrNull { it.prayer == identifiedNextPrayer }
        val activeItem = prayerItems.firstOrNull { it.prayer == identifiedActivePrayer }

        return PrayerTimesDay(
            date = date,
            hijriDate = hijriFormatted,
            gregorianDate = cached.gregorianFormatted,
            prayers = prayerItems,
            sunrise = cached.sunriseTime,
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
     * of any prayer today. Handles midnight wrap-around for late-night prayers (e.g. Isya at 23:58 -> 00:01) (F17).
     * Used by SalatSign to trigger silent slideshow interruption.
     */
    fun getPrayerAtExactTimeStrike(
        todayPrayerTimes: PrayerTimesDay,
        currentTime: LocalTime = LocalTime.now(),
        windowMinutes: Long = 5,
        isMosqueSignage: Boolean = false
    ): Prayer? {
        val windowSeconds = windowMinutes * 60
        val zuhurItem = todayPrayerTimes.prayers.firstOrNull { it.prayer == Prayer.ZUHUR }
        if (isMosqueSignage && zuhurItem != null && zuhurItem.isFriday && zuhurItem.jumuahSittings.isNotEmpty()) {
            for (sitting in zuhurItem.jumuahSittings) {
                if (isWithinStrikeWindow(sitting, currentTime, windowSeconds)) {
                    return Prayer.ZUHUR
                }
            }
        }
        for (item in todayPrayerTimes.prayers) {
            if (isMosqueSignage && item.prayer == Prayer.ZUHUR && item.isFriday && item.jumuahSittings.isNotEmpty()) {
                continue
            }
            val prayerTime = item.time ?: continue
            if (isWithinStrikeWindow(prayerTime, currentTime, windowSeconds)) {
                return item.prayer
            }
        }
        return null
    }

    /**
     * ZonedDateTime-aware exact strike window check that cleanly handles prayers crossing midnight (F17).
     */
    fun getPrayerAtExactTimeStrike(
        todayPrayerTimes: PrayerTimesDay,
        nowZoned: ZonedDateTime,
        windowMinutes: Long = 5,
        isMosqueSignage: Boolean = false
    ): Prayer? {
        return getPrayerAtExactTimeStrike(
            todayPrayerTimes = todayPrayerTimes,
            currentTime = nowZoned.toLocalTime(),
            windowMinutes = windowMinutes,
            isMosqueSignage = isMosqueSignage
        )
    }

    private fun isWithinStrikeWindow(prayerTime: LocalTime, currentTime: LocalTime, windowSeconds: Long): Boolean {
        val directDiff = ChronoUnit.SECONDS.between(prayerTime, currentTime)
        if (directDiff in 0..windowSeconds) return true
        // Midnight wrap-around: e.g. prayer at 23:58, currentTime at 00:01 -> directDiff = -86220, +86400 = +180s
        val wrappedDiff = directDiff + 86_400L
        return wrappedDiff in 0..windowSeconds
    }

    /**
     * Calculates all valid prayer events for a given calendar date as ZonedDateTime instances.
     * Null times are omitted (P4.2).
     */
    fun getDayPrayerEvents(
        date: LocalDate,
        config: PrayerConfig,
        settings: AppSettings? = null
    ): List<PrayerEvent> {
        val coordinates = Coordinates(config.latitude, config.longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val parameters = getCalculationParameters(config)

        val prayerTimes = PrayerTimes(coordinates, dateComponents, parameters)

        val events = mutableListOf<PrayerEvent>()
        dateToZonedDateTime(prayerTimes.fajr, date, config.zoneId)?.let { events.add(PrayerEvent(Prayer.SUBUH, it)) }

        val zuhurZoned = dateToZonedDateTime(prayerTimes.dhuhr, date, config.zoneId)
        if (zuhurZoned != null) {
            // SalatSchedule always schedules Friday Adhan at calculated Zuhr (D16/D18); SalatSign is strictly silent
            events.add(PrayerEvent(Prayer.ZUHUR, zuhurZoned))
        }

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
        config: PrayerConfig,
        settings: AppSettings? = null
    ): PrayerEvent? {
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)

        val todayEvents = getDayPrayerEvents(today, config, settings)
        val tomorrowEvents = getDayPrayerEvents(tomorrow, config, settings)

        val allEvents = todayEvents + tomorrowEvents
        return allEvents.firstOrNull { it.at.isAfter(now) }
    }

    fun nextPrayerEvent(
        now: ZonedDateTime,
        settings: AppSettings
    ): PrayerEvent? = nextPrayerEvent(now, PrayerConfig.fromAppSettings(settings), settings)

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
        config: PrayerConfig,
        settings: AppSettings? = null
    ): PrayerReminderEvent? {
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)

        val todayEvents = getDayPrayerEvents(today, config, settings)
        val tomorrowEvents = getDayPrayerEvents(tomorrow, config, settings)

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
    ): PrayerReminderEvent? = nextReminderEvent(now, PrayerConfig.fromAppSettings(settings), settings)

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
