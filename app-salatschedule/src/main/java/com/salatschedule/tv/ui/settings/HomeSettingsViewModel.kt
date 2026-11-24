// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salatschedule.tv.SalatScheduleApp
import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Home Prayer Times Settings ViewModel for SalatSchedule.
 * Manages prayer calculation, Hijri, display, 15-min reminders, and per-prayer Adhan audio.
 */
class HomeSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalatScheduleApp
    private val repository = app.settingsRepository
    private val scheduler = app.alarmScheduler

    val settings: StateFlow<AppSettings> = repository.appSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings(isMosqueMode = false)
    )

    val isSettingsLoaded: StateFlow<Boolean> = repository.appSettingsFlow
        .map { true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun selectCity(city: CityPreset) {
        viewModelScope.launch {
            repository.selectCity(city)
            val updated = settings.value.copy(
                cityName = city.name,
                latitude = city.latitude,
                longitude = city.longitude,
                calculationMethodName = city.calculationMethod,
                zoneId = city.zoneId
            )
            scheduler.scheduleNext(updated)
        }
    }

    fun setCalculationMethod(methodName: String) {
        viewModelScope.launch {
            repository.setCalculationMethod(methodName)
            scheduler.scheduleNext(settings.value.copy(calculationMethodName = methodName))
        }
    }

    fun setMadhab(madhab: String) {
        viewModelScope.launch {
            repository.setMadhab(madhab)
            scheduler.scheduleNext(settings.value.copy(madhab = madhab))
        }
    }

    fun setHighLatitudeRule(rule: String?) {
        viewModelScope.launch {
            repository.setHighLatitudeRule(rule)
            scheduler.scheduleNext(settings.value.copy(highLatitudeRule = rule))
        }
    }

    fun setPrayerOffset(prayer: Prayer, minutes: Int) {
        viewModelScope.launch {
            repository.setPrayerOffset(prayer, minutes)
            val current = settings.value
            val updated = when (prayer) {
                Prayer.SUBUH -> current.copy(offsetSubuh = minutes)
                Prayer.ZUHUR -> current.copy(offsetZuhur = minutes)
                Prayer.ASHR -> current.copy(offsetAshr = minutes)
                Prayer.MAGRIB -> current.copy(offsetMagrib = minutes)
                Prayer.ISYA -> current.copy(offsetIsya = minutes)
            }
            scheduler.scheduleNext(updated)
        }
    }

    fun setSunriseOffset(minutes: Int) {
        viewModelScope.launch { repository.setSunriseOffset(minutes) }
    }

    fun setHijriOffset(offsetDays: Int) {
        viewModelScope.launch { repository.updateHijriOffset(offsetDays) }
    }

    fun setHijriChangeAtMidnight(changeAtMidnight: Boolean) {
        viewModelScope.launch { repository.setHijriChangeAtMidnight(changeAtMidnight) }
    }

    fun togglePrePrayerReminder(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPrePrayerReminder(enabled)
            scheduler.scheduleNext(settings.value.copy(prePrayerReminderEnabled = enabled))
        }
    }

    fun setAdhanAudio(prayer: Prayer, option: AdhanAudioOption) {
        viewModelScope.launch {
            repository.updateAdhanOption(prayer, option)
            val current = settings.value
            val updated = when (prayer) {
                Prayer.SUBUH -> current.copy(adhanSubuh = option)
                Prayer.ZUHUR -> current.copy(adhanZuhur = option)
                Prayer.ASHR -> current.copy(adhanAshr = option)
                Prayer.MAGRIB -> current.copy(adhanMagrib = option)
                Prayer.ISYA -> current.copy(adhanIsya = option)
            }
            scheduler.scheduleNext(updated)
        }
    }

    fun setCustomAdhan(prayer: Prayer, relativeFileName: String, displayName: String) {
        val option = AdhanAudioOption(
            type = AdhanAudioType.CUSTOM_FILE,
            displayName = displayName,
            customUri = relativeFileName
        )
        setAdhanAudio(prayer, option)
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.setAppLanguage(language) }
    }

    fun set24HourFormat(enabled: Boolean) {
        viewModelScope.launch { repository.set24HourFormat(enabled) }
    }

    fun setShowArabicPrayerNames(enabled: Boolean) {
        viewModelScope.launch { repository.setShowArabicPrayerNames(enabled) }
    }

    fun toggleIslamicEvent(eventId: String) {
        val current = settings.value.enabledIslamicEventIds
        val updated = if (eventId in current) current - eventId else current + eventId
        viewModelScope.launch { repository.setEnabledIslamicEventIds(updated) }
    }

    fun setShowNextEventOnHome(show: Boolean) {
        viewModelScope.launch { repository.setShowNextEventOnHome(show) }
    }

    fun setScreensaverScheduleEnabled(enabled: Boolean) {
        val s = settings.value
        if (!enabled && !com.salatsign.tv.engine.ScreensaverRotationEngine.canDisableSlide(
                com.salatsign.tv.engine.ScreensaverSlideKind.SCHEDULE,
                s.screensaverScheduleEnabled,
                s.screensaverCalendarEnabled,
                s.screensaverQuranEnabled
            )
        ) return
        viewModelScope.launch { repository.setScreensaverScheduleEnabled(enabled) }
    }

    fun setScreensaverCalendarEnabled(enabled: Boolean) {
        val s = settings.value
        if (!enabled && !com.salatsign.tv.engine.ScreensaverRotationEngine.canDisableSlide(
                com.salatsign.tv.engine.ScreensaverSlideKind.CALENDAR,
                s.screensaverScheduleEnabled,
                s.screensaverCalendarEnabled,
                s.screensaverQuranEnabled
            )
        ) return
        viewModelScope.launch { repository.setScreensaverCalendarEnabled(enabled) }
    }

    fun setScreensaverQuranEnabled(enabled: Boolean) {
        val s = settings.value
        if (!enabled && !com.salatsign.tv.engine.ScreensaverRotationEngine.canDisableSlide(
                com.salatsign.tv.engine.ScreensaverSlideKind.QURAN,
                s.screensaverScheduleEnabled,
                s.screensaverCalendarEnabled,
                s.screensaverQuranEnabled
            )
        ) return
        viewModelScope.launch { repository.setScreensaverQuranEnabled(enabled) }
    }

    fun setScreensaverScheduleDurationSeconds(seconds: Int) {
        viewModelScope.launch { repository.setScreensaverScheduleDurationSeconds(seconds) }
    }

    fun setScreensaverCalendarDurationSeconds(seconds: Int) {
        viewModelScope.launch { repository.setScreensaverCalendarDurationSeconds(seconds) }
    }

    fun completeSetup() {
        viewModelScope.launch {
            repository.setMosqueMode(false)
            repository.setSetupCompleted(true)
            scheduler.scheduleNext(settings.value.copy(isMosqueMode = false))
        }
    }

    fun restartSetupWizard() {
        viewModelScope.launch { repository.setSetupCompleted(false) }
    }
}
