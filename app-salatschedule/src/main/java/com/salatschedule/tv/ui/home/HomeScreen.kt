// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.ui.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.salatsign.core.tv.R
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerTimeItem
import com.salatsign.tv.ui.common.LiveClockHeader
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.pulsingGlow
import com.salatsign.tv.ui.common.tvFocusable
import com.salatsign.tv.ui.common.tvPrayerCardBorder
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.EmeraldLight
import com.salatsign.tv.ui.theme.TvTypography

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissAdhanHint: () -> Unit = {}
) {
    val initialFocusRequester = remember { FocusRequester() }
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        initialFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .overscanPadding()
    ) {
        val prayerTimes = uiState.prayerTimesDay

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Dual Header (Branding + Gregorian/Hijri date + Live clock)
            LiveClockHeader(
                isMosqueMode = false,
                gregorianDate = prayerTimes?.gregorianDate ?: "",
                hijriDate = prayerTimes?.hijriDate ?: "",
                cityName = uiState.settings.cityName,
                zoneId = uiState.settings.zoneId,
                isViewingToday = uiState.isViewingToday,
                is24HourFormat = uiState.settings.is24HourFormat,
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
                onTodayClick = onTodayClick,
                onSettingsClick = onOpenSettings,
                initialFocusRequester = initialFocusRequester
            )

            // One-time Adhan Silent hint (F1)
            if (!uiState.settings.hasConfiguredAudibleAdhan() && !uiState.settings.adhanHintDismissed) {
                AdhanSilentHintBanner(
                    onOpenSettings = onOpenSettings,
                    onDismiss = onDismissAdhanHint,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (prayerTimes != null) {
                val activePrayerItem = prayerTimes.prayers.firstOrNull { it.isActive }
                val nextPrayerItem = prayerTimes.nextPrayer
                // Center Spotlight Card: Current Active Prayer hero + Next Prayer live countdown
                if (uiState.isViewingToday && nextPrayerItem != null) {
                    NextPrayerSpotlightCard(
                        activePrayer = activePrayerItem,
                        nextPrayer = nextPrayerItem,
                        countdown = prayerTimes.countdownFormatted,
                        sunrise = prayerTimes.formattedSunrise(uiState.settings.is24HourFormat),
                        isReminderEnabled = uiState.settings.prePrayerReminderEnabled,
                        adhanType = uiState.settings.adhanFor(nextPrayerItem.prayer).type,
                        is24HourFormat = uiState.settings.is24HourFormat,
                        showArabicPrayerNames = uiState.settings.showArabicPrayerNames,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                    )
                } else {
                    // Non-today summary header
                    NonTodaySummaryCard(
                        dateFormatted = prayerTimes.gregorianDate,
                        sunrise = prayerTimes.formattedSunrise(uiState.settings.is24HourFormat),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5 Daily Prayers Row
                PrayerCardsRow(
                    prayers = prayerTimes.prayers,
                    is24HourFormat = uiState.settings.is24HourFormat,
                    showArabicPrayerNames = uiState.settings.showArabicPrayerNames,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f)
                )
            }
        }
    }
}

@Composable
fun NextPrayerSpotlightCard(
    activePrayer: PrayerTimeItem? = null,
    nextPrayer: PrayerTimeItem,
    countdown: String,
    sunrise: String,
    isReminderEnabled: Boolean,
    adhanType: AdhanAudioType,
    is24HourFormat: Boolean = false,
    showArabicPrayerNames: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val heroPrayer = activePrayer ?: nextPrayer
    val isShowingCurrent = activePrayer != null
    val heroLabel = stringResource(heroPrayer.prayer.labelRes())
    val nextLabel = stringResource(nextPrayer.prayer.labelRes())

    // Non-focusable display panel: spotlight is informational only
    val spotlightShape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .clip(spotlightShape)
            .background(colorScheme.surfaceVariant, spotlightShape)
            .border(1.5.dp, AccentEmerald.copy(alpha = 0.45f), spotlightShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Current Active Prayer (or Next Prayer between Sunrise & Dhuhr) + Ticking Countdown
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isShowingCurrent) AccentEmerald else AccentEmerald.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isShowingCurrent) {
                                stringResource(R.string.home_badge_active_prayer)
                            } else {
                                stringResource(R.string.home_badge_next_prayer)
                            },
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isShowingCurrent) Color.White else EmeraldLight
                            )
                        )
                    }

                    if (showArabicPrayerNames) {
                        Text(
                            text = heroPrayer.prayer.arabicName,
                            maxLines = 1,
                            style = TvTypography.ArabicTitle.copy(fontSize = 22.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = heroLabel,
                    maxLines = 1,
                    style = TvTypography.HeaderTitle.copy(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isShowingCurrent) {
                            "$nextLabel ${nextPrayer.formattedTime(is24HourFormat)} • $countdown"
                        } else {
                            stringResource(R.string.home_countdown_to_adhan, countdown)
                        },
                        maxLines = 1,
                        softWrap = false,
                        style = TvTypography.CountdownText.copy(fontSize = 24.sp)
                    )
                }
            }

            // Right: Hero Prayer Time & Status Badges
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = heroPrayer.formattedTime(is24HourFormat),
                    maxLines = 1,
                    softWrap = false,
                    style = TvTypography.ActivePrayerTime.copy(
                        fontSize = if (is24HourFormat) 60.sp else 52.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sunrise info badge
                    Row(
                        modifier = Modifier
                            .background(colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.home_sunrise, sunrise),
                            style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                        )
                    }

                    // Reminder Badge
                    Row(
                        modifier = Modifier
                            .background(colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isReminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = if (isReminderEnabled) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isReminderEnabled) stringResource(R.string.home_reminder_active) else stringResource(R.string.home_reminder_inactive),
                            style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NonTodaySummaryCard(
    dateFormatted: String,
    sunrise: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    val summaryShape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .clip(summaryShape)
            .background(colorScheme.surface, summaryShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_archive_future_schedule),
                    style = TvTypography.Subtitle.copy(color = AccentGold)
                )
                Text(
                    text = dateFormatted,
                    style = TvTypography.HeaderTitle.copy(fontSize = 32.sp, color = colorScheme.onSurface)
                )
            }
            Row(
                modifier = Modifier
                    .background(colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.home_sunrise, sunrise),
                    style = TvTypography.BodyLabel.copy(fontSize = 16.sp, color = colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}

@Composable
fun PrayerCardsRow(
    prayers: List<PrayerTimeItem>,
    is24HourFormat: Boolean = false,
    showArabicPrayerNames: Boolean = true,
    isMosqueMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        for (item in prayers) {
            PrayerCard(
                item = item,
                is24HourFormat = is24HourFormat,
                showArabicPrayerNames = showArabicPrayerNames,
                isMosqueMode = isMosqueMode,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            )
        }
    }
}

@Composable
fun PrayerCard(
    item: PrayerTimeItem,
    is24HourFormat: Boolean = false,
    showArabicPrayerNames: Boolean = true,
    isMosqueMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val prayerLabel = if (isMosqueMode && item.prayer == Prayer.ZUHUR && item.isFriday) {
        stringResource(R.string.prayer_jumuah)
    } else {
        stringResource(item.prayer.labelRes())
    }

    val shape = RoundedCornerShape(16.dp)
    val containerColor = if (item.isNext) colorScheme.surfaceVariant else colorScheme.surface
    val borderModifier = when {
        item.isActive -> Modifier.pulsingGlow(shape = shape, glowColor = AccentEmerald)
        item.isNext -> Modifier.border(width = 2.dp, color = AccentGold.copy(alpha = 0.85f), shape = shape)
        else -> Modifier
    }

    // Non-focusable display panel: prayer cards are informational only
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor, shape)
            .then(borderModifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Arabic Name
            if (showArabicPrayerNames) {
                Text(
                    text = item.prayer.arabicName,
                    maxLines = 1,
                    style = TvTypography.ArabicTitle.copy(
                        fontSize = 18.sp,
                        color = if (item.isNext) AccentGold else colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Localized Name
            Text(
                text = prayerLabel,
                maxLines = 1,
                style = TvTypography.CardTitle.copy(
                    fontSize = 20.sp,
                    fontWeight = if (item.isNext) FontWeight.Bold else FontWeight.Medium,
                    color = colorScheme.onSurface
                )
            )

            // Prayer Time (Sized cleanly so 12h "12:30 pm" never wraps or clips in 5-column TV grid)
            Text(
                text = item.formattedTime(is24HourFormat),
                maxLines = 1,
                softWrap = false,
                style = if (item.isNext) {
                    TvTypography.InactivePrayerTime.copy(
                        fontSize = if (is24HourFormat) 32.sp else 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentEmerald
                    )
                } else {
                    TvTypography.InactivePrayerTime.copy(
                        fontSize = if (is24HourFormat) 28.sp else 24.sp,
                        color = colorScheme.onSurface
                    )
                }
            )

            // Jumu'ah secondary note (e.g. "Zuhr begins 12:00 pm") and extra sittings for Mosque Mode
            if (isMosqueMode && item.prayer == Prayer.ZUHUR && item.isFriday) {
                val note = item.secondaryTimeNote
                if (note != null) {
                    Text(
                        text = note,
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 11.sp,
                            color = AccentGold
                        )
                    )
                }
                if (item.jumuahSittings.size > 1) {
                    val s2 = PrayerTimeItem.formatLocalTime(item.jumuahSittings[1], is24HourFormat)
                    Text(
                        text = stringResource(R.string.settings_jumuah_sitting_n, 2) + ": " + s2,
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 10.sp,
                            color = colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            // Iqamah indicator if configured
            val formattedIqamah = item.formattedIqamahTime(is24HourFormat)
            if (formattedIqamah != null) {
                Text(
                    text = stringResource(R.string.home_iqamah_time, formattedIqamah),
                    style = TvTypography.BodyLabel.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentGold
                    )
                )
            }

            // Status label
            if (item.isActive) {
                Box(
                    modifier = Modifier
                        .background(AccentEmerald, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_badge_active_prayer),
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            } else if (item.isNext) {
                Box(
                    modifier = Modifier
                        .background(AccentGold.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_badge_next),
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun AdhanSilentHintBanner(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        onClick = onOpenSettings,
        colors = CardDefaults.colors(
            containerColor = colorScheme.surfaceVariant,
            focusedContainerColor = colorScheme.surface
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp)),
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable(shape = RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.home_adhan_silent_hint),
                    style = TvTypography.BodyLabel.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = AccentGold,
                    contentColor = Color.White,
                    focusedContentColor = Color.Black
                ),
                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                modifier = Modifier.tvFocusable()
            ) {
                Text(
                    text = stringResource(R.string.home_adhan_silent_hint_dismiss),
                    style = TvTypography.ButtonText.copy(fontSize = 12.sp)
                )
            }
        }
    }
}
