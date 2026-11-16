package com.salatsign.tv.ui.mosque

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salatsign.tv.SalatSignApp
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerTimesDay
import com.salatsign.tv.engine.PrayerCalculationEngine
import kotlinx.coroutines.Job
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

sealed interface SlideState {
    data class PrayerSchedule(val durationSeconds: Int) : SlideState
    data class AnnouncementImage(
        val slideIndex: Int,
        val slide: AnnouncementSlide,
        val durationSeconds: Int
    ) : SlideState
}

data class MosqueSignageUiState(
    val currentSlideState: SlideState = SlideState.PrayerSchedule(60),
    val prayerTimesDay: PrayerTimesDay? = null,
    val settings: AppSettings = AppSettings(),
    val interruptedActivePrayer: Prayer? = null
)

class MosqueSlideshowViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalatSignApp
    private val settingsRepository = app.settingsRepository

    private val _slideState = MutableStateFlow<SlideState>(SlideState.PrayerSchedule(60))
    val slideState: StateFlow<SlideState> = _slideState.asStateFlow()

    private val _interruptedActivePrayer = MutableStateFlow<Prayer?>(null)
    val interruptedActivePrayer: StateFlow<Prayer?> = _interruptedActivePrayer.asStateFlow()

    private val _tickerTime = MutableStateFlow(LocalTime.now())
    private var slideshowLoopJob: Job? = null

    init {
        // 1-second ticker for prayer strike monitoring and live clock
        viewModelScope.launch {
            while (isActive) {
                _tickerTime.value = LocalTime.now()
                delay(1000)
            }
        }

        // Start slideshow loop
        startSlideshowLoop()
    }

    private fun startSlideshowLoop() {
        slideshowLoopJob?.cancel()
        slideshowLoopJob = viewModelScope.launch {
            while (isActive) {
                // Get current settings
                val settings = settingsRepository.appSettingsFlow.stateIn(viewModelScope).value
                val today = LocalDate.now()
                val now = LocalTime.now()
                val prayerTimes = PrayerCalculationEngine.calculatePrayerTimesDay(
                    date = today,
                    latitude = settings.latitude,
                    longitude = settings.longitude,
                    calculationMethodName = settings.calculationMethodName,
                    hijriOffsetDays = settings.hijriOffsetDays,
                    referenceTime = now,
                    referenceDate = today,
                    zoneId = ZoneId.systemDefault()
                )

                // Check if currently within exact prayer strike window (5 minutes)
                val strikingPrayer = PrayerCalculationEngine.getPrayerAtExactTimeStrike(prayerTimes, now, 5)
                if (strikingPrayer != null) {
                    _interruptedActivePrayer.value = strikingPrayer
                    // Stay on Prayer Schedule silently pulsing during exact strike
                    _slideState.value = SlideState.PrayerSchedule(settings.scheduleDurationSeconds)
                    delay(5000) // Re-evaluate in 5 seconds
                    continue
                } else {
                    _interruptedActivePrayer.value = null
                }

                // Step 1: Show Prayer Schedule for T_schedule seconds
                val scheduleDuration = settings.scheduleDurationSeconds.coerceAtLeast(10)
                _slideState.value = SlideState.PrayerSchedule(scheduleDuration)

                // Wait for scheduleDuration with 1-second interval checks for exact prayer strike
                for (sec in 0 until scheduleDuration) {
                    delay(1000)
                    val currentNow = LocalTime.now()
                    val strike = PrayerCalculationEngine.getPrayerAtExactTimeStrike(prayerTimes, currentNow, 5)
                    if (strike != null) {
                        _interruptedActivePrayer.value = strike
                        break
                    }
                }

                if (_interruptedActivePrayer.value != null) {
                    continue
                }

                // Step 2: Transition through Announcement Slides
                val slides = settings.announcementSlides
                if (slides.isNotEmpty()) {
                    for ((index, slide) in slides.withIndex()) {
                        val slideDuration = slide.durationSeconds.coerceAtLeast(3)
                        _slideState.value = SlideState.AnnouncementImage(
                            slideIndex = index,
                            slide = slide,
                            durationSeconds = slideDuration
                        )

                        // Wait for slideDuration with exact strike interruption check
                        var interrupted = false
                        for (sec in 0 until slideDuration) {
                            delay(1000)
                            val currentNow = LocalTime.now()
                            val strike = PrayerCalculationEngine.getPrayerAtExactTimeStrike(prayerTimes, currentNow, 5)
                            if (strike != null) {
                                // Silent Exact-Time Interruption: Cancel slide immediately and return to schedule
                                _interruptedActivePrayer.value = strike
                                _slideState.value = SlideState.PrayerSchedule(scheduleDuration)
                                interrupted = true
                                break
                            }
                        }

                        if (interrupted) {
                            break
                        }
                    }
                }
            }
        }
    }

    val uiState: StateFlow<MosqueSignageUiState> = combine(
        _slideState,
        settingsRepository.appSettingsFlow,
        _tickerTime,
        _interruptedActivePrayer
    ) { currentSlide, settings, currentTime, activePrayer ->
        val today = LocalDate.now()
        val prayerTimes = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = today,
            latitude = settings.latitude,
            longitude = settings.longitude,
            calculationMethodName = settings.calculationMethodName,
            hijriOffsetDays = settings.hijriOffsetDays,
            referenceTime = currentTime,
            referenceDate = today,
            zoneId = ZoneId.systemDefault()
        )

        // If interrupted active prayer is present, override isActive flag
        val updatedPrayers = if (activePrayer != null) {
            prayerTimes.prayers.map { item ->
                if (item.prayer == activePrayer) {
                    item.copy(isActive = true)
                } else {
                    item.copy(isActive = false)
                }
            }
        } else {
            prayerTimes.prayers
        }

        MosqueSignageUiState(
            currentSlideState = currentSlide,
            prayerTimesDay = prayerTimes.copy(prayers = updatedPrayers),
            settings = settings,
            interruptedActivePrayer = activePrayer
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MosqueSignageUiState()
    )
}
