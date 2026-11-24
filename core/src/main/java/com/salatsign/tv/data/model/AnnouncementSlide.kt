// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class SlideType {
    IMAGE,
    TEXT
}

@Serializable
enum class SlideTextAlign {
    CENTER,
    LEFT,
    RIGHT
}

@Serializable
enum class SlideFontSize {
    SMALL,
    MEDIUM,
    LARGE
}

@Serializable
data class SlideTextLine(
    val text: String = "",
    val align: SlideTextAlign = SlideTextAlign.CENTER,
    val fontSize: SlideFontSize = SlideFontSize.MEDIUM
)

@Serializable
data class AnnouncementSlide(
    val id: String,
    val type: SlideType = SlideType.IMAGE,
    val title: String = "",
    val body: String = "",
    val lines: List<SlideTextLine> = emptyList(),
    val imageFile: String? = null,
    val durationSeconds: Int = 10,
    val enabled: Boolean = true,
    val textAlign: SlideTextAlign = SlideTextAlign.CENTER,
    val startDate: String? = null,
    val endDate: String? = null,
    val sortOrder: Int = 0
) {
    /**
     * Returns per-row styled lines for rendering/editing, falling back cleanly
     * to legacy [body] + [textAlign] for slides saved before per-row alignment existed.
     */
    fun effectiveLines(): List<SlideTextLine> {
        if (lines.isNotEmpty()) return lines
        if (body.isBlank()) return emptyList()
        return body.split("\n").map { SlideTextLine(text = it, align = textAlign) }
    }
}

@Serializable
data class SlideContainer(
    val revision: Int = 0,
    val slides: List<AnnouncementSlide> = emptyList()
)
