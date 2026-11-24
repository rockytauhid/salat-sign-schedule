// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.service

import com.salatsign.core.tv.R as CoreR
import com.salatschedule.tv.R
import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.ui.common.labelRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhanAudioResolverTest {

    @Test
    fun resolveRawResource_beepType_resolvesBeepRaw() {
        val res = AdhanAudioResolver.resolveRawResource(AdhanAudioType.BEEP)
        assertEquals(R.raw.adhan_beep, res)
    }

    @Test
    fun resolveRawResource_legacyDefaultTypes_resolveBeepRaw() {
        assertEquals(R.raw.adhan_beep, AdhanAudioResolver.resolveRawResource(AdhanAudioType.DEFAULT_SUBUH))
        assertEquals(R.raw.adhan_beep, AdhanAudioResolver.resolveRawResource(AdhanAudioType.DEFAULT_STANDARD))
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

    @Test
    fun adhanAudioOption_builtInDefaults_containNoHardcodedDisplayStrings() {
        assertTrue(AdhanAudioOption.Beep.displayName.isEmpty())
        assertTrue(AdhanAudioOption.DefaultStandard.displayName.isEmpty())
        assertTrue(AdhanAudioOption.DefaultSubuh.displayName.isEmpty())
        assertTrue(AdhanAudioOption.Silent.displayName.isEmpty())
    }

    @Test
    fun adhanAudioOption_labelsMapToCorrectStringResources() {
        assertEquals(CoreR.string.adhan_beep_beep, AdhanAudioOption.Beep.labelRes())
        assertEquals(CoreR.string.adhan_beep_beep, AdhanAudioOption.DefaultStandard.labelRes())
        assertEquals(CoreR.string.adhan_beep_beep, AdhanAudioOption.DefaultSubuh.labelRes())
        assertEquals(CoreR.string.adhan_silent, AdhanAudioOption.Silent.labelRes())
        val customOption = AdhanAudioOption(type = AdhanAudioType.CUSTOM_FILE, displayName = "custom.mp3")
        assertEquals(CoreR.string.adhan_custom_file, customOption.labelRes())
    }
}
