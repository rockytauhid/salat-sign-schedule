// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.screensaver

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.salatschedule.tv.R as AppR
import com.salatsign.core.tv.R
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.ClassifiedIslamicEvent
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerTimeItem
import com.salatsign.tv.data.model.PrayerTimesDay
import com.salatsign.tv.data.model.QuranVerseItem
import com.salatsign.tv.data.repository.ISettingsRepository
import com.salatsign.tv.engine.DailyPrayerCacheKey
import com.salatsign.tv.engine.IslamicCalendarSlideRows
import com.salatsign.tv.engine.PrayerDayCache
import com.salatsign.tv.engine.QuranDurationCalculator
import com.salatsign.tv.engine.QuranShuffleBag
import com.salatsign.tv.engine.QuranVerseRepository
import com.salatsign.tv.engine.ScreensaverDefaults
import com.salatsign.tv.engine.ScreensaverRotationEngine
import com.salatsign.tv.engine.ScreensaverSlideKind
import com.salatsign.tv.ui.common.LocaleHelper
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.slides.QuranVerseSlide
import com.salatsign.tv.ui.slides.SharedIslamicCalendarSlide
import com.salatsign.tv.ui.theme.SalatSignTheme
import com.salatsign.tv.ui.theme.TvTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

private val ScreensaverBg = Color(0xFF060E0A)

private sealed interface ScreensaverSlideState {
    data class Schedule(
        val clockText: String,
        val gregorianDate: String,
        val hijriDate: String,
        val prayers: List<PrayerTimeItem>,
        val sunriseText: String,
        val nextPrayer: PrayerTimeItem?,
        val minutesUntilNext: Long,
        val is24HourFormat: Boolean,
        val showArabicPrayerNames: Boolean
    ) : ScreensaverSlideState

    data class Calendar(
        val rows: List<ClassifiedIslamicEvent>,
        val hijriDate: String,
        val gregorianDate: String
    ) : ScreensaverSlideState

    data class Quran(
        val verse: QuranVerseItem,
        val showTranslation: Boolean
    ) : ScreensaverSlideState
}

/**
 * Self-contained screensaver host shared by [SalatScreensaverService] and [ScreensaverPreviewActivity].
 * Always renders in Dark Theme, loads DataStore/Quran off the main thread, updates clock once per minute,
 * recomputes prayer/calendar rows once per day, and shifts content by <= 8px every 60s for burn-in safety.
 */
@Composable
fun ScreensaverHost(
    settingsRepository: ISettingsRepository,
    quranVerseRepository: QuranVerseRepository
) {
    val context = LocalContext.current
    var loadedSettings by remember { mutableStateOf<AppSettings?>(null) }
    var quranRefs by remember { mutableStateOf<List<String>>(emptyList()) }
    var minuteTick by remember { mutableLongStateOf(0L) }
    var activeSlide by remember { mutableStateOf<ScreensaverSlideState?>(null) }
    var overflowSkipCounter by remember { mutableIntStateOf(0) }

    val prayerDayCache = remember { PrayerDayCache() }
    var shuffleBag by remember { mutableStateOf(QuranShuffleBag()) }

    // Initial non-blocking load off the main thread
    LaunchedEffect(Unit) {
        val (settings, refs) = withContext(Dispatchers.IO) {
            val s = settingsRepository.appSettingsFlow.first()
            val r = runCatching { quranVerseRepository.allReferences() }.getOrDefault(emptyList())
            s to r
        }
        loadedSettings = settings
        quranRefs = refs
    }

    // Minute ticker aligned to minute boundary (clock + burn-in shift update once per minute)
    LaunchedEffect(Unit) {
        while (isActive) {
            val nowMillis = System.currentTimeMillis()
            val millisToNextMinute = 60_000L - (nowMillis % 60_000L)
            delay(millisToNextMinute.coerceIn(1_000L, 60_000L))
            minuteTick++
        }
    }

    // Slide rotation loop (recomputes prayer times & calendar rows only when day or settings change)
    LaunchedEffect(loadedSettings, quranRefs) {
        val settings = loadedSettings ?: return@LaunchedEffect
        var cachedDate: LocalDate? = null
        var cachedPrayerDay: PrayerTimesDay? = null
        var cachedCalendarRows: List<ClassifiedIslamicEvent> = emptyList()
        var slideIdx = 0

        suspend fun ensureDailyData(nowZoned: ZonedDateTime): Pair<PrayerTimesDay, List<ClassifiedIslamicEvent>> {
            val today = nowZoned.toLocalDate()
            if (cachedDate != today || cachedPrayerDay == null) {
                val (day, calRows) = withContext(Dispatchers.IO) {
                    val hijriMonths = runCatching {
                        LocaleHelper.wrap(context, settings.appLanguage)
                            .resources.getStringArray(R.array.hijri_months).toList()
                    }.getOrDefault(emptyList())
                    val key = DailyPrayerCacheKey(
                        date = today,
                        config = settings.prayerConfig(),
                        hijriOffsetDays = settings.hijriOffsetDays,
                        hijriChangeAtMidnight = settings.hijriChangeAtMidnight,
                        iqamahOffsets = emptyMap(),
                        hijriMonthNames = hijriMonths
                    )
                    val computedDay = prayerDayCache.getOrCompute(key, nowZoned)
                    val rows = IslamicCalendarSlideRows.compute(
                        today = today,
                        enabledIds = settings.enabledIslamicEventIds,
                        adjustmentDays = settings.hijriOffsetDays
                    )
                    computedDay to rows
                }
                cachedDate = today
                cachedPrayerDay = day
                cachedCalendarRows = calRows
            }
            return cachedPrayerDay!! to cachedCalendarRows
        }

        while (isActive) {
            val zone = runCatching { ZoneId.of(settings.zoneId) }.getOrDefault(ZoneId.systemDefault())
            val nowZoned = ZonedDateTime.now(zone)
            val (prayerDay, calendarRows) = ensureDailyData(nowZoned)

            val activeKinds = ScreensaverRotationEngine.buildActiveSlides(
                scheduleEnabled = settings.screensaverScheduleEnabled,
                calendarEnabled = settings.screensaverCalendarEnabled,
                quranEnabled = settings.screensaverQuranEnabled,
                hasCalendarEvents = calendarRows.isNotEmpty(),
                hasQuranData = quranRefs.isNotEmpty()
            )
            if (slideIdx >= activeKinds.size) slideIdx = 0
            val currentKind = activeKinds[slideIdx]
            var durationSeconds = settings.screensaverScheduleDurationSeconds

            when (currentKind) {
                ScreensaverSlideKind.SCHEDULE -> {
                    val nowTime = nowZoned.toLocalTime()
                    val prayers = ScreensaverRotationEngine.normalizeHomePrayers(prayerDay)
                    val nextPrayer = prayers.firstOrNull { it.time?.isAfter(nowTime) == true } ?: prayers.firstOrNull()
                    val minsUntil = nextPrayer?.let {
                        ScreensaverRotationEngine.minutesUntilNextPrayer(nowTime, it)
                    } ?: 0L
                    activeSlide = ScreensaverSlideState.Schedule(
                        clockText = ScreensaverRotationEngine.formatClockMinutes(nowTime, settings.is24HourFormat),
                        gregorianDate = prayerDay.gregorianDate,
                        hijriDate = prayerDay.hijriDate,
                        prayers = prayers,
                        sunriseText = prayerDay.formattedSunrise(settings.is24HourFormat),
                        nextPrayer = nextPrayer,
                        minutesUntilNext = minsUntil,
                        is24HourFormat = settings.is24HourFormat,
                        showArabicPrayerNames = settings.showArabicPrayerNames
                    )
                    durationSeconds = settings.screensaverScheduleDurationSeconds
                }

                ScreensaverSlideKind.CALENDAR -> {
                    activeSlide = ScreensaverSlideState.Calendar(
                        rows = calendarRows,
                        hijriDate = prayerDay.hijriDate,
                        gregorianDate = prayerDay.gregorianDate
                    )
                    durationSeconds = settings.screensaverCalendarDurationSeconds
                }

                ScreensaverSlideKind.QURAN -> {
                    val verseLang = if (settings.appLanguage == AppLanguage.AR) AppLanguage.EN else settings.appLanguage
                    val drawn = shuffleBag.next(quranRefs)
                    val verse = if (drawn != null) {
                        shuffleBag = drawn.second
                        withContext(Dispatchers.IO) {
                            quranVerseRepository.getVerse(drawn.first, verseLang)
                        }
                    } else {
                        null
                    }
                    if (verse != null) {
                        activeSlide = ScreensaverSlideState.Quran(
                            verse = verse,
                            showTranslation = true
                        )
                        durationSeconds = QuranDurationCalculator.autoDurationSeconds(
                            arabicText = verse.arabicText,
                            translationText = verse.translationText,
                            showTranslation = true
                        )
                    } else {
                        slideIdx = ScreensaverRotationEngine.nextIndex(slideIdx, activeKinds.size)
                        continue
                    }
                }
            }

            // Wait for slide duration; check overflow skip and refresh clock on minute boundary if staying on Schedule
            val startSkip = overflowSkipCounter
            val totalMs = durationSeconds * 1000L
            var elapsedMs = 0L
            while (elapsedMs < totalMs && isActive) {
                val step = minOf(500L, totalMs - elapsedMs)
                delay(step)
                elapsedMs += step
                if (overflowSkipCounter != startSkip) break
                if (currentKind == ScreensaverSlideKind.SCHEDULE && activeKinds.size == 1) {
                    val refreshedNow = ZonedDateTime.now(zone).toLocalTime()
                    val cur = activeSlide as? ScreensaverSlideState.Schedule
                    val formatted = ScreensaverRotationEngine.formatClockMinutes(refreshedNow, settings.is24HourFormat)
                    if (cur != null && cur.clockText != formatted) {
                        val nextP = cur.prayers.firstOrNull { it.time?.isAfter(refreshedNow) == true } ?: cur.prayers.firstOrNull()
                        val mins = nextP?.let { ScreensaverRotationEngine.minutesUntilNextPrayer(refreshedNow, it) } ?: 0L
                        activeSlide = cur.copy(clockText = formatted, nextPrayer = nextP, minutesUntilNext = mins)
                    }
                }
            }

            slideIdx = ScreensaverRotationEngine.nextIndex(slideIdx, activeKinds.size)
        }
    }

    val (dx, dy) = ScreensaverRotationEngine.burnInOffsetPx(minuteTick)

    SalatSignTheme(isDark = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreensaverBg)
        ) {
            val current = activeSlide
            if (current != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(dx, dy) }
                ) {
                    Crossfade(
                        targetState = current,
                        animationSpec = tween(durationMillis = ScreensaverDefaults.CROSSFADE_DURATION_MS),
                        label = "screensaver_slide_crossfade"
                    ) { slide ->
                        when (slide) {
                            is ScreensaverSlideState.Schedule -> ScreensaverScheduleSlide(slide)
                            is ScreensaverSlideState.Calendar -> SharedIslamicCalendarSlide(
                                rows = slide.rows,
                                hijriDate = slide.hijriDate,
                                gregorianDate = slide.gregorianDate,
                                modifier = Modifier.background(ScreensaverBg)
                            )
                            is ScreensaverSlideState.Quran -> QuranVerseSlide(
                                verse = slide.verse,
                                showTranslation = slide.showTranslation,
                                onOverflowSkip = { overflowSkipCounter++ },
                                modifier = Modifier.background(ScreensaverBg)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreensaverScheduleSlide(slide: ScreensaverSlideState.Schedule) {
    val colorScheme = MaterialTheme.colorScheme
    val hours = slide.minutesUntilNext / 60
    val mins = slide.minutesUntilNext % 60
    val nextLabel = slide.nextPrayer?.let { stringResource(it.prayer.labelRes(isFriday = false)) }.orEmpty()
    val countdownText = when {
        nextLabel.isBlank() -> ""
        hours > 0 -> stringResource(AppR.string.screensaver_countdown_hm, nextLabel, hours.toInt(), mins.toInt())
        else -> stringResource(AppR.string.screensaver_countdown_m, nextLabel, mins.toInt())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreensaverBg)
            .overscanPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Large clock (no seconds) + Gregorian & Hijri dates (baseline-aligned at bottom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = slide.clockText,
                    modifier = Modifier.alignByBaseline(),
                    style = TvTypography.ActivePrayerTime.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground.copy(alpha = 0.92f)
                    )
                )

                Column(
                    modifier = Modifier.alignBy(LastBaseline),
                    horizontalAlignment = Alignment.End
                ) {
                    if (slide.hijriDate.isNotBlank()) {
                        Text(
                            text = slide.hijriDate,
                            style = TvTypography.Subtitle.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.secondary.copy(alpha = 0.90f)
                            )
                        )
                    }
                    if (slide.gregorianDate.isNotBlank()) {
                        Text(
                            text = slide.gregorianDate,
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 19.sp,
                                color = colorScheme.onBackground.copy(alpha = 0.78f)
                            )
                        )
                    }
                }
            }

            // Center: Next prayer countdown
            if (countdownText.isNotBlank()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countdownText,
                        style = TvTypography.HeaderTitle.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.primary
                        )
                    )
                }
            }

            // Bottom: 5 prayers + Sunrise row with next prayer highlighted
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val cardShape = RoundedCornerShape(14.dp)
                val nextPrayerEnum = slide.nextPrayer?.prayer

                for (item in slide.prayers) {
                    val isNext = item.prayer == nextPrayerEnum
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isNext) colorScheme.surfaceVariant.copy(alpha = 0.85f)
                                else colorScheme.surface.copy(alpha = 0.55f),
                                cardShape
                            )
                            .then(
                                if (isNext) Modifier.border(2.dp, colorScheme.primary, cardShape)
                                else Modifier
                            )
                            .padding(vertical = 18.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (slide.showArabicPrayerNames) {
                            Text(
                                text = item.prayer.arabicName,
                                style = TvTypography.BodyLabel.copy(
                                    fontSize = 14.sp,
                                    color = colorScheme.secondary.copy(alpha = if (isNext) 0.95f else 0.65f)
                                )
                            )
                        }
                        Text(
                            text = stringResource(item.prayer.labelRes(isFriday = false)),
                            style = TvTypography.CardTitle.copy(
                                fontSize = 18.sp,
                                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                                color = if (isNext) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.82f)
                            )
                        )
                        Text(
                            text = item.formattedTime(slide.is24HourFormat),
                            style = TvTypography.HeaderTitle.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNext) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        )
                    }

                    // Sunrise card placed chronologically right after Fajr (Subuh) and before Dhuhr (Zuhur)
                    if (item.prayer == Prayer.SUBUH) {
                        Column(
                            modifier = Modifier
                                .weight(0.9f)
                                .background(colorScheme.surface.copy(alpha = 0.40f), cardShape)
                                .padding(vertical = 18.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (slide.showArabicPrayerNames) {
                                Text(
                                    text = "الشروق",
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 14.sp,
                                        color = colorScheme.onSurface.copy(alpha = 0.48f)
                                    )
                                )
                            }
                            Text(
                                text = stringResource(R.string.prayer_sunrise),
                                style = TvTypography.CardTitle.copy(
                                    fontSize = 17.sp,
                                    color = colorScheme.onSurface.copy(alpha = 0.68f)
                                )
                            )
                            Text(
                                text = slide.sunriseText,
                                style = TvTypography.HeaderTitle.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface.copy(alpha = 0.72f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
