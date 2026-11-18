// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.service

import com.salatsign.tv.R
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.ui.common.labelRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PrayerNotificationLabelTest {

    @Test
    fun prayerEnum_onlyTransfersEnumNameInAlarmExtras() {
        for (prayer in Prayer.entries) {
            val serializedName = prayer.name
            val parsedPrayer = Prayer.valueOf(serializedName)
            assertEquals(prayer, parsedPrayer)
        }
    }

    @Test
    fun prayerLabels_resolveToStringResources() {
        assertEquals(R.string.prayer_subuh, Prayer.SUBUH.labelRes())
        assertEquals(R.string.prayer_zuhur, Prayer.ZUHUR.labelRes(isFriday = false))
        assertEquals(R.string.prayer_jumuah, Prayer.ZUHUR.labelRes(isFriday = true))
        assertEquals(R.string.prayer_ashr, Prayer.ASHR.labelRes())
        assertEquals(R.string.prayer_magrib, Prayer.MAGRIB.labelRes())
        assertEquals(R.string.prayer_isya, Prayer.ISYA.labelRes())
    }

    @Test
    fun notificationChannelResources_existAndAreDistinct() {
        assertNotNull(R.string.notif_channel_adhan_name)
        assertNotNull(R.string.notif_channel_reminder_name)
        assertNotNull(R.string.notif_reminder_title)
        assertNotNull(R.string.notif_reminder_text)
        assertNotNull(R.string.notif_adhan_title)
        assertNotNull(R.string.notif_adhan_text)
    }
}
