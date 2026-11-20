// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PrayerDayCacheTest {

    private val sydneyZone = ZoneId.of("Australia/Sydney")
    private val canberraConfig = PrayerConfig(
        latitude = -35.2809,
        longitude = 149.1300,
        zoneId = sydneyZone,
        methodName = "MUSLIM_WORLD_LEAGUE"
    )

    @Test
    fun getOrCompute_reusesCachedDayAcrossSeconds_andUpdatesCountdown() {
        val cache = PrayerDayCache()
        val date = LocalDate.of(2026, 6, 15)
        val key = DailyPrayerCacheKey(date = date, config = canberraConfig)

        val t0 = ZonedDateTime.of(date, LocalTime.of(10, 0, 0), sydneyZone)
        val t1 = ZonedDateTime.of(date, LocalTime.of(10, 0, 1), sydneyZone)
        val t2 = ZonedDateTime.of(date, LocalTime.of(10, 0, 5), sydneyZone)

        val day0 = cache.getOrCompute(key, t0)
        val day1 = cache.getOrCompute(key, t1)
        val day2 = cache.getOrCompute(key, t2)

        assertEquals("Should only compute astronomical times once for the same day/config", 1, cache.recomputeCount)
        assertEquals(day0.secondsUntilNext - 1, day1.secondsUntilNext)
        assertEquals(day0.secondsUntilNext - 5, day2.secondsUntilNext)

        // Verify parity with uncached direct call
        val direct = PrayerCalculationEngine.calculatePrayerTimesDay(date = date, config = canberraConfig, referenceZoned = t2)
        assertEquals(direct, day2)
    }

    @Test
    fun getOrCompute_recomputesWhenDateOrConfigChanges() {
        val cache = PrayerDayCache()
        val day1 = LocalDate.of(2026, 6, 15)
        val day2 = LocalDate.of(2026, 6, 16)

        val key1 = DailyPrayerCacheKey(date = day1, config = canberraConfig)
        cache.getOrCompute(key1, ZonedDateTime.of(day1, LocalTime.of(12, 0), sydneyZone))
        assertEquals(1, cache.recomputeCount)

        // Date changes
        val key2 = DailyPrayerCacheKey(date = day2, config = canberraConfig)
        cache.getOrCompute(key2, ZonedDateTime.of(day2, LocalTime.of(0, 0, 1), sydneyZone))
        assertEquals(2, cache.recomputeCount)

        // Config changes (offset)
        val key3 = DailyPrayerCacheKey(
            date = day2,
            config = canberraConfig.copy(offsetsMinutes = mapOf(Prayer.ZUHUR to 5))
        )
        cache.getOrCompute(key3, ZonedDateTime.of(day2, LocalTime.of(0, 0, 2), sydneyZone))
        assertEquals(3, cache.recomputeCount)
    }

    @Test
    fun getOrCompute_handlesMaghribHijriRolloverWithoutRecomputingAstronomy() {
        val cache = PrayerDayCache()
        val date = LocalDate.of(2026, 6, 15)
        val key = DailyPrayerCacheKey(
            date = date,
            config = canberraConfig,
            hijriChangeAtMidnight = false
        )

        val beforeMaghrib = cache.getOrCompute(key, ZonedDateTime.of(date, LocalTime.of(15, 0), sydneyZone))
        val afterMaghrib = cache.getOrCompute(key, ZonedDateTime.of(date, LocalTime.of(19, 0), sydneyZone))

        assertEquals("Maghrib rollover should not require recomputing astronomical times", 1, cache.recomputeCount)
        assertNotEquals("Hijri date must advance after Maghrib", beforeMaghrib.hijriDate, afterMaghrib.hijriDate)
    }
}
