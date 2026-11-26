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
import com.salatsign.tv.ui.common.pulsingGlow
import com.salatsign.tv.ui.common.tvFocusable
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.EmeraldLight
import com.salatsign.tv.ui.theme.LocalIsDarkTheme
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
    onOpenSettings: () -> Unit
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
                isMosqueSignage = false,
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

            if (prayerTimes != null) {
                val activePrayerItem = prayerTimes.prayers.firstOrNull { it.isActive }
                val nextPrayerItem = prayerTimes.nextPrayer
                // Center Spotlight Card: 5-minute Prayer-Time Alert Banner on exact strike, else Current/Next Prayer hero
                if (uiState.isViewingToday && uiState.activeStrikePrayer != null) {
                    val strikeItem = prayerTimes.prayers.firstOrNull { it.prayer == uiState.activeStrikePrayer }
                        ?: prayerTimes.prayers.first()
                    HomeActiveStrikeBanner(
                        prayer = strikeItem,
                        is24HourFormat = uiState.settings.is24HourFormat,
                        showArabicPrayerNames = uiState.settings.showArabicPrayerNames,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                    )
                } else if (uiState.isViewingToday && nextPrayerItem != null) {
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
fun HomeActiveStrikeBanner(
    prayer: PrayerTimeItem,
    is24HourFormat: Boolean = false,
    showArabicPrayerNames: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalIsDarkTheme.current
    val panelBg = if (isDark) colorScheme.surfaceVariant else colorScheme.surface
    val prayerLabel = stringResource(prayer.prayer.labelRes())
    val strikeShape = RoundedCornerShape(20.dp)
    val accentPrimary = colorScheme.primary
    val accentSecondary = colorScheme.secondary

    Box(
        modifier = modifier
            .clip(strikeShape)
            .background(panelBg, strikeShape)
            .pulsingGlow(shape = strikeShape, glowColor = accentSecondary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(accentPrimary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
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

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.mosque_strike_prayer_name, prayerLabel),
                            maxLines = 1,
                            style = TvTypography.HeaderTitle.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorScheme.onSurfaceVariant
                            )
                        )
                        if (showArabicPrayerNames) {
                            Text(
                                text = prayer.prayer.arabicName,
                                maxLines = 1,
                                style = TvTypography.ArabicTitle.copy(fontSize = 30.sp, color = accentSecondary)
                            )
                        }
                    }
                }

                Text(
                    text = prayer.formattedTime(is24HourFormat),
                    maxLines = 1,
                    softWrap = false,
                    style = TvTypography.ActivePrayerTime.copy(
                        fontSize = if (is24HourFormat) 64.sp else 54.sp,
                        color = accentPrimary
                    )
                )
            }

            // Full-width right-aligned Surah 4:103 reminder with reference on second row
            Text(
                text = stringResource(R.string.mosque_strike_admonition),
                maxLines = 3,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                style = TvTypography.Subtitle.copy(
                    fontSize = 19.sp,
                    lineHeight = 25.sp,
                    color = accentSecondary
                )
            )
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
    val isDark = LocalIsDarkTheme.current
    val panelBg = if (isDark) colorScheme.surfaceVariant else colorScheme.surface
    val badgeBg = if (isDark) colorScheme.surface else colorScheme.surfaceVariant
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
            .background(panelBg, spotlightShape)
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
                Box(
                    modifier = Modifier
                        .background(
                            if (isShowingCurrent) accentSecondary.copy(alpha = 0.18f) else accentPrimary.copy(alpha = 0.16f),
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
                            color = if (isShowingCurrent) accentSecondary else accentPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = heroLabel,
                        maxLines = 1,
                        style = TvTypography.HeaderTitle.copy(
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorScheme.onSurfaceVariant
                        )
                    )
                    if (showArabicPrayerNames) {
                        Text(
                            text = heroPrayer.prayer.arabicName,
                            maxLines = 1,
                            style = TvTypography.ArabicTitle.copy(fontSize = 30.sp, color = accentSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reminder Badge
                Row(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(8.dp))
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

            // Right: Big Live Countdown & "Until <Next Prayer>"
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = countdown,
                    maxLines = 1,
                    softWrap = false,
                    style = TvTypography.ActivePrayerTime.copy(
                        fontSize = 58.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.schedule_until_prayer, nextLabel),
                    maxLines = 1,
                    softWrap = false,
                    style = TvTypography.CountdownText.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurfaceVariant
                    )
                )
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
