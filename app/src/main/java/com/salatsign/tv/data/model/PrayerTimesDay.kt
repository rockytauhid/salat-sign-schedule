package com.salatsign.tv.data.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PrayerTimesDay(
    val date: LocalDate,
    val hijriDate: String,
    val gregorianDate: String,
    val prayers: List<PrayerTimeItem>,
    val sunrise: LocalTime,
    val nextPrayer: PrayerTimeItem?,
    val activePrayer: PrayerTimeItem?,
    val secondsUntilNext: Long
) {
    val countdownFormatted: String
        get() {
            if (secondsUntilNext <= 0) return "00:00:00"
            val hours = secondsUntilNext / 3600
            val minutes = (secondsUntilNext % 3600) / 60
            val seconds = secondsUntilNext % 60
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }
}
