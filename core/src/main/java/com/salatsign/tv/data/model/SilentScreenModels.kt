// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.model

import java.time.ZonedDateTime

enum class SilentAnchor {
    IQAMAH,
    ADHAN
}

enum class SilentWindowKind {
    JUMUAH,
    PRAYER,
    MANUAL
}

data class PrayerSilentConfig(
    val enabled: Boolean = false,
    val anchor: SilentAnchor = SilentAnchor.IQAMAH,
    val leadMinutes: Int = 0,
    val durationMinutes: Int = DEFAULT_DURATION_MINUTES
) {
    fun sanitized(): PrayerSilentConfig = copy(
        leadMinutes = leadMinutes.coerceIn(MIN_LEAD_MINUTES, MAX_LEAD_MINUTES),
        durationMinutes = durationMinutes.coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES)
    )

    companion object {
        const val MIN_LEAD_MINUTES = 0
        const val MAX_LEAD_MINUTES = 15
        const val MIN_DURATION_MINUTES = 5
        const val MAX_DURATION_MINUTES = 60
        const val DEFAULT_DURATION_MINUTES = 15
    }
}

data class SilentScreenConfig(
    val jumuahEnabled: Boolean = true,
    val jumuahLeadMinutes: Int = 0,
    val jumuahDurationMinutes: Int = DEFAULT_JUMUAH_DURATION_MINUTES,
    val dailyPrayers: Map<Prayer, PrayerSilentConfig> = defaultDailyPrayers(),
    val manualSilentUntilEpochMillis: Long = 0L
) {
    fun forPrayer(prayer: Prayer): PrayerSilentConfig =
        dailyPrayers[prayer] ?: PrayerSilentConfig()

    fun sanitized(): SilentScreenConfig = copy(
        jumuahLeadMinutes = jumuahLeadMinutes.coerceIn(MIN_LEAD_MINUTES, MAX_LEAD_MINUTES),
        jumuahDurationMinutes = jumuahDurationMinutes.coerceIn(MIN_JUMUAH_DURATION_MINUTES, MAX_JUMUAH_DURATION_MINUTES),
        dailyPrayers = Prayer.entries.associateWith { prayer -> forPrayer(prayer).sanitized() },
        manualSilentUntilEpochMillis = manualSilentUntilEpochMillis.coerceAtLeast(0L)
    )

    companion object {
        const val MIN_LEAD_MINUTES = 0
        const val MAX_LEAD_MINUTES = 15
        const val MIN_JUMUAH_DURATION_MINUTES = 30
        const val MAX_JUMUAH_DURATION_MINUTES = 120
        const val DEFAULT_JUMUAH_DURATION_MINUTES = 45
        val MANUAL_DURATION_OPTIONS_MINUTES: List<Int> = listOf(15, 30, 45)
        val LEAD_PRESET_OPTIONS_MINUTES: List<Int> = listOf(0, 5, 10, 15)
        val JUMUAH_DURATION_PRESET_MINUTES: List<Int> = listOf(30, 45, 60, 75, 90, 120)
        val DAILY_DURATION_PRESET_MINUTES: List<Int> = listOf(5, 10, 15, 20, 30, 45, 60)

        fun defaultDailyPrayers(): Map<Prayer, PrayerSilentConfig> =
            Prayer.entries.associateWith { PrayerSilentConfig() }
    }
}

data class SilentWindow(
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val kind: SilentWindowKind
)

data class ActiveSilentState(
    val kind: SilentWindowKind,
    val until: ZonedDateTime
)
