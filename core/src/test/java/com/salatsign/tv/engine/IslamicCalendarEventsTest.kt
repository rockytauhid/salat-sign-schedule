// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

import com.salatsign.tv.data.model.IslamicEventDefinitions
import com.salatsign.tv.data.model.IslamicEventRowState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

class IslamicCalendarEventsTest {

    @Test
    fun occurrencesForYear_allEnabledCountsOrderingAndRamadanMonthLengthClipping() {
        val allIds = IslamicEventDefinitions.all.map { it.id }.toSet()

        // Test across multiple Hijri years (find both a 29-day Ramadan and a 30-day Ramadan)
        var saw29DayRamadan = false
        var saw30DayRamadan = false

        for (year in 1445..1452) {
            val occurrences = IslamicCalendarEvents.occurrencesForYear(year, adjustmentDays = 0, enabledIds = allIds)

            // Every non-monthly event appears once; white_days appears 12 times
            val countsById = occurrences.groupingBy { it.eventId }.eachCount()
            for (def in IslamicEventDefinitions.all) {
                val expectedCount = if (def.hijriMonth == 0) 12 else 1
                assertEquals("Year $year event ${def.id}", expectedCount, countsById[def.id])
            }

            // Strictly sorted by startDate
            for (i in 1 until occurrences.size) {
                assertFalse(
                    "Year $year not sorted at index $i",
                    occurrences[i].startDate.isBefore(occurrences[i - 1].startDate)
                )
            }

            // last_ten_ramadan clips to actual Ramadan month length (29 or 30)
            val ramadanLen = HijrahDate.of(year, 9, 1).lengthOfMonth()
            if (ramadanLen == 29) saw29DayRamadan = true
            if (ramadanLen == 30) saw30DayRamadan = true
            val lastTen = occurrences.first { it.eventId == "last_ten_ramadan" }
            assertEquals(21, lastTen.hijriStart.day)
            assertEquals(ramadanLen, lastTen.hijriEnd.day)
        }

        assertTrue("Expected at least one 29-day Ramadan in 1445..1452", saw29DayRamadan)
        assertTrue("Expected at least one 30-day Ramadan in 1445..1452", saw30DayRamadan)
    }

    @Test
    fun roundTrip_withHijriDisplayOffsetMinus2ToPlus2() {
        val enabled = IslamicEventDefinitions.defaultEnabledIds
        for (offset in -2..2) {
            val occurrences = IslamicCalendarEvents.occurrencesForYear(1448, adjustmentDays = offset, enabledIds = enabled)
            assertTrue(occurrences.isNotEmpty())
            for (occ in occurrences) {
                val roundTripped = HijriCalendarEngine.getHijrahDate(
                    date = occ.startDate,
                    offsetDays = offset,
                    changeAtMidnight = true
                )
                assertEquals("Offset $offset event ${occ.eventId} year", occ.hijriStart.year, roundTripped.get(ChronoField.YEAR))
                assertEquals("Offset $offset event ${occ.eventId} month", occ.hijriStart.month, roundTripped.get(ChronoField.MONTH_OF_YEAR))
                assertEquals("Offset $offset event ${occ.eventId} day", occ.hijriStart.day, roundTripped.get(ChronoField.DAY_OF_MONTH))
            }
        }
    }

    @Test
    fun classification_todaySingleAndRange_nextDistinctAndCrossYear_pastDimmed() {
        val enabled = IslamicEventDefinitions.defaultEnabledIds
        val occurrences1448 = IslamicCalendarEvents.occurrencesForYear(1448, adjustmentDays = 0, enabledIds = enabled)

        // 1. Single-day TODAY (Eid al-Fitr) -> TODAY and NEXT are on different rows
        val eidFitr = occurrences1448.first { it.eventId == "eid_fitr" }
        val classifiedOnEid = IslamicCalendarState.classify(occurrences1448, today = eidFitr.startDate)
        val todayRow = classifiedOnEid.first { it.occurrence.eventId == "eid_fitr" }
        val nextRow = classifiedOnEid.first { it.state == IslamicEventRowState.NEXT }
        assertEquals(IslamicEventRowState.TODAY, todayRow.state)
        assertEquals(0L, todayRow.daysRelativeToToday)
        assertEquals("arafah", nextRow.occurrence.eventId)
        assertTrue(nextRow.daysRelativeToToday > 0L)

        // Prior event (Ramadan start) is PAST
        val ramadanRow = classifiedOnEid.first { it.occurrence.eventId == "ramadan_start" }
        assertEquals(IslamicEventRowState.PAST, ramadanRow.state)
        assertTrue(ramadanRow.daysRelativeToToday < 0L)

        // 2. Inside a range event (last_ten_ramadan, day 25) -> TODAY, and NEXT is strictly after today (eid_fitr)
        val lastTen = occurrences1448.first { it.eventId == "last_ten_ramadan" }
        val insideLastTen = lastTen.startDate.plusDays(4)
        val classifiedInRange = IslamicCalendarState.classify(occurrences1448, today = insideLastTen)
        assertEquals(IslamicEventRowState.TODAY, classifiedInRange.first { it.occurrence.eventId == "last_ten_ramadan" }.state)
        assertEquals("eid_fitr", classifiedInRange.first { it.state == IslamicEventRowState.NEXT }.occurrence.eventId)

        // 3. Cross-year NEXT: after Eid al-Adha in 1448, nextEvent() finds 1 Muharram 1449
        val eidAdha = occurrences1448.first { it.eventId == "eid_adha" }
        val afterEidAdha = eidAdha.endDate.plusDays(1)
        val nextAcrossYear = IslamicCalendarState.nextEvent(afterEidAdha, adjustmentDays = 0, enabledIds = enabled)
        assertNotNull(nextAcrossYear)
        assertEquals("new_year", nextAcrossYear!!.occurrence.eventId)
        assertEquals(1449, nextAcrossYear.occurrence.hijriStart.year)
        assertEquals(IslamicEventRowState.NEXT, nextAcrossYear.state)
    }

    @Test
    fun disabledEvents_neverAppear_andYearClampingEnforced() {
        val onlyEid = setOf("eid_fitr", "eid_adha")
        val occurrences = IslamicCalendarEvents.occurrencesForYear(1448, adjustmentDays = 0, enabledIds = onlyEid)
        assertEquals(listOf("eid_fitr", "eid_adha"), occurrences.map { it.eventId })

        val currentYear = 1448
        assertEquals(1445, IslamicCalendarEvents.clampHijriYear(1440, currentYear))
        assertEquals(1451, IslamicCalendarEvents.clampHijriYear(1460, currentYear))
        assertFalse(IslamicCalendarEvents.canGoPreviousYear(1445, currentYear))
        assertTrue(IslamicCalendarEvents.canGoPreviousYear(1446, currentYear))
        assertFalse(IslamicCalendarEvents.canGoNextYear(1451, currentYear))
        assertTrue(IslamicCalendarEvents.canGoNextYear(1450, currentYear))

        // Today scroll target prefers TODAY over NEXT
        val eidFitr = occurrences.first { it.eventId == "eid_fitr" }
        val classified = IslamicCalendarState.classify(occurrences, today = eidFitr.startDate)
        assertEquals(0, IslamicCalendarState.initialScrollIndex(classified))
    }

    @Test
    fun configuredTimezoneDayBoundary_sydneyAndAdelaide() {
        val sydney = ZoneId.of("Australia/Sydney")
        val adelaide = ZoneId.of("Australia/Adelaide")
        // 2026-07-03 23:45 in Adelaide (+09:30) is already 2026-07-04 00:15 in Sydney (+10:00)
        val instantInAdelaide = ZonedDateTime.of(LocalDate.of(2026, 7, 3), LocalTime.of(23, 45), adelaide)

        val adelaideToday = IslamicCalendarEvents.todayInZone(adelaide) { instantInAdelaide }
        val sydneyToday = IslamicCalendarEvents.todayInZone(sydney) { instantInAdelaide }

        assertEquals(LocalDate.of(2026, 7, 3), adelaideToday)
        assertEquals(LocalDate.of(2026, 7, 4), sydneyToday)
    }

    @Test
    fun mosqueSlideRows_past14DayLimit_todayRange_maxSix_crossYear_andEmptyWhenDisabled() {
        val enabled = IslamicEventDefinitions.defaultEnabledIds
        val occ1448 = IslamicCalendarEvents.occurrencesForYear(1448, adjustmentDays = 0, enabledIds = enabled)

        // All disabled -> empty list (slide skipped)
        assertTrue(IslamicCalendarSlideRows.compute(LocalDate.of(2026, 7, 1), emptySet(), 0).isEmpty())

        // 5 days after Eid al-Fitr -> Eid al-Fitr appears as recent PAST (within 14 days), followed by NEXT (Arafah)
        val eidFitr = occ1448.first { it.eventId == "eid_fitr" }
        val fiveDaysAfter = eidFitr.endDate.plusDays(5)
        val rows5 = IslamicCalendarSlideRows.compute(fiveDaysAfter, enabled, 0)
        assertTrue(rows5.size <= 6)
        assertEquals("eid_fitr", rows5.first().occurrence.eventId)
        assertEquals(IslamicEventRowState.PAST, rows5.first().state)
        assertEquals(-5L, rows5.first().daysRelativeToToday)
        assertEquals("arafah", rows5[1].occurrence.eventId)
        assertEquals(IslamicEventRowState.NEXT, rows5[1].state)

        // 20 days after Eid al-Fitr -> NO past event (> 14 days), first row is NEXT
        val twentyDaysAfter = eidFitr.endDate.plusDays(20)
        val rows20 = IslamicCalendarSlideRows.compute(twentyDaysAfter, enabled, 0)
        assertTrue(rows20.none { it.state == IslamicEventRowState.PAST })
        assertEquals(IslamicEventRowState.NEXT, rows20.first().state)

        // TODAY inside last_ten_ramadan range -> TODAY row + NEXT row (Eid al-Fitr), max 6 rows
        val lastTen = occ1448.first { it.eventId == "last_ten_ramadan" }
        val rowsInRange = IslamicCalendarSlideRows.compute(lastTen.startDate.plusDays(3), enabled, 0)
        assertTrue(rowsInRange.size <= 6)
        assertTrue(rowsInRange.any { it.occurrence.eventId == "last_ten_ramadan" && it.state == IslamicEventRowState.TODAY })
        assertTrue(rowsInRange.any { it.occurrence.eventId == "eid_fitr" && it.state == IslamicEventRowState.NEXT })

        // Cross-year: 2 days after Eid al-Adha 1448 -> PAST Eid al-Adha + NEXT 1 Muharram 1449
        val eidAdha = occ1448.first { it.eventId == "eid_adha" }
        val rowsCrossYear = IslamicCalendarSlideRows.compute(eidAdha.endDate.plusDays(2), enabled, 0)
        assertEquals("eid_adha", rowsCrossYear[0].occurrence.eventId)
        assertEquals(IslamicEventRowState.PAST, rowsCrossYear[0].state)
        assertEquals("new_year", rowsCrossYear[1].occurrence.eventId)
        assertEquals(1449, rowsCrossYear[1].occurrence.hijriStart.year)
        assertEquals(IslamicEventRowState.NEXT, rowsCrossYear[1].state)
    }
}
