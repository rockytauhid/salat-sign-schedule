package com.salatsign.tv.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salatsign.tv.SalatSignApp
import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.Prayer
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
            // Refresh alarms: if mosque mode enabled, scheduler will cancel all alarms!
            val updated = settings.value.copy(isMosqueMode = enable)
            scheduler.scheduleAlarmsForToday(updated)
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
            scheduler.scheduleAlarmsForToday(updated)
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
            scheduler.scheduleAlarmsForToday(updated)
        }
    }

    fun updateMosqueBranding(name: String, subtitle: String, logoUri: String?) {
        viewModelScope.launch {
            repository.updateMosqueBranding(name, subtitle, logoUri)
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
}
