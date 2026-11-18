// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.service

import com.salatsign.tv.R
import com.salatsign.tv.data.model.AdhanAudioType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdhanAudioResolverTest {

    @Test
    fun resolveRawResource_subuhType_resolvesSubuhRaw() {
        val res = AdhanAudioResolver.resolveRawResource(AdhanAudioType.DEFAULT_SUBUH)
        assertEquals(R.raw.adhan_subuh, res)
    }

    @Test
    fun resolveRawResource_standardType_resolvesDefaultRaw() {
        val res = AdhanAudioResolver.resolveRawResource(AdhanAudioType.DEFAULT_STANDARD)
        assertEquals(R.raw.adhan_default, res)
    }

    @Test
    fun resolveRawResource_silentType_returnsNull() {
        val res = AdhanAudioResolver.resolveRawResource(AdhanAudioType.SILENT)
        assertNull(res)
    }

    @Test
    fun resolveRawResource_customFileType_returnsNull() {
        val res = AdhanAudioResolver.resolveRawResource(AdhanAudioType.CUSTOM_FILE)
        assertNull(res)
    }
}
