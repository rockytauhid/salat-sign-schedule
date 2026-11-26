// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.mosque

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salatsign.core.tv.R
import com.salatsign.tv.SalatSignApp
import com.salatsign.tv.data.model.ActiveSilentState
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.ClassifiedIslamicEvent
import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerConfig
import com.salatsign.tv.data.model.PrayerTimesDay
import com.salatsign.tv.data.model.QuranVerseItem
import com.salatsign.tv.data.model.SilentScreenConfig
import com.salatsign.tv.data.model.SilentWindow
import com.salatsign.tv.data.model.SilentWindowKind
import com.salatsign.tv.engine.BuiltInSlideId
import com.salatsign.tv.engine.DailyPrayerCacheKey
import com.salatsign.tv.engine.IslamicCalendarSlideRows
import com.salatsign.tv.engine.PrayerCalculationEngine
import com.salatsign.tv.engine.PrayerDayCache
import com.salatsign.tv.engine.QuranShuffleBag
import com.salatsign.tv.engine.RotationSlideItem
import com.salatsign.tv.engine.SilentScreenCalculator
import com.salatsign.tv.engine.SlideshowRotationBuilder
import com.salatsign.tv.engine.WallClockSlideshowScheduler
import com.salatsign.tv.engine.WallClockSlot
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
    data class IslamicCalendar(
        val rows: List<ClassifiedIslamicEvent>,
        val durationSeconds: Int
    ) : SlideState
    data class QuranVerse(
        val verse: QuranVerseItem,
        val showTranslation: Boolean,
        val durationSeconds: Int
    ) : SlideState
    data class SilentScreen(
        val kind: SilentWindowKind
    ) : SlideState
}

data class MosqueSignageUiState(
    val currentSlideState: SlideState = SlideState.PrayerSchedule(60),
    val prayerTimesDay: PrayerTimesDay? = null,
    val settings: AppSettings = AppSettings(),
    val interruptedActivePrayer: Prayer? = null,
    val activeSilentState: ActiveSilentState? = null
)

private data class SilentWindowsCacheKey(
    val date: LocalDate,
    val prayerConfig: PrayerConfig,
    val silentConfig: SilentScreenConfig,
    val jumuahMode: JumuahMode,
    val jumuahTimes: List<String>,
    val iqamahOffsets: Map<Prayer, Int>
)

class MosqueSlideshowViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalatSignApp
    private val settingsRepository = app.settingsRepository
    private val contentRepository = app.contentRepository
    private val quranRepository = app.quranVerseRepository

    @Volatile
    private var skipCurrentQuranRequested: Boolean = false

    fun skipCurrentQuranVerse() {
        skipCurrentQuranRequested = true
    }

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

    @Volatile
    private var cachedSilentKey: SilentWindowsCacheKey? = null
    @Volatile
    private var cachedSilentWindows: List<SilentWindow> = emptyList()

    private fun evaluateSilentScreen(settings: AppSettings, nowZoned: ZonedDateTime): ActiveSilentState? {
        val zone = nowZoned.zone
        val manualUntil = SilentScreenCalculator.manualUntilZoned(settings.silentScreen.manualSilentUntilEpochMillis, zone)
        // Fast path: manual override does not even need scheduled window recomputation
        if (manualUntil != null && nowZoned.isBefore(manualUntil)) {
            return ActiveSilentState(kind = SilentWindowKind.MANUAL, until = manualUntil)
        }
        val key = SilentWindowsCacheKey(
            date = nowZoned.toLocalDate(),
            prayerConfig = settings.prayerConfig(),
            silentConfig = settings.silentScreen.copy(manualSilentUntilEpochMillis = 0L),
            jumuahMode = settings.jumuahMode,
            jumuahTimes = settings.jumuahTimes,
            iqamahOffsets = settings.iqamahOffsets()
        )
        val windows = if (cachedSilentKey == key) {
            cachedSilentWindows
        } else {
            val computed = SilentScreenCalculator.silentScreenWindowsAround(
                centerDate = key.date,
                prayerConfig = key.prayerConfig,
                silentConfig = key.silentConfig,
                jumuahMode = key.jumuahMode,
                jumuahTimes = key.jumuahTimes,
                iqamahOffsets = key.iqamahOffsets
            )
            cachedSilentKey = key
            cachedSilentWindows = computed
            computed
        }
        return SilentScreenCalculator.activeSilentState(nowZoned, windows, manualUntil)
    }

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
            isMosqueSignage = true,
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
            var activeQuranSlotKey: Triple<Long, Int, Pair<String, Boolean>>? = null
            var activeQuranSlide: SlideState.QuranVerse? = null

            while (isActive) {
                val settings = settingsState.value
                val zone = runCatching { ZoneId.of(settings.zoneId) }.getOrDefault(ZoneId.systemDefault())
                val nowZoned = ZonedDateTime.now(zone)
                val now = nowZoned.toLocalTime()
                val prayerTimes = getPrayerTimesDay(settings, nowZoned)
                val scheduleDuration = settings.scheduleDurationSeconds.coerceAtLeast(10)

                // Highest priority: Silent Screen ("Jumu'ah / Prayer in progress")
                val silentNow = evaluateSilentScreen(settings, nowZoned)
                if (silentNow != null) {
                    _interruptedActivePrayer.value = null
                    activeQuranSlotKey = null
                    activeQuranSlide = null
                    _slideState.value = SlideState.SilentScreen(silentNow.kind)
                } else {
                    // Exact prayer strike window (5 minutes): hold on Prayer Schedule
                    val strikingPrayer = evaluateStrike(prayerTimes, now, settings.showStrikeHighlight)
                    if (strikingPrayer != null) {
                        _interruptedActivePrayer.value = strikingPrayer
                        activeQuranSlotKey = null
                        activeQuranSlide = null
                        _slideState.value = SlideState.PrayerSchedule(scheduleDuration)
                    } else {
                        _interruptedActivePrayer.value = null

                        // Decision D25: Pure offline wall-clock slideshow sync (epochSeconds % totalCycleSeconds)
                        val filterToday = nowZoned.toLocalDate()
                        val allSlides = contentRepository.slidesFlow.value.slides
                        val activeSlides = contentRepository.getActiveSlides(allSlides, filterToday)
                        val calendarSlideRows = if (settings.islamicCalendarSlideEnabled) {
                            IslamicCalendarSlideRows.compute(
                                today = filterToday,
                                enabledIds = settings.enabledIslamicEventIds,
                                adjustmentDays = settings.hijriOffsetDays
                            )
                        } else {
                            emptyList()
                        }
                        val quranRefs = quranRepository.allReferences()
                        val builtInRegistrations = BuiltInSlideRegistry.registrationsFor(
                            settings = settings,
                            islamicCalendarHasContent = calendarSlideRows.isNotEmpty(),
                            quranVerseHasContent = quranRefs.isNotEmpty()
                        )
                        val rotationPlan = SlideshowRotationBuilder.build(activeSlides, builtInRegistrations)

                        val wallSlots = buildList {
                            add(WallClockSlot(slotId = "schedule", durationSeconds = scheduleDuration))
                            for (item in rotationPlan.runtimeItems) {
                                when (item) {
                                    is RotationSlideItem.Announcement -> add(
                                        WallClockSlot(
                                            slotId = "announcement:${item.slide.id}",
                                            durationSeconds = item.slide.durationSeconds.coerceAtLeast(3)
                                        )
                                    )
                                    is RotationSlideItem.BuiltIn -> when (item.id) {
                                        BuiltInSlideId.ISLAMIC_CALENDAR -> add(
                                            WallClockSlot(
                                                slotId = "builtin:ISLAMIC_CALENDAR",
                                                durationSeconds = settings.islamicCalendarSlideDurationSeconds.coerceIn(10, 60)
                                            )
                                        )
                                        BuiltInSlideId.QURAN_VERSE -> add(
                                            WallClockSlot(
                                                slotId = "builtin:QURAN_VERSE",
                                                durationSeconds = WallClockSlideshowScheduler.resolveQuranSlotDurationSeconds(
                                                    settings.quranVerseDurationSeconds
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        val resolution = WallClockSlideshowScheduler.resolve(
                            epochSeconds = nowZoned.toEpochSecond(),
                            slots = wallSlots
                        )

                        if (resolution == null || resolution.slotIndex == 0) {
                            activeQuranSlotKey = null
                            activeQuranSlide = null
                            _slideState.value = SlideState.PrayerSchedule(scheduleDuration)
                        } else {
                            val rotationIndex = resolution.slotIndex - 1
                            when (val item = rotationPlan.runtimeItems.getOrNull(rotationIndex)) {
                                is RotationSlideItem.Announcement -> {
                                    activeQuranSlotKey = null
                                    activeQuranSlide = null
                                    _slideState.value = SlideState.AnnouncementImage(
                                        slideIndex = rotationIndex,
                                        slide = item.slide,
                                        durationSeconds = resolution.durationSeconds
                                    )
                                }
                                is RotationSlideItem.BuiltIn -> when (item.id) {
                                    BuiltInSlideId.ISLAMIC_CALENDAR -> {
                                        activeQuranSlotKey = null
                                        activeQuranSlide = null
                                        _slideState.value = if (calendarSlideRows.isNotEmpty()) {
                                            SlideState.IslamicCalendar(
                                                rows = calendarSlideRows,
                                                durationSeconds = resolution.durationSeconds
                                            )
                                        } else {
                                            SlideState.PrayerSchedule(scheduleDuration)
                                        }
                                    }
                                    BuiltInSlideId.QURAN_VERSE -> {
                                        val slotKey = Triple(
                                            resolution.cycleIndex,
                                            resolution.slotIndex,
                                            settings.appLanguage.name to settings.quranShowTranslation
                                        )
                                        if (activeQuranSlotKey != slotKey || skipCurrentQuranRequested || activeQuranSlide == null) {
                                            skipCurrentQuranRequested = false
                                            val bag = QuranShuffleBag.deserialize(settings.quranShuffleBagState, quranRefs)
                                            val draw = bag.next(quranRefs)
                                            if (draw != null) {
                                                val (chosenRef, nextBag) = draw
                                                settingsRepository.setQuranShuffleBagState(nextBag.serialize())
                                                val verse = quranRepository.getVerse(chosenRef, settings.appLanguage)
                                                activeQuranSlide = verse?.let {
                                                    SlideState.QuranVerse(
                                                        verse = it,
                                                        showTranslation = settings.quranShowTranslation,
                                                        durationSeconds = resolution.durationSeconds
                                                    )
                                                }
                                                activeQuranSlotKey = slotKey
                                            } else {
                                                activeQuranSlide = null
                                                activeQuranSlotKey = null
                                            }
                                        }
                                        _slideState.value = activeQuranSlide ?: SlideState.PrayerSchedule(scheduleDuration)
                                    }
                                }
                                null -> {
                                    activeQuranSlotKey = null
                                    activeQuranSlide = null
                                    _slideState.value = SlideState.PrayerSchedule(scheduleDuration)
                                }
                            }
                        }
                    }
                }

                // Sleep aligned to the next wall-clock second boundary so NTP-synced TVs flip together
                val delayMs = (1000L - (System.currentTimeMillis() % 1000L)).coerceIn(100L, 1000L)
                delay(delayMs)
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
        val silentActive = evaluateSilentScreen(settings, nowZoned)
        val effectiveSlide = if (silentActive != null) {
            SlideState.SilentScreen(silentActive.kind)
        } else if (currentSlide is SlideState.SilentScreen) {
            SlideState.PrayerSchedule(settings.scheduleDurationSeconds.coerceAtLeast(10))
        } else {
            currentSlide
        }
        val effectiveActivePrayer = if (silentActive != null) null else activePrayer
        val updatedPrayers = applyActivePrayerOverride(prayerTimes, effectiveActivePrayer)

        MosqueSignageUiState(
            currentSlideState = effectiveSlide,
            prayerTimesDay = prayerTimes.copy(prayers = updatedPrayers),
            settings = settings,
            interruptedActivePrayer = effectiveActivePrayer,
            activeSilentState = silentActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MosqueSignageUiState(settings = AppSettings())
    )

    companion object {
        const val STRIKE_WINDOW_MINUTES = 5L

        /**
         * Evaluates whether [now] falls within the silent visual strike window for any prayer
         * (including Friday Jumu'ah sittings for SalatSign).
         */
        fun evaluateStrike(
            prayerTimes: PrayerTimesDay,
            now: LocalTime,
            showStrikeHighlight: Boolean
        ): Prayer? {
            if (!showStrikeHighlight) return null
            return PrayerCalculationEngine.getPrayerAtExactTimeStrike(
                prayerTimes,
                now,
                STRIKE_WINDOW_MINUTES,
                isMosqueSignage = true
            )
        }

        /**
         * Overrides [PrayerTimesDay.prayers] so only [activePrayer] is flagged active during a strike.
         */
        fun applyActivePrayerOverride(
            prayerTimes: PrayerTimesDay,
            activePrayer: Prayer?
        ) = if (activePrayer != null) {
            prayerTimes.prayers.map { item ->
                item.copy(isActive = item.prayer == activePrayer)
            }
        } else {
            prayerTimes.prayers
        }
    }
}
