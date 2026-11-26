// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data

import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.SlideType
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.PopularCityPresets
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.ui.common.labelRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataModelAndSettingsTest {

    @Test
    fun prayerEnum_hasFiveDailyPrayers() {
        assertEquals(5, Prayer.entries.size)
    }

    @Test
    fun prayerEnum_fromId_resolvesAccurately() {
        assertEquals(Prayer.SUBUH, Prayer.fromId("subuh"))
        assertEquals(Prayer.SUBUH, Prayer.fromId("SUBUH"))
        assertEquals(Prayer.ZUHUR, Prayer.fromId("zuhur"))
        assertEquals(Prayer.ASHR, Prayer.fromId("ashr"))
        assertEquals(Prayer.MAGRIB, Prayer.fromId("magrib"))
        assertEquals(Prayer.ISYA, Prayer.fromId("isya"))
        // Fallback for unknown
        assertEquals(Prayer.SUBUH, Prayer.fromId("unknown"))
    }

    @Test
    fun appSettings_defaults_areCorrect() {
        val settings = AppSettings()

        assertFalse("Setup must not be completed by default", settings.isSetupCompleted)
        assertEquals(ThemeMode.ALWAYS_DARK, settings.themeMode)
        assertEquals(MosqueIconPreset.DOME_CRESCENT, settings.mosqueIconPreset)
        assertEquals(MosqueFontPreset.MODERN_SANS, settings.mosqueFontPreset)
        assertTrue("15-min reminder must be enabled by default", settings.prePrayerReminderEnabled)
        assertEquals(0, settings.hijriOffsetDays)
        assertEquals("Canberra, ACT, Australia", settings.cityName)
        assertEquals("Australia/Sydney", settings.zoneId)
        assertEquals("SHAFI", settings.madhab)
        org.junit.Assert.assertNull(settings.highLatitudeRule)
        assertEquals(0, settings.offsetSubuh)
        assertEquals(0, settings.iqamahSubuh)
        assertFalse(settings.hijriChangeAtMidnight)
        assertTrue("Visual strike highlight should be true by default", settings.showStrikeHighlight)
        assertEquals(AppLanguage.SYSTEM, settings.appLanguage)
        assertFalse("12-hour format should be default for en-AU (D2)", settings.is24HourFormat)
        assertTrue("Arabic prayer names should be shown by default", settings.showArabicPrayerNames)
        assertEquals("", settings.mosqueName)
        assertEquals("", settings.mosqueSubtitle)

        assertFalse(settings.homeModePinRequired)
        assertFalse(settings.hasCustomPin)
        assertEquals("", settings.pinSaltHex)
        assertEquals("", settings.pinHashHex)
        assertEquals(0, settings.pinFailedAttempts)
        assertEquals(0L, settings.pinLockedUntilEpochMillis)
        assertFalse(settings.defaultPinPromptDismissed)

        assertEquals(AdhanAudioType.SILENT, settings.adhanFor(Prayer.SUBUH).type)
        assertEquals(AdhanAudioType.SILENT, settings.adhanFor(Prayer.ZUHUR).type)
        assertEquals(AdhanAudioType.SILENT, settings.adhanFor(Prayer.ASHR).type)
        assertEquals(AdhanAudioType.SILENT, settings.adhanFor(Prayer.MAGRIB).type)
        assertEquals(AdhanAudioType.SILENT, settings.adhanFor(Prayer.ISYA).type)
        assertFalse(settings.hasConfiguredAudibleAdhan())
        assertFalse(settings.adhanHintDismissed)
    }

    @Test
    fun popularCityPresets_containAustralianAndMajorCities() {
        assertTrue(PopularCityPresets.isNotEmpty())
        val firstCity = PopularCityPresets.first()
        assertTrue("Australian presets should come first (Canberra)", firstCity.name.contains("Canberra"))
        assertEquals("Australia/Sydney", firstCity.zoneId)

        val cityNames = PopularCityPresets.map { it.name }
        assertTrue(cityNames.any { it.contains("Sydney") })
        assertTrue(cityNames.any { it.contains("Melbourne") })
        assertTrue(cityNames.any { it.contains("Brisbane") })
        assertTrue(cityNames.any { it.contains("Adelaide") })
        assertTrue(cityNames.any { it.contains("Perth") })
        assertTrue(cityNames.any { it.contains("Jakarta") })
        assertTrue(cityNames.any { it.contains("Surabaya") })
        assertTrue(cityNames.any { it.contains("Singapore") })
        assertTrue(cityNames.any { it.contains("Makkah") })
    }

    @Test
    fun announcementSlide_attributes() {
        val slide = AnnouncementSlide(
            id = "slide_1",
            type = SlideType.IMAGE,
            title = "Kajian Ahad Pagi",
            imageFile = "slide1.jpg",
            durationSeconds = 15
        )

        assertEquals("slide_1", slide.id)
        assertEquals(SlideType.IMAGE, slide.type)
        assertEquals("slide1.jpg", slide.imageFile)
        assertEquals("Kajian Ahad Pagi", slide.title)
        assertEquals(15, slide.durationSeconds)
        assertTrue(slide.enabled)
    }

    @Test
    fun timeFormatting_honours12HourAnd24HourSetting() {
        val morning = java.time.LocalTime.of(5, 12)
        val afternoon = java.time.LocalTime.of(13, 5)

        val itemMorning = com.salatsign.tv.data.model.PrayerTimeItem(prayer = Prayer.SUBUH, time = morning, iqamahMinutesAfter = 15)
        val itemAfternoon = com.salatsign.tv.data.model.PrayerTimeItem(prayer = Prayer.ZUHUR, time = afternoon)

        assertEquals("5:12 am", itemMorning.formattedTime(is24Hour = false))
        assertEquals("05:12", itemMorning.formattedTime(is24Hour = true))
        assertEquals("5:27 am", itemMorning.formattedIqamahTime(is24Hour = false))
        assertEquals("05:27", itemMorning.formattedIqamahTime(is24Hour = true))

        assertEquals("1:05 pm", itemAfternoon.formattedTime(is24Hour = false))
        assertEquals("13:05", itemAfternoon.formattedTime(is24Hour = true))

        val day = com.salatsign.tv.data.model.PrayerTimesDay(
            date = java.time.LocalDate.of(2026, 6, 15),
            hijriDate = "29 Dhu al-Hijjah 1447 H",
            gregorianDate = "Monday, 15 June 2026",
            prayers = listOf(itemMorning, itemAfternoon),
            sunrise = java.time.LocalTime.of(7, 8),
            nextPrayer = itemMorning,
            activePrayer = null,
            secondsUntilNext = 60
        )
        assertEquals("7:08 am", day.formattedSunrise(is24Hour = false))
        assertEquals("07:08", day.formattedSunrise(is24Hour = true))
    }

    @Test
    fun settingsEnums_allMapToStringResourcesInLabels() {
        for (mode in ThemeMode.entries) {
            assertTrue(mode.labelRes() != 0)
        }
        for (preset in MosqueIconPreset.entries) {
            assertTrue(preset.labelRes() != 0)
        }
        for (font in MosqueFontPreset.entries) {
            assertTrue(font.labelRes() != 0)
        }
    }
}
