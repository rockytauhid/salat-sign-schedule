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
