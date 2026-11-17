// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.model

import java.time.ZonedDateTime

data class PrayerEvent(
    val prayer: Prayer,
    val at: ZonedDateTime
)

data class PrayerReminderEvent(
    val prayer: Prayer,
    val prayerAt: ZonedDateTime,
    val reminderAt: ZonedDateTime
)
