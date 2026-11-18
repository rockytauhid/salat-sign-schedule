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
    val iqamahMinutesAfter: Int = 0
) {
    val formattedTime: String
        get() = time?.format(TimeFormatter24) ?: "--:--"

    fun formattedTime(is24Hour: Boolean): String {
        val formatter = if (is24Hour) TimeFormatter24 else TimeFormatter12
        return time?.format(formatter) ?: "--:--"
    }

    @Deprecated("Use stringResource(prayer.labelRes(...)) in UI layer")
    val displayLabel: String
        get() = if (prayer == Prayer.ZUHUR && isFriday) "Jumu'ah" else prayer.name

    val iqamahTime: LocalTime?
        get() = if (iqamahMinutesAfter > 0 && time != null) time.plusMinutes(iqamahMinutesAfter.toLong()) else null

    val formattedIqamahTime: String?
        get() = iqamahTime?.format(TimeFormatter24)

    fun formattedIqamahTime(is24Hour: Boolean): String? {
        val formatter = if (is24Hour) TimeFormatter24 else TimeFormatter12
        return iqamahTime?.format(formatter)
    }

    companion object {
        val TimeFormatter24: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        val TimeFormatter12: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        // Backwards compatibility alias
        val TimeFormatter: DateTimeFormatter = TimeFormatter24
    }
}
