// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.mosque

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.salatsign.tv.R
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerTimeItem
import com.salatsign.tv.ui.common.LiveClockHeader
import com.salatsign.tv.ui.common.getMosqueTitleStyle
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.pulsingGlow
import com.salatsign.tv.ui.home.PrayerCardsRow
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.EmeraldLight
import com.salatsign.tv.ui.theme.TvTypography

@Composable
fun MosqueSignageScreen(
    uiState: MosqueSignageUiState,
    onOpenSettings: () -> Unit
) {
    val screenFocusRequester = remember { FocusRequester() }
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        screenFocusRequester.requestFocus()
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
                } else if (prayerTimes.nextPrayer != null) {
                    MosqueNextPrayerBanner(
                        nextPrayer = prayerTimes.nextPrayer,
                        countdown = prayerTimes.countdownFormatted,
                        is24HourFormat = uiState.settings.is24HourFormat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5 Prayers Grid / Row
                PrayerCardsRow(
                    prayers = prayerTimes.prayers,
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
    is24HourFormat: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val prayerLabel = if (prayer.prayer == Prayer.ZUHUR && prayer.isFriday) {
        stringResource(R.string.prayer_jumuah)
    } else {
        stringResource(prayer.prayer.labelRes())
    }

    Card(
        onClick = {},
        colors = CardDefaults.colors(containerColor = colorScheme.surfaceVariant),
        shape = CardDefaults.shape(RoundedCornerShape(20.dp)),
        modifier = modifier
            .pulsingGlow(shape = RoundedCornerShape(20.dp), glowColor = AccentEmerald)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(AccentEmerald, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.mosque_strike_title),
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Text(
                        text = prayer.prayer.arabicName,
                        style = TvTypography.ArabicTitle.copy(fontSize = 32.sp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.mosque_strike_prayer_name, prayerLabel),
                    style = TvTypography.HeaderTitle.copy(
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                val formattedIqamah = prayer.formattedIqamahTime(is24HourFormat)
                if (formattedIqamah != null) {
                    Text(
                        text = stringResource(R.string.mosque_iqamah_prefix, formattedIqamah),
                        style = TvTypography.Subtitle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AccentEmerald)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = stringResource(R.string.mosque_strike_admonition),
                    style = TvTypography.Subtitle.copy(fontSize = 22.sp, color = AccentGold)
                )
            }

            Text(
                text = prayer.formattedTime(is24HourFormat),
                style = TvTypography.ActivePrayerTime.copy(fontSize = 84.sp, color = AccentEmerald)
            )
        }
    }
}

@Composable
fun MosqueNextPrayerBanner(
    nextPrayer: PrayerTimeItem,
    countdown: String,
    is24HourFormat: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val prayerLabel = if (nextPrayer.prayer == Prayer.ZUHUR && nextPrayer.isFriday) {
        stringResource(R.string.prayer_jumuah)
    } else {
        stringResource(nextPrayer.prayer.labelRes())
    }

    Card(
        onClick = {},
        colors = CardDefaults.colors(containerColor = colorScheme.surface),
        shape = CardDefaults.shape(RoundedCornerShape(20.dp)),
        modifier = modifier
            .border(width = 1.5.dp, color = AccentEmerald.copy(alpha = 0.35f), shape = RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(AccentEmerald.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.mosque_next_prayer_badge),
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        )
                    }

                    Text(
                        text = nextPrayer.prayer.arabicName,
                        style = TvTypography.ArabicTitle.copy(fontSize = 26.sp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = prayerLabel,
                    style = TvTypography.HeaderTitle.copy(
                        fontSize = 50.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.onSurface
                    )
                )

                if (nextPrayer.secondaryTimeNote != null) {
                    Text(
                        text = nextPrayer.secondaryTimeNote,
                        style = TvTypography.Subtitle.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentGold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = stringResource(R.string.mosque_countdown_to_prayer, countdown),
                            style = TvTypography.CountdownText.copy(fontSize = 46.sp)
                        )
                    }

                    val formattedIqamah = nextPrayer.formattedIqamahTime(is24HourFormat)
                    if (formattedIqamah != null) {
                        Box(
                            modifier = Modifier
                                .background(AccentEmerald.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.mosque_iqamah_prefix, formattedIqamah),
                                style = TvTypography.BodyLabel.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentEmerald
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
                    text = nextPrayer.formattedTime(is24HourFormat),
                    style = TvTypography.ActivePrayerTime.copy(fontSize = 80.sp, color = colorScheme.onSurface)
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
