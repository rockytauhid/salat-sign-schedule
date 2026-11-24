// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

import com.salatsign.tv.data.model.PrayerTimeItem
import com.salatsign.tv.data.model.PrayerTimesDay
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ScreensaverSlideKind {
    SCHEDULE,
    CALENDAR,
    QURAN
}

object ScreensaverDefaults {
    const val MIN_DURATION_SECONDS = 10
    const val MAX_DURATION_SECONDS = 60
    const val DEFAULT_SCHEDULE_DURATION_SECONDS = 20
    const val DEFAULT_CALENDAR_DURATION_SECONDS = 15
    const val BURN_IN_MAX_SHIFT_PX = 8
    const val CROSSFADE_DURATION_MS = 500

    fun clampDuration(seconds: Int): Int = seconds.coerceIn(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS)
}

/**
 * Pure-Kotlin state and rotation logic for the SalatSchedule Android TV Screensaver.
 */
object ScreensaverRotationEngine {

    private val burnInPattern: List<Pair<Int, Int>> = listOf(
        0 to 0,
        4 to 0,
        8 to 4,
        4 to 8,
        0 to 8,
        -4 to 8,
        -8 to 4,
        -4 to 0,
        -8 to -4,
        -4 to -8,
        0 to -8,
        4 to -8,
        8 to -4,
        4 to 0
    )

    /**
     * Computes the ordered list of slides to rotate: Schedule -> Calendar -> Quran -> repeat.
     * Skips Calendar when no events are enabled/available, skips Quran when data is missing,
     * and guarantees at least [ScreensaverSlideKind.SCHEDULE] is returned.
     */
    fun buildActiveSlides(
        scheduleEnabled: Boolean,
        calendarEnabled: Boolean,
        quranEnabled: Boolean,
        hasCalendarEvents: Boolean,
        hasQuranData: Boolean
    ): List<ScreensaverSlideKind> {
        val slides = mutableListOf<ScreensaverSlideKind>()
        if (scheduleEnabled) slides.add(ScreensaverSlideKind.SCHEDULE)
        if (calendarEnabled && hasCalendarEvents) slides.add(ScreensaverSlideKind.CALENDAR)
        if (quranEnabled && hasQuranData) slides.add(ScreensaverSlideKind.QURAN)
        if (slides.isEmpty()) {
            slides.add(ScreensaverSlideKind.SCHEDULE)
        }
        return slides
    }

    /**
     * Ensures that disabling a toggle never leaves all three slides disabled.
     * Returns true if disabling [target] is allowed given the current enabled flags.
     */
    fun canDisableSlide(
        target: ScreensaverSlideKind,
        scheduleEnabled: Boolean,
        calendarEnabled: Boolean,
        quranEnabled: Boolean
    ): Boolean {
        val remaining = listOf(
            ScreensaverSlideKind.SCHEDULE to scheduleEnabled,
            ScreensaverSlideKind.CALENDAR to calendarEnabled,
            ScreensaverSlideKind.QURAN to quranEnabled
        ).count { (kind, enabled) -> kind != target && enabled }
        return remaining >= 1
    }

    fun nextIndex(currentIndex: Int, totalSlides: Int): Int {
        if (totalSlides <= 1) return 0
        return (currentIndex + 1) % totalSlides
    }

    /**
     * Returns a subtle (dx, dy) pixel shift within [-8, +8] px for OLED/LCD burn-in mitigation,
     * advancing once per minute.
     */
    fun burnInOffsetPx(minuteTick: Long): Pair<Int, Int> {
        val idx = ((minuteTick % burnInPattern.size) + burnInPattern.size).toInt() % burnInPattern.size
        return burnInPattern[idx]
    }

    /**
     * Formats the clock without seconds (updates once per minute for low CPU/memory).
     */
    fun formatClockMinutes(time: LocalTime, is24HourFormat: Boolean): String {
        val pattern = if (is24HourFormat) "HH:mm" else "h:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
    }

    /**
     * Computes (hours, minutes) remaining until [nextPrayer] from [now].
     * Wraps cleanly past midnight when the next prayer is tomorrow's Fajr.
     */
    fun minutesUntilNextPrayer(now: LocalTime, nextPrayer: PrayerTimeItem): Long {
        val rawMinutes = Duration.between(now, nextPrayer.time).toMinutes()
        return if (rawMinutes < 0) {
            rawMinutes + 24 * 60
        } else {
            rawMinutes
        }
    }

    /**
     * Returns the Home-mode prayer list for the screensaver, ensuring Friday Zuhr keeps
     * the plain Zuhr label (`isFriday = false` for Home mode).
     */
    fun normalizeHomePrayers(prayerTimesDay: PrayerTimesDay): List<PrayerTimeItem> =
        prayerTimesDay.prayers.map { it.copy(isFriday = false) }
}
