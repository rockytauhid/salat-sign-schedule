// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salatsign.core.tv.R
import com.salatschedule.tv.SalatScheduleApp
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerTimesDay
import com.salatsign.tv.engine.DailyPrayerCacheKey
import com.salatsign.tv.engine.PrayerCalculationEngine
import com.salatsign.tv.engine.PrayerDayCache
import com.salatsign.tv.ui.common.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val isViewingToday: Boolean = true,
    val prayerTimesDay: PrayerTimesDay? = null,
    val settings: AppSettings = AppSettings(),
    val activeStrikePrayer: Prayer? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalatScheduleApp
    private val settingsRepository = app.settingsRepository

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    private val _ticker = MutableStateFlow(0L)
    private val prayerDayCache = PrayerDayCache()

    init {
        // 1-second ticker for live countdown & accurate prayer transitions
        viewModelScope.launch {
            while (isActive) {
                _ticker.value = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedDate,
        settingsRepository.appSettingsFlow,
        _ticker
    ) { customDate, settings, _ ->
        val zone = runCatching { ZoneId.of(settings.zoneId) }.getOrDefault(ZoneId.systemDefault())
        val nowZoned = ZonedDateTime.now(zone)
        val today = nowZoned.toLocalDate()
        val date = customDate ?: today
        val isToday = (date == today)

        val hijriMonthNames = runCatching {
            LocaleHelper.wrap(app, settings.appLanguage).resources.getStringArray(R.array.hijri_months).toList()
        }.getOrDefault(emptyList())

        val key = DailyPrayerCacheKey(
            date = date,
            config = settings.prayerConfig(),
            hijriOffsetDays = settings.hijriOffsetDays,
            hijriChangeAtMidnight = settings.hijriChangeAtMidnight,
            iqamahOffsets = settings.iqamahOffsets(),
            hijriMonthNames = hijriMonthNames
        )
        val prayerTimes = prayerDayCache.getOrCompute(key, nowZoned)
        val strikePrayer = if (isToday && settings.showStrikeHighlight) {
            PrayerCalculationEngine.getPrayerAtExactTimeStrike(
                prayerTimes,
                nowZoned.toLocalTime(),
                5L,
                isMosqueSignage = false
            )
        } else {
            null
        }

        HomeUiState(
            selectedDate = date,
            isViewingToday = isToday,
            prayerTimesDay = prayerTimes,
            settings = settings,
            activeStrikePrayer = strikePrayer
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun previousDay() {
        val current = uiState.value.selectedDate
        _selectedDate.value = current.minusDays(1)
    }

    fun nextDay() {
        val current = uiState.value.selectedDate
        _selectedDate.value = current.plusDays(1)
    }

    fun resetToToday() {
        _selectedDate.value = null
    }
}
