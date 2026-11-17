package com.salatsign.tv.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salatsign.tv.BuildConfig
import com.salatsign.tv.SalatSignApp
import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.SlideContainer
import com.salatsign.tv.data.model.SlideType
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.data.repository.ContentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalatSignApp
    private val repository = app.settingsRepository
    private val contentRepository = app.contentRepository
    private val scheduler = app.alarmScheduler

    val settings: StateFlow<AppSettings> = repository.appSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val slideContainer: StateFlow<SlideContainer> = contentRepository.slidesFlow

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
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch {
            val sampleSlide = AnnouncementSlide(
                id = "slide_${System.currentTimeMillis()}",
                type = SlideType.TEXT,
                title = "Kajian Rutin & Infaq Masjid",
                body = "Mari ramaikan shalat berjamaah dan kajian rutin setiap malam Ahad ba'da Magrib.",
                durationSeconds = 10
            )
            contentRepository.addSlide(sampleSlide)
        }
    }

    fun clearSlides() {
        viewModelScope.launch {
            val current = contentRepository.slidesFlow.value
            contentRepository.replaceAll(current.revision, emptyList())
        }
    }

    fun deleteSlide(slideId: String) {
        viewModelScope.launch {
            contentRepository.deleteSlide(slideId)
        }
    }

    fun addTextSlide(title: String, body: String, durationSeconds: Int) {
        viewModelScope.launch {
            val slide = AnnouncementSlide(
                id = "slide_${System.currentTimeMillis()}",
                type = SlideType.TEXT,
                title = title.take(ContentRepository.MAX_TITLE_LENGTH),
                body = body.take(ContentRepository.MAX_BODY_LENGTH),
                durationSeconds = durationSeconds.coerceIn(
                    ContentRepository.MIN_DURATION_SECONDS,
                    ContentRepository.MAX_DURATION_SECONDS
                )
            )
            contentRepository.addSlide(slide)
        }
    }

    fun addImageSlide(title: String, imageFileName: String, durationSeconds: Int) {
        viewModelScope.launch {
            val slide = AnnouncementSlide(
                id = "slide_${System.currentTimeMillis()}",
                type = SlideType.IMAGE,
                title = title.take(ContentRepository.MAX_TITLE_LENGTH),
                imageFile = imageFileName,
                durationSeconds = durationSeconds.coerceIn(
                    ContentRepository.MIN_DURATION_SECONDS,
                    ContentRepository.MAX_DURATION_SECONDS
                )
            )
            contentRepository.addSlide(slide)
        }
    }

    fun setCustomLogo(relativeFileName: String) {
        viewModelScope.launch {
            repository.setMosqueIconPreset(MosqueIconPreset.CUSTOM)
            repository.updateMosqueBranding(settings.value.mosqueName, settings.value.mosqueSubtitle, relativeFileName)
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

    fun setCustomPin(pin: String) {
        viewModelScope.launch {
            repository.setCustomPin(pin)
        }
    }

    fun resetToDefaultPin() {
        viewModelScope.launch {
            repository.resetToDefaultPin()
        }
    }

    fun toggleHomeModePinRequired(required: Boolean) {
        viewModelScope.launch {
            repository.setHomeModePinRequired(required)
        }
    }

    fun recordPinSuccess() {
        viewModelScope.launch {
            repository.recordPinSuccess()
        }
    }

    fun recordPinFailure(newFailedAttempts: Int, lockedUntilEpochMillis: Long) {
        viewModelScope.launch {
            repository.recordPinFailure(newFailedAttempts, lockedUntilEpochMillis)
        }
    }

    fun dismissDefaultPinPrompt() {
        viewModelScope.launch {
            repository.dismissDefaultPinPrompt()
        }
    }

    fun setCalculationMethod(methodName: String) {
        viewModelScope.launch {
            repository.setCalculationMethod(methodName)
            val updated = settings.value.copy(calculationMethodName = methodName)
            scheduler.scheduleNext(updated)
        }
    }

    fun setMadhab(madhab: String) {
        viewModelScope.launch {
            repository.setMadhab(madhab)
            val updated = settings.value.copy(madhab = madhab)
            scheduler.scheduleNext(updated)
        }
    }

    fun setHighLatitudeRule(rule: String?) {
        viewModelScope.launch {
            repository.setHighLatitudeRule(rule)
            val updated = settings.value.copy(highLatitudeRule = rule)
            scheduler.scheduleNext(updated)
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
        viewModelScope.launch {
            repository.setSunriseOffset(minutes)
        }
    }

    fun setIqamahOffset(prayer: Prayer, minutes: Int) {
        viewModelScope.launch {
            repository.setIqamahOffset(prayer, minutes)
        }
    }

    fun setHijriChangeAtMidnight(changeAtMidnight: Boolean) {
        viewModelScope.launch {
            repository.setHijriChangeAtMidnight(changeAtMidnight)
        }
    }

    fun setMosqueModeAdhan(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMosqueModeAdhan(enabled)
            val updated = settings.value.copy(mosqueModeAdhanEnabled = enabled)
            scheduler.scheduleNext(updated)
        }
    }

    fun setMosqueModeReminders(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMosqueModeReminders(enabled)
            val updated = settings.value.copy(mosqueModeRemindersEnabled = enabled)
            scheduler.scheduleNext(updated)
        }
    }

    fun setShowStrikeHighlight(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowStrikeHighlight(enabled)
        }
    }

    fun setZoneId(zoneId: String) {
        viewModelScope.launch {
            repository.setZoneId(zoneId)
            val updated = settings.value.copy(zoneId = zoneId)
            scheduler.scheduleNext(updated)
        }
    }

    fun setAppLanguage(language: com.salatsign.tv.data.model.AppLanguage) {
        viewModelScope.launch {
            repository.setAppLanguage(language)
        }
    }

    fun set24HourFormat(enabled: Boolean) {
        viewModelScope.launch {
            repository.set24HourFormat(enabled)
        }
    }

    fun setShowArabicPrayerNames(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowArabicPrayerNames(enabled)
        }
    }
}
