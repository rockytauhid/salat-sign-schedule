// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.model

enum class AdhanAudioType {
    DEFAULT_SUBUH,
    DEFAULT_STANDARD,
    CUSTOM_FILE,
    SILENT
}

data class AdhanAudioOption(
    val type: AdhanAudioType = AdhanAudioType.SILENT,
    val displayName: String = "",
    val customUri: String? = null
) {
    companion object {
        val DefaultStandard = AdhanAudioOption(
            type = AdhanAudioType.DEFAULT_STANDARD,
            displayName = ""
        )
        val DefaultSubuh = AdhanAudioOption(
            type = AdhanAudioType.DEFAULT_SUBUH,
            displayName = ""
        )
        val Silent = AdhanAudioOption(
            type = AdhanAudioType.SILENT,
            displayName = ""
        )
    }
}
