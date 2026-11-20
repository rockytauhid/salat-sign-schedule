// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerConfig
import com.salatsign.tv.data.model.PrayerTimesDay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Cache key for daily astronomical prayer time & Hijri date computation (F10).
 */
data class DailyPrayerCacheKey(
    val date: LocalDate,
    val config: PrayerConfig,
    val hijriOffsetDays: Int = 0,
    val hijriChangeAtMidnight: Boolean = false,
    val iqamahOffsets: Map<Prayer, Int> = emptyMap(),
    val isMosqueMode: Boolean = false,
    val jumuahMode: JumuahMode = JumuahMode.FIXED,
    val jumuahTimes: List<String> = emptyList(),
    val is24HourFormat: Boolean = false,
    val hijriMonthNames: List<String> = emptyList()
)

/**
 * Precomputed astronomical prayer times and formatted dates for a single day.
 * Reused across 1-second UI ticks so only the cheap countdown/next-prayer evaluation runs per second.
 */
data class CachedDayTimes(
    val date: LocalDate,
    val isFriday: Boolean,
    val subuhTime: LocalTime?,
    val sunriseTime: LocalTime?,
    val effectiveZuhurTime: LocalTime?,
    val ashrTime: LocalTime?,
    val magribTime: LocalTime?,
    val isyaTime: LocalTime?,
    val tomorrowSubuh: LocalTime?,
    val jumuahSittings: List<LocalTime>,
    val secondaryJumuahNote: String?,
    val maghribZoned: ZonedDateTime?,
    val hijriBeforeMaghrib: String,
    val hijriAfterMaghrib: String,
    val gregorianFormatted: String
)

/**
 * Single-entry cache keyed by (date + configuration) to avoid recomputing
 * full astronomical PrayerTimes and HijrahDate every second on low-power TV hardware (F10).
 */
class PrayerDayCache {

    private val lock = Any()
    private var cachedKey: DailyPrayerCacheKey? = null
    private var cachedDay: CachedDayTimes? = null

    /**
     * Counter of full astronomical recomputations performed (useful for unit testing and diagnostics).
     */
    var recomputeCount: Int = 0
        private set

    fun getOrCompute(
        key: DailyPrayerCacheKey,
        referenceZoned: ZonedDateTime
    ): PrayerTimesDay {
        val dayTimes = synchronized(lock) {
            val existingKey = cachedKey
            val existingDay = cachedDay
            if (existingKey == key && existingDay != null) {
                existingDay
            } else {
                val computed = PrayerCalculationEngine.computeCachedDayTimes(key)
                cachedKey = key
                cachedDay = computed
                recomputeCount++
                computed
            }
        }
        return PrayerCalculationEngine.evaluateForReferenceTime(dayTimes, key, referenceZoned)
    }

    fun clear() {
        synchronized(lock) {
            cachedKey = null
            cachedDay = null
        }
    }
}
