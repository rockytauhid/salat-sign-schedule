package com.salatsign.tv.data

import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.SlideType
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.PopularCityPresets
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.service.PrayerAlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataModelAndSettingsTest {

    @Test
    fun prayerAlarm_requestCodesAreUnique() {
        assertNotEquals(PrayerAlarmScheduler.REQUEST_CODE_ADHAN, PrayerAlarmScheduler.REQUEST_CODE_REMINDER)
        assertEquals(100, PrayerAlarmScheduler.REQUEST_CODE_ADHAN)
        assertEquals(200, PrayerAlarmScheduler.REQUEST_CODE_REMINDER)
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
        assertFalse("Mosque Mode must be disabled by default", settings.isMosqueMode)
        assertEquals(ThemeMode.ALWAYS_DARK, settings.themeMode)
        assertEquals(MosqueIconPreset.DOME_CRESCENT, settings.mosqueIconPreset)
        assertEquals(MosqueFontPreset.MODERN_SANS, settings.mosqueFontPreset)
        assertTrue("15-min reminder must be enabled by default", settings.prePrayerReminderEnabled)
        assertEquals(0, settings.hijriOffsetDays)
        assertEquals("Jakarta", settings.cityName)

        assertEquals(AdhanAudioType.DEFAULT_SUBUH, settings.adhanFor(Prayer.SUBUH).type)
        assertEquals(AdhanAudioType.DEFAULT_STANDARD, settings.adhanFor(Prayer.ZUHUR).type)
        assertEquals(AdhanAudioType.DEFAULT_STANDARD, settings.adhanFor(Prayer.ASHR).type)
        assertEquals(AdhanAudioType.DEFAULT_STANDARD, settings.adhanFor(Prayer.MAGRIB).type)
        assertEquals(AdhanAudioType.DEFAULT_STANDARD, settings.adhanFor(Prayer.ISYA).type)
    }

    @Test
    fun popularCityPresets_containMajorCities() {
        assertTrue(PopularCityPresets.isNotEmpty())
        val cityNames = PopularCityPresets.map { it.name }
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
}
