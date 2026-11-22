// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerConfig
import com.salatsign.tv.data.model.PrayerEvent
import com.salatsign.tv.data.model.PrayerSilentConfig
import com.salatsign.tv.data.model.SilentAnchor
import com.salatsign.tv.data.model.SilentScreenConfig
import com.salatsign.tv.data.model.SilentWindow
import com.salatsign.tv.data.model.SilentWindowKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class SilentScreenCalculatorTest {

    private val brisbaneZone = ZoneId.of("Australia/Brisbane")
    private val sydneyZone = ZoneId.of("Australia/Sydney")
    private val adelaideZone = ZoneId.of("Australia/Adelaide")

    private val brisbaneConfig = PrayerConfig(
        latitude = -27.4698,
        longitude = 153.0251,
        methodName = "MUSLIM_WORLD_LEAGUE",
        madhab = "SHAFI",
        zoneId = brisbaneZone
    )

    private fun sampleEvents(date: LocalDate, zone: ZoneId, ishaTime: LocalTime = LocalTime.of(19, 30)): List<PrayerEvent> = listOf(
        PrayerEvent(Prayer.SUBUH, ZonedDateTime.of(date, LocalTime.of(5, 0), zone)),
        PrayerEvent(Prayer.ZUHUR, ZonedDateTime.of(date, LocalTime.of(12, 15), zone)),
        PrayerEvent(Prayer.ASHR, ZonedDateTime.of(date, LocalTime.of(15, 30), zone)),
        PrayerEvent(Prayer.MAGRIB, ZonedDateTime.of(date, LocalTime.of(18, 0), zone)),
        PrayerEvent(Prayer.ISYA, ZonedDateTime.of(date, ishaTime, zone))
    )

    @Test
    fun jumuah_lead0_duration45_isActiveDuringWindowOnly() {
        val friday = LocalDate.of(2026, 7, 3) // Friday
        val events = sampleEvents(friday, brisbaneZone)
        val windows = SilentScreenCalculator.silentScreenWindows(
            date = friday,
            prayerConfig = brisbaneConfig,
            silentConfig = SilentScreenConfig(jumuahEnabled = true, jumuahLeadMinutes = 0, jumuahDurationMinutes = 45),
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("13:00"),
            iqamahOffsets = emptyMap(),
            events = events
        )

        assertEquals(1, windows.size)
        assertEquals(SilentWindowKind.JUMUAH, windows[0].kind)
        assertEquals(ZonedDateTime.of(friday, LocalTime.of(13, 0), brisbaneZone), windows[0].start)
        assertEquals(ZonedDateTime.of(friday, LocalTime.of(13, 45), brisbaneZone), windows[0].end)

        assertFalse(SilentScreenCalculator.isSilentScreenActive(ZonedDateTime.of(friday, LocalTime.of(12, 59, 59), brisbaneZone), windows, null))
        assertTrue(SilentScreenCalculator.isSilentScreenActive(ZonedDateTime.of(friday, LocalTime.of(13, 0, 0), brisbaneZone), windows, null))
        assertTrue(SilentScreenCalculator.isSilentScreenActive(ZonedDateTime.of(friday, LocalTime.of(13, 44, 59), brisbaneZone), windows, null))
        assertFalse(SilentScreenCalculator.isSilentScreenActive(ZonedDateTime.of(friday, LocalTime.of(13, 45, 0), brisbaneZone), windows, null))
    }

    @Test
    fun jumuah_lead10_starts10MinutesBeforeSitting() {
        val friday = LocalDate.of(2026, 7, 3)
        val windows = SilentScreenCalculator.silentScreenWindows(
            date = friday,
            prayerConfig = brisbaneConfig,
            silentConfig = SilentScreenConfig(jumuahEnabled = true, jumuahLeadMinutes = 10, jumuahDurationMinutes = 45),
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("13:00"),
            iqamahOffsets = emptyMap(),
            events = sampleEvents(friday, brisbaneZone)
        )

        assertEquals(1, windows.size)
        assertEquals(ZonedDateTime.of(friday, LocalTime.of(12, 50), brisbaneZone), windows[0].start)
        assertEquals(ZonedDateTime.of(friday, LocalTime.of(13, 45), brisbaneZone), windows[0].end)
    }

    @Test
    fun jumuah_twoOverlappingSittings_mergeIntoOneContinuousWindow() {
        val friday = LocalDate.of(2026, 7, 3)
        val windows = SilentScreenCalculator.silentScreenWindows(
            date = friday,
            prayerConfig = brisbaneConfig,
            silentConfig = SilentScreenConfig(jumuahEnabled = true, jumuahLeadMinutes = 5, jumuahDurationMinutes = 45),
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("12:30", "13:15"),
            iqamahOffsets = emptyMap(),
            events = sampleEvents(friday, brisbaneZone)
        )

        // Sitting 1: 12:25–13:15, Sitting 2: 13:10–14:00 -> merged 12:25–14:00
        assertEquals(1, windows.size)
        assertEquals(SilentWindowKind.JUMUAH, windows[0].kind)
        assertEquals(ZonedDateTime.of(friday, LocalTime.of(12, 25), brisbaneZone), windows[0].start)
        assertEquals(ZonedDateTime.of(friday, LocalTime.of(14, 0), brisbaneZone), windows[0].end)
    }

    @Test
    fun jumuah_notConfigured_producesZeroWindowsEvenWhenEnabled() {
        val friday = LocalDate.of(2026, 7, 3)
        val windows = SilentScreenCalculator.silentScreenWindows(
            date = friday,
            prayerConfig = brisbaneConfig,
            silentConfig = SilentScreenConfig(jumuahEnabled = true, jumuahLeadMinutes = 0, jumuahDurationMinutes = 45),
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = emptyList(),
            iqamahOffsets = emptyMap(),
            events = sampleEvents(friday, brisbaneZone)
        )

        assertTrue(windows.isEmpty())
    }

    @Test
    fun manualOverride_takesPriorityAndStopsCleanlyWhenCleared() {
        val friday = LocalDate.of(2026, 7, 3)
        val now = ZonedDateTime.of(friday, LocalTime.of(10, 0), brisbaneZone)
        val manualUntil = now.plusMinutes(30)

        val active = SilentScreenCalculator.activeSilentState(now, emptyList(), manualUntil)
        assertNotNull(active)
        assertEquals(SilentWindowKind.MANUAL, active!!.kind)
        assertEquals(manualUntil, active.until)

        // Manual overrides even during a Jumu'ah window
        val jumuahWindow = SilentWindow(
            start = ZonedDateTime.of(friday, LocalTime.of(13, 0), brisbaneZone),
            end = ZonedDateTime.of(friday, LocalTime.of(13, 45), brisbaneZone),
            kind = SilentWindowKind.JUMUAH
        )
        val duringJumuah = ZonedDateTime.of(friday, LocalTime.of(13, 10), brisbaneZone)
        val manualDuringJumuah = SilentScreenCalculator.activeSilentState(
            now = duringJumuah,
            windows = listOf(jumuahWindow),
            manualUntil = duringJumuah.plusMinutes(15)
        )
        assertEquals(SilentWindowKind.MANUAL, manualDuringJumuah?.kind)

        // Stop manual -> no longer active outside scheduled windows
        assertNull(SilentScreenCalculator.activeSilentState(now, emptyList(), null))
        assertFalse(SilentScreenCalculator.isSilentScreenActive(manualUntil, emptyList(), manualUntil))
    }

    @Test
    fun dailyPrayers_disabledByDefault() {
        val defaults = SilentScreenConfig()
        assertTrue(defaults.jumuahEnabled)
        for (prayer in Prayer.entries) {
            val cfg = defaults.forPrayer(prayer)
            assertFalse(cfg.enabled)
            assertEquals(SilentAnchor.IQAMAH, cfg.anchor)
            assertEquals(0, cfg.leadMinutes)
            assertEquals(15, cfg.durationMinutes)
        }

        val thursday = LocalDate.of(2026, 7, 2)
        val windows = SilentScreenCalculator.silentScreenWindows(
            date = thursday,
            prayerConfig = brisbaneConfig,
            silentConfig = defaults,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("13:00"),
            iqamahOffsets = mapOf(Prayer.SUBUH to 15, Prayer.ZUHUR to 10),
            events = sampleEvents(thursday, brisbaneZone)
        )
        assertTrue(windows.isEmpty())
    }

    @Test
    fun dailyPrayers_iqamahVsAdhanAnchors_andMissingIqamahGuard() {
        assertFalse(SilentScreenCalculator.canEnableDailySilent(SilentAnchor.IQAMAH, 0))
        assertTrue(SilentScreenCalculator.canEnableDailySilent(SilentAnchor.IQAMAH, 10))
        assertTrue(SilentScreenCalculator.canEnableDailySilent(SilentAnchor.ADHAN, 0))

        val thursday = LocalDate.of(2026, 7, 2)
        val silentConfig = SilentScreenConfig(
            dailyPrayers = mapOf(
                // Subuh at 05:00 + 15m iqamah = 05:15, lead 2 -> 05:13..05:30
                Prayer.SUBUH to PrayerSilentConfig(enabled = true, anchor = SilentAnchor.IQAMAH, leadMinutes = 2, durationMinutes = 15),
                // Zuhr IQAMAH anchor but iqamah offset is 0 -> MUST NOT produce window
                Prayer.ZUHUR to PrayerSilentConfig(enabled = true, anchor = SilentAnchor.IQAMAH, leadMinutes = 0, durationMinutes = 15),
                // Maghrib ADHAN anchor at 18:00, lead 5, duration 20 -> 17:55..18:20
                Prayer.MAGRIB to PrayerSilentConfig(enabled = true, anchor = SilentAnchor.ADHAN, leadMinutes = 5, durationMinutes = 20)
            )
        )

        val windows = SilentScreenCalculator.silentScreenWindows(
            date = thursday,
            prayerConfig = brisbaneConfig,
            silentConfig = silentConfig,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = emptyList(),
            iqamahOffsets = mapOf(Prayer.SUBUH to 15, Prayer.ZUHUR to 0),
            events = sampleEvents(thursday, brisbaneZone)
        )

        assertEquals(2, windows.size)
        assertEquals(ZonedDateTime.of(thursday, LocalTime.of(5, 13), brisbaneZone), windows[0].start)
        assertEquals(ZonedDateTime.of(thursday, LocalTime.of(5, 30), brisbaneZone), windows[0].end)
        assertEquals(SilentWindowKind.PRAYER, windows[0].kind)

        assertEquals(ZonedDateTime.of(thursday, LocalTime.of(17, 55), brisbaneZone), windows[1].start)
        assertEquals(ZonedDateTime.of(thursday, LocalTime.of(18, 20), brisbaneZone), windows[1].end)
        assertEquals(SilentWindowKind.PRAYER, windows[1].kind)
    }

    @Test
    fun fridayZuhr_usesJumuahOnly_noDoubleDailyWindow() {
        val friday = LocalDate.of(2026, 7, 3)
        val silentConfig = SilentScreenConfig(
            jumuahEnabled = true,
            jumuahLeadMinutes = 0,
            jumuahDurationMinutes = 45,
            dailyPrayers = mapOf(
                Prayer.ZUHUR to PrayerSilentConfig(enabled = true, anchor = SilentAnchor.ADHAN, leadMinutes = 0, durationMinutes = 20)
            )
        )

        // With Jumu'ah sitting at 13:00 and Zuhr at 12:15 -> ONLY Jumu'ah window at 13:00..13:45
        val windows = SilentScreenCalculator.silentScreenWindows(
            date = friday,
            prayerConfig = brisbaneConfig,
            silentConfig = silentConfig,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("13:00"),
            iqamahOffsets = mapOf(Prayer.ZUHUR to 10),
            events = sampleEvents(friday, brisbaneZone)
        )
        assertEquals(1, windows.size)
        assertEquals(SilentWindowKind.JUMUAH, windows[0].kind)
        assertEquals(ZonedDateTime.of(friday, LocalTime.of(13, 0), brisbaneZone), windows[0].start)

        // Even with Jumu'ah disabled on Friday, daily Zuhr does not trigger on Friday
        val windowsNoJumuah = SilentScreenCalculator.silentScreenWindows(
            date = friday,
            prayerConfig = brisbaneConfig,
            silentConfig = silentConfig.copy(jumuahEnabled = false),
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("13:00"),
            iqamahOffsets = mapOf(Prayer.ZUHUR to 10),
            events = sampleEvents(friday, brisbaneZone)
        )
        assertTrue(windowsNoJumuah.isEmpty())
    }

    @Test
    fun lateIsyaCrossingMidnight_remainsActiveAfterMidnight() {
        val saturday = LocalDate.of(2026, 7, 4)
        val sunday = LocalDate.of(2026, 7, 5)

        val satEvents = sampleEvents(saturday, brisbaneZone, ishaTime = LocalTime.of(23, 45))
        val sunEvents = sampleEvents(sunday, brisbaneZone, ishaTime = LocalTime.of(23, 45))

        val silentConfig = SilentScreenConfig(
            jumuahEnabled = false,
            dailyPrayers = mapOf(
                Prayer.ISYA to PrayerSilentConfig(enabled = true, anchor = SilentAnchor.IQAMAH, leadMinutes = 0, durationMinutes = 45)
            )
        )
        val iqamah = mapOf(Prayer.ISYA to 10) // Iqamah at 23:55 -> window 23:55..00:40 next day

        val windows = SilentScreenCalculator.mergeWindows(
            SilentScreenCalculator.silentScreenWindows(saturday, brisbaneConfig, silentConfig, JumuahMode.FIXED, emptyList(), iqamah, satEvents) +
                SilentScreenCalculator.silentScreenWindows(sunday, brisbaneConfig, silentConfig, JumuahMode.FIXED, emptyList(), iqamah, sunEvents)
        )

        val afterMidnight = ZonedDateTime.of(sunday, LocalTime.of(0, 20), brisbaneZone)
        val afterWindow = ZonedDateTime.of(sunday, LocalTime.of(0, 41), brisbaneZone)
        assertTrue(SilentScreenCalculator.isSilentScreenActive(afterMidnight, windows, null))
        assertFalse(SilentScreenCalculator.isSilentScreenActive(afterWindow, windows, null))
    }

    @Test
    fun dstTransitionDay_sydneyAndHalfHourZone_adelaide() {
        // Australia/Sydney DST starts first Sunday in October (2026-10-04: 02:00 jumps to 03:00, UTC+10 -> UTC+11)
        val sydneyDstDay = LocalDate.of(2026, 10, 4)
        val sydneyPrayerConfig = PrayerConfig(
            latitude = -33.8688,
            longitude = 151.2093,
            methodName = "MUSLIM_WORLD_LEAGUE",
            madhab = "SHAFI",
            zoneId = sydneyZone
        )
        val dailyAllAdhan = SilentScreenConfig(
            jumuahEnabled = false,
            dailyPrayers = mapOf(
                Prayer.SUBUH to PrayerSilentConfig(enabled = true, anchor = SilentAnchor.ADHAN, leadMinutes = 0, durationMinutes = 20),
                Prayer.ISYA to PrayerSilentConfig(enabled = true, anchor = SilentAnchor.ADHAN, leadMinutes = 0, durationMinutes = 30)
            )
        )

        val sydneyWindows = SilentScreenCalculator.silentScreenWindowsAround(
            centerDate = sydneyDstDay,
            prayerConfig = sydneyPrayerConfig,
            silentConfig = dailyAllAdhan,
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = emptyList(),
            iqamahOffsets = emptyMap()
        )
        val sydneyEvents = PrayerCalculationEngine.getDayPrayerEvents(sydneyDstDay, sydneyPrayerConfig)
        val sydneySubuh = sydneyEvents.first { it.prayer == Prayer.SUBUH }.at
        assertEquals("+11:00", sydneySubuh.offset.id)
        assertTrue(SilentScreenCalculator.isSilentScreenActive(sydneySubuh.plusMinutes(5), sydneyWindows, null))
        assertFalse(SilentScreenCalculator.isSilentScreenActive(sydneySubuh.plusMinutes(25), sydneyWindows, null))

        // Australia/Adelaide half-hour timezone (UTC+09:30 in winter)
        val adelaideFriday = LocalDate.of(2026, 7, 3)
        val adelaidePrayerConfig = PrayerConfig(
            latitude = -34.9285,
            longitude = 138.6007,
            methodName = "MUSLIM_WORLD_LEAGUE",
            madhab = "SHAFI",
            zoneId = adelaideZone
        )
        val adelaideWindows = SilentScreenCalculator.silentScreenWindowsAround(
            centerDate = adelaideFriday,
            prayerConfig = adelaidePrayerConfig,
            silentConfig = SilentScreenConfig(jumuahEnabled = true, jumuahLeadMinutes = 5, jumuahDurationMinutes = 45),
            jumuahMode = JumuahMode.FIXED,
            jumuahTimes = listOf("13:15"),
            iqamahOffsets = emptyMap()
        )
        val insideAdelaide = ZonedDateTime.of(adelaideFriday, LocalTime.of(13, 20), adelaideZone)
        assertEquals("+09:30", insideAdelaide.offset.id)
        val active = SilentScreenCalculator.activeSilentState(insideAdelaide, adelaideWindows, null)
        assertEquals(SilentWindowKind.JUMUAH, active?.kind)
    }

    @Test
    fun validationRanges_andSmartCopyToAllPrayers() {
        val unsanitized = SilentScreenConfig(
            jumuahLeadMinutes = 99,
            jumuahDurationMinutes = 5, // below min 30
            dailyPrayers = mapOf(
                Prayer.SUBUH to PrayerSilentConfig(enabled = true, anchor = SilentAnchor.ADHAN, leadMinutes = -4, durationMinutes = 120)
            ),
            manualSilentUntilEpochMillis = -500L
        ).sanitized()

        assertEquals(15, unsanitized.jumuahLeadMinutes)
        assertEquals(30, unsanitized.jumuahDurationMinutes)
        assertEquals(0, unsanitized.forPrayer(Prayer.SUBUH).leadMinutes)
        assertEquals(60, unsanitized.forPrayer(Prayer.SUBUH).durationMinutes)
        assertEquals(0L, unsanitized.manualSilentUntilEpochMillis)

        // Smart copy with IQAMAH anchor: skips prayers with iqamah == 0
        val copyResult = SilentScreenCalculator.copySettingsToAllPrayers(
            source = PrayerSilentConfig(enabled = true, anchor = SilentAnchor.IQAMAH, leadMinutes = 5, durationMinutes = 20),
            iqamahOffsets = mapOf(Prayer.SUBUH to 15, Prayer.ZUHUR to 10, Prayer.ASHR to 0, Prayer.MAGRIB to 5, Prayer.ISYA to 0)
        )
        assertEquals(3, copyResult.enabledCount)
        assertEquals(listOf(Prayer.ASHR, Prayer.ISYA), copyResult.skippedWithoutIqamah)
        assertTrue(copyResult.updatedPrayers.getValue(Prayer.SUBUH).enabled)
        assertFalse(copyResult.updatedPrayers.getValue(Prayer.ASHR).enabled)
        assertEquals(20, copyResult.updatedPrayers.getValue(Prayer.ASHR).durationMinutes)
        assertEquals(5, copyResult.updatedPrayers.getValue(Prayer.ASHR).leadMinutes)
    }

    @Test
    fun manualUntilEpochMillis_roundTripsAcrossRestart() {
        val until = ZonedDateTime.of(LocalDate.of(2026, 7, 3), LocalTime.of(13, 30), brisbaneZone)
        val persistedMillis = until.toInstant().toEpochMilli()

        val restored = SilentScreenCalculator.manualUntilZoned(persistedMillis, brisbaneZone)
        assertEquals(until, restored)
        assertTrue(SilentScreenCalculator.isSilentScreenActive(until.minusMinutes(1), emptyList(), restored))
        assertFalse(SilentScreenCalculator.isSilentScreenActive(until.plusSeconds(1), emptyList(), restored))
        assertNull(SilentScreenCalculator.manualUntilZoned(0L, brisbaneZone))
    }
}
