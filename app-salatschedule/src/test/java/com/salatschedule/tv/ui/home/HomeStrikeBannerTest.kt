// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.ui.home

import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerConfig
import com.salatsign.tv.engine.PrayerCalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HomeStrikeBannerTest {

    private val zone = ZoneId.of("Australia/Brisbane")
    private val date = LocalDate.of(2026, 11, 26)
    private val config = PrayerConfig(
        latitude = -27.4698,
        longitude = 153.0251,
        zoneId = zone,
        methodName = "MUSLIM_WORLD_LEAGUE"
    )

    @Test
    fun `strike banner activates within 5 minutes when enabled and viewing today`() {
        val ref = ZonedDateTime.of(date, java.time.LocalTime.of(12, 0), zone)
        val day = PrayerCalculationEngine.calculatePrayerTimesDay(date = date, config = config, referenceZoned = ref)
        val dhuhrTime = day.prayers.first { it.prayer == Prayer.ZUHUR }.time!!

        val result = HomeViewModel.resolveActiveStrikePrayer(
            prayerTimes = day,
            nowTime = dhuhrTime.plusMinutes(2),
            isToday = true,
            showStrikeHighlight = true
        )
        assertEquals(Prayer.ZUHUR, result)
    }

    @Test
    fun `strike banner is suppressed when showStrikeHighlight is disabled`() {
        val ref = ZonedDateTime.of(date, java.time.LocalTime.of(12, 0), zone)
        val day = PrayerCalculationEngine.calculatePrayerTimesDay(date = date, config = config, referenceZoned = ref)
        val dhuhrTime = day.prayers.first { it.prayer == Prayer.ZUHUR }.time!!

        val result = HomeViewModel.resolveActiveStrikePrayer(
            prayerTimes = day,
            nowTime = dhuhrTime.plusMinutes(2),
            isToday = true,
            showStrikeHighlight = false
        )
        assertNull(result)
    }

    @Test
    fun `strike banner is suppressed when viewing a non-today date`() {
        val ref = ZonedDateTime.of(date, java.time.LocalTime.of(12, 0), zone)
        val day = PrayerCalculationEngine.calculatePrayerTimesDay(date = date, config = config, referenceZoned = ref)
        val dhuhrTime = day.prayers.first { it.prayer == Prayer.ZUHUR }.time!!

        val result = HomeViewModel.resolveActiveStrikePrayer(
            prayerTimes = day,
            nowTime = dhuhrTime.plusMinutes(2),
            isToday = false,
            showStrikeHighlight = true
        )
        assertNull(result)
    }
}
