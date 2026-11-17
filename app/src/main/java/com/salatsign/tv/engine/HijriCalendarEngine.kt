package com.salatsign.tv.engine

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoField
import java.util.Locale

object HijriCalendarEngine {

    val EnglishHijriMonthNames = listOf(
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

    val IndonesianHijriMonthNames = listOf(
        "Muharram",
        "Safar",
        "Rabiul Awal",
        "Rabiul Akhir",
        "Jumadil Awal",
        "Jumadil Akhir",
        "Rajab",
        "Sya'ban",
        "Ramadhan",
        "Syawal",
        "Dzulqa'dah",
        "Dzulhijjah"
    )

    val ArabicHijriMonthNames = listOf(
        "مُحَرَّم",
        "صَفَر",
        "رَبِيع الأوَّل",
        "رَبِيع الآخِر",
        "جُمَادَى الأولى",
        "جُمَادَى الآخِرة",
        "رَجَب",
        "شَعْبَان",
        "رَمَضَان",
        "شَوَّال",
        "ذُو القَعْدَة",
        "ذُو الحِجَّة"
    )

    // Backwards compatibility alias
    val HijriMonthNames = EnglishHijriMonthNames

    /**
     * Converts a LocalDate into a formatted Hijriyah date string taking into account
     * the user-configured offset (-2 to +2 days), Maghrib rollover (D8), and language.
     */
    fun formatHijriDate(
        date: LocalDate,
        offsetDays: Int = 0,
        now: ZonedDateTime? = null,
        maghribTime: ZonedDateTime? = null,
        changeAtMidnight: Boolean = false,
        monthNames: List<String> = EnglishHijriMonthNames,
        yearSuffix: String = "H"
    ): String {
        return try {
            val effectiveDate = calculateEffectiveDate(date, offsetDays, now, maghribTime, changeAtMidnight)
            val hijrahDate = HijrahDate.from(effectiveDate)
            val day = hijrahDate.get(ChronoField.DAY_OF_MONTH)
            val monthIndex = hijrahDate.get(ChronoField.MONTH_OF_YEAR) - 1
            val year = hijrahDate.get(ChronoField.YEAR)

            val monthName = if (monthIndex in monthNames.indices) {
                monthNames[monthIndex]
            } else if (monthIndex in EnglishHijriMonthNames.indices) {
                EnglishHijriMonthNames[monthIndex]
            } else {
                "${monthIndex + 1}"
            }

            "$day $monthName $year $yearSuffix"
        } catch (_: Exception) {
            val fallbackDate = calculateEffectiveDate(date, offsetDays, now, maghribTime, changeAtMidnight)
            "${fallbackDate.dayOfMonth} $yearSuffix"
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
     * Formats the Gregorian date into a readable string using locale-aware formatting.
     * Enforces Latin digits even in Arabic (D9).
     */
    fun formatGregorianDate(date: LocalDate, locale: Locale = Locale.forLanguageTag("en-AU")): String {
        return try {
            val effectiveLocale = if (locale.language == "ar") {
                Locale.forLanguageTag("ar-u-nu-latn")
            } else {
                locale
            }
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(effectiveLocale)
            date.format(formatter)
        } catch (_: Exception) {
            val fallbackFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)
            date.format(fallbackFormatter)
        }
    }
}
