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
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.ui.platform.LocalContext
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
import com.salatsign.tv.engine.IslamicCalendarState
import com.salatsign.tv.ui.common.LiveClockHeader
import com.salatsign.tv.ui.common.PrayerCardsRow
import com.salatsign.tv.ui.common.formatRelativeDaysLabel
import com.salatsign.tv.ui.common.islamicEventNameRes
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.tvFocusable
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.EmeraldLight
import com.salatsign.tv.ui.theme.TvTypography
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClick: () -> Unit,
    onOpenCalendar: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onDismissAdhanHint: () -> Unit = {}
) {
    val initialFocusRequester = remember { FocusRequester() }
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    val nextIslamicEvent = remember(
        uiState.selectedDate,
        uiState.settings.zoneId,
        uiState.settings.hijriOffsetDays,
        uiState.settings.enabledIslamicEventIds,
        uiState.settings.showNextEventOnHome
    ) {
        if (!uiState.settings.showNextEventOnHome) {
            null
        } else {
            val zone = runCatching { ZoneId.of(uiState.settings.zoneId) }.getOrDefault(ZoneId.systemDefault())
            val today = ZonedDateTime.now(zone).toLocalDate()
            IslamicCalendarState.nextEvent(
                today = today,
                adjustmentDays = uiState.settings.hijriOffsetDays,
                enabledIds = uiState.settings.enabledIslamicEventIds
            )
        }
    }

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
                onCalendarClick = onOpenCalendar,
                onSettingsClick = onOpenSettings,
                initialFocusRequester = initialFocusRequester
            )

            // Optional subtle next Islamic event line
            if (nextIslamicEvent != null) {
                val eventName = stringResource(islamicEventNameRes(nextIslamicEvent.occurrence.eventId))
                val rel = formatRelativeDaysLabel(nextIslamicEvent.daysRelativeToToday, context)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.islamic_cal_next_summary, eventName, rel),
                        maxLines = 1,
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onBackground.copy(alpha = 0.85f)
                        )
                    )
                }
            }

            // Session-scoped Adhan Silent hint (reappears on next app launch if still all Silent)
            if (!uiState.settings.hasConfiguredAudibleAdhan() && !uiState.adhanHintDismissedInSession) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5 Daily Prayers + Sunrise Row
                PrayerCardsRow(
                    prayers = prayerTimes.prayers,
                    sunriseTime = prayerTimes.formattedSunrise(uiState.settings.is24HourFormat),
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
    isReminderEnabled: Boolean,
    adhanType: AdhanAudioType,
    is24HourFormat: Boolean = false,
    showArabicPrayerNames: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentPrimary = colorScheme.primary
    val accentSecondary = colorScheme.secondary
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
            .border(1.5.dp, accentPrimary.copy(alpha = 0.45f), spotlightShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Current Active Prayer (or Next Prayer between Sunrise & Dhuhr) + Ticking Countdown
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 24.dp)
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
                                stringResource(R.string.home_badge_next_prayer)
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

                Spacer(modifier = Modifier.height(8.dp))

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
                            stringResource(R.string.home_countdown_to_adhan, countdown)
                        },
                        maxLines = 1,
                        softWrap = false,
                        style = TvTypography.CountdownText.copy(fontSize = 24.sp, color = accentSecondary)
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
                        fontSize = if (is24HourFormat) 64.sp else 54.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                        tint = if (isReminderEnabled) accentPrimary else colorScheme.onSurface.copy(alpha = 0.6f),
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

@Composable
fun NonTodaySummaryCard(
    dateFormatted: String,
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
                    style = TvTypography.Subtitle.copy(color = colorScheme.secondary)
                )
                Text(
                    text = dateFormatted,
                    style = TvTypography.HeaderTitle.copy(fontSize = 32.sp, color = colorScheme.onSurface)
                )
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
    val bannerShape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(bannerShape)
            .background(colorScheme.surfaceVariant, bannerShape)
            .border(1.dp, colorScheme.onSurface.copy(alpha = 0.14f), bannerShape)
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
                tint = colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.home_adhan_silent_hint),
                style = TvTypography.BodyLabel.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.colors(
                    containerColor = colorScheme.primary.copy(alpha = 0.22f),
                    focusedContainerColor = colorScheme.primary,
                    contentColor = colorScheme.onSurface,
                    focusedContentColor = Color.White
                ),
                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                modifier = Modifier.tvFocusable()
            ) {
                Text(
                    text = stringResource(R.string.home_adhan_silent_hint_settings),
                    style = TvTypography.ButtonText.copy(fontSize = 12.sp)
                )
            }
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.colors(
                    containerColor = colorScheme.onSurface.copy(alpha = 0.12f),
                    focusedContainerColor = colorScheme.secondary,
                    contentColor = colorScheme.onSurface,
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
