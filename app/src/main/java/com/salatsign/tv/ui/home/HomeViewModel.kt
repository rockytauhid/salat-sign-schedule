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

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val isViewingToday: Boolean = true,
    val prayerTimesDay: PrayerTimesDay? = null,
    val settings: AppSettings = AppSettings()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalatSignApp
    private val settingsRepository = app.settingsRepository

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _ticker = MutableStateFlow(LocalTime.now())

    init {
        // 1-second ticker for live countdown & accurate prayer transitions
        viewModelScope.launch {
            while (isActive) {
                _ticker.value = LocalTime.now()
                delay(1000)
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedDate,
        settingsRepository.appSettingsFlow,
        _ticker
    ) { date, settings, currentTime ->
        val today = LocalDate.now()
        val isToday = (date == today)

        val prayerTimes = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = date,
            latitude = settings.latitude,
            longitude = settings.longitude,
            calculationMethodName = settings.calculationMethodName,
            hijriOffsetDays = settings.hijriOffsetDays,
            referenceTime = currentTime,
            referenceDate = today,
            zoneId = ZoneId.systemDefault()
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
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun resetToToday() {
        _selectedDate.value = LocalDate.now()
    }
}
