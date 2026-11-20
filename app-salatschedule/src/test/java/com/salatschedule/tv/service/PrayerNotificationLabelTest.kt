// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.service

import com.salatsign.core.tv.R as CoreR
import com.salatschedule.tv.R
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
        assertEquals(CoreR.string.prayer_subuh, Prayer.SUBUH.labelRes())
        assertEquals(CoreR.string.prayer_zuhur, Prayer.ZUHUR.labelRes(isFriday = false))
        assertEquals(CoreR.string.prayer_jumuah, Prayer.ZUHUR.labelRes(isFriday = true))
        assertEquals(CoreR.string.prayer_ashr, Prayer.ASHR.labelRes())
        assertEquals(CoreR.string.prayer_magrib, Prayer.MAGRIB.labelRes())
        assertEquals(CoreR.string.prayer_isya, Prayer.ISYA.labelRes())
    }

    @Test
    fun notificationChannelResources_existAndAreDistinct() {
        assertNotNull(CoreR.string.notif_channel_adhan_name)
        assertNotNull(CoreR.string.notif_channel_reminder_name)
        assertNotNull(CoreR.string.notif_reminder_title)
        assertNotNull(CoreR.string.notif_reminder_text)
        assertNotNull(CoreR.string.notif_adhan_title)
        assertNotNull(CoreR.string.notif_adhan_text)
    }
}
