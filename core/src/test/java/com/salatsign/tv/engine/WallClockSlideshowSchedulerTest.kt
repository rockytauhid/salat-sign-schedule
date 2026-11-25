// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WallClockSlideshowSchedulerTest {

    @Test
    fun resolve_returnsNullForEmptySlots() {
        assertNull(WallClockSlideshowScheduler.resolve(1_700_000_000L, emptyList()))
    }

    @Test
    fun resolve_mapsEpochSecondsDeterministicallyAcrossCycleBoundaries() {
        val slots = listOf(
            WallClockSlot("schedule", 60),
            WallClockSlot("announcement:a1", 15),
            WallClockSlot("builtin:QURAN_VERSE", 25)
        )
        // Total cycle = 100s
        val atStart = WallClockSlideshowScheduler.resolve(1_700_000_000L, slots)!!
        assertEquals(17_000_000L, atStart.cycleIndex)
        assertEquals(0, atStart.slotIndex)
        assertEquals("schedule", atStart.slotId)
        assertEquals(0, atStart.elapsedInSlotSeconds)
        assertEquals(60, atStart.remainingInSlotSeconds)

        val atAnnouncement = WallClockSlideshowScheduler.resolve(1_700_000_060L, slots)!!
        assertEquals(17_000_000L, atAnnouncement.cycleIndex)
        assertEquals(1, atAnnouncement.slotIndex)
        assertEquals("announcement:a1", atAnnouncement.slotId)
        assertEquals(0, atAnnouncement.elapsedInSlotSeconds)
        assertEquals(15, atAnnouncement.remainingInSlotSeconds)

        val atQuranEnd = WallClockSlideshowScheduler.resolve(1_700_000_099L, slots)!!
        assertEquals(17_000_000L, atQuranEnd.cycleIndex)
        assertEquals(2, atQuranEnd.slotIndex)
        assertEquals("builtin:QURAN_VERSE", atQuranEnd.slotId)
        assertEquals(24, atQuranEnd.elapsedInSlotSeconds)
        assertEquals(1, atQuranEnd.remainingInSlotSeconds)

        val nextCycle = WallClockSlideshowScheduler.resolve(1_700_000_100L, slots)!!
        assertEquals(17_000_001L, nextCycle.cycleIndex)
        assertEquals(0, nextCycle.slotIndex)
        assertEquals("schedule", nextCycle.slotId)
    }

    @Test
    fun resolve_twoTvsWithSameEpochResolveIdenticalSlot() {
        val slots = listOf(
            WallClockSlot("schedule", 45),
            WallClockSlot("announcement:slide-1", 12),
            WallClockSlot("builtin:ISLAMIC_CALENDAR", 18),
            WallClockSlot("builtin:QURAN_VERSE", WallClockSlideshowScheduler.resolveQuranSlotDurationSeconds(0))
        )
        val tvA = WallClockSlideshowScheduler.resolve(1_795_567_890L, slots)
        val tvB = WallClockSlideshowScheduler.resolve(1_795_567_890L, slots)
        assertEquals(tvA, tvB)
    }

    @Test
    fun resolveQuranSlotDurationSeconds_usesFixedDefaultForAutoAndClampsManual() {
        assertEquals(
            WallClockSlideshowScheduler.DEFAULT_AUTO_QURAN_SLOT_SECONDS,
            WallClockSlideshowScheduler.resolveQuranSlotDurationSeconds(0)
        )
        assertEquals(
            WallClockSlideshowScheduler.DEFAULT_AUTO_QURAN_SLOT_SECONDS,
            WallClockSlideshowScheduler.resolveQuranSlotDurationSeconds(-5)
        )
        assertEquals(10, WallClockSlideshowScheduler.resolveQuranSlotDurationSeconds(5))
        assertEquals(30, WallClockSlideshowScheduler.resolveQuranSlotDurationSeconds(30))
        assertEquals(90, WallClockSlideshowScheduler.resolveQuranSlotDurationSeconds(120))
    }
}
