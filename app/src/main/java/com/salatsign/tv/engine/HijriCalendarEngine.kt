package com.salatsign.tv.engine

import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale

object HijriCalendarEngine {

    private val HijriMonthNames = listOf(
        "Muharram",
        "Safar",
        "Rabi' al-Awwal",
        "Rabi' al-Thani",
        "Jumada al-Ula",
        "Jumada al-Akhirah",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhu al-Qi'dah",
        "Dhu al-Hijjah"
    )

    private val GregorianFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
    private val GregorianFormatterEn = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)

    /**
     * Converts a LocalDate into a formatted Hijriyah date string taking into account
     * the user-configured offset (-2 to +2 days).
     */
    fun formatHijriDate(date: LocalDate, offsetDays: Int): String {
        return try {
            val adjustedDate = date.plusDays(offsetDays.toLong())
            val hijrahDate = HijrahDate.from(adjustedDate)
            val day = hijrahDate.get(ChronoField.DAY_OF_MONTH)
            val monthIndex = hijrahDate.get(ChronoField.MONTH_OF_YEAR) - 1
            val year = hijrahDate.get(ChronoField.YEAR)

            val monthName = if (monthIndex in HijriMonthNames.indices) {
                HijriMonthNames[monthIndex]
            } else {
                "Bulan ${monthIndex + 1}"
            }

            "$day $monthName $year H"
        } catch (e: Exception) {
            // Fallback in case of system chronology variance
            val fallbackDate = date.plusDays(offsetDays.toLong())
            "${fallbackDate.dayOfMonth} Hijriyah"
        }
    }

    /**
     * Formats the Gregorian date into a readable string (e.g. "Senin, 17 November 2026").
     */
    fun formatGregorianDate(date: LocalDate, locale: Locale = Locale("id", "ID")): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale)
            date.format(formatter)
        } catch (e: Exception) {
            date.format(GregorianFormatterEn)
        }
    }
}
