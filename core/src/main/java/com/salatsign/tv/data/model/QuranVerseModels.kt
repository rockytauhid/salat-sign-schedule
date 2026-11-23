// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.model

import kotlinx.serialization.Serializable

@Serializable
data class QuranTextEntry(
    val text: String
)

@Serializable
data class SurahMeta(
    val arabic: String,
    val transliteration: String
)

data class QuranVerseItem(
    val reference: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahArabicName: String,
    val surahTransliteration: String,
    val arabicText: String,
    val translationText: String
)

object QuranSlideDefaults {
    const val DURATION_AUTO: Int = 0
    const val MIN_FIXED_DURATION_SECONDS: Int = 10
    const val MAX_FIXED_DURATION_SECONDS: Int = 90
    const val MIN_AUTO_DURATION_SECONDS: Int = 15
    const val MAX_AUTO_DURATION_SECONDS: Int = 60
    val DURATION_PRESETS: List<Int> = listOf(0, 15, 20, 30, 45, 60, 90)
}
