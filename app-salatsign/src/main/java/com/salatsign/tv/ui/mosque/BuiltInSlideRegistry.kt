// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.mosque

import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.engine.BuiltInSlideId
import com.salatsign.tv.engine.BuiltInSlideRegistration

/**
 * Single registration point for optional built-in slides in SalatSign.
 * Keeps slideshow loop, settings budget UI, and Phone Controller API in complete agreement.
 */
object BuiltInSlideRegistry {
    fun registrationsFor(
        settings: AppSettings,
        islamicCalendarHasContent: Boolean = true,
        quranVerseHasContent: Boolean = true
    ): List<BuiltInSlideRegistration> = listOf(
        BuiltInSlideRegistration(
            id = BuiltInSlideId.ISLAMIC_CALENDAR,
            enabled = settings.islamicCalendarSlideEnabled,
            hasContent = islamicCalendarHasContent,
            displayName = "Islamic Calendar"
        ),
        BuiltInSlideRegistration(
            id = BuiltInSlideId.QURAN_VERSE,
            enabled = settings.quranVerseEnabled,
            hasContent = quranVerseHasContent,
            displayName = "Quran Verse"
        )
    )
}
