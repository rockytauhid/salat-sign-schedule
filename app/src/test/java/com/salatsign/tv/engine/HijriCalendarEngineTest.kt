package com.salatsign.tv.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.util.Locale

class HijriCalendarEngineTest {

    private val testDate = LocalDate.of(2026, 11, 17)
    private val zone = ZoneId.of("Australia/Sydney")

    @Test
    fun formatHijriDate_returnsValidHijriFormat() {
        val hijriDate = HijriCalendarEngine.formatHijriDate(testDate, 0)
        assertFalse(hijriDate.isBlank())
        assertTrue("Hijri date should contain 'H' suffix", hijriDate.endsWith("H") || hijriDate.contains("Hijri"))
    }

    @Test
    fun formatHijriDate_on2026_11_17_isJumadaAlAkhirah1448() {
        val hijrahDate = HijriCalendarEngine.getHijrahDate(testDate, 0)
        val monthOfYear = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
        val year = hijrahDate.get(ChronoField.YEAR)

        // Month 6 of Islamic Calendar is Jumada al-Akhirah (or Jumada al-Tsaniyah)
        assertEquals("Month of 2026-11-17 must be Jumada al-Akhirah (month 6)", 6, monthOfYear)
        assertEquals("Year must be 1448 H", 1448, year)

        val formatted = HijriCalendarEngine.formatHijriDate(testDate, 0)
        assertTrue("Formatted date must contain 'Jumada al-Akhirah' (got: $formatted)", formatted.contains("Jumada al-Akhirah"))
        assertTrue("Formatted date must contain '1448 H' (got: $formatted)", formatted.contains("1448 H"))
    }

    @Test
    fun formatHijriDate_maghribRollover_advancesDateAtSunset() {
        val maghribZoned = ZonedDateTime.of(testDate, LocalTime.of(19, 45), zone)

        // 1. Before Maghrib (e.g. 18:00) -> Baseline day
        val beforeMaghrib = ZonedDateTime.of(testDate, LocalTime.of(18, 0), zone)
        val dateBefore = HijriCalendarEngine.getHijrahDate(
            date = testDate,
            offsetDays = 0,
            now = beforeMaghrib,
            maghribTime = maghribZoned,
            changeAtMidnight = false
        )
        val dayBefore = dateBefore.get(ChronoField.DAY_OF_MONTH)

        // 2. Exactly at / after Maghrib (e.g. 19:46) -> Advances by 1 day
        val afterMaghrib = ZonedDateTime.of(testDate, LocalTime.of(19, 46), zone)
        val dateAfter = HijriCalendarEngine.getHijrahDate(
            date = testDate,
            offsetDays = 0,
            now = afterMaghrib,
            maghribTime = maghribZoned,
            changeAtMidnight = false
        )
        val dayAfter = dateAfter.get(ChronoField.DAY_OF_MONTH)

        assertEquals("After Maghrib sunset, Hijri date must advance by 1 day", dayBefore + 1, dayAfter)
    }

    @Test
    fun formatHijriDate_midnightRollover_doesNotAdvanceAtSunset() {
        val maghribZoned = ZonedDateTime.of(testDate, LocalTime.of(19, 45), zone)
        val afterMaghrib = ZonedDateTime.of(testDate, LocalTime.of(21, 0), zone)

        // When changeAtMidnight is true, it does not advance at sunset
        val dateAtNight = HijriCalendarEngine.getHijrahDate(
            date = testDate,
            offsetDays = 0,
            now = afterMaghrib,
            maghribTime = maghribZoned,
            changeAtMidnight = true
        )
        val baseline = HijriCalendarEngine.getHijrahDate(testDate, 0)

        assertEquals("When changeAtMidnight is true, date must match baseline day before midnight",
            baseline.get(ChronoField.DAY_OF_MONTH),
            dateAtNight.get(ChronoField.DAY_OF_MONTH)
        )
    }

    @Test
    fun formatHijriDate_appliesOffset_adjustsDate() {
        val baseline = HijriCalendarEngine.formatHijriDate(testDate, 0)
        val plusOneDay = HijriCalendarEngine.formatHijriDate(testDate, 1)
        val minusOneDay = HijriCalendarEngine.formatHijriDate(testDate, -1)

        assertNotEquals(baseline, plusOneDay)
        assertNotEquals(baseline, minusOneDay)
        assertNotEquals(plusOneDay, minusOneDay)
    }

    @Test
    fun formatGregorianDate_returnsLocalizedFormat() {
        val dateId = HijriCalendarEngine.formatGregorianDate(testDate, Locale("id", "ID"))
        val dateEn = HijriCalendarEngine.formatGregorianDate(testDate, Locale.ENGLISH)

        assertTrue("Indonesian date should contain 'November'", dateId.contains("November"))
        assertTrue("Indonesian date should contain '2026'", dateId.contains("2026"))

        assertTrue("English date should contain 'November'", dateEn.contains("November"))
        assertTrue("English date should contain 'Tuesday'", dateEn.contains("Tuesday"))
    }
}
