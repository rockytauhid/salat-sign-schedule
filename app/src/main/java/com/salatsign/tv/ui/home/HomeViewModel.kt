// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salatsign.tv.SalatSignApp
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.PrayerTimesDay
import com.salatsign.tv.engine.PrayerCalculationEngine
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
    val settings: AppSettings = AppSettings()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalatSignApp
    private val settingsRepository = app.settingsRepository

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    private val _ticker = MutableStateFlow(0L)

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

        val config = settings.prayerConfig()
        val prayerTimes = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = date,
            config = config,
            referenceZoned = nowZoned,
            hijriOffsetDays = settings.hijriOffsetDays,
            hijriChangeAtMidnight = settings.hijriChangeAtMidnight,
            iqamahOffsets = settings.iqamahOffsets()
        )

        HomeUiState(
            selectedDate = date,
            isViewingToday = isToday,
            prayerTimesDay = prayerTimes,
            settings = settings
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

    fun dismissAdhanHint() {
        viewModelScope.launch {
            settingsRepository.dismissAdhanHint()
        }
    }
}
