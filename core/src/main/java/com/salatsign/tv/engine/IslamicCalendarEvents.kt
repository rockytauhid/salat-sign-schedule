// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

import com.salatsign.tv.data.model.ClassifiedIslamicEvent
import com.salatsign.tv.data.model.EventOccurrence
import com.salatsign.tv.data.model.HijriDay
import com.salatsign.tv.data.model.IslamicEventDefinitions
import com.salatsign.tv.data.model.IslamicEventRowState
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

/**
 * Pure-Kotlin, on-demand Islamic calendar event calculator (no stored event data, no Android types).
 * Uses the exact inverse of [HijriCalendarEngine]'s display offset so every event round-trips cleanly.
 */
object IslamicCalendarEvents {

    /**
     * Converts a Hijri date to its Gregorian daytime date using the inverse of [HijriCalendarEngine]'s
     * offset convention (`HijriCalendarEngine` computes `HijrahDate.from(gregorian.plusDays(offset))`).
     */
    fun toGregorianDate(hijriYear: Int, hijriMonth: Int, hijriDay: Int, adjustmentDays: Int): LocalDate {
        val clampedAdj = adjustmentDays.coerceIn(-2, 2)
        val baseGregorian = LocalDate.from(HijrahDate.of(hijriYear, hijriMonth, hijriDay))
        return baseGregorian.minusDays(clampedAdj.toLong())
    }

    /**
     * Returns today's Hijri year for a daytime Gregorian date in the configured mosque/home timezone.
     * Maghrib rollover does not shift which Gregorian date an event falls on.
     */
    fun currentHijriYear(today: LocalDate, adjustmentDays: Int): Int {
        val hijrah = HijriCalendarEngine.getHijrahDate(
            date = today,
            offsetDays = adjustmentDays.coerceIn(-2, 2),
            changeAtMidnight = true
        )
        return hijrah.get(ChronoField.YEAR)
    }

    fun todayInZone(zoneId: ZoneId, nowProvider: () -> ZonedDateTime = { ZonedDateTime.now(zoneId) }): LocalDate {
        return nowProvider().withZoneSameInstant(zoneId).toLocalDate()
    }

    fun clampHijriYear(targetYear: Int, currentHijriYear: Int): Int {
        val minYear = currentHijriYear - IslamicEventDefinitions.PAST_YEARS
        val maxYear = currentHijriYear + IslamicEventDefinitions.FUTURE_YEARS
        return targetYear.coerceIn(minYear, maxYear)
    }

    fun canGoPreviousYear(displayedYear: Int, currentHijriYear: Int): Boolean =
        displayedYear > currentHijriYear - IslamicEventDefinitions.PAST_YEARS

    fun canGoNextYear(displayedYear: Int, currentHijriYear: Int): Boolean =
        displayedYear < currentHijriYear + IslamicEventDefinitions.FUTURE_YEARS

    /**
     * Returns the Gregorian start and end dates spanning the given Hijri year.
     */
    fun gregorianSpanForHijriYear(hijriYear: Int, adjustmentDays: Int): Pair<LocalDate, LocalDate> {
        val start = toGregorianDate(hijriYear, 1, 1, adjustmentDays)
        val lastMonthLength = HijrahDate.of(hijriYear, 12, 1).lengthOfMonth()
        val end = toGregorianDate(hijriYear, 12, lastMonthLength, adjustmentDays)
        return start to end
    }

    /**
     * Computes all enabled Islamic event occurrences for [hijriYear], sorted chronologically.
     * Ranges (`lengthDays > 1` or `untilMonthEnd`) are clipped to the actual month length (29 or 30 days).
     */
    fun occurrencesForYear(
        hijriYear: Int,
        adjustmentDays: Int,
        enabledIds: Set<String>
    ): List<EventOccurrence> {
        if (enabledIds.isEmpty()) return emptyList()
        val clampedAdj = adjustmentDays.coerceIn(-2, 2)
        val results = mutableListOf<EventOccurrence>()

        for (def in IslamicEventDefinitions.all) {
            if (def.id !in enabledIds) continue
            val months = if (def.hijriMonth == 0) (1..12).toList() else listOf(def.hijriMonth)
            for (month in months) {
                val monthLength = HijrahDate.of(hijriYear, month, 1).lengthOfMonth()
                val startDay = def.startDay.coerceIn(1, monthLength)
                val endDay = if (def.untilMonthEnd) {
                    monthLength
                } else {
                    (startDay + def.lengthDays - 1).coerceIn(startDay, monthLength)
                }

                val startDate = toGregorianDate(hijriYear, month, startDay, clampedAdj)
                val endDate = toGregorianDate(hijriYear, month, endDay, clampedAdj)
                results.add(
                    EventOccurrence(
                        eventId = def.id,
                        startDate = startDate,
                        endDate = endDate,
                        hijriStart = HijriDay(hijriYear, month, startDay),
                        hijriEnd = HijriDay(hijriYear, month, endDay)
                    )
                )
            }
        }

        return results.sortedWith(
            compareBy<EventOccurrence> { it.startDate }
                .thenBy { it.endDate }
                .thenBy { IslamicEventDefinitions.all.indexOfFirst { d -> d.id == it.eventId } }
        )
    }
}

object IslamicCalendarState {

    /**
     * Classifies [occurrences] relative to [today] with priority:
     * 1. TODAY (today falls on single-day event or inside range)
     * 2. NEXT (first event starting strictly after today; distinct row from TODAY)
     * 3. PAST (ended before today)
     * 4. UPCOMING (everything else)
     */
    fun classify(
        occurrences: List<EventOccurrence>,
        today: LocalDate,
        globalNextEvent: EventOccurrence? = null
    ): List<ClassifiedIslamicEvent> {
        val nextTarget = globalNextEvent ?: occurrences.firstOrNull { it.startDate.isAfter(today) }

        return occurrences.map { occ ->
            val isToday = !today.isBefore(occ.startDate) && !today.isAfter(occ.endDate)
            val isPast = occ.endDate.isBefore(today)
            val isNext = !isToday && nextTarget != null &&
                occ.eventId == nextTarget.eventId &&
                occ.startDate == nextTarget.startDate &&
                occ.endDate == nextTarget.endDate

            val state = when {
                isToday -> IslamicEventRowState.TODAY
                isNext -> IslamicEventRowState.NEXT
                isPast -> IslamicEventRowState.PAST
                else -> IslamicEventRowState.UPCOMING
            }

            val relativeDays = when {
                isToday -> 0L
                isPast -> -ChronoUnit.DAYS.between(occ.endDate, today)
                else -> ChronoUnit.DAYS.between(today, occ.startDate)
            }

            ClassifiedIslamicEvent(
                occurrence = occ,
                state = state,
                daysRelativeToToday = relativeDays
            )
        }
    }

    /**
     * Finds the next enabled event strictly after [today], looking into the next Hijri year if needed.
     */
    fun nextEvent(
        today: LocalDate,
        adjustmentDays: Int,
        enabledIds: Set<String>
    ): ClassifiedIslamicEvent? {
        if (enabledIds.isEmpty()) return null
        val curYear = IslamicCalendarEvents.currentHijriYear(today, adjustmentDays)
        val combined = (-1..1).flatMap { delta ->
            IslamicCalendarEvents.occurrencesForYear(curYear + delta, adjustmentDays, enabledIds)
        }.sortedWith(compareBy<EventOccurrence> { it.startDate }.thenBy { it.endDate })

        val next = combined.firstOrNull { it.startDate.isAfter(today) } ?: return null
        val daysUntil = ChronoUnit.DAYS.between(today, next.startDate)
        return ClassifiedIslamicEvent(
            occurrence = next,
            state = IslamicEventRowState.NEXT,
            daysRelativeToToday = daysUntil
        )
    }

    /**
     * Returns the index to scroll to when opening or tapping "Today" on the current Hijri year's list:
     * prefers the first TODAY row, then NEXT row, otherwise 0.
     */
    fun initialScrollIndex(rows: List<ClassifiedIslamicEvent>): Int {
        val todayIdx = rows.indexOfFirst { it.state == IslamicEventRowState.TODAY }
        if (todayIdx >= 0) return todayIdx
        val nextIdx = rows.indexOfFirst { it.state == IslamicEventRowState.NEXT }
        if (nextIdx >= 0) return nextIdx
        return 0
    }
}

/**
 * Pure row-selection logic for the read-only SalatSign Mosque Islamic Calendar slide (max 6 rows).
 */
object IslamicCalendarSlideRows {

    fun compute(
        today: LocalDate,
        enabledIds: Set<String>,
        adjustmentDays: Int
    ): List<ClassifiedIslamicEvent> {
        if (enabledIds.isEmpty()) return emptyList()
        val curYear = IslamicCalendarEvents.currentHijriYear(today, adjustmentDays)
        val combined = (-1..1).flatMap { delta ->
            IslamicCalendarEvents.occurrencesForYear(curYear + delta, adjustmentDays, enabledIds)
        }.sortedWith(
            compareBy<EventOccurrence> { it.startDate }
                .thenBy { it.endDate }
                .thenBy { IslamicEventDefinitions.all.indexOfFirst { d -> d.id == it.eventId } }
        )

        val recentPast = combined
            .filter { it.endDate.isBefore(today) }
            .filter { ChronoUnit.DAYS.between(it.endDate, today) <= IslamicEventDefinitions.SLIDE_PAST_WINDOW_DAYS }
            .maxByOrNull { it.endDate }

        val todayEvents = combined.filter { !today.isBefore(it.startDate) && !today.isAfter(it.endDate) }

        val upcomingEvents = combined
            .filter { it.startDate.isAfter(today) }
            .take(IslamicEventDefinitions.SLIDE_MAX_UPCOMING_ROWS)

        val selected = mutableListOf<ClassifiedIslamicEvent>()
        if (recentPast != null) {
            selected.add(
                ClassifiedIslamicEvent(
                    occurrence = recentPast,
                    state = IslamicEventRowState.PAST,
                    daysRelativeToToday = -ChronoUnit.DAYS.between(recentPast.endDate, today)
                )
            )
        }
        for (ev in todayEvents) {
            selected.add(
                ClassifiedIslamicEvent(
                    occurrence = ev,
                    state = IslamicEventRowState.TODAY,
                    daysRelativeToToday = 0L
                )
            )
        }
        upcomingEvents.forEachIndexed { idx, ev ->
            selected.add(
                ClassifiedIslamicEvent(
                    occurrence = ev,
                    state = if (idx == 0) IslamicEventRowState.NEXT else IslamicEventRowState.UPCOMING,
                    daysRelativeToToday = ChronoUnit.DAYS.between(today, ev.startDate)
                )
            )
        }

        // Enforce strict max 6 rows (if multiple TODAY events push over 6, keep TODAY + NEXT + nearest rows)
        if (selected.size <= IslamicEventDefinitions.SLIDE_MAX_ROWS) {
            return selected
        }
        val trimmed = mutableListOf<ClassifiedIslamicEvent>()
        val mustKeep = selected.filter { it.state == IslamicEventRowState.TODAY || it.state == IslamicEventRowState.NEXT }
        val remainingBudget = (IslamicEventDefinitions.SLIDE_MAX_ROWS - mustKeep.size).coerceAtLeast(0)
        val extras = selected.filter { it.state != IslamicEventRowState.TODAY && it.state != IslamicEventRowState.NEXT }
            .take(remainingBudget)
        val keepSet = (mustKeep + extras).toSet()
        for (row in selected) {
            if (row in keepSet && trimmed.size < IslamicEventDefinitions.SLIDE_MAX_ROWS) {
                trimmed.add(row)
            }
        }
        return trimmed
    }
}
