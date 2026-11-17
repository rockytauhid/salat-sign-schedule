package com.salatsign.tv.data.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class PrayerTimeItem(
    val prayer: Prayer,
    val time: LocalTime?,
    val isNext: Boolean = false,
    val isActive: Boolean = false,
    val isFriday: Boolean = false,
    val iqamahMinutesAfter: Int = 0
) {
    val formattedTime: String
        get() = time?.format(TimeFormatter) ?: "--:--"

    val displayLabel: String
        get() = if (prayer == Prayer.ZUHUR && isFriday) "Jum'at" else prayer.displayName

    val iqamahTime: LocalTime?
        get() = if (iqamahMinutesAfter > 0 && time != null) time.plusMinutes(iqamahMinutesAfter.toLong()) else null

    val formattedIqamahTime: String?
        get() = iqamahTime?.format(TimeFormatter)

    companion object {
        val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
