// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.service

import android.content.Intent
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.engine.PrayerCalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmSchedulingAndReceiverTest {

    @Test
    fun bootAndTimeReceiver_allowsOnlyExpectedSystemActions() {
        val allowed = BootAndTimeReceiver.ALLOWED_ACTIONS
        assertEquals(4, allowed.size)
        assertTrue(Intent.ACTION_BOOT_COMPLETED in allowed)
        assertTrue(Intent.ACTION_TIME_CHANGED in allowed)
        assertTrue(Intent.ACTION_TIMEZONE_CHANGED in allowed)
        assertTrue(Intent.ACTION_MY_PACKAGE_REPLACED in allowed)

        assertFalse("com.evil.SPOOFED_BOOT" in allowed)
        assertFalse(Intent.ACTION_VIEW in allowed)
    }

    @Test
    fun prayerAlarmScheduler_usesDistinctRequestCodesAndActions() {
        assertNotEquals(PrayerAlarmScheduler.REQUEST_CODE_ADHAN, PrayerAlarmScheduler.REQUEST_CODE_REMINDER)
        assertNotEquals(PrayerAlarmScheduler.ACTION_PLAY_ADHAN, PrayerAlarmScheduler.ACTION_PRE_PRAYER_REMINDER)
        assertNotEquals(PrayerAlarmScheduler.ACTION_PLAY_ADHAN, PrayerAlarmScheduler.ACTION_STOP_ADHAN)
    }

    @Test
    fun nextPrayerAndReminderEvents_chainAcrossMidnightAndLeadBy15Minutes() {
        val zone = ZoneId.of("Australia/Sydney")
        val settings = AppSettings(
            latitude = -33.8688,
            longitude = 151.2093,
            zoneId = "Australia/Sydney"
        )
        val config = settings.prayerConfig()

        // Late night (23:50): next prayer must roll over to tomorrow's Subuh
        val lateNight = ZonedDateTime.of(2026, 6, 15, 23, 50, 0, 0, zone)
        val nextPrayer = PrayerCalculationEngine.nextPrayerEvent(lateNight, config)
        assertNotNull(nextPrayer)
        assertEquals(Prayer.SUBUH, nextPrayer!!.prayer)
        assertEquals(lateNight.toLocalDate().plusDays(1), nextPrayer.at.toLocalDate())

        val nextReminder = PrayerCalculationEngine.nextReminderEvent(lateNight, config)
        assertNotNull(nextReminder)
        assertEquals(Prayer.SUBUH, nextReminder!!.prayer)
        assertEquals(15L, Duration.between(nextReminder.reminderAt, nextPrayer.at).toMinutes())
    }

    @Test
    fun isAlarmStale_flagsAlarmsDelayedBeyondFiveMinutes() {
        val now = 1_700_000_000_000L
        assertFalse(PrayerCalculationEngine.isAlarmStale(scheduledAtEpochMillis = now - 60_000L, nowEpochMillis = now))
        assertFalse(PrayerCalculationEngine.isAlarmStale(scheduledAtEpochMillis = now - 5 * 60_000L, nowEpochMillis = now))
        assertTrue(PrayerCalculationEngine.isAlarmStale(scheduledAtEpochMillis = now - (5 * 60_000L + 1_000L), nowEpochMillis = now))
        // Future or missing timestamp is never stale
        assertFalse(PrayerCalculationEngine.isAlarmStale(scheduledAtEpochMillis = now + 10_000L, nowEpochMillis = now))
        assertFalse(PrayerCalculationEngine.isAlarmStale(scheduledAtEpochMillis = 0L, nowEpochMillis = now))
    }
}
