// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerConfig
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class JumuahWarningReason {
    SITTING_BEFORE_ZUHR,
    SITTING_AFTER_ASR
}

data class JumuahValidationWarning(
    val date: LocalDate,
    val sittingIndex: Int,
    val sittingTime: LocalTime,
    val zuhurTime: LocalTime,
    val ashrTime: LocalTime,
    val reason: JumuahWarningReason
)

data class JumuahValidationResult(
    val isValid: Boolean,
    val warnings: List<JumuahValidationWarning> = emptyList()
) {
    val firstWarning: JumuahValidationWarning? get() = warnings.firstOrNull()
}

object JumuahCalculator {

    private val timeFormatters = listOf(
        DateTimeFormatter.ofPattern("HH:mm"),
        DateTimeFormatter.ofPattern("H:mm")
    )

    fun parseLocalTime(input: String): LocalTime? {
        val trimmed = input.trim()
        for (formatter in timeFormatters) {
            val parsed = runCatching { LocalTime.parse(trimmed, formatter) }.getOrNull()
            if (parsed != null) return parsed
        }
        return runCatching { LocalTime.parse(trimmed) }.getOrNull()
    }

    /**
     * Resolves Jumu'ah sittings as ZonedDateTime instances for a given Friday.
     * If [jumuahTimes] is empty, defaults to the calculated Zuhr instant.
     */
    fun resolveSittings(
        date: LocalDate,
        config: PrayerConfig,
        jumuahMode: JumuahMode,
        jumuahTimes: List<String>,
        calculatedZuhr: ZonedDateTime
    ): List<ZonedDateTime> {
        val nonBlank = jumuahTimes.filter { it.isNotBlank() }.take(3)
        if (nonBlank.isEmpty()) {
            return listOf(calculatedZuhr)
        }

        return when (jumuahMode) {
            JumuahMode.FIXED -> {
                nonBlank.mapNotNull { timeStr ->
                    val local = parseLocalTime(timeStr) ?: return@mapNotNull null
                    ZonedDateTime.of(date, local, config.zoneId)
                }.ifEmpty { listOf(calculatedZuhr) }.sorted()
            }
            JumuahMode.AFTER_ZUHR -> {
                nonBlank.mapNotNull { offsetStr ->
                    val minutes = offsetStr.trim().toIntOrNull() ?: return@mapNotNull null
                    calculatedZuhr.plusMinutes(minutes.toLong())
                }.ifEmpty { listOf(calculatedZuhr) }.sorted()
            }
        }
    }

    /**
     * Validates Jumu'ah sittings over the next [weeksToCheck] Fridays (default: 53 weeks).
     * Sittings on or after Asr are deemed impossible (isValid = false).
     * Sittings before Zuhr produce warnings naming the affected date and times.
     */
    fun validateSittings(
        startDate: LocalDate,
        config: PrayerConfig,
        jumuahMode: JumuahMode,
        jumuahTimes: List<String>,
        weeksToCheck: Int = 53
    ): JumuahValidationResult {
        val nonBlank = jumuahTimes.filter { it.isNotBlank() }.take(3)
        if (nonBlank.isEmpty()) {
            return JumuahValidationResult(isValid = true)
        }

        val warnings = mutableListOf<JumuahValidationWarning>()
        var currentFriday = startDate
        while (currentFriday.dayOfWeek != DayOfWeek.FRIDAY) {
            currentFriday = currentFriday.plusDays(1)
        }

        for (w in 0 until weeksToCheck) {
            val friday = currentFriday.plusWeeks(w.toLong())
            val events = PrayerCalculationEngine.getDayPrayerEvents(friday, config)
            val zuhurEvent = events.firstOrNull { it.prayer == Prayer.ZUHUR } ?: continue
            val ashrEvent = events.firstOrNull { it.prayer == Prayer.ASHR } ?: continue

            val zuhurZoned = zuhurEvent.at
            val asrZoned = ashrEvent.at

            val sittings = resolveSittings(friday, config, jumuahMode, nonBlank, zuhurZoned)
            sittings.forEachIndexed { index, sittingZoned ->
                if (sittingZoned.isBefore(zuhurZoned)) {
                    warnings.add(
                        JumuahValidationWarning(
                            date = friday,
                            sittingIndex = index + 1,
                            sittingTime = sittingZoned.toLocalTime(),
                            zuhurTime = zuhurZoned.toLocalTime(),
                            ashrTime = asrZoned.toLocalTime(),
                            reason = JumuahWarningReason.SITTING_BEFORE_ZUHR
                        )
                    )
                }
                if (!sittingZoned.isBefore(asrZoned)) {
                    warnings.add(
                        JumuahValidationWarning(
                            date = friday,
                            sittingIndex = index + 1,
                            sittingTime = sittingZoned.toLocalTime(),
                            zuhurTime = zuhurZoned.toLocalTime(),
                            ashrTime = asrZoned.toLocalTime(),
                            reason = JumuahWarningReason.SITTING_AFTER_ASR
                        )
                    )
                }
            }
        }

        val isValid = warnings.none { it.reason == JumuahWarningReason.SITTING_AFTER_ASR }
        return JumuahValidationResult(isValid = isValid, warnings = warnings)
    }
}
