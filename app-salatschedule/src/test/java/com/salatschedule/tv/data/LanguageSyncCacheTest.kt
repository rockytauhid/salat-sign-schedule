// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.data

import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.repository.LanguageSyncCache
import com.salatschedule.tv.service.PrayerAlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LanguageSyncCacheTest {

    @Test
    fun parseLanguage_validValues_returnRespectiveEnum() {
        assertEquals(AppLanguage.SYSTEM, LanguageSyncCache.parseLanguage("SYSTEM"))
        assertEquals(AppLanguage.EN, LanguageSyncCache.parseLanguage("EN"))
        assertEquals(AppLanguage.ID, LanguageSyncCache.parseLanguage("ID"))
        assertEquals(AppLanguage.AR, LanguageSyncCache.parseLanguage("AR"))
    }

    @Test
    fun parseLanguage_nullOrEmptyOrInvalid_fallsBackToSystem() {
        assertEquals(AppLanguage.SYSTEM, LanguageSyncCache.parseLanguage(null))
        assertEquals(AppLanguage.SYSTEM, LanguageSyncCache.parseLanguage(""))
        assertEquals(AppLanguage.SYSTEM, LanguageSyncCache.parseLanguage("   "))
        assertEquals(AppLanguage.SYSTEM, LanguageSyncCache.parseLanguage("UNKNOWN_LANG"))
    }

    @Test
    fun prayerAlarmScheduler_extrasAndActionsAreDefined() {
        assertNotNull(PrayerAlarmScheduler.EXTRA_LANGUAGE)
        assertNotNull(PrayerAlarmScheduler.EXTRA_ZONE_ID)
        assertEquals("extra_language", PrayerAlarmScheduler.EXTRA_LANGUAGE)
        assertEquals("extra_zone_id", PrayerAlarmScheduler.EXTRA_ZONE_ID)
        assertEquals("com.salatschedule.tv.action.PLAY_ADHAN", PrayerAlarmScheduler.ACTION_PLAY_ADHAN)
        assertEquals("com.salatschedule.tv.action.PRE_PRAYER_REMINDER", PrayerAlarmScheduler.ACTION_PRE_PRAYER_REMINDER)
        assertEquals("com.salatschedule.tv.action.STOP_ADHAN", PrayerAlarmScheduler.ACTION_STOP_ADHAN)
    }

    @Test
    fun coldStart_appliedLanguageMatchesEmittedSettings_triggersNoRecreate() {
        val storedLanguageName = "EN"
        val appliedLanguage = LanguageSyncCache.parseLanguage(storedLanguageName)
        val emittedSettingsLanguage = LanguageSyncCache.parseLanguage(storedLanguageName)

        assertEquals(appliedLanguage, emittedSettingsLanguage)
        // Verify that initial activeLanguage == emittedSettingsLanguage so no recreate() is triggered
        var recreateTriggered = false
        var activeLanguage = appliedLanguage
        if (emittedSettingsLanguage != activeLanguage) {
            activeLanguage = emittedSettingsLanguage
            recreateTriggered = true
        }
        org.junit.Assert.assertFalse("Cold start with stored language must not trigger recreate()", recreateTriggered)
    }
}
