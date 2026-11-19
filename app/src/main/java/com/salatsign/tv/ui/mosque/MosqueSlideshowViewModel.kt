// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.mosque

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salatsign.tv.R
import com.salatsign.tv.SalatSignApp
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerTimesDay
import com.salatsign.tv.engine.DailyPrayerCacheKey
import com.salatsign.tv.engine.PrayerCalculationEngine
import com.salatsign.tv.engine.PrayerDayCache
import com.salatsign.tv.ui.common.LocaleHelper
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
import java.time.ZonedDateTime

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
    private val contentRepository = app.contentRepository

    private val settingsState = settingsRepository.appSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    private val _slideState = MutableStateFlow<SlideState>(SlideState.PrayerSchedule(60))
    val slideState: StateFlow<SlideState> = _slideState.asStateFlow()

    private val _interruptedActivePrayer = MutableStateFlow<Prayer?>(null)
    val interruptedActivePrayer: StateFlow<Prayer?> = _interruptedActivePrayer.asStateFlow()

    private val _tickerTime = MutableStateFlow(LocalTime.now())
    private var slideshowLoopJob: Job? = null
    private val prayerDayCache = PrayerDayCache()

    private fun getPrayerTimesDay(settings: AppSettings, nowZoned: ZonedDateTime): PrayerTimesDay {
        val hijriMonthNames = runCatching {
            LocaleHelper.wrap(app, settings.appLanguage).resources.getStringArray(R.array.hijri_months).toList()
        }.getOrDefault(emptyList())
        val key = DailyPrayerCacheKey(
            date = nowZoned.toLocalDate(),
            config = settings.prayerConfig(),
            hijriOffsetDays = settings.hijriOffsetDays,
            hijriChangeAtMidnight = settings.hijriChangeAtMidnight,
            iqamahOffsets = settings.iqamahOffsets(),
            isMosqueMode = settings.isMosqueMode,
            jumuahMode = settings.jumuahMode,
            jumuahTimes = settings.jumuahTimes,
            is24HourFormat = settings.is24HourFormat,
            hijriMonthNames = hijriMonthNames
        )
        return prayerDayCache.getOrCompute(key, nowZoned)
    }

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
                val settings = settingsState.value
                val zone = runCatching { ZoneId.of(settings.zoneId) }.getOrDefault(ZoneId.systemDefault())
                val nowZoned = ZonedDateTime.now(zone)
                val now = nowZoned.toLocalTime()

                val prayerTimes = getPrayerTimesDay(settings, nowZoned)

                // Check if currently within exact prayer strike window (5 minutes)
                val strikingPrayer = if (settings.showStrikeHighlight) {
                    PrayerCalculationEngine.getPrayerAtExactTimeStrike(prayerTimes, now, 5, isMosqueMode = settings.isMosqueMode)
                } else null

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
                    val currentNow = ZonedDateTime.now(zone).toLocalTime()
                    val strike = if (settings.showStrikeHighlight) {
                        PrayerCalculationEngine.getPrayerAtExactTimeStrike(prayerTimes, currentNow, 5)
                    } else null

                    if (strike != null) {
                        _interruptedActivePrayer.value = strike
                        break
                    }
                }

                if (_interruptedActivePrayer.value != null) {
                    continue
                }

                // Step 2: Transition through Announcement Slides
                val currentSettings = settingsState.value
                val currentZone = runCatching { ZoneId.of(currentSettings.zoneId) }.getOrDefault(ZoneId.systemDefault())
                val filterToday = ZonedDateTime.now(currentZone).toLocalDate()
                val allSlides = contentRepository.slidesFlow.value.slides
                val activeSlides = contentRepository.getActiveSlides(allSlides, filterToday)
                if (activeSlides.isNotEmpty()) {
                    for ((index, slide) in activeSlides.withIndex()) {
                        // Recompute today and prayerTimes at the start of every slide
                        val slideSettings = settingsState.value
                        val slideZone = runCatching { ZoneId.of(slideSettings.zoneId) }.getOrDefault(ZoneId.systemDefault())
                        val slideNowZoned = ZonedDateTime.now(slideZone)
                        val slideNow = slideNowZoned.toLocalTime()

                        val slidePrayerTimes = getPrayerTimesDay(slideSettings, slideNowZoned)

                        // Check strike at the start of this slide
                        val startStrike = if (slideSettings.showStrikeHighlight) {
                            PrayerCalculationEngine.getPrayerAtExactTimeStrike(slidePrayerTimes, slideNow, 5, isMosqueMode = slideSettings.isMosqueMode)
                        } else null

                        if (startStrike != null) {
                            _interruptedActivePrayer.value = startStrike
                            _slideState.value = SlideState.PrayerSchedule(slideSettings.scheduleDurationSeconds.coerceAtLeast(10))
                            break
                        }

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
                            val currentNow = ZonedDateTime.now(slideZone).toLocalTime()
                            val strike = if (slideSettings.showStrikeHighlight) {
                                PrayerCalculationEngine.getPrayerAtExactTimeStrike(slidePrayerTimes, currentNow, 5)
                            } else null

                            if (strike != null) {
                                // Silent Exact-Time Interruption: Cancel slide immediately and return to schedule
                                _interruptedActivePrayer.value = strike
                                _slideState.value = SlideState.PrayerSchedule(slideSettings.scheduleDurationSeconds.coerceAtLeast(10))
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
        settingsState,
        _tickerTime,
        _interruptedActivePrayer
    ) { currentSlide, settings, _, activePrayer ->
        val zone = runCatching { ZoneId.of(settings.zoneId) }.getOrDefault(ZoneId.systemDefault())
        val nowZoned = ZonedDateTime.now(zone)

        val prayerTimes = getPrayerTimesDay(settings, nowZoned)

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
