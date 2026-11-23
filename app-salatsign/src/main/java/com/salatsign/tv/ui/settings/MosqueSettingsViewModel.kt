// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salatsign.tv.SalatSignApp
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerSilentConfig
import com.salatsign.tv.data.model.SlideContainer
import com.salatsign.tv.data.model.SlideType
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.engine.CopyPrayerSilentResult
import com.salatsign.tv.engine.SilentScreenCalculator
import com.salatsign.tv.engine.SlideBudget
import com.salatsign.tv.ui.mosque.BuiltInSlideRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Strictly silent Mosque Mode Settings ViewModel for SalatSign.
 * No alarm scheduler, no adhan audio, no notification reminders.
 */
class MosqueSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalatSignApp
    private val repository = app.settingsRepository
    private val contentRepository = app.contentRepository

    val settings: StateFlow<AppSettings> = repository.appSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings(isMosqueMode = true)
    )

    val slides: StateFlow<SlideContainer> = contentRepository.slidesFlow

    private val _slideLimitError = MutableStateFlow(false)
    val slideLimitError: StateFlow<Boolean> = _slideLimitError.asStateFlow()

    val isPhoneControllerRunning: StateFlow<Boolean> = app.localServer.isRunning
    val phoneControllerUrl: StateFlow<String?> = app.phoneControllerUrl

    fun clearSlideLimitError() {
        _slideLimitError.value = false
    }

    private fun currentEnabledBuiltInCount(): Int =
        BuiltInSlideRegistry.registrationsFor(settings.value).count { it.enabled }

    private fun currentEnabledAnnouncementCount(): Int =
        contentRepository.slidesFlow.value.slides.count { it.enabled }

    fun setPhoneControllerEnabled(enabled: Boolean) {
        app.setPhoneControllerEnabled(enabled)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun selectCity(city: CityPreset) {
        viewModelScope.launch { repository.selectCity(city) }
    }

    fun setCalculationMethod(methodName: String) {
        viewModelScope.launch { repository.setCalculationMethod(methodName) }
    }

    fun setMadhab(madhab: String) {
        viewModelScope.launch { repository.setMadhab(madhab) }
    }

    fun setHighLatitudeRule(rule: String?) {
        viewModelScope.launch { repository.setHighLatitudeRule(rule) }
    }

    fun setPrayerOffset(prayer: Prayer, minutes: Int) {
        viewModelScope.launch { repository.setPrayerOffset(prayer, minutes) }
    }

    fun setSunriseOffset(minutes: Int) {
        viewModelScope.launch { repository.setSunriseOffset(minutes) }
    }

    fun setIqamahOffset(prayer: Prayer, minutes: Int) {
        viewModelScope.launch { repository.setIqamahOffset(prayer, minutes) }
    }

    fun setHijriOffset(offsetDays: Int) {
        viewModelScope.launch { repository.updateHijriOffset(offsetDays) }
    }

    fun setHijriChangeAtMidnight(changeAtMidnight: Boolean) {
        viewModelScope.launch { repository.setHijriChangeAtMidnight(changeAtMidnight) }
    }

    fun setShowStrikeHighlight(enabled: Boolean) {
        viewModelScope.launch { repository.setShowStrikeHighlight(enabled) }
    }

    fun setMosqueName(name: String) {
        viewModelScope.launch {
            repository.updateMosqueBranding(name, settings.value.mosqueSubtitle, settings.value.mosqueLogoUri)
        }
    }

    fun setMosqueSubtitle(subtitle: String) {
        viewModelScope.launch {
            repository.updateMosqueBranding(settings.value.mosqueName, subtitle, settings.value.mosqueLogoUri)
        }
    }

    fun setMosqueIconPreset(preset: MosqueIconPreset) {
        viewModelScope.launch { repository.setMosqueIconPreset(preset) }
    }

    fun setMosqueFontPreset(preset: MosqueFontPreset) {
        viewModelScope.launch { repository.setMosqueFontPreset(preset) }
    }

    fun setScheduleDuration(seconds: Int) {
        viewModelScope.launch { repository.updateScheduleDuration(seconds) }
    }

    fun clearSlides() {
        _slideLimitError.value = false
        viewModelScope.launch {
            val current = contentRepository.slidesFlow.value
            contentRepository.replaceAll(current.revision, emptyList())
        }
    }

    fun deleteSlide(slideId: String) {
        _slideLimitError.value = false
        viewModelScope.launch { contentRepository.deleteSlide(slideId) }
    }

    fun toggleSlideEnabled(slideId: String): Boolean {
        val current = contentRepository.slidesFlow.value.slides.firstOrNull { it.id == slideId } ?: return false
        val targetEnabled = !current.enabled
        if (targetEnabled && !SlideBudget.canEnableOrAddAnnouncement(
                enabledAnnouncements = currentEnabledAnnouncementCount(),
                enabledBuiltIns = currentEnabledBuiltInCount()
            )
        ) {
            _slideLimitError.value = true
            return false
        }
        _slideLimitError.value = false
        viewModelScope.launch { contentRepository.updateSlide(current.copy(enabled = targetEnabled)) }
        return true
    }

    fun addTextSlide(title: String, body: String, durationSeconds: Int, enabled: Boolean = true): Boolean {
        if (enabled && !SlideBudget.canEnableOrAddAnnouncement(
                enabledAnnouncements = currentEnabledAnnouncementCount(),
                enabledBuiltIns = currentEnabledBuiltInCount()
            )
        ) {
            _slideLimitError.value = true
            return false
        }
        _slideLimitError.value = false
        viewModelScope.launch {
            val nextOrder = (contentRepository.slidesFlow.value.slides.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val slide = AnnouncementSlide(
                id = "slide_${System.currentTimeMillis()}",
                type = SlideType.TEXT,
                title = title,
                body = body,
                durationSeconds = durationSeconds.coerceIn(3, 120),
                enabled = enabled,
                sortOrder = nextOrder
            )
            contentRepository.addSlide(slide)
        }
        return true
    }

    fun addImageSlide(title: String, imageFile: String, durationSeconds: Int, enabled: Boolean = true): Boolean {
        val effectiveEnabled = if (enabled && !SlideBudget.canEnableOrAddAnnouncement(
                enabledAnnouncements = currentEnabledAnnouncementCount(),
                enabledBuiltIns = currentEnabledBuiltInCount()
            )
        ) {
            _slideLimitError.value = true
            false
        } else {
            _slideLimitError.value = false
            enabled
        }
        viewModelScope.launch {
            val nextOrder = (contentRepository.slidesFlow.value.slides.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val slide = AnnouncementSlide(
                id = "slide_${System.currentTimeMillis()}",
                type = SlideType.IMAGE,
                title = title,
                imageFile = imageFile,
                durationSeconds = durationSeconds.coerceIn(3, 120),
                enabled = effectiveEnabled,
                sortOrder = nextOrder
            )
            contentRepository.addSlide(slide)
        }
        return effectiveEnabled
    }

    fun setCustomLogo(relativeFileName: String) {
        viewModelScope.launch {
            repository.setMosqueIconPreset(MosqueIconPreset.CUSTOM)
            repository.updateMosqueBranding(settings.value.mosqueName, settings.value.mosqueSubtitle, relativeFileName)
        }
    }

    fun setAdminPin(newPin: String) {
        viewModelScope.launch { repository.setCustomPin(newPin) }
    }

    fun recordPinSuccess() {
        viewModelScope.launch { repository.recordPinSuccess() }
    }

    fun recordPinFailure(newFailedAttempts: Int, lockedUntilEpochMillis: Long) {
        viewModelScope.launch { repository.recordPinFailure(newFailedAttempts, lockedUntilEpochMillis) }
    }

    fun dismissDefaultPinPrompt() {
        viewModelScope.launch { repository.dismissDefaultPinPrompt() }
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

    fun setJumuahMode(mode: JumuahMode) {
        viewModelScope.launch { repository.setJumuahMode(mode) }
    }

    fun setJumuahTimes(times: List<String>) {
        viewModelScope.launch { repository.setJumuahTimes(times) }
    }

    fun setSilentJumuahEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSilentJumuahEnabled(enabled) }
    }

    fun setSilentJumuahLeadMinutes(minutes: Int) {
        viewModelScope.launch { repository.setSilentJumuahLeadMinutes(minutes) }
    }

    fun setSilentJumuahDurationMinutes(minutes: Int) {
        viewModelScope.launch { repository.setSilentJumuahDurationMinutes(minutes) }
    }

    fun setSilentDailyPrayerConfig(prayer: Prayer, config: PrayerSilentConfig) {
        val currentIqamah = settings.value.iqamahFor(prayer)
        val canEnable = SilentScreenCalculator.canEnableDailySilent(config.anchor, currentIqamah)
        val safeConfig = config.sanitized().let { if (!canEnable) it.copy(enabled = false) else it }
        viewModelScope.launch { repository.setSilentDailyPrayerConfig(prayer, safeConfig) }
    }

    fun copySilentSettingsToAllPrayers(source: PrayerSilentConfig): CopyPrayerSilentResult {
        val result = SilentScreenCalculator.copySettingsToAllPrayers(source, settings.value.iqamahOffsets())
        viewModelScope.launch { repository.setSilentDailyPrayerConfigs(result.updatedPrayers) }
        return result
    }

    fun startManualSilentScreen(durationMinutes: Int) {
        val clamped = durationMinutes.coerceIn(1, 120)
        val until = System.currentTimeMillis() + clamped * 60_000L
        viewModelScope.launch { repository.setManualSilentUntil(until) }
    }

    fun stopManualSilentScreen() {
        viewModelScope.launch { repository.setManualSilentUntil(0L) }
    }

    fun completeSetup() {
        viewModelScope.launch {
            repository.setMosqueMode(true)
            repository.setSetupCompleted(true)
        }
    }

    fun restartSetupWizard() {
        viewModelScope.launch { repository.setSetupCompleted(false) }
    }
}
