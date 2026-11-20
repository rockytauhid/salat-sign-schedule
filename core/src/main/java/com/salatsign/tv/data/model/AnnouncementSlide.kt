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
data class AnnouncementSlide(
    val id: String,
    val type: SlideType = SlideType.IMAGE,
    val title: String = "",
    val body: String = "",
    val imageFile: String? = null,
    val durationSeconds: Int = 10,
    val enabled: Boolean = true,
    val startDate: String? = null,
    val endDate: String? = null,
    val sortOrder: Int = 0
)

@Serializable
data class SlideContainer(
    val revision: Int = 0,
    val slides: List<AnnouncementSlide> = emptyList()
)
