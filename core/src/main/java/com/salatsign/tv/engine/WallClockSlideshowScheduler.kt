// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

/**
 * A single slot in the deterministic wall-clock slideshow cycle.
 */
data class WallClockSlot(
    val slotId: String,
    val durationSeconds: Int
)

/**
 * The resolved active slot at a given Unix epoch second.
 */
data class WallClockResolution(
    val cycleIndex: Long,
    val slotIndex: Int,
    val slotId: String,
    val durationSeconds: Int,
    val elapsedInSlotSeconds: Int,
    val remainingInSlotSeconds: Int
)

/**
 * Pure, offline wall-clock slideshow scheduler (Decision D25).
 *
 * Maps `epochSeconds % totalCycleSeconds` to a deterministic slide slot so all TVs
 * sharing the same synced config and NTP wall clock transition slides together within
 * ~1 second with zero peer network traffic, zero leader election, and offline resilience.
 *
 * Quran verse selection remains local per TV (each TV draws from its own shuffle bag)
 * while occupying a fixed wall-clock slot duration so TVs stay aligned on transitions.
 */
object WallClockSlideshowScheduler {

    /**
     * Fixed wall-clock slot duration used when Quran Verse duration is set to Auto (`<= 0`).
     * Keeping a constant slot width ensures all TVs share the same `totalCycleSeconds`
     * even when each TV displays a different verse length.
     */
    const val DEFAULT_AUTO_QURAN_SLOT_SECONDS = 25

    fun resolveQuranSlotDurationSeconds(configuredDurationSeconds: Int): Int {
        return if (configuredDurationSeconds <= 0) {
            DEFAULT_AUTO_QURAN_SLOT_SECONDS
        } else {
            configuredDurationSeconds.coerceIn(10, 90)
        }
    }

    fun resolve(
        epochSeconds: Long,
        slots: List<WallClockSlot>
    ): WallClockResolution? {
        if (slots.isEmpty()) return null
        val sanitizedDurations = slots.map { it.durationSeconds.coerceAtLeast(1) }
        val totalCycleSeconds = sanitizedDurations.sumOf { it.toLong() }
        if (totalCycleSeconds <= 0L) return null

        val safeEpoch = epochSeconds.coerceAtLeast(0L)
        val cycleIndex = safeEpoch / totalCycleSeconds
        var offset = (safeEpoch % totalCycleSeconds).toInt()

        for (i in slots.indices) {
            val dur = sanitizedDurations[i]
            if (offset < dur) {
                return WallClockResolution(
                    cycleIndex = cycleIndex,
                    slotIndex = i,
                    slotId = slots[i].slotId,
                    durationSeconds = dur,
                    elapsedInSlotSeconds = offset,
                    remainingInSlotSeconds = dur - offset
                )
            }
            offset -= dur
        }

        // Unreachable for positive durations, but safe fallback to slot 0.
        val firstDur = sanitizedDurations[0]
        return WallClockResolution(
            cycleIndex = cycleIndex,
            slotIndex = 0,
            slotId = slots[0].slotId,
            durationSeconds = firstDur,
            elapsedInSlotSeconds = 0,
            remainingInSlotSeconds = firstDur
        )
    }
}
