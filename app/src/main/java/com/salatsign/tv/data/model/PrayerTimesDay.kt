// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.model

import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

data class PrayerTimesDay(
    val date: LocalDate,
    val hijriDate: String,
    val gregorianDate: String,
    val prayers: List<PrayerTimeItem>,
    val sunrise: LocalTime?,
    val nextPrayer: PrayerTimeItem?,
    val activePrayer: PrayerTimeItem?,
    val secondsUntilNext: Long
) {
    val hasNullTimes: Boolean
        get() = prayers.any { it.time == null } || sunrise == null

    val hasIqamahConfigured: Boolean
        get() = prayers.any { it.iqamahMinutesAfter > 0 }

    val formattedSunrise: String
        get() = sunrise?.format(PrayerTimeItem.TimeFormatter) ?: "--:--"

    fun formattedSunrise(is24Hour: Boolean): String {
        val s = sunrise ?: return "--:--"
        return PrayerTimeItem.formatLocalTime(s, is24Hour)
    }

    val countdownFormatted: String
        get() {
            if (secondsUntilNext <= 0) return "00:00:00"
            val hours = secondsUntilNext / 3600
            val minutes = (secondsUntilNext % 3600) / 60
            val seconds = secondsUntilNext % 60
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }
}
