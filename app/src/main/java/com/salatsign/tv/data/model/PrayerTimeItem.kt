// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PrayerTimeItem(
    val prayer: Prayer,
    val time: LocalTime?,
    val isNext: Boolean = false,
    val isActive: Boolean = false,
    val isFriday: Boolean = false,
    val iqamahMinutesAfter: Int = 0,
    val jumuahSittings: List<LocalTime> = emptyList(),
    val secondaryTimeNote: String? = null
) {
    val formattedTime: String
        get() = time?.format(TimeFormatter24) ?: "--:--"

    fun formattedTime(is24Hour: Boolean): String {
        val t = time ?: return "--:--"
        return formatLocalTime(t, is24Hour)
    }

    @Deprecated("Use stringResource(prayer.labelRes(...)) in UI layer")
    val displayLabel: String
        get() = if (prayer == Prayer.ZUHUR && isFriday) "Jumu'ah" else prayer.name

    val iqamahTime: LocalTime?
        get() = if (iqamahMinutesAfter > 0 && time != null) time.plusMinutes(iqamahMinutesAfter.toLong()) else null

    val formattedIqamahTime: String?
        get() = iqamahTime?.format(TimeFormatter24)

    fun formattedIqamahTime(is24Hour: Boolean): String? {
        val t = iqamahTime ?: return null
        return formatLocalTime(t, is24Hour)
    }

    companion object {
        val TimeFormatter24: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        val TimeFormatter12: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("en-AU"))
        // Backwards compatibility alias
        val TimeFormatter: DateTimeFormatter = TimeFormatter24

        fun formatLocalTime(time: LocalTime, is24Hour: Boolean): String {
            return if (is24Hour) {
                time.format(TimeFormatter24)
            } else {
                time.format(TimeFormatter12).lowercase(Locale.ENGLISH)
            }
        }
    }
}
