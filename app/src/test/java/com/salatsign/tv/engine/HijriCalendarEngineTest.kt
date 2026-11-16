package com.salatsign.tv.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class HijriCalendarEngineTest {

    private val testDate = LocalDate.of(2026, 11, 17)

    @Test
    fun formatHijriDate_returnsValidHijriFormat() {
        val hijriDate = HijriCalendarEngine.formatHijriDate(testDate, 0)
        assertFalse(hijriDate.isBlank())
        assertTrue("Hijri date should contain 'H' suffix", hijriDate.endsWith("H") || hijriDate.contains("Hijri"))
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
