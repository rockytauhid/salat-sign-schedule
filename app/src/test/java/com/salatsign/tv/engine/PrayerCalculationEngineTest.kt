package com.salatsign.tv.engine

import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerConfig
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

class PrayerCalculationEngineTest {

    // Jakarta coordinates
    private val latitude = -6.2088
    private val longitude = 106.8456
    private val calculationMethod = "SINGAPORE"
    private val testDate = LocalDate.of(2026, 11, 17)
    private val zoneId = ZoneId.of("Asia/Jakarta")

    @Test
    fun calculatePrayerTimesDay_returnsAllFiveDailyPrayers() {
        val result = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = testDate,
            latitude = latitude,
            longitude = longitude,
            calculationMethodName = calculationMethod,
            hijriOffsetDays = 0,
            referenceTime = LocalTime.of(12, 0),
            referenceDate = testDate,
            zoneId = zoneId
        )

        assertEquals(5, result.prayers.size)
        assertEquals(listOf(Prayer.SUBUH, Prayer.ZUHUR, Prayer.ASHR, Prayer.MAGRIB, Prayer.ISYA), result.prayers.map { it.prayer })

        val subuh = result.prayers.first { it.prayer == Prayer.SUBUH }.time
        val zuhur = result.prayers.first { it.prayer == Prayer.ZUHUR }.time
        val ashr = result.prayers.first { it.prayer == Prayer.ASHR }.time
        val magrib = result.prayers.first { it.prayer == Prayer.MAGRIB }.time
        val isya = result.prayers.first { it.prayer == Prayer.ISYA }.time

        assertNotNull(subuh)
        assertNotNull(result.sunrise)
        assertNotNull(zuhur)
        assertNotNull(ashr)
        assertNotNull(magrib)
        assertNotNull(isya)

        // Verify sequential chronological order
        assertTrue("Subuh ($subuh) must be before Sunrise (${result.sunrise})", subuh!!.isBefore(result.sunrise!!))
        assertTrue("Sunrise (${result.sunrise}) must be before Zuhur ($zuhur)", result.sunrise!!.isBefore(zuhur!!))
        assertTrue("Zuhur ($zuhur) must be before Ashr ($ashr)", zuhur.isBefore(ashr!!))
        assertTrue("Ashr ($ashr) must be before Magrib ($magrib)", ashr.isBefore(magrib!!))
        assertTrue("Magrib ($magrib) must be before Isya ($isya)", magrib.isBefore(isya!!))
    }

    @Test
    fun calculatePrayerTimesDay_identifiesNextPrayer_beforeSubuh() {
        val earlyMorning = LocalTime.of(3, 0)
        val result = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = testDate,
            latitude = latitude,
            longitude = longitude,
            calculationMethodName = calculationMethod,
            hijriOffsetDays = 0,
            referenceTime = earlyMorning,
            referenceDate = testDate,
            zoneId = zoneId
        )

        assertNotNull(result.nextPrayer)
        assertEquals(Prayer.SUBUH, result.nextPrayer?.prayer)
        assertTrue(result.secondsUntilNext > 0)
    }

    @Test
    fun calculatePrayerTimesDay_identifiesNextPrayer_betweenSubuhAndZuhur() {
        val midMorning = LocalTime.of(8, 0)
        val result = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = testDate,
            latitude = latitude,
            longitude = longitude,
            calculationMethodName = calculationMethod,
            hijriOffsetDays = 0,
            referenceTime = midMorning,
            referenceDate = testDate,
            zoneId = zoneId
        )

        assertNotNull(result.nextPrayer)
        assertEquals(Prayer.ZUHUR, result.nextPrayer?.prayer)
        assertTrue(result.secondsUntilNext > 0)
    }

    @Test
    fun calculatePrayerTimesDay_identifiesNextPrayer_lateNight_transitionsToTomorrowSubuh() {
        val lateNight = LocalTime.of(23, 30)
        val result = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = testDate,
            latitude = latitude,
            longitude = longitude,
            calculationMethodName = calculationMethod,
            hijriOffsetDays = 0,
            referenceTime = lateNight,
            referenceDate = testDate,
            zoneId = zoneId
        )

        assertNotNull(result.nextPrayer)
        assertEquals(Prayer.SUBUH, result.nextPrayer?.prayer)
        assertTrue("Seconds until tomorrow's Subuh must be positive", result.secondsUntilNext > 0)
    }

    @Test
    fun countdownFormatted_formatsCorrectly() {
        val result = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = testDate,
            latitude = latitude,
            longitude = longitude,
            calculationMethodName = calculationMethod,
            hijriOffsetDays = 0,
            referenceTime = LocalTime.of(8, 0),
            referenceDate = testDate,
            zoneId = zoneId
        )

        val countdown = result.countdownFormatted
        assertTrue("Countdown format must match HH:MM:SS (got: $countdown)", countdown.matches(Regex("\\d{2}:\\d{2}:\\d{2}")))
    }

    @Test
    fun getPrayerAtExactTimeStrike_detectsExactStrikeWithinFiveMinutes() {
        val result = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = testDate,
            latitude = latitude,
            longitude = longitude,
            calculationMethodName = calculationMethod,
            hijriOffsetDays = 0,
            referenceTime = LocalTime.of(12, 0),
            referenceDate = testDate,
            zoneId = zoneId
        )

        val magribTime = result.prayers.first { it.prayer == Prayer.MAGRIB }.time!!

        // 1. Exact strike at 0 seconds
        val strikeExact = PrayerCalculationEngine.getPrayerAtExactTimeStrike(result, magribTime, 5)
        assertEquals(Prayer.MAGRIB, strikeExact)

        // 2. 2 minutes later -> still within 5 min window
        val strikePlusTwoMin = PrayerCalculationEngine.getPrayerAtExactTimeStrike(result, magribTime.plusMinutes(2), 5)
        assertEquals(Prayer.MAGRIB, strikePlusTwoMin)

        // 3. 6 minutes later -> beyond 5 min window
        val strikePlusSixMin = PrayerCalculationEngine.getPrayerAtExactTimeStrike(result, magribTime.plusMinutes(6), 5)
        assertNull(strikePlusSixMin)

        // 4. Before prayer time -> null
        val strikeBefore = PrayerCalculationEngine.getPrayerAtExactTimeStrike(result, magribTime.minusMinutes(1), 5)
        assertNull(strikeBefore)
    }

    @Test
    fun nextPrayerEvent_afterIsya_returnsTomorrowSubuh() {
        val lateNight = ZonedDateTime.of(testDate, LocalTime.of(23, 0), zoneId)
        val nextEvent = PrayerCalculationEngine.nextPrayerEvent(
            now = lateNight,
            latitude = latitude,
            longitude = longitude,
            calculationMethodName = calculationMethod
        )

        assertNotNull(nextEvent)
        assertEquals(Prayer.SUBUH, nextEvent!!.prayer)
        assertEquals(testDate.plusDays(1), nextEvent.at.toLocalDate())
        assertTrue(nextEvent.at.isAfter(lateNight))
    }

    @Test
    fun nextPrayerEvent_aroundMidnight_resolvesConsistently() {
        val justBeforeMidnight = ZonedDateTime.of(testDate, LocalTime.of(23, 59, 30), zoneId)
        val justAfterMidnight = ZonedDateTime.of(testDate.plusDays(1), LocalTime.of(0, 0, 30), zoneId)

        val eventBefore = PrayerCalculationEngine.nextPrayerEvent(
            now = justBeforeMidnight,
            latitude = latitude,
            longitude = longitude,
            calculationMethodName = calculationMethod
        )
        val eventAfter = PrayerCalculationEngine.nextPrayerEvent(
            now = justAfterMidnight,
            latitude = latitude,
            longitude = longitude,
            calculationMethodName = calculationMethod
        )

        assertNotNull(eventBefore)
        assertNotNull(eventAfter)
        assertEquals(Prayer.SUBUH, eventBefore!!.prayer)
        assertEquals(Prayer.SUBUH, eventAfter!!.prayer)
        assertEquals(testDate.plusDays(1), eventBefore.at.toLocalDate())
        assertEquals(testDate.plusDays(1), eventAfter.at.toLocalDate())
        assertEquals("Both times must resolve to the identical Subuh instant", eventBefore.at, eventAfter.at)
    }

    @Test
    fun nextPrayerEvent_worksForAustraliaBrisbane_noDst() {
        val brisbaneZone = ZoneId.of("Australia/Brisbane")
        val brisbaneLat = -27.4698
        val brisbaneLng = 153.0251
        val lateEvening = ZonedDateTime.of(testDate, LocalTime.of(22, 0), brisbaneZone)

        val nextEvent = PrayerCalculationEngine.nextPrayerEvent(
            now = lateEvening,
            latitude = brisbaneLat,
            longitude = brisbaneLng,
            calculationMethodName = "MUSLIM_WORLD_LEAGUE"
        )

        assertNotNull(nextEvent)
        assertEquals(Prayer.SUBUH, nextEvent!!.prayer)
        assertEquals(testDate.plusDays(1), nextEvent.at.toLocalDate())
        assertEquals(brisbaneZone, nextEvent.at.zone)
    }

    @Test
    fun nextPrayerEvent_worksForAustraliaSydney_acrossDstTransitions() {
        val sydneyZone = ZoneId.of("Australia/Sydney")
        val sydneyLat = -33.8688
        val sydneyLng = 151.2093

        // 1. April DST End transition (2026-04-05 clocks change from AEDT to AEST)
        val nightBeforeDstEnd = ZonedDateTime.of(LocalDate.of(2026, 4, 4), LocalTime.of(23, 30), sydneyZone)
        val nextEventApril = PrayerCalculationEngine.nextPrayerEvent(
            now = nightBeforeDstEnd,
            latitude = sydneyLat,
            longitude = sydneyLng,
            calculationMethodName = "MUSLIM_WORLD_LEAGUE"
        )
        assertNotNull(nextEventApril)
        assertEquals(Prayer.SUBUH, nextEventApril!!.prayer)
        assertEquals(LocalDate.of(2026, 4, 5), nextEventApril.at.toLocalDate())

        // 2. October DST Start transition (2026-10-04 clocks change from AEST to AEDT)
        val nightBeforeDstStart = ZonedDateTime.of(LocalDate.of(2026, 10, 3), LocalTime.of(23, 30), sydneyZone)
        val nextEventOct = PrayerCalculationEngine.nextPrayerEvent(
            now = nightBeforeDstStart,
            latitude = sydneyLat,
            longitude = sydneyLng,
            calculationMethodName = "MUSLIM_WORLD_LEAGUE"
        )
        assertNotNull(nextEventOct)
        assertEquals(Prayer.SUBUH, nextEventOct!!.prayer)
        assertEquals(LocalDate.of(2026, 10, 4), nextEventOct.at.toLocalDate())
    }

    @Test
    fun nextPrayerEvent_worksForAustraliaAdelaide_halfHourOffset() {
        val adelaideZone = ZoneId.of("Australia/Adelaide")
        val adelaideLat = -34.9285
        val adelaideLng = 138.6007
        val lateEvening = ZonedDateTime.of(testDate, LocalTime.of(22, 30), adelaideZone)

        val nextEvent = PrayerCalculationEngine.nextPrayerEvent(
            now = lateEvening,
            latitude = adelaideLat,
            longitude = adelaideLng,
            calculationMethodName = "MUSLIM_WORLD_LEAGUE"
        )

        assertNotNull(nextEvent)
        assertEquals(Prayer.SUBUH, nextEvent!!.prayer)
        assertEquals(testDate.plusDays(1), nextEvent.at.toLocalDate())
        assertEquals(adelaideZone, nextEvent.at.zone)
    }

    @Test
    fun nextReminderEvent_schedulesFifteenMinutesBeforePrayer() {
        val midMorning = ZonedDateTime.of(testDate, LocalTime.of(8, 0), zoneId)
        val reminderEvent = PrayerCalculationEngine.nextReminderEvent(
            now = midMorning,
            latitude = latitude,
            longitude = longitude,
            calculationMethodName = calculationMethod
        )

        assertNotNull(reminderEvent)
        assertEquals(Prayer.ZUHUR, reminderEvent!!.prayer)
        assertEquals(reminderEvent.prayerAt.minusMinutes(15), reminderEvent.reminderAt)
        assertTrue(reminderEvent.reminderAt.isAfter(midMorning))
    }

    @Test
    fun isAlarmStale_detectsStaleAlarmsAccurately() {
        val now = 1_000_000_000_000L

        // Fired 2 minutes late -> not stale (< 5 mins)
        val twoMinAgo = now - (2 * 60 * 1000L)
        assertFalse(PrayerCalculationEngine.isAlarmStale(twoMinAgo, now))

        // Fired 6 minutes late -> stale (> 5 mins)
        val sixMinAgo = now - (6 * 60 * 1000L)
        assertTrue(PrayerCalculationEngine.isAlarmStale(sixMinAgo, now))

        // Fired right on time
        assertFalse(PrayerCalculationEngine.isAlarmStale(now, now))

        // Scheduled in the future (early)
        assertFalse(PrayerCalculationEngine.isAlarmStale(now + 10000L, now))

        // Invalid scheduledAt (0)
        assertFalse(PrayerCalculationEngine.isAlarmStale(0L, now))
    }

    @Test
    fun prayerCalculation_offsetsShiftPrayerTimes() {
        val baseConfig = PrayerConfig(
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            methodName = calculationMethod
        )
        val baseline = PrayerCalculationEngine.calculatePrayerTimesDay(testDate, baseConfig)
        val baseSubuh = baseline.prayers.first { it.prayer == Prayer.SUBUH }.time!!
        val baseMagrib = baseline.prayers.first { it.prayer == Prayer.MAGRIB }.time!!

        val adjustedConfig = baseConfig.copy(
            offsetsMinutes = mapOf(Prayer.SUBUH to 5, Prayer.MAGRIB to -3)
        )
        val adjusted = PrayerCalculationEngine.calculatePrayerTimesDay(testDate, adjustedConfig)
        val adjSubuh = adjusted.prayers.first { it.prayer == Prayer.SUBUH }.time!!
        val adjMagrib = adjusted.prayers.first { it.prayer == Prayer.MAGRIB }.time!!

        assertEquals(baseSubuh.plusMinutes(5), adjSubuh)
        assertEquals(baseMagrib.minusMinutes(3), adjMagrib)
    }

    @Test
    fun prayerCalculation_madhabChangesAsrTime() {
        val shafiConfig = PrayerConfig(
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            methodName = calculationMethod,
            madhab = "SHAFI"
        )
        val hanafiConfig = shafiConfig.copy(madhab = "HANAFI")

        val shafiTimes = PrayerCalculationEngine.calculatePrayerTimesDay(testDate, shafiConfig)
        val hanafiTimes = PrayerCalculationEngine.calculatePrayerTimesDay(testDate, hanafiConfig)

        val shafiAsr = shafiTimes.prayers.first { it.prayer == Prayer.ASHR }.time!!
        val hanafiAsr = hanafiTimes.prayers.first { it.prayer == Prayer.ASHR }.time!!

        assertTrue("Hanafi Asr ($hanafiAsr) must be after Shafi Asr ($shafiAsr)", hanafiAsr.isAfter(shafiAsr))
        val diffMinutes = java.time.Duration.between(shafiAsr, hanafiAsr).toMinutes()
        assertTrue("Hanafi Asr should be 40-70 minutes later than Shafi (got: $diffMinutes min)", diffMinutes in 40..70)
    }

    @Test
    fun prayerCalculation_twentyFourHourActivePrayerSemantics() {
        val config = PrayerConfig(
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            methodName = calculationMethod
        )

        // 1. At 01:00 AM (midnight / early morning before Subuh): Isya from yesterday is active!
        val earlyMorningZoned = ZonedDateTime.of(testDate, LocalTime.of(1, 0), zoneId)
        val timesEarly = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = testDate,
            config = config,
            referenceZoned = earlyMorningZoned
        )
        val isyaEarly = timesEarly.prayers.first { it.prayer == Prayer.ISYA }
        assertTrue("At 01:00 AM before Subuh, Isya must be active (24h continuous semantics)", isyaEarly.isActive)
        assertEquals(Prayer.SUBUH, timesEarly.nextPrayer?.prayer)

        // 2. At 08:00 AM (between Subuh and Zuhur): Subuh is active
        val midMorningZoned = ZonedDateTime.of(testDate, LocalTime.of(8, 0), zoneId)
        val timesMidMorning = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = testDate,
            config = config,
            referenceZoned = midMorningZoned
        )
        val subuhMid = timesMidMorning.prayers.first { it.prayer == Prayer.SUBUH }
        assertTrue("At 08:00 AM, Subuh must be active", subuhMid.isActive)
        assertEquals(Prayer.ZUHUR, timesMidMorning.nextPrayer?.prayer)

        // 3. At 13:00 PM (between Zuhur and Ashr): Zuhur is active
        val afternoonZoned = ZonedDateTime.of(testDate, LocalTime.of(13, 0), zoneId)
        val timesAfternoon = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = testDate,
            config = config,
            referenceZoned = afternoonZoned
        )
        val zuhurAft = timesAfternoon.prayers.first { it.prayer == Prayer.ZUHUR }
        assertTrue("At 13:00 PM, Zuhur must be active", zuhurAft.isActive)
        assertEquals(Prayer.ASHR, timesAfternoon.nextPrayer?.prayer)

        // 4. At 22:00 PM (after Isya): Isya is active
        val nightZoned = ZonedDateTime.of(testDate, LocalTime.of(22, 0), zoneId)
        val timesNight = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = testDate,
            config = config,
            referenceZoned = nightZoned
        )
        val isyaNight = timesNight.prayers.first { it.prayer == Prayer.ISYA }
        assertTrue("At 22:00 PM, Isya must be active", isyaNight.isActive)
        assertEquals(Prayer.SUBUH, timesNight.nextPrayer?.prayer)
    }

    @Test
    fun prayerCalculation_fridayJumuahLabeling() {
        val config = PrayerConfig(
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            methodName = calculationMethod
        )

        // 2026-11-20 is Friday
        val fridayDate = LocalDate.of(2026, 11, 20)
        assertEquals(java.time.DayOfWeek.FRIDAY, fridayDate.dayOfWeek)
        val fridayTimes = PrayerCalculationEngine.calculatePrayerTimesDay(fridayDate, config)
        val fridayZuhur = fridayTimes.prayers.first { it.prayer == Prayer.ZUHUR }
        assertEquals("Jum'at", fridayZuhur.displayLabel)

        // 2026-11-17 is Tuesday
        val tuesdayDate = LocalDate.of(2026, 11, 17)
        val tuesdayTimes = PrayerCalculationEngine.calculatePrayerTimesDay(tuesdayDate, config)
        val tuesdayZuhur = tuesdayTimes.prayers.first { it.prayer == Prayer.ZUHUR }
        assertEquals("Zuhur", tuesdayZuhur.displayLabel)
    }

    @Test
    fun prayerCalculation_iqamahOffsets() {
        val config = PrayerConfig(
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            methodName = calculationMethod
        )
        val iqamahMap = mapOf(
            Prayer.SUBUH to 15,
            Prayer.MAGRIB to 10
        )
        val times = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = testDate,
            config = config,
            iqamahOffsets = iqamahMap
        )

        val subuh = times.prayers.first { it.prayer == Prayer.SUBUH }
        val magrib = times.prayers.first { it.prayer == Prayer.MAGRIB }
        val zuhur = times.prayers.first { it.prayer == Prayer.ZUHUR }

        assertEquals(subuh.time?.plusMinutes(15), subuh.iqamahTime)
        assertNotNull(subuh.formattedIqamahTime)

        assertEquals(magrib.time?.plusMinutes(10), magrib.iqamahTime)
        assertNotNull(magrib.formattedIqamahTime)

        assertNull("Zuhur has no iqamah configured", zuhur.iqamahTime)
        assertNull(zuhur.formattedIqamahTime)
    }

    @Test
    fun prayerCalculation_highLatitudeNullableTimes_andRules() {
        // Direct model verification of null time fallback to "--:--"
        val nullItem = com.salatsign.tv.data.model.PrayerTimeItem(prayer = Prayer.SUBUH, time = null)
        assertEquals("--:--", nullItem.formattedTime)

        // Tromsø, Norway (69.6492 N) during June solstice (midnight sun) produces null astronomical times
        val polarSummerDate = LocalDate.of(2026, 6, 21)
        val polarConfig = PrayerConfig(
            latitude = 69.6492,
            longitude = 18.9553,
            zoneId = ZoneId.of("Europe/Oslo"),
            methodName = "MUSLIM_WORLD_LEAGUE",
            highLatitudeRule = "MIDDLE_OF_THE_NIGHT"
        )

        val polarResult = PrayerCalculationEngine.calculatePrayerTimesDay(polarSummerDate, polarConfig)
        // Verify hasNullTimes is detected and null prayers format to "--:--" without crashing
        assertTrue("Polar midnight sun must flag hasNullTimes", polarResult.hasNullTimes)
        for (p in polarResult.prayers) {
            if (p.time == null) {
                assertEquals("--:--", p.formattedTime)
            }
        }
    }
}
