// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.mosque

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.salatsign.core.tv.R
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerTimeItem
import com.salatsign.tv.ui.common.LiveClockHeader
import com.salatsign.tv.ui.common.getMosqueTitleStyle
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.PrayerCardsRow
import com.salatsign.tv.ui.common.pulsingGlow
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.EmeraldLight
import com.salatsign.tv.ui.theme.TvTypography

@Composable
fun MosqueSignageScreen(
    uiState: MosqueSignageUiState,
    adminHintTrigger: Long = 0L,
    onOpenSettings: () -> Unit
) {
    val screenFocusRequester = remember { FocusRequester() }
    val colorScheme = MaterialTheme.colorScheme
    var showAdminHint by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        screenFocusRequester.requestFocus()
    }

    LaunchedEffect(adminHintTrigger) {
        if (adminHintTrigger > 0L) {
            showAdminHint = true
            delay(4000L)
            showAdminHint = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .focusRequester(screenFocusRequester)
            .focusable()
            // Hidden Admin Access: Key.Menu (DPAD_CENTER 3s hold handled at root Activity)
            .onKeyEvent { keyEvent ->
                if (keyEvent.key == Key.Menu && keyEvent.type == KeyEventType.KeyUp) {
                    onOpenSettings()
                    true
                } else {
                    false
                }
            }
    ) {
        Crossfade(
            targetState = uiState.currentSlideState,
            animationSpec = tween(durationMillis = 800),
            label = "mosque_slideshow_crossfade"
        ) { slideState ->
            when (slideState) {
                is SlideState.PrayerSchedule -> {
                    MosqueScheduleView(
                        uiState = uiState
                    )
                }
                is SlideState.AnnouncementImage -> {
                    MosqueAnnouncementView(
                        slide = slideState.slide,
                        uiState = uiState
                    )
                }
                is SlideState.SilentScreen -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0E1110))
                    )
                }
            }
        }

        // Sliding remote-control instruction banner when user taps D-pad or OK
        AnimatedVisibility(
            visible = showAdminHint,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(250)),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(Color(0xFF0A2218).copy(alpha = 0.94f), RoundedCornerShape(14.dp))
                    .border(1.5.dp, AccentGold.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.mosque_hold_ok_toast),
                    style = TvTypography.BodyLabel.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun MosqueScheduleView(
    uiState: MosqueSignageUiState
) {
    val prayerTimes = uiState.prayerTimesDay
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .overscanPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // White-Label Header with Custom Mosque Icon & Font Presets
            LiveClockHeader(
                isMosqueMode = true,
                gregorianDate = prayerTimes?.gregorianDate ?: "",
                hijriDate = prayerTimes?.hijriDate ?: "",
                cityName = uiState.settings.cityName,
                zoneId = uiState.settings.zoneId,
                mosqueName = uiState.settings.mosqueName,
                mosqueSubtitle = uiState.settings.mosqueSubtitle,
                mosqueLogoUri = uiState.settings.mosqueLogoUri,
                mosqueIconPreset = uiState.settings.mosqueIconPreset,
                mosqueFontPreset = uiState.settings.mosqueFontPreset,
                isViewingToday = true,
                is24HourFormat = uiState.settings.is24HourFormat
            )

            if (prayerTimes != null) {
                // Mosque Big Next / Active Prayer Banner
                val nextPrayerItem = prayerTimes.nextPrayer
                if (uiState.interruptedActivePrayer != null) {
                    // Exact-Time Strike Pulsing Banner
                    MosqueActiveStrikeBanner(
                        prayer = prayerTimes.prayers.firstOrNull { it.prayer == uiState.interruptedActivePrayer }
                            ?: prayerTimes.prayers.first(),
                        is24HourFormat = uiState.settings.is24HourFormat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                    )
                } else if (nextPrayerItem != null) {
                    MosqueNextPrayerBanner(
                        activePrayer = prayerTimes.activePrayer,
                        nextPrayer = nextPrayerItem,
                        countdown = prayerTimes.countdownFormatted,
                        is24HourFormat = uiState.settings.is24HourFormat,
                        showArabicPrayerNames = uiState.settings.showArabicPrayerNames,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5 Daily Prayers + Sunrise Row
                PrayerCardsRow(
                    prayers = prayerTimes.prayers,
                    sunriseTime = prayerTimes.formattedSunrise(uiState.settings.is24HourFormat),
                    is24HourFormat = uiState.settings.is24HourFormat,
                    showArabicPrayerNames = uiState.settings.showArabicPrayerNames,
                    isMosqueMode = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f)
                )
            }
        }
    }
}

@Composable
fun MosqueActiveStrikeBanner(
    prayer: PrayerTimeItem,
    is24HourFormat: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val prayerLabel = if (prayer.prayer == Prayer.ZUHUR && prayer.isFriday) {
        stringResource(R.string.prayer_jumuah)
    } else {
        stringResource(prayer.prayer.labelRes())
    }

    val strikeShape = RoundedCornerShape(20.dp)
    val accentPrimary = colorScheme.primary
    val accentSecondary = colorScheme.secondary
    Box(
        modifier = modifier
            .clip(strikeShape)
            .background(colorScheme.surfaceVariant, strikeShape)
            .pulsingGlow(shape = strikeShape, glowColor = accentPrimary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(accentPrimary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.mosque_strike_title),
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Text(
                        text = prayer.prayer.arabicName,
                        maxLines = 1,
                        style = TvTypography.ArabicTitle.copy(fontSize = 28.sp, color = accentSecondary)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.mosque_strike_prayer_name, prayerLabel),
                    maxLines = 1,
                    style = TvTypography.HeaderTitle.copy(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                val formattedIqamah = prayer.formattedIqamahTime(is24HourFormat)
                if (formattedIqamah != null) {
                    Text(
                        text = stringResource(R.string.mosque_iqamah_prefix, formattedIqamah),
                        maxLines = 1,
                        style = TvTypography.Subtitle.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accentPrimary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = stringResource(R.string.mosque_strike_admonition),
                    maxLines = 2,
                    style = TvTypography.Subtitle.copy(fontSize = 20.sp, color = accentSecondary)
                )
            }

            Text(
                text = prayer.formattedTime(is24HourFormat),
                maxLines = 1,
                softWrap = false,
                style = TvTypography.ActivePrayerTime.copy(
                    fontSize = if (is24HourFormat) 68.sp else 58.sp,
                    color = accentPrimary
                )
            )
        }
    }
}

@Composable
fun MosqueNextPrayerBanner(
    activePrayer: PrayerTimeItem? = null,
    nextPrayer: PrayerTimeItem,
    countdown: String,
    is24HourFormat: Boolean = false,
    showArabicPrayerNames: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentPrimary = colorScheme.primary
    val accentSecondary = colorScheme.secondary
    val heroPrayer = activePrayer ?: nextPrayer
    val isShowingCurrent = activePrayer != null
    val heroLabel = if (heroPrayer.prayer == Prayer.ZUHUR && heroPrayer.isFriday) {
        stringResource(R.string.prayer_jumuah)
    } else {
        stringResource(heroPrayer.prayer.labelRes())
    }
    val nextLabel = if (nextPrayer.prayer == Prayer.ZUHUR && nextPrayer.isFriday) {
        stringResource(R.string.prayer_jumuah)
    } else {
        stringResource(nextPrayer.prayer.labelRes())
    }

    val bannerShape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .clip(bannerShape)
            .background(colorScheme.surfaceVariant, bannerShape)
            .border(width = 1.5.dp, color = accentPrimary.copy(alpha = 0.45f), shape = bannerShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isShowingCurrent) accentPrimary else accentPrimary.copy(alpha = 0.16f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isShowingCurrent) {
                                stringResource(R.string.home_badge_active_prayer)
                            } else {
                                stringResource(R.string.mosque_next_prayer_badge)
                            },
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isShowingCurrent) Color.White else accentPrimary
                            )
                        )
                    }

                    if (showArabicPrayerNames) {
                        Text(
                            text = heroPrayer.prayer.arabicName,
                            maxLines = 1,
                            style = TvTypography.ArabicTitle.copy(fontSize = 24.sp, color = accentSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = heroLabel,
                    maxLines = 1,
                    style = TvTypography.HeaderTitle.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.onSurfaceVariant
                    )
                )

                val secondaryNote = heroPrayer.secondaryTimeNote
                if (secondaryNote != null) {
                    Text(
                        text = secondaryNote,
                        maxLines = 1,
                        style = TvTypography.Subtitle.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = accentSecondary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = accentSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isShowingCurrent) {
                                "$nextLabel ${nextPrayer.formattedTime(is24HourFormat)} • $countdown"
                            } else {
                                stringResource(R.string.mosque_countdown_to_prayer, countdown)
                            },
                            maxLines = 1,
                            softWrap = false,
                            style = TvTypography.CountdownText.copy(fontSize = 24.sp, color = accentSecondary)
                        )
                    }

                    val formattedIqamah = heroPrayer.formattedIqamahTime(is24HourFormat)
                    if (formattedIqamah != null) {
                        Box(
                            modifier = Modifier
                                .background(accentPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.mosque_iqamah_prefix, formattedIqamah),
                                maxLines = 1,
                                style = TvTypography.BodyLabel.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentPrimary
                                )
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = heroPrayer.formattedTime(is24HourFormat),
                    maxLines = 1,
                    softWrap = false,
                    style = TvTypography.ActivePrayerTime.copy(
                        fontSize = if (is24HourFormat) 68.sp else 58.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun MosqueAnnouncementView(
    slide: com.salatsign.tv.data.model.AnnouncementSlide,
    uiState: MosqueSignageUiState
) {
    val context = LocalContext.current
    val prayerTimes = uiState.prayerTimesDay

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (slide.type) {
            com.salatsign.tv.data.model.SlideType.TEXT -> {
                MosqueTextSlideContent(
                    slide = slide,
                    modifier = Modifier.fillMaxSize()
                )
            }
            com.salatsign.tv.data.model.SlideType.IMAGE -> {
                val imageFile = slide.imageFile?.let { java.io.File(context.filesDir, "content/images/$it") }
                if (imageFile != null && imageFile.exists()) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = slide.title.ifBlank { stringResource(R.string.desc_announcement_slide) },
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    MosqueTextSlideContent(
                        slide = slide,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Overlay: Bottom floating prayer mini-bar with live clock so congregation always sees time
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xDD000000), Color(0xF5000000))
                    )
                )
                .padding(horizontal = 48.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Slide Title or Mosque Name
                Column {
                    if (slide.title.isNotBlank()) {
                        Text(
                            text = slide.title,
                            style = TvTypography.HeaderTitle.copy(fontSize = 28.sp, color = Color.White)
                        )
                    } else if (uiState.settings.mosqueName.isNotBlank()) {
                        Text(
                            text = uiState.settings.mosqueName,
                            style = getMosqueTitleStyle(uiState.settings.mosqueFontPreset, Color.White).copy(fontSize = 28.sp)
                        )
                    }
                    if (uiState.settings.mosqueSubtitle.isNotBlank()) {
                        Text(
                            text = uiState.settings.mosqueSubtitle,
                            style = TvTypography.Subtitle.copy(fontSize = 18.sp, color = AccentGold)
                        )
                    }
                }

                // Center: 5 Prayers mini times
                if (prayerTimes != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (p in prayerTimes.prayers) {
                            val prayerLabel = if (p.prayer == Prayer.ZUHUR && p.isFriday) {
                                stringResource(R.string.prayer_jumuah)
                            } else {
                                stringResource(p.prayer.labelRes())
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = prayerLabel,
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 14.sp,
                                        fontWeight = if (p.isNext) FontWeight.Bold else FontWeight.Normal,
                                        color = if (p.isNext) AccentGold else Color(0xFFD1D5DB)
                                    )
                                )
                                Text(
                                    text = p.formattedTime(uiState.settings.is24HourFormat),
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (p.isNext) AccentEmerald else Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Right: Hijri Date
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = prayerTimes?.hijriDate ?: "",
                        style = TvTypography.Subtitle.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentGold
                        )
                    )
                    Text(
                        text = prayerTimes?.gregorianDate ?: "",
                        style = TvTypography.BodyLabel.copy(fontSize = 16.sp, color = Color.White)
                    )
                }
            }
        }
    }
}

@Composable
fun MosqueTextSlideContent(
    slide: com.salatsign.tv.data.model.AnnouncementSlide,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorScheme.surfaceVariant,
                        colorScheme.background
                    )
                )
            )
            .padding(horizontal = 80.dp, vertical = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (slide.title.isNotBlank()) {
                Text(
                    text = slide.title,
                    style = TvTypography.HeaderTitle.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        textAlign = TextAlign.Center,
                        textDirection = TextDirection.ContentOrLtr
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (slide.body.isNotBlank()) {
                Text(
                    text = slide.body,
                    style = TvTypography.BodyLabel.copy(
                        fontSize = 30.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Normal,
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        textDirection = TextDirection.ContentOrLtr
                    )
                )
            }
        }
    }
}
