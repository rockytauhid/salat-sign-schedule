// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.mosque

import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerConfig
import com.salatsign.tv.engine.PrayerCalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class MosqueSignageBehaviorTest {

    private val canberraZone = ZoneId.of("Australia/Sydney")
    private val canberraConfig = PrayerConfig(
        latitude = -35.2809,
        longitude = 149.1300,
        zoneId = canberraZone,
        methodName = "MUSLIM_WORLD_LEAGUE",
        madhab = "SHAFI"
    )

    @Test
    fun evaluateStrike_returnsActivePrayerWithin5MinuteWindow() {
        val date = LocalDate.of(2026, 11, 18) // Wednesday
        val nowZoned = ZonedDateTime.of(date, LocalTime.of(12, 0), canberraZone)
        val day = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = date,
            config = canberraConfig,
            referenceZoned = nowZoned,
            isMosqueSignage = true
        )
        val magribTime = day.prayers.first { it.prayer == Prayer.MAGRIB }.time!!

        assertEquals(
            Prayer.MAGRIB,
            MosqueSlideshowViewModel.evaluateStrike(day, magribTime, showStrikeHighlight = true)
        )
        assertEquals(
            Prayer.MAGRIB,
            MosqueSlideshowViewModel.evaluateStrike(day, magribTime.plusMinutes(4), showStrikeHighlight = true)
        )
        assertNull(
            MosqueSlideshowViewModel.evaluateStrike(day, magribTime.plusMinutes(6), showStrikeHighlight = true)
        )
    }

    @Test
    fun evaluateStrike_returnsNullWhenHighlightDisabled() {
        val date = LocalDate.of(2026, 11, 18)
        val nowZoned = ZonedDateTime.of(date, LocalTime.of(12, 0), canberraZone)
        val day = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = date,
            config = canberraConfig,
            referenceZoned = nowZoned,
            isMosqueSignage = true
        )
        val subuhTime = day.prayers.first { it.prayer == Prayer.SUBUH }.time!!

        assertNull(
            MosqueSlideshowViewModel.evaluateStrike(day, subuhTime, showStrikeHighlight = false)
        )
    }

    @Test
    fun evaluateStrike_onFridayTriggersOnConfiguredJumuahSitting() {
        val friday = LocalDate.of(2026, 11, 20)
        assertEquals(DayOfWeek.FRIDAY, friday.dayOfWeek)

        val nowZoned = ZonedDateTime.of(friday, LocalTime.of(11, 0), canberraZone)
        val day = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = friday,
            config = canberraConfig,
            referenceZoned = nowZoned,
            isMosqueSignage = true,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("13:15", "14:00")
        )

        assertEquals(
            Prayer.ZUHUR,
            MosqueSlideshowViewModel.evaluateStrike(day, LocalTime.of(13, 15), showStrikeHighlight = true)
        )
        assertEquals(
            Prayer.ZUHUR,
            MosqueSlideshowViewModel.evaluateStrike(day, LocalTime.of(14, 2), showStrikeHighlight = true)
        )
        assertNull(
            MosqueSlideshowViewModel.evaluateStrike(day, LocalTime.of(13, 30), showStrikeHighlight = true)
        )
    }

    @Test
    fun applyActivePrayerOverride_marksOnlyInterruptedPrayerActive() {
        val date = LocalDate.of(2026, 11, 18)
        val nowZoned = ZonedDateTime.of(date, LocalTime.of(10, 0), canberraZone)
        val day = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = date,
            config = canberraConfig,
            referenceZoned = nowZoned,
            isMosqueSignage = true
        )

        val overridden = MosqueSlideshowViewModel.applyActivePrayerOverride(day, Prayer.ASHR)
        assertTrue(overridden.first { it.prayer == Prayer.ASHR }.isActive)
        assertTrue(overridden.filter { it.prayer != Prayer.ASHR }.none { it.isActive })

        val unchanged = MosqueSlideshowViewModel.applyActivePrayerOverride(day, null)
        assertEquals(day.prayers, unchanged)
        assertFalse(overridden == day.prayers && !day.prayers.first { it.prayer == Prayer.ASHR }.isActive)
    }
}
