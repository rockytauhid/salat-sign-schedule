// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerTimeItem
import com.salatsign.tv.data.model.PrayerTimesDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

class ScreensaverRotationEngineTest {

    @Test
    fun buildActiveSlides_allEnabledWithData_rotatesScheduleCalendarQuran() {
        val slides = ScreensaverRotationEngine.buildActiveSlides(
            scheduleEnabled = true,
            calendarEnabled = true,
            quranEnabled = true,
            hasCalendarEvents = true,
            hasQuranData = true
        )
        assertEquals(
            listOf(ScreensaverSlideKind.SCHEDULE, ScreensaverSlideKind.CALENDAR, ScreensaverSlideKind.QURAN),
            slides
        )
    }

    @Test
    fun buildActiveSlides_skipsCalendarWhenNoEvents() {
        val slides = ScreensaverRotationEngine.buildActiveSlides(
            scheduleEnabled = true,
            calendarEnabled = true,
            quranEnabled = true,
            hasCalendarEvents = false,
            hasQuranData = true
        )
        assertEquals(
            listOf(ScreensaverSlideKind.SCHEDULE, ScreensaverSlideKind.QURAN),
            slides
        )
    }

    @Test
    fun buildActiveSlides_skipsQuranWhenDataMissing() {
        val slides = ScreensaverRotationEngine.buildActiveSlides(
            scheduleEnabled = true,
            calendarEnabled = true,
            quranEnabled = true,
            hasCalendarEvents = true,
            hasQuranData = false
        )
        assertEquals(
            listOf(ScreensaverSlideKind.SCHEDULE, ScreensaverSlideKind.CALENDAR),
            slides
        )
    }

    @Test
    fun buildActiveSlides_allDisabledOrUnavailable_fallsBackToSchedule() {
        assertEquals(
            listOf(ScreensaverSlideKind.SCHEDULE),
            ScreensaverRotationEngine.buildActiveSlides(
                scheduleEnabled = false,
                calendarEnabled = false,
                quranEnabled = false,
                hasCalendarEvents = true,
                hasQuranData = true
            )
        )
        assertEquals(
            listOf(ScreensaverSlideKind.SCHEDULE),
            ScreensaverRotationEngine.buildActiveSlides(
                scheduleEnabled = false,
                calendarEnabled = true,
                quranEnabled = true,
                hasCalendarEvents = false,
                hasQuranData = false
            )
        )
    }

    @Test
    fun canDisableSlide_preventsDisablingLastEnabledSlide() {
        assertTrue(
            ScreensaverRotationEngine.canDisableSlide(
                ScreensaverSlideKind.SCHEDULE,
                scheduleEnabled = true,
                calendarEnabled = true,
                quranEnabled = false
            )
        )
        assertFalse(
            ScreensaverRotationEngine.canDisableSlide(
                ScreensaverSlideKind.SCHEDULE,
                scheduleEnabled = true,
                calendarEnabled = false,
                quranEnabled = false
            )
        )
    }

    @Test
    fun clampDuration_enforces10To60Seconds() {
        assertEquals(10, ScreensaverDefaults.clampDuration(1))
        assertEquals(10, ScreensaverDefaults.clampDuration(10))
        assertEquals(20, ScreensaverDefaults.clampDuration(20))
        assertEquals(60, ScreensaverDefaults.clampDuration(60))
        assertEquals(60, ScreensaverDefaults.clampDuration(120))
    }

    @Test
    fun burnInOffsetPx_alwaysWithin8PxBounds() {
        for (minute in 0L..50L) {
            val (dx, dy) = ScreensaverRotationEngine.burnInOffsetPx(minute)
            assertTrue(abs(dx) <= ScreensaverDefaults.BURN_IN_MAX_SHIFT_PX)
            assertTrue(abs(dy) <= ScreensaverDefaults.BURN_IN_MAX_SHIFT_PX)
        }
    }

    @Test
    fun minutesUntilNextPrayer_handlesSameDayAndPastMidnight() {
        val asr = PrayerTimeItem(Prayer.ASHR, LocalTime.of(15, 30))
        assertEquals(72L, ScreensaverRotationEngine.minutesUntilNextPrayer(LocalTime.of(14, 18), asr))

        val fajr = PrayerTimeItem(Prayer.SUBUH, LocalTime.of(5, 0))
        assertEquals(360L, ScreensaverRotationEngine.minutesUntilNextPrayer(LocalTime.of(23, 0), fajr))
    }

    @Test
    fun normalizeHomePrayers_clearsFridayJumuahFlag() {
        val friday = PrayerTimesDay(
            date = LocalDate.of(2026, 11, 20),
            gregorianDate = "Fri, 20 Nov 2026",
            hijriDate = "10 Jumada al-Thani 1448",
            prayers = listOf(
                PrayerTimeItem(Prayer.ZUHUR, LocalTime.of(12, 30), isFriday = true)
            ),
            sunrise = LocalTime.of(6, 0),
            nextPrayer = null,
            activePrayer = null,
            secondsUntilNext = 3600L
        )
        val normalized = ScreensaverRotationEngine.normalizeHomePrayers(friday)
        assertFalse(normalized.first().isFriday)
    }
}
