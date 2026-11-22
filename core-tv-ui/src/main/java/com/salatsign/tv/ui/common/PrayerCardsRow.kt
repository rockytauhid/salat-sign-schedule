// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.salatsign.core.tv.R
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerTimeItem
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.TvTypography

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
                        .padding(horizontal = 10.dp, vertical = 3.dp)
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
                        .padding(horizontal = 10.dp, vertical = 3.dp)
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
