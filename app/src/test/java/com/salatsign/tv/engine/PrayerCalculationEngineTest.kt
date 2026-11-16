package com.salatsign.tv.engine

import com.salatsign.tv.data.model.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

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

        // Verify sequential chronological order
        assertTrue("Subuh ($subuh) must be before Sunrise (${result.sunrise})", subuh.isBefore(result.sunrise))
        assertTrue("Sunrise (${result.sunrise}) must be before Zuhur ($zuhur)", result.sunrise.isBefore(zuhur))
        assertTrue("Zuhur ($zuhur) must be before Ashr ($ashr)", zuhur.isBefore(ashr))
        assertTrue("Ashr ($ashr) must be before Magrib ($magrib)", ashr.isBefore(magrib))
        assertTrue("Magrib ($magrib) must be before Isya ($isya)", magrib.isBefore(isya))
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

        val magribTime = result.prayers.first { it.prayer == Prayer.MAGRIB }.time

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
}
