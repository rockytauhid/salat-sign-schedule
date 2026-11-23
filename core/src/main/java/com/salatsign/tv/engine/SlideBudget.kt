// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

import com.salatsign.tv.data.model.AnnouncementSlide

/**
 * Identifiers for optional built-in slide sources in SalatSign.
 * Adding a new built-in slide only requires adding its ID and registering a [BuiltInSlideRegistration].
 */
enum class BuiltInSlideId {
    ISLAMIC_CALENDAR,
    QURAN_VERSE
}

/**
 * Registration record for an optional built-in slide source.
 *
 * @property id Stable identifier for the built-in slide.
 * @property enabled Whether the admin enabled this slide (consumes 1 rotation slot when true).
 * @property hasContent Whether the slide currently has content to display (if false, skipped at runtime while still occupying its slot).
 * @property displayName Human-readable label for over-limit warnings and slot breakdown.
 */
data class BuiltInSlideRegistration(
    val id: BuiltInSlideId,
    val enabled: Boolean,
    val hasContent: Boolean = true,
    val displayName: String = ""
)

sealed interface RotationSlideItem {
    data class Announcement(val slide: AnnouncementSlide) : RotationSlideItem
    data class BuiltIn(val id: BuiltInSlideId) : RotationSlideItem
}

/**
 * Pure-Kotlin 6-slide rotation budget rules shared across TV Settings, Slideshow ViewModel,
 * and Local Phone Controller API (no Android types).
 */
object SlideBudget {
    const val MAX_ROTATION_SLIDES: Int = 6
    const val MAX_STORED_ANNOUNCEMENTS: Int = 30

    fun usedSlots(enabledAnnouncements: Int, enabledBuiltIns: Int): Int {
        return enabledAnnouncements.coerceAtLeast(0) + enabledBuiltIns.coerceAtLeast(0)
    }

    fun canEnableBuiltIn(enabledAnnouncements: Int, enabledBuiltIns: Int): Boolean {
        return usedSlots(enabledAnnouncements, enabledBuiltIns) + 1 <= MAX_ROTATION_SLIDES
    }

    fun canEnableOrAddAnnouncement(enabledAnnouncements: Int, enabledBuiltIns: Int): Boolean {
        return usedSlots(enabledAnnouncements, enabledBuiltIns) + 1 <= MAX_ROTATION_SLIDES
    }

    fun allowedAnnouncements(enabledBuiltIns: Int): Int {
        return (MAX_ROTATION_SLIDES - enabledBuiltIns.coerceAtLeast(0)).coerceAtLeast(0)
    }
}

data class SlideshowRotationPlan(
    val runtimeItems: List<RotationSlideItem>,
    val allocatedCount: Int,
    val totalEnabledCount: Int,
    val enabledAnnouncementCount: Int,
    val enabledBuiltInCount: Int,
    val hiddenAnnouncements: List<AnnouncementSlide>,
    val hiddenBuiltIns: List<BuiltInSlideRegistration>
) {
    val isOverLimit: Boolean
        get() = hiddenAnnouncements.isNotEmpty() || hiddenBuiltIns.isNotEmpty()

    val hiddenCount: Int
        get() = hiddenAnnouncements.size + hiddenBuiltIns.size
}

/**
 * Single source of truth for SalatSign slideshow rotation order and the 6-slide cap:
 * Prayer schedule screen (outside this list) -> enabled announcements (by sortOrder)
 * -> enabled built-in slides in registration order (Islamic Calendar, then Quran Verse).
 *
 * Legacy installs with > 6 enabled slides are never deleted; only the first 6 slots run,
 * and excess items are reported in [SlideshowRotationPlan.hiddenAnnouncements] / [SlideshowRotationPlan.hiddenBuiltIns].
 */
object SlideshowRotationBuilder {

    fun build(
        announcements: List<AnnouncementSlide>,
        builtIns: List<BuiltInSlideRegistration>
    ): SlideshowRotationPlan {
        val enabledAnnouncements = announcements
            .filter { it.enabled }
            .sortedBy { it.sortOrder }
        val enabledBuiltIns = builtIns.filter { it.enabled }

        val runtimeItems = mutableListOf<RotationSlideItem>()
        val hiddenAnnouncements = mutableListOf<AnnouncementSlide>()
        val hiddenBuiltIns = mutableListOf<BuiltInSlideRegistration>()
        var slotsUsed = 0

        for (slide in enabledAnnouncements) {
            if (slotsUsed < SlideBudget.MAX_ROTATION_SLIDES) {
                runtimeItems.add(RotationSlideItem.Announcement(slide))
                slotsUsed++
            } else {
                hiddenAnnouncements.add(slide)
            }
        }

        for (builtIn in enabledBuiltIns) {
            if (slotsUsed < SlideBudget.MAX_ROTATION_SLIDES) {
                // Built-in occupies 1 slot while enabled; skip at runtime if it has nothing to show
                slotsUsed++
                if (builtIn.hasContent) {
                    runtimeItems.add(RotationSlideItem.BuiltIn(builtIn.id))
                }
            } else {
                hiddenBuiltIns.add(builtIn)
            }
        }

        return SlideshowRotationPlan(
            runtimeItems = runtimeItems,
            allocatedCount = slotsUsed,
            totalEnabledCount = enabledAnnouncements.size + enabledBuiltIns.size,
            enabledAnnouncementCount = enabledAnnouncements.size,
            enabledBuiltInCount = enabledBuiltIns.size,
            hiddenAnnouncements = hiddenAnnouncements,
            hiddenBuiltIns = hiddenBuiltIns
        )
    }
}
