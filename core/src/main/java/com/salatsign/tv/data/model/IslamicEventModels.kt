// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.model

import java.time.LocalDate

enum class IslamicEventGroup {
    MAJOR,
    COMMONLY_OBSERVED,
    FASTING_AND_OTHER
}

data class IslamicEventDefinition(
    val id: String,
    val hijriMonth: Int, // 0 = every month (1..12)
    val startDay: Int,
    val lengthDays: Int = 1,
    val untilMonthEnd: Boolean = false,
    val group: IslamicEventGroup,
    val defaultEnabled: Boolean
)

data class HijriDay(
    val year: Int,
    val month: Int,
    val day: Int
)

data class EventOccurrence(
    val eventId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val hijriStart: HijriDay,
    val hijriEnd: HijriDay
) {
    val isRange: Boolean get() = endDate.isAfter(startDate)
}

enum class IslamicEventRowState {
    TODAY,
    NEXT,
    PAST,
    UPCOMING
}

data class ClassifiedIslamicEvent(
    val occurrence: EventOccurrence,
    val state: IslamicEventRowState,
    val daysRelativeToToday: Long // 0 when TODAY; > 0 for future (days until startDate); < 0 for past (negative days since endDate)
)

object IslamicEventDefinitions {
    const val PAST_YEARS: Int = 3
    const val FUTURE_YEARS: Int = 3
    const val SLIDE_MAX_ROWS: Int = 6
    const val SLIDE_PAST_WINDOW_DAYS: Long = 14L
    const val SLIDE_MAX_UPCOMING_ROWS: Int = 5

    const val MIN_SLIDE_DURATION_SECONDS: Int = 10
    const val MAX_SLIDE_DURATION_SECONDS: Int = 60
    const val DEFAULT_SLIDE_DURATION_SECONDS: Int = 15

    val all: List<IslamicEventDefinition> = listOf(
        // Major (default ON)
        IslamicEventDefinition("new_year", hijriMonth = 1, startDay = 1, group = IslamicEventGroup.MAJOR, defaultEnabled = true),
        IslamicEventDefinition("ashura", hijriMonth = 1, startDay = 10, group = IslamicEventGroup.MAJOR, defaultEnabled = true),
        IslamicEventDefinition("ramadan_start", hijriMonth = 9, startDay = 1, group = IslamicEventGroup.MAJOR, defaultEnabled = true),
        IslamicEventDefinition("eid_fitr", hijriMonth = 10, startDay = 1, group = IslamicEventGroup.MAJOR, defaultEnabled = true),
        IslamicEventDefinition("arafah", hijriMonth = 12, startDay = 9, group = IslamicEventGroup.MAJOR, defaultEnabled = true),
        IslamicEventDefinition("eid_adha", hijriMonth = 12, startDay = 10, group = IslamicEventGroup.MAJOR, defaultEnabled = true),

        // Commonly observed (default ON)
        IslamicEventDefinition("mawlid", hijriMonth = 3, startDay = 12, group = IslamicEventGroup.COMMONLY_OBSERVED, defaultEnabled = true),
        IslamicEventDefinition("isra_miraj", hijriMonth = 7, startDay = 27, group = IslamicEventGroup.COMMONLY_OBSERVED, defaultEnabled = true),
        IslamicEventDefinition("nisfu_shaban", hijriMonth = 8, startDay = 15, group = IslamicEventGroup.COMMONLY_OBSERVED, defaultEnabled = true),
        IslamicEventDefinition("nuzul_quran", hijriMonth = 9, startDay = 17, group = IslamicEventGroup.COMMONLY_OBSERVED, defaultEnabled = true),
        IslamicEventDefinition("last_ten_ramadan", hijriMonth = 9, startDay = 21, untilMonthEnd = true, group = IslamicEventGroup.COMMONLY_OBSERVED, defaultEnabled = true),

        // Fasting and other days (default OFF)
        IslamicEventDefinition("tasua", hijriMonth = 1, startDay = 9, group = IslamicEventGroup.FASTING_AND_OTHER, defaultEnabled = false),
        IslamicEventDefinition("six_shawwal", hijriMonth = 10, startDay = 2, lengthDays = 6, group = IslamicEventGroup.FASTING_AND_OTHER, defaultEnabled = false),
        IslamicEventDefinition("white_days", hijriMonth = 0, startDay = 13, lengthDays = 3, group = IslamicEventGroup.FASTING_AND_OTHER, defaultEnabled = false),
        IslamicEventDefinition("first_ten_dhulhijjah", hijriMonth = 12, startDay = 1, lengthDays = 10, group = IslamicEventGroup.FASTING_AND_OTHER, defaultEnabled = false),
        IslamicEventDefinition("tashreeq", hijriMonth = 12, startDay = 11, lengthDays = 3, group = IslamicEventGroup.FASTING_AND_OTHER, defaultEnabled = false)
    )

    val byId: Map<String, IslamicEventDefinition> = all.associateBy { it.id }

    val defaultEnabledIds: Set<String> = all.filter { it.defaultEnabled }.map { it.id }.toSet()

    fun byGroup(group: IslamicEventGroup): List<IslamicEventDefinition> = all.filter { it.group == group }
}
