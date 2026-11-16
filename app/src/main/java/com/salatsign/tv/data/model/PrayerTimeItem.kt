package com.salatsign.tv.data.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class PrayerTimeItem(
    val prayer: Prayer,
    val time: LocalTime,
    val isNext: Boolean = false,
    val isActive: Boolean = false
) {
    val formattedTime: String
        get() = time.format(TimeFormatter)

    companion object {
        val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
