package com.salatsign.tv.engine

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale

object HijriCalendarEngine {

    val HijriMonthNames = listOf(
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
     * the user-configured offset (-2 to +2 days) and Maghrib rollover (D8).
     */
    fun formatHijriDate(
        date: LocalDate,
        offsetDays: Int = 0,
        now: ZonedDateTime? = null,
        maghribTime: ZonedDateTime? = null,
        changeAtMidnight: Boolean = false
    ): String {
        return try {
            val effectiveDate = calculateEffectiveDate(date, offsetDays, now, maghribTime, changeAtMidnight)
            val hijrahDate = HijrahDate.from(effectiveDate)
            val day = hijrahDate.get(ChronoField.DAY_OF_MONTH)
            val monthIndex = hijrahDate.get(ChronoField.MONTH_OF_YEAR) - 1
            val year = hijrahDate.get(ChronoField.YEAR)

            val monthName = if (monthIndex in HijriMonthNames.indices) {
                HijriMonthNames[monthIndex]
            } else {
                "Bulan ${monthIndex + 1}"
            }

            "$day $monthName $year H"
        } catch (_: Exception) {
            val fallbackDate = calculateEffectiveDate(date, offsetDays, now, maghribTime, changeAtMidnight)
            "${fallbackDate.dayOfMonth} Hijriyah"
        }
    }

    fun getHijrahDate(
        date: LocalDate,
        offsetDays: Int = 0,
        now: ZonedDateTime? = null,
        maghribTime: ZonedDateTime? = null,
        changeAtMidnight: Boolean = false
    ): HijrahDate {
        val effectiveDate = calculateEffectiveDate(date, offsetDays, now, maghribTime, changeAtMidnight)
        return HijrahDate.from(effectiveDate)
    }

    private fun calculateEffectiveDate(
        date: LocalDate,
        offsetDays: Int,
        now: ZonedDateTime?,
        maghribTime: ZonedDateTime?,
        changeAtMidnight: Boolean
    ): LocalDate {
        var baseDate = date.plusDays(offsetDays.toLong())
        // Maghrib rollover (D8): By default, Islamic calendar advances at sunset (Maghrib).
        // If changeAtMidnight is false and now >= maghribTime, advance date by 1.
        if (!changeAtMidnight && now != null && maghribTime != null) {
            if (!now.isBefore(maghribTime)) {
                baseDate = baseDate.plusDays(1)
            }
        }
        return baseDate
    }

    /**
     * Formats the Gregorian date into a readable string (e.g. "Senin, 17 November 2026").
     */
    fun formatGregorianDate(date: LocalDate, locale: Locale = Locale("id", "ID")): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale)
            date.format(formatter)
        } catch (_: Exception) {
            date.format(GregorianFormatterEn)
        }
    }
}
