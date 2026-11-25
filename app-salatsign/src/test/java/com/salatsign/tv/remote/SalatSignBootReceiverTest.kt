// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.remote

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SalatSignBootReceiverTest {

    @Test
    fun allowedActions_onlyPermitsBootAndPackageReplace() {
        val allowed = SalatSignBootReceiver.ALLOWED_ACTIONS
        assertEquals(2, allowed.size)
        assertTrue(Intent.ACTION_BOOT_COMPLETED in allowed)
        assertTrue(Intent.ACTION_MY_PACKAGE_REPLACED in allowed)

        assertFalse("com.evil.SPOOFED_BOOT" in allowed)
        assertFalse(Intent.ACTION_VIEW in allowed)
    }
}
