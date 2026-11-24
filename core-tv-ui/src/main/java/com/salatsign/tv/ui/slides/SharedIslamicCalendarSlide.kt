// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.slides

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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.salatsign.core.tv.R
import com.salatsign.tv.data.model.ClassifiedIslamicEvent
import com.salatsign.tv.data.model.IslamicEventRowState
import com.salatsign.tv.ui.common.formatGregorianEventSpan
import com.salatsign.tv.ui.common.formatHijriEventSpan
import com.salatsign.tv.ui.common.formatRelativeDaysLabel
import com.salatsign.tv.ui.common.islamicEventNameRes
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.theme.TvTypography

/**
 * Shared read-only Islamic Calendar slide used by both SalatSign (Mosque slideshow)
 * and SalatSchedule (Android TV Screensaver). Takes plain data and active MaterialTheme.
 */
@Composable
fun SharedIslamicCalendarSlide(
    rows: List<ClassifiedIslamicEvent>,
    hijriDate: String,
    gregorianDate: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val monthNames = stringArrayResource(R.array.hijri_months).toList()

    val compact = rows.size >= 5
    val nameFontSize = if (compact) 25.sp else 32.sp
    val dateFontSize = if (compact) 18.sp else 22.sp
    val relFontSize = if (compact) 18.sp else 22.sp
    val rowVerticalPad = if (compact) 8.dp else 12.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .overscanPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = stringResource(R.string.islamic_cal_title),
                    style = TvTypography.HeaderTitle.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.onBackground
                    )
                )

                Column(horizontalAlignment = Alignment.End) {
                    if (hijriDate.isNotBlank()) {
                        Text(
                            text = hijriDate,
                            style = TvTypography.Subtitle.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.secondary
                            )
                        )
                    }
                    if (gregorianDate.isNotBlank()) {
                        Text(
                            text = gregorianDate,
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 18.sp,
                                color = colorScheme.onBackground.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp, Alignment.CenterVertically)
            ) {
                for (row in rows) {
                    val isToday = row.state == IslamicEventRowState.TODAY
                    val isNext = row.state == IslamicEventRowState.NEXT
                    val isPast = row.state == IslamicEventRowState.PAST
                    val rowShape = RoundedCornerShape(12.dp)

                    val bg = when {
                        isToday -> colorScheme.surfaceVariant
                        isPast -> colorScheme.surface.copy(alpha = 0.70f)
                        else -> colorScheme.surface
                    }
                    val borderColor = when {
                        isToday -> colorScheme.primary
                        isNext -> colorScheme.secondary
                        else -> Color.Transparent
                    }
                    val textAlpha = if (isPast) 0.62f else 1.0f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bg, rowShape)
                            .then(
                                if (borderColor != Color.Transparent) {
                                    Modifier.border(2.dp, borderColor, rowShape)
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 18.dp, vertical = rowVerticalPad),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1.15f)
                        ) {
                            Icon(
                                imageVector = when (row.state) {
                                    IslamicEventRowState.TODAY -> Icons.Default.Star
                                    IslamicEventRowState.NEXT -> Icons.Default.Event
                                    IslamicEventRowState.PAST -> Icons.Default.History
                                    IslamicEventRowState.UPCOMING -> Icons.Default.CalendarToday
                                },
                                contentDescription = null,
                                tint = when (row.state) {
                                    IslamicEventRowState.TODAY -> colorScheme.primary
                                    IslamicEventRowState.NEXT -> colorScheme.secondary
                                    IslamicEventRowState.PAST -> colorScheme.onSurface.copy(alpha = 0.58f)
                                    IslamicEventRowState.UPCOMING -> colorScheme.onSurface.copy(alpha = 0.8f)
                                },
                                modifier = Modifier.size(if (compact) 26.dp else 30.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(islamicEventNameRes(row.occurrence.eventId)),
                                        maxLines = 2,
                                        style = TvTypography.CardTitle.copy(
                                            fontSize = nameFontSize,
                                            lineHeight = (nameFontSize.value + 4f).sp,
                                            fontWeight = if (isToday || isNext) FontWeight.Bold else FontWeight.SemiBold,
                                            color = colorScheme.onSurface.copy(alpha = textAlpha)
                                        )
                                    )
                                    if (isToday || isNext) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isToday) colorScheme.primary else colorScheme.secondary,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = stringResource(
                                                    if (isToday) R.string.islamic_cal_badge_today
                                                    else R.string.islamic_cal_badge_next
                                                ),
                                                style = TvTypography.BodyLabel.copy(
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(0.85f)
                        ) {
                            Text(
                                text = formatHijriEventSpan(row.occurrence, monthNames),
                                style = TvTypography.BodyLabel.copy(
                                    fontSize = dateFontSize,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.secondary.copy(alpha = if (isPast) 0.65f else 1.0f)
                                )
                            )
                            Text(
                                text = formatGregorianEventSpan(row.occurrence),
                                style = TvTypography.BodyLabel.copy(
                                    fontSize = (dateFontSize.value - 2f).sp,
                                    color = colorScheme.onSurface.copy(alpha = if (isPast) 0.58f else 0.85f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = formatRelativeDaysLabel(row.daysRelativeToToday, context),
                            style = TvTypography.BodyLabel.copy(
                                fontSize = relFontSize,
                                fontWeight = if (isToday || isNext) FontWeight.Bold else FontWeight.Medium,
                                color = when (row.state) {
                                    IslamicEventRowState.TODAY -> colorScheme.primary
                                    IslamicEventRowState.NEXT -> colorScheme.secondary
                                    IslamicEventRowState.PAST -> colorScheme.onSurface.copy(alpha = 0.58f)
                                    IslamicEventRowState.UPCOMING -> colorScheme.onSurface.copy(alpha = 0.85f)
                                }
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.islamic_cal_moon_note_short),
                style = TvTypography.BodyLabel.copy(
                    fontSize = 14.sp,
                    color = colorScheme.onBackground.copy(alpha = 0.72f)
                )
            )
        }
    }
}
