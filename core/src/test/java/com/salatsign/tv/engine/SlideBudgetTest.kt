// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.SlideType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlideBudgetTest {

    private fun slide(id: String, enabled: Boolean = true, sortOrder: Int = 0): AnnouncementSlide =
        AnnouncementSlide(
            id = id,
            type = SlideType.TEXT,
            title = "Slide $id",
            body = "Body $id",
            enabled = enabled,
            sortOrder = sortOrder
        )

    @Test
    fun slideBudget_allCombinationsOfBuiltInsAndAnnouncements() {
        for (builtIns in 0..2) {
            assertEquals(6 - builtIns, SlideBudget.allowedAnnouncements(builtIns))
            for (announcements in 0..8) {
                val used = SlideBudget.usedSlots(announcements, builtIns)
                assertEquals(announcements + builtIns, used)
                val expectedCanAdd = (used + 1) <= 6
                assertEquals(expectedCanAdd, SlideBudget.canEnableOrAddAnnouncement(announcements, builtIns))
                assertEquals(expectedCanAdd, SlideBudget.canEnableBuiltIn(announcements, builtIns))
            }
        }
    }

    @Test
    fun enablingBuiltIn_refusedAtLimit_allowedAfterDisablingAnnouncement() {
        // 6 enabled announcements, 0 built-ins -> cannot enable built-in
        assertFalse(SlideBudget.canEnableBuiltIn(enabledAnnouncements = 6, enabledBuiltIns = 0))
        // Disable 1 announcement -> 5 enabled announcements -> can enable 1 built-in
        assertTrue(SlideBudget.canEnableBuiltIn(enabledAnnouncements = 5, enabledBuiltIns = 0))
        // With 1 built-in enabled (used = 6) -> cannot enable 2nd built-in or add announcement
        assertFalse(SlideBudget.canEnableBuiltIn(enabledAnnouncements = 5, enabledBuiltIns = 1))
        assertFalse(SlideBudget.canEnableOrAddAnnouncement(enabledAnnouncements = 5, enabledBuiltIns = 1))
        // With both built-ins enabled, max 4 announcements allowed
        assertEquals(4, SlideBudget.allowedAnnouncements(enabledBuiltIns = 2))
        assertTrue(SlideBudget.canEnableOrAddAnnouncement(enabledAnnouncements = 3, enabledBuiltIns = 2))
        assertFalse(SlideBudget.canEnableOrAddAnnouncement(enabledAnnouncements = 4, enabledBuiltIns = 2))
    }

    @Test
    fun disabledAnnouncements_doNotCountTowardBudget() {
        val slides = listOf(
            slide("a1", enabled = true, sortOrder = 1),
            slide("a2", enabled = true, sortOrder = 2),
            slide("d1", enabled = false, sortOrder = 3),
            slide("d2", enabled = false, sortOrder = 4),
            slide("d3", enabled = false, sortOrder = 5),
            slide("d4", enabled = false, sortOrder = 6),
            slide("d5", enabled = false, sortOrder = 7)
        )
        val builtIns = listOf(
            BuiltInSlideRegistration(BuiltInSlideId.ISLAMIC_CALENDAR, enabled = true, hasContent = true),
            BuiltInSlideRegistration(BuiltInSlideId.QURAN_VERSE, enabled = true, hasContent = true)
        )
        val plan = SlideshowRotationBuilder.build(slides, builtIns)

        assertEquals(2, plan.enabledAnnouncementCount)
        assertEquals(2, plan.enabledBuiltInCount)
        assertEquals(4, plan.allocatedCount)
        assertFalse(plan.isOverLimit)
        assertEquals(4, plan.runtimeItems.size)
    }

    @Test
    fun rotationOrder_announcementsBySortOrder_thenIslamicCalendar_thenQuranVerse() {
        val slides = listOf(
            slide("s3", enabled = true, sortOrder = 30),
            slide("s1", enabled = true, sortOrder = 10),
            slide("s2", enabled = true, sortOrder = 20)
        )
        val builtIns = listOf(
            BuiltInSlideRegistration(BuiltInSlideId.ISLAMIC_CALENDAR, enabled = true, hasContent = true),
            BuiltInSlideRegistration(BuiltInSlideId.QURAN_VERSE, enabled = true, hasContent = true)
        )

        val plan = SlideshowRotationBuilder.build(slides, builtIns)
        assertEquals(5, plan.runtimeItems.size)
        assertEquals("s1", (plan.runtimeItems[0] as RotationSlideItem.Announcement).slide.id)
        assertEquals("s2", (plan.runtimeItems[1] as RotationSlideItem.Announcement).slide.id)
        assertEquals("s3", (plan.runtimeItems[2] as RotationSlideItem.Announcement).slide.id)
        assertEquals(BuiltInSlideId.ISLAMIC_CALENDAR, (plan.runtimeItems[3] as RotationSlideItem.BuiltIn).id)
        assertEquals(BuiltInSlideId.QURAN_VERSE, (plan.runtimeItems[4] as RotationSlideItem.BuiltIn).id)
    }

    @Test
    fun emptyBuiltIn_skippedAtRuntime_butStillConsumesItsSlot() {
        val slides = (1..5).map { slide("a$it", enabled = true, sortOrder = it) }
        val builtIns = listOf(
            BuiltInSlideRegistration(BuiltInSlideId.ISLAMIC_CALENDAR, enabled = true, hasContent = false, displayName = "Islamic Calendar"),
            BuiltInSlideRegistration(BuiltInSlideId.QURAN_VERSE, enabled = true, hasContent = true, displayName = "Quran Verse")
        )

        val plan = SlideshowRotationBuilder.build(slides, builtIns)
        // 5 announcements + 1 Islamic Calendar (empty, skipped at runtime) = 6 slots used; Quran Verse is pushed over limit!
        assertEquals(6, plan.allocatedCount)
        assertEquals(5, plan.runtimeItems.size) // Islamic calendar skipped at runtime
        assertTrue(plan.isOverLimit)
        assertEquals(1, plan.hiddenBuiltIns.size)
        assertEquals(BuiltInSlideId.QURAN_VERSE, plan.hiddenBuiltIns[0].id)
    }

    @Test
    fun legacyOverLimitData_neverDeletes_usesFirstSixSlotsAndListsHidden() {
        val slides = (1..8).map { slide("legacy$it", enabled = true, sortOrder = it) }
        val builtIns = listOf(
            BuiltInSlideRegistration(BuiltInSlideId.ISLAMIC_CALENDAR, enabled = true, hasContent = true, displayName = "Islamic Calendar")
        )

        val plan = SlideshowRotationBuilder.build(slides, builtIns)
        assertEquals(6, plan.allocatedCount)
        assertEquals(6, plan.runtimeItems.size)
        assertEquals(9, plan.totalEnabledCount)
        assertTrue(plan.isOverLimit)
        assertEquals(3, plan.hiddenCount)
        assertEquals(listOf("legacy7", "legacy8"), plan.hiddenAnnouncements.map { it.id })
        assertEquals(listOf(BuiltInSlideId.ISLAMIC_CALENDAR), plan.hiddenBuiltIns.map { it.id })
        // Original input list untouched
        assertEquals(8, slides.size)
    }
}
