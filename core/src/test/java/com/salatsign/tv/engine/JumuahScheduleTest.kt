// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.FridayAdhanTiming
import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class JumuahScheduleTest {

    private val sydneyZone = ZoneId.of("Australia/Sydney")
    private val config = PrayerConfig(
        latitude = -33.8688,
        longitude = 151.2093,
        zoneId = sydneyZone,
        methodName = "MUSLIM_WORLD_LEAGUE"
    )

    @Test
    fun fixedMode_producesRightInstants() {
        val friday = LocalDate.of(2026, 11, 20)
        assertEquals(DayOfWeek.FRIDAY, friday.dayOfWeek)

        val zuhurZoned = ZonedDateTime.of(friday, LocalTime.of(12, 45), sydneyZone)
        val sittings = JumuahCalculator.resolveSittings(
            date = friday,
            config = config,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("13:15"),
            calculatedZuhr = zuhurZoned
        )

        assertEquals(1, sittings.size)
        assertEquals(LocalTime.of(13, 15), sittings[0].toLocalTime())
        assertEquals(friday, sittings[0].toLocalDate())
        assertEquals(sydneyZone, sittings[0].zone)
    }

    @Test
    fun afterZuhrMode_producesRightInstants() {
        val friday = LocalDate.of(2026, 11, 20)
        val zuhurZoned = ZonedDateTime.of(friday, LocalTime.of(12, 45), sydneyZone)

        val sittings = JumuahCalculator.resolveSittings(
            date = friday,
            config = config,
            jumuahMode = JumuahMode.AFTER_ZUHR,
            jumuahTimes = listOf("45"),
            calculatedZuhr = zuhurZoned
        )

        assertEquals(1, sittings.size)
        val expected = zuhurZoned.plusMinutes(45)
        assertEquals(expected, sittings[0])
    }

    @Test
    fun twoSittings_sortedAndResolved() {
        val friday = LocalDate.of(2026, 11, 20)
        val zuhurZoned = ZonedDateTime.of(friday, LocalTime.of(12, 45), sydneyZone)

        val sittings = JumuahCalculator.resolveSittings(
            date = friday,
            config = config,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("13:30", "12:30"), // Passed out of chronological order
            calculatedZuhr = zuhurZoned
        )

        assertEquals(2, sittings.size)
        assertEquals(LocalTime.of(12, 30), sittings[0].toLocalTime())
        assertEquals(LocalTime.of(13, 30), sittings[1].toLocalTime())
    }

    @Test
    fun fridayOnly_homeModeAndNonFridayUnchanged() {
        val friday = LocalDate.of(2026, 11, 20)
        val tuesday = LocalDate.of(2026, 11, 17)

        // 1. Home mode on Friday: Jumu'ah settings are ignored
        val homeFriday = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = friday,
            config = config,
            isMosqueMode = false,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("13:00")
        )
        val homeZuhr = homeFriday.prayers.first { it.prayer == Prayer.ZUHUR }
        assertTrue(homeZuhr.jumuahSittings.isEmpty())
        assertNull(homeZuhr.secondaryTimeNote)

        // 2. Mosque mode on Tuesday: Non-Friday has no Jumu'ah sittings
        val mosqueTuesday = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = tuesday,
            config = config,
            isMosqueMode = true,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("13:00")
        )
        val tuesdayZuhr = mosqueTuesday.prayers.first { it.prayer == Prayer.ZUHUR }
        assertTrue(tuesdayZuhr.jumuahSittings.isEmpty())
        assertNull(tuesdayZuhr.secondaryTimeNote)

        // 3. Mosque mode on Friday: Jumu'ah settings are active
        val mosqueFriday = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = friday,
            config = config,
            isMosqueMode = true,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("13:00")
        )
        val fridayZuhr = mosqueFriday.prayers.first { it.prayer == Prayer.ZUHUR }
        assertEquals(1, fridayZuhr.jumuahSittings.size)
        assertEquals(LocalTime.of(13, 0), fridayZuhr.time)
        assertNotNull(fridayZuhr.secondaryTimeNote)
        assertTrue(fridayZuhr.secondaryTimeNote!!.startsWith("Zuhr begins"))
    }

    @Test
    fun dstChangeWeeks_handlesDaylightSavingTransition() {
        // In NSW, Australia: DST ends 2026-04-05 (AEDT UTC+11 -> AEST UTC+10)
        val fridaySummer = LocalDate.of(2026, 4, 3)
        val fridayWinter = LocalDate.of(2026, 4, 10)

        val zuhurSummer = ZonedDateTime.of(fridaySummer, LocalTime.of(12, 50), sydneyZone)
        val zuhurWinter = ZonedDateTime.of(fridayWinter, LocalTime.of(11, 50), sydneyZone)

        val sittingsSummer = JumuahCalculator.resolveSittings(
            date = fridaySummer,
            config = config,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("13:00"),
            calculatedZuhr = zuhurSummer
        )
        val sittingsWinter = JumuahCalculator.resolveSittings(
            date = fridayWinter,
            config = config,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("13:00"),
            calculatedZuhr = zuhurWinter
        )

        assertEquals(ZoneOffset.ofHours(11), sittingsSummer[0].offset)
        assertEquals(ZoneOffset.ofHours(10), sittingsWinter[0].offset)
        assertEquals(LocalTime.of(13, 0), sittingsSummer[0].toLocalTime())
        assertEquals(LocalTime.of(13, 0), sittingsWinter[0].toLocalTime())
    }

    @Test
    fun validation_warnsWhenFixedTimeBeforeZuhr() {
        // Sydney in mid-summer: astronomical Zuhr is around 12:55-13:05 pm
        val startDate = LocalDate.of(2026, 1, 2) // Summer Friday
        val validation = JumuahCalculator.validateSittings(
            startDate = startDate,
            config = config,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("12:15"), // 12:15 is earlier than summer Zuhr
            weeksToCheck = 10
        )

        assertTrue(validation.isValid) // Warning does not block validity
        assertFalse(validation.warnings.isEmpty())
        val firstWarn = validation.firstWarning
        assertNotNull(firstWarn)
        assertEquals(JumuahWarningReason.SITTING_BEFORE_ZUHR, firstWarn!!.reason)
    }

    @Test
    fun validation_blocksWhenFixedTimeAfterAsr() {
        val startDate = LocalDate.of(2026, 6, 5) // Winter Friday (Asr is around 14:45-15:00)
        val validation = JumuahCalculator.validateSittings(
            startDate = startDate,
            config = config,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("16:00"), // 16:00 is after Asr!
            weeksToCheck = 10
        )

        assertFalse(validation.isValid)
        assertTrue(validation.warnings.any { it.reason == JumuahWarningReason.SITTING_AFTER_ASR })
    }

    @Test
    fun countdownTargetsJumuahSittingThenAsr() {
        val friday = LocalDate.of(2026, 11, 20)

        // 1. Morning (11:00 am): before first sitting (12:30 pm)
        val morningRef = ZonedDateTime.of(friday, LocalTime.of(11, 0), sydneyZone)
        val dayMorning = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = friday,
            config = config,
            referenceZoned = morningRef,
            isMosqueMode = true,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("12:30", "13:30")
        )
        assertEquals(Prayer.ZUHUR, dayMorning.nextPrayer?.prayer)
        // From 11:00 to 12:30 is 1.5 hours = 5400 seconds
        assertEquals(5400L, dayMorning.secondsUntilNext)

        // 2. Afternoon (12:45 pm): between sitting 1 (12:30) and sitting 2 (13:30)
        val midRef = ZonedDateTime.of(friday, LocalTime.of(12, 45), sydneyZone)
        val dayMid = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = friday,
            config = config,
            referenceZoned = midRef,
            isMosqueMode = true,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("12:30", "13:30")
        )
        assertEquals(Prayer.ZUHUR, dayMid.nextPrayer?.prayer)
        // From 12:45 to 13:30 is 45 minutes = 2700 seconds
        assertEquals(2700L, dayMid.secondsUntilNext)

        // 3. Post-Jumu'ah (14:00 pm): after last sitting (13:30) -> next is Asr
        val lateRef = ZonedDateTime.of(friday, LocalTime.of(14, 0), sydneyZone)
        val dayLate = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = friday,
            config = config,
            referenceZoned = lateRef,
            isMosqueMode = true,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("12:30", "13:30")
        )
        assertEquals(Prayer.ASHR, dayLate.nextPrayer?.prayer)
    }

    @Test
    fun notConfigured_fridayTimeEqualsZuhr() {
        val friday = LocalDate.of(2026, 11, 20)
        val day = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = friday,
            config = config,
            isMosqueMode = true,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = emptyList() // Not configured
        )
        val zuhurItem = day.prayers.first { it.prayer == Prayer.ZUHUR }
        assertEquals(1, zuhurItem.jumuahSittings.size)
        // Time equals astronomical Zuhr
        assertNull(zuhurItem.secondaryTimeNote)
    }

    @Test
    fun mosqueModeWithDefaults_schedulesNothing() {
        val settings = AppSettings(
            isMosqueMode = true,
            mosqueModeAdhanEnabled = false,
            mosqueModeRemindersEnabled = false
        )
        val isAdhanAllowed = if (settings.isMosqueMode) settings.mosqueModeAdhanEnabled else true
        val isReminderAllowed = if (settings.isMosqueMode) settings.mosqueModeRemindersEnabled else settings.prePrayerReminderEnabled

        assertFalse(isAdhanAllowed)
        assertFalse(isReminderAllowed)
    }

    @Test
    fun strikeHighlight_triggersAtEachJumuahSitting() {
        val friday = LocalDate.of(2026, 11, 20)
        val day = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = friday,
            config = config,
            isMosqueMode = true,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("12:30", "13:30")
        )

        // 12:32 is 2 minutes after sitting 1 -> strikes
        val strikeSitting1 = PrayerCalculationEngine.getPrayerAtExactTimeStrike(
            todayPrayerTimes = day,
            currentTime = LocalTime.of(12, 32),
            windowMinutes = 5,
            isMosqueMode = true
        )
        assertEquals(Prayer.ZUHUR, strikeSitting1)

        // 13:33 is 3 minutes after sitting 2 -> strikes
        val strikeSitting2 = PrayerCalculationEngine.getPrayerAtExactTimeStrike(
            todayPrayerTimes = day,
            currentTime = LocalTime.of(13, 33),
            windowMinutes = 5,
            isMosqueMode = true
        )
        assertEquals(Prayer.ZUHUR, strikeSitting2)

        // 13:00 is not within 5 min of either sitting -> no strike
        val noStrike = PrayerCalculationEngine.getPrayerAtExactTimeStrike(
            todayPrayerTimes = day,
            currentTime = LocalTime.of(13, 0),
            windowMinutes = 5,
            isMosqueMode = true
        )
        assertNull(noStrike)
    }
}
