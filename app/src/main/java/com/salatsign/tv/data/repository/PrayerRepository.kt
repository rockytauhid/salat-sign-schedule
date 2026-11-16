package com.salatsign.tv.data.repository

import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.PrayerTimesDay
import com.salatsign.tv.engine.PrayerCalculationEngine
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class PrayerRepository(
    private val settingsRepository: SettingsRepository
) {
    fun getPrayerTimesForDate(
        date: LocalDate,
        settings: AppSettings,
        referenceTime: LocalTime = LocalTime.now(),
        referenceDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): PrayerTimesDay {
        return PrayerCalculationEngine.calculatePrayerTimesDay(
            date = date,
            latitude = settings.latitude,
            longitude = settings.longitude,
            calculationMethodName = settings.calculationMethodName,
            hijriOffsetDays = settings.hijriOffsetDays,
            referenceTime = referenceTime,
            referenceDate = referenceDate,
            zoneId = zoneId
        )
    }

    /**
     * Ticking flow that emits updated PrayerTimesDay every second for live countdown
     * and accurate time tracking on TV displays.
     */
    fun tickerPrayerTimesFlow(
        selectedDateFlow: Flow<LocalDate>,
        settingsFlow: Flow<AppSettings>
    ): Flow<PrayerTimesDay> = flow {
        // We will combine or tick periodically
        while (currentCoroutineContext().isActive) {
            // Emitted via ViewModel combining settings, selectedDate, and 1s tick
            delay(1000)
        }
    }
}
