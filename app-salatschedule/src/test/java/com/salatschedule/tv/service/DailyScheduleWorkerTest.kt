// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.service

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyScheduleWorkerTest {

    @Test
    fun computeInitialDelayMinutes_before0005_targetsSameDay0005InConfiguredZone() {
        val zone = ZoneId.of("Australia/Adelaide")
        val now = ZonedDateTime.of(LocalDate.of(2026, 6, 15), LocalTime.of(0, 1), zone)

        val delay = DailyScheduleWorker.computeInitialDelayMinutes(zoneId = zone, now = now)
        assertEquals(4L, delay)
    }

    @Test
    fun computeInitialDelayMinutes_after0005_targetsNextDay0005InConfiguredZone() {
        val zone = ZoneId.of("Australia/Perth")
        val now = ZonedDateTime.of(LocalDate.of(2026, 6, 15), LocalTime.of(23, 5), zone)

        val delay = DailyScheduleWorker.computeInitialDelayMinutes(zoneId = zone, now = now)
        assertEquals(60L, delay)
    }

    @Test
    fun computeInitialDelayMinutes_convertsInstantToConfiguredZone() {
        // 13:05 UTC on June 15 is 23:05 AEST in Australia/Sydney (+10) -> 60 mins until 00:05 AEST
        val utcNow = ZonedDateTime.of(LocalDate.of(2026, 6, 15), LocalTime.of(13, 5), ZoneId.of("UTC"))
        val sydneyZone = ZoneId.of("Australia/Sydney")

        val delay = DailyScheduleWorker.computeInitialDelayMinutes(zoneId = sydneyZone, now = utcNow)
        assertEquals(60L, delay)
    }
}
