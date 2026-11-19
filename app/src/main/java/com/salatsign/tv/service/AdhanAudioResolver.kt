// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.service

import androidx.annotation.RawRes
import com.salatsign.tv.R
import com.salatsign.tv.data.model.AdhanAudioType

/**
 * Resolves bundled raw audio resources strictly based on the configured AdhanAudioType.
 */
object AdhanAudioResolver {

    /**
     * Maps an AdhanAudioType to its corresponding raw resource ID.
     * Returns null if the type is SILENT or CUSTOM_FILE.
     */
    @RawRes
    fun resolveRawResource(type: AdhanAudioType): Int? = when (type) {
        AdhanAudioType.DEFAULT_SUBUH -> R.raw.adhan_subuh
        AdhanAudioType.DEFAULT_STANDARD -> R.raw.adhan_default
        AdhanAudioType.CUSTOM_FILE, AdhanAudioType.SILENT -> null
    }
}
