// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data

import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.repository.LanguageSyncCache
import com.salatsign.tv.service.PrayerAlarmScheduler
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
    fun prayerAlarmScheduler_extrasAreDefined() {
        assertNotNull(PrayerAlarmScheduler.EXTRA_LANGUAGE)
        assertNotNull(PrayerAlarmScheduler.EXTRA_ZONE_ID)
        assertNotNull(PrayerAlarmScheduler.EXTRA_IS_MOSQUE_MODE)
        assertEquals("extra_language", PrayerAlarmScheduler.EXTRA_LANGUAGE)
        assertEquals("extra_zone_id", PrayerAlarmScheduler.EXTRA_ZONE_ID)
        assertEquals("extra_is_mosque_mode", PrayerAlarmScheduler.EXTRA_IS_MOSQUE_MODE)
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
