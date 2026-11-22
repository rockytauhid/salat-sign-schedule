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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
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
    sunriseTime: String? = null,
    is24HourFormat: Boolean = false,
    showArabicPrayerNames: Boolean = true,
    isMosqueMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hasSunriseCard = !sunriseTime.isNullOrBlank()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (hasSunriseCard) 10.dp else 14.dp)
    ) {
        for (item in prayers) {
            PrayerCard(
                item = item,
                is24HourFormat = is24HourFormat,
                showArabicPrayerNames = showArabicPrayerNames,
                isMosqueMode = isMosqueMode,
                isCompactSixColumn = hasSunriseCard,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            )
            if (hasSunriseCard && item.prayer == Prayer.SUBUH) {
                SunriseCard(
                    sunriseTime = sunriseTime!!,
                    is24HourFormat = is24HourFormat,
                    showArabicPrayerNames = showArabicPrayerNames,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SunriseCard(
    sunriseTime: String,
    is24HourFormat: Boolean = false,
    showArabicPrayerNames: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentPrimary = colorScheme.primary
    val accentSecondary = colorScheme.secondary
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(colorScheme.surface.copy(alpha = 0.82f), shape)
            .border(width = 1.dp, color = accentSecondary.copy(alpha = 0.40f), shape = shape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showArabicPrayerNames) {
                Text(
                    text = "الشروق",
                    maxLines = 1,
                    style = TvTypography.ArabicTitle.copy(
                        fontSize = 17.sp,
                        color = accentSecondary
                    )
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = stringResource(R.string.prayer_sunrise),
                maxLines = 1,
                style = TvTypography.CardTitle.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface.copy(alpha = 0.85f)
                )
            )

            Text(
                text = sunriseTime,
                maxLines = 1,
                softWrap = false,
                style = TvTypography.InactivePrayerTime.copy(
                    fontSize = if (is24HourFormat) 26.sp else 21.sp,
                    color = accentSecondary
                )
            )

            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = stringResource(R.string.prayer_sunrise),
                tint = accentSecondary,
                modifier = Modifier.size(18.dp)
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
    isCompactSixColumn: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentPrimary = colorScheme.primary
    val accentSecondary = colorScheme.secondary
    val prayerLabel = if (isMosqueMode && item.prayer == Prayer.ZUHUR && item.isFriday) {
        stringResource(R.string.prayer_jumuah)
    } else {
        stringResource(item.prayer.labelRes())
    }

    val shape = RoundedCornerShape(16.dp)
    val containerColor = if (item.isNext) colorScheme.surfaceVariant else colorScheme.surface
    val borderModifier = when {
        item.isActive -> Modifier.pulsingGlow(shape = shape, glowColor = accentPrimary)
        item.isNext -> Modifier.border(width = 2.dp, color = accentSecondary.copy(alpha = 0.9f), shape = shape)
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
                .padding(horizontal = if (isCompactSixColumn) 8.dp else 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Arabic Name
            if (showArabicPrayerNames) {
                Text(
                    text = item.prayer.arabicName,
                    maxLines = 1,
                    style = TvTypography.ArabicTitle.copy(
                        fontSize = if (isCompactSixColumn) 17.sp else 18.sp,
                        color = if (item.isNext) accentSecondary else colorScheme.onSurface.copy(alpha = 0.65f)
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
                    fontSize = if (isCompactSixColumn) 18.sp else 20.sp,
                    fontWeight = if (item.isNext) FontWeight.Bold else FontWeight.Medium,
                    color = colorScheme.onSurface
                )
            )

            // Prayer Time (Sized cleanly so 12h "12:30 pm" never wraps or clips in 5- or 6-column TV grid)
            Text(
                text = item.formattedTime(is24HourFormat),
                maxLines = 1,
                softWrap = false,
                style = if (item.isNext) {
                    TvTypography.InactivePrayerTime.copy(
                        fontSize = when {
                            is24HourFormat && isCompactSixColumn -> 28.sp
                            is24HourFormat -> 32.sp
                            isCompactSixColumn -> 23.sp
                            else -> 26.sp
                        },
                        fontWeight = FontWeight.Bold,
                        color = accentPrimary
                    )
                } else {
                    TvTypography.InactivePrayerTime.copy(
                        fontSize = when {
                            is24HourFormat && isCompactSixColumn -> 26.sp
                            is24HourFormat -> 28.sp
                            isCompactSixColumn -> 21.sp
                            else -> 24.sp
                        },
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
                            color = accentSecondary
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
                        color = accentSecondary
                    )
                )
            }

            // Status label
            if (item.isActive) {
                Box(
                    modifier = Modifier
                        .background(accentPrimary, RoundedCornerShape(6.dp))
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
                        .background(accentSecondary.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_badge_next),
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentSecondary
                        )
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}
