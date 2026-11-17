package com.salatsign.tv.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salatsign.tv.SalatSignApp
import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalatSignApp
    private val repository = app.settingsRepository
    private val scheduler = app.alarmScheduler

    val settings: StateFlow<AppSettings> = repository.appSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun toggleMosqueMode(enable: Boolean) {
        viewModelScope.launch {
            repository.setMosqueMode(enable)
            val updated = settings.value.copy(isMosqueMode = enable)
            scheduler.scheduleNext(updated)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun selectCity(city: CityPreset) {
        viewModelScope.launch {
            repository.selectCity(city)
            val updated = settings.value.copy(
                latitude = city.latitude,
                longitude = city.longitude,
                cityName = city.name,
                calculationMethodName = city.calculationMethod
            )
            scheduler.scheduleNext(updated)
        }
    }

    fun setHijriOffset(offset: Int) {
        viewModelScope.launch {
            repository.updateHijriOffset(offset)
        }
    }

    fun togglePrePrayerReminder(enable: Boolean) {
        viewModelScope.launch {
            repository.setPrePrayerReminder(enable)
            val updated = settings.value.copy(prePrayerReminderEnabled = enable)
            scheduler.scheduleNext(updated)
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
        viewModelScope.launch {
            repository.setMosqueIconPreset(preset)
        }
    }

    fun setMosqueFontPreset(preset: MosqueFontPreset) {
        viewModelScope.launch {
            repository.setMosqueFontPreset(preset)
        }
    }

    fun updateScheduleDuration(seconds: Int) {
        viewModelScope.launch {
            repository.updateScheduleDuration(seconds)
        }
    }

    fun addSampleAnnouncementSlide() {
        viewModelScope.launch {
            val currentSlides = settings.value.announcementSlides.toMutableList()
            val newSlide = AnnouncementSlide(
                id = "slide_${System.currentTimeMillis()}",
                imageUri = "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=1200&q=80",
                title = "Kajian Rutin & Infaq Masjid",
                durationSeconds = 10
            )
            currentSlides.add(newSlide)
            repository.updateSlides(currentSlides)
        }
    }

    fun clearSlides() {
        viewModelScope.launch {
            repository.updateSlides(emptyList())
        }
    }

    fun completeSetup() {
        viewModelScope.launch {
            repository.setSetupCompleted(true)
            scheduler.scheduleNext(settings.value)
        }
    }

    fun restartSetupWizard() {
        viewModelScope.launch {
            repository.setSetupCompleted(false)
        }
    }
}
