// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

import com.salatsign.tv.data.model.ActiveSilentState
import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerConfig
import com.salatsign.tv.data.model.PrayerEvent
import com.salatsign.tv.data.model.PrayerSilentConfig
import com.salatsign.tv.data.model.SilentAnchor
import com.salatsign.tv.data.model.SilentScreenConfig
import com.salatsign.tv.data.model.SilentWindow
import com.salatsign.tv.data.model.SilentWindowKind
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class CopyPrayerSilentResult(
    val updatedPrayers: Map<Prayer, PrayerSilentConfig>,
    val enabledCount: Int,
    val skippedWithoutIqamah: List<Prayer>
)

/**
 * Pure, timezone-aware calculator for Mosque Silent Screen windows.
 * Uses ZonedDateTime throughout so DST shifts, half-hour zones, and midnight crossings work cleanly.
 */
object SilentScreenCalculator {

    fun canEnableDailySilent(anchor: SilentAnchor, iqamahMinutes: Int): Boolean {
        return anchor == SilentAnchor.ADHAN || iqamahMinutes > 0
    }

    /**
     * Copies [source] anchor, lead, and duration to all 5 prayers.
     * Copies `enabled` to every prayer where it is valid; for `IQAMAH`-anchored prayers without an
     * iqamah delay, leaves `enabled = false` and reports them in [CopyPrayerSilentResult.skippedWithoutIqamah].
     */
    fun copySettingsToAllPrayers(
        source: PrayerSilentConfig,
        iqamahOffsets: Map<Prayer, Int>
    ): CopyPrayerSilentResult {
        val clean = source.sanitized()
        val updated = linkedMapOf<Prayer, PrayerSilentConfig>()
        val skipped = mutableListOf<Prayer>()
        var enabledCount = 0

        for (prayer in Prayer.entries) {
            val iqamahMins = iqamahOffsets[prayer] ?: 0
            val canEnable = canEnableDailySilent(clean.anchor, iqamahMins)
            val effectiveEnabled = if (clean.enabled && !canEnable) {
                skipped.add(prayer)
                false
            } else {
                clean.enabled
            }
            if (effectiveEnabled) enabledCount++
            updated[prayer] = clean.copy(enabled = effectiveEnabled)
        }

        return CopyPrayerSilentResult(
            updatedPrayers = updated,
            enabledCount = enabledCount,
            skippedWithoutIqamah = skipped
        )
    }

    /**
     * Computes merged silent-screen windows for a single [date].
     */
    fun silentScreenWindows(
        date: LocalDate,
        prayerConfig: PrayerConfig,
        silentConfig: SilentScreenConfig,
        jumuahMode: JumuahMode,
        jumuahTimes: List<String>,
        iqamahOffsets: Map<Prayer, Int>,
        events: List<PrayerEvent> = PrayerCalculationEngine.getDayPrayerEvents(date, prayerConfig)
    ): List<SilentWindow> {
        val clean = silentConfig.sanitized()
        val raw = mutableListOf<SilentWindow>()
        val isFriday = date.dayOfWeek == DayOfWeek.FRIDAY

        // 1. Jumu'ah windows (Fridays only, effective only when at least one sitting is configured)
        val configuredSittings = jumuahTimes.filter { it.isNotBlank() }.take(3)
        if (isFriday && clean.jumuahEnabled && configuredSittings.isNotEmpty()) {
            val zuhrEvent = events.firstOrNull { it.prayer == Prayer.ZUHUR }
            if (zuhrEvent != null) {
                val sittings = JumuahCalculator.resolveSittings(
                    date = date,
                    config = prayerConfig,
                    jumuahMode = jumuahMode,
                    jumuahTimes = configuredSittings,
                    calculatedZuhr = zuhrEvent.at
                )
                for (sittingStart in sittings) {
                    val start = sittingStart.minusMinutes(clean.jumuahLeadMinutes.toLong())
                    val end = sittingStart.plusMinutes(clean.jumuahDurationMinutes.toLong())
                    if (end.isAfter(start)) {
                        raw.add(SilentWindow(start = start, end = end, kind = SilentWindowKind.JUMUAH))
                    }
                }
            }
        }

        // 2. Daily prayer windows (Friday Zuhr is handled by Jumu'ah only)
        for (event in events) {
            if (isFriday && event.prayer == Prayer.ZUHUR) continue
            val cfg = clean.forPrayer(event.prayer).sanitized()
            if (!cfg.enabled) continue

            val anchorTime = when (cfg.anchor) {
                SilentAnchor.IQAMAH -> {
                    val iqamahMins = iqamahOffsets[event.prayer] ?: 0
                    if (iqamahMins <= 0) continue
                    event.at.plusMinutes(iqamahMins.toLong())
                }
                SilentAnchor.ADHAN -> event.at
            }

            val start = anchorTime.minusMinutes(cfg.leadMinutes.toLong())
            val end = anchorTime.plusMinutes(cfg.durationMinutes.toLong())
            if (end.isAfter(start)) {
                raw.add(SilentWindow(start = start, end = end, kind = SilentWindowKind.PRAYER))
            }
        }

        return mergeWindows(raw)
    }

    /**
     * Computes merged silent-screen windows across yesterday, today, and tomorrow in the mosque timezone
     * so windows crossing midnight (e.g. late Isya or early Subuh lead) remain seamless.
     */
    fun silentScreenWindowsAround(
        centerDate: LocalDate,
        prayerConfig: PrayerConfig,
        silentConfig: SilentScreenConfig,
        jumuahMode: JumuahMode,
        jumuahTimes: List<String>,
        iqamahOffsets: Map<Prayer, Int>
    ): List<SilentWindow> {
        val all = mutableListOf<SilentWindow>()
        for (offset in -1L..1L) {
            val d = centerDate.plusDays(offset)
            all.addAll(
                silentScreenWindows(
                    date = d,
                    prayerConfig = prayerConfig,
                    silentConfig = silentConfig,
                    jumuahMode = jumuahMode,
                    jumuahTimes = jumuahTimes,
                    iqamahOffsets = iqamahOffsets
                )
            )
        }
        return mergeWindows(all)
    }

    /**
     * Evaluates whether the silent screen should be active at [now] and returns the active kind & end time.
     * Manual override takes priority over all scheduled windows.
     */
    fun activeSilentState(
        now: ZonedDateTime,
        windows: List<SilentWindow>,
        manualUntil: ZonedDateTime?
    ): ActiveSilentState? {
        if (manualUntil != null && now.isBefore(manualUntil)) {
            return ActiveSilentState(kind = SilentWindowKind.MANUAL, until = manualUntil)
        }

        val matching = windows.filter { !now.isBefore(it.start) && now.isBefore(it.end) }
        if (matching.isEmpty()) return null

        val preferred = matching.firstOrNull { it.kind == SilentWindowKind.JUMUAH } ?: matching.first()
        val latestEnd = matching.maxOf { it.end }
        return ActiveSilentState(kind = preferred.kind, until = latestEnd)
    }

    fun isSilentScreenActive(
        now: ZonedDateTime,
        windows: List<SilentWindow>,
        manualUntil: ZonedDateTime?
    ): Boolean = activeSilentState(now, windows, manualUntil) != null

    fun manualUntilZoned(epochMillis: Long, zoneId: ZoneId): ZonedDateTime? {
        if (epochMillis <= 0L) return null
        return Instant.ofEpochMilli(epochMillis).atZone(zoneId)
    }

    internal fun mergeWindows(windows: List<SilentWindow>): List<SilentWindow> {
        if (windows.size <= 1) return windows.sortedBy { it.start }
        val sorted = windows.sortedWith(compareBy<SilentWindow> { it.start }.thenBy { it.end })
        val merged = mutableListOf<SilentWindow>()
        var current = sorted.first()

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (!next.start.isAfter(current.end)) {
                val combinedEnd = if (next.end.isAfter(current.end)) next.end else current.end
                val combinedKind = if (current.kind == SilentWindowKind.JUMUAH || next.kind == SilentWindowKind.JUMUAH) {
                    SilentWindowKind.JUMUAH
                } else {
                    current.kind
                }
                current = SilentWindow(start = current.start, end = combinedEnd, kind = combinedKind)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }
}
