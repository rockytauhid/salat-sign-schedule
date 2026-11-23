// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.salatsign.core.tv.R
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.ClassifiedIslamicEvent
import com.salatsign.tv.data.model.IslamicEventRowState
import com.salatsign.tv.engine.IslamicCalendarEvents
import com.salatsign.tv.engine.IslamicCalendarState
import com.salatsign.tv.ui.common.formatGregorianEventSpan
import com.salatsign.tv.ui.common.formatGregorianYearSpan
import com.salatsign.tv.ui.common.formatHijriEventSpan
import com.salatsign.tv.ui.common.formatRelativeDaysLabel
import com.salatsign.tv.ui.common.islamicEventNameRes
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.tvCardScale
import com.salatsign.tv.ui.common.tvSelectableSurfaceBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceColors
import com.salatsign.tv.ui.common.tvSurfaceScale
import com.salatsign.tv.ui.theme.TvTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Navigable Islamic Calendar screen for SalatSchedule (Home app).
 * Browses +/- 3 Hijri years on demand with TODAY / NEXT / PAST / UPCOMING state marks.
 */
@Composable
fun IslamicCalendarScreen(
    settings: AppSettings,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val monthNames = stringArrayResource(R.array.hijri_months).toList()
    val zone = remember(settings.zoneId) {
        runCatching { ZoneId.of(settings.zoneId) }.getOrDefault(ZoneId.systemDefault())
    }

    var today by remember(zone) { mutableStateOf(ZonedDateTime.now(zone).toLocalDate()) }
    // Re-evaluate at configured-zone midnight (checks every minute)
    LaunchedEffect(zone) {
        while (true) {
            delay(60_000L)
            val current = ZonedDateTime.now(zone).toLocalDate()
            if (current != today) {
                today = current
            }
        }
    }

    val currentHijriYear = remember(today, settings.hijriOffsetDays) {
        IslamicCalendarEvents.currentHijriYear(today, settings.hijriOffsetDays)
    }
    var displayedYear by remember(currentHijriYear) { mutableIntStateOf(currentHijriYear) }
    var limitNotice by remember { mutableStateOf<String?>(null) }

    val canGoPrev = IslamicCalendarEvents.canGoPreviousYear(displayedYear, currentHijriYear)
    val canGoNext = IslamicCalendarEvents.canGoNextYear(displayedYear, currentHijriYear)

    // Compute and cache ONLY the displayed Hijri year + global next event
    val globalNext = remember(today, settings.hijriOffsetDays, settings.enabledIslamicEventIds) {
        IslamicCalendarState.nextEvent(today, settings.hijriOffsetDays, settings.enabledIslamicEventIds)
    }
    val yearSpan = remember(displayedYear, settings.hijriOffsetDays) {
        IslamicCalendarEvents.gregorianSpanForHijriYear(displayedYear, settings.hijriOffsetDays)
    }
    val classifiedRows = remember(displayedYear, today, settings.hijriOffsetDays, settings.enabledIslamicEventIds, globalNext) {
        val occs = IslamicCalendarEvents.occurrencesForYear(
            hijriYear = displayedYear,
            adjustmentDays = settings.hijriOffsetDays,
            enabledIds = settings.enabledIslamicEventIds
        )
        IslamicCalendarState.classify(
            occurrences = occs,
            today = today,
            globalNextEvent = globalNext?.occurrence
        )
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val todayBtnFocus = remember { FocusRequester() }
    val limitPastText = stringResource(R.string.islamic_cal_limit_past)
    val limitFutureText = stringResource(R.string.islamic_cal_limit_future)

    fun stepYear(delta: Int) {
        val candidate = displayedYear + delta
        val clamped = IslamicCalendarEvents.clampHijriYear(candidate, currentHijriYear)
        if (clamped == displayedYear) {
            limitNotice = if (delta < 0) limitPastText else limitFutureText
        } else {
            limitNotice = null
            displayedYear = clamped
            scope.launch { listState.scrollToItem(0) }
        }
    }

    fun jumpToToday() {
        limitNotice = null
        displayedYear = currentHijriYear
        val targetIndex = IslamicCalendarState.initialScrollIndex(classifiedRows)
        scope.launch { listState.scrollToItem(targetIndex) }
    }

    // On initial open, scroll so TODAY or NEXT row is visible
    LaunchedEffect(currentHijriYear) {
        val targetIndex = IslamicCalendarState.initialScrollIndex(classifiedRows)
        if (targetIndex > 0) {
            listState.scrollToItem(targetIndex)
        }
        runCatching { todayBtnFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .overscanPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header bar: Title + Year Controls + Today + Back
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.islamic_cal_year_ah, displayedYear),
                            style = TvTypography.HeaderTitle.copy(fontSize = 34.sp, color = colorScheme.onBackground)
                        )
                        Box(
                            modifier = Modifier
                                .background(colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = formatGregorianYearSpan(yearSpan.first, yearSpan.second),
                                style = TvTypography.BodyLabel.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.primary
                                )
                            )
                        }
                    }

                    if (globalNext != null) {
                        val nextName = stringResource(islamicEventNameRes(globalNext.occurrence.eventId))
                        val rel = formatRelativeDaysLabel(globalNext.daysRelativeToToday, context)
                        Text(
                            text = stringResource(R.string.islamic_cal_next_summary, nextName, rel),
                            style = TvTypography.Subtitle.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.secondary
                            )
                        )
                    }
                }

                // Year navigation & Today / Back actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val btnShape = RoundedCornerShape(10.dp)

                    Surface(
                        onClick = { stepYear(-1) },
                        shape = ClickableSurfaceDefaults.shape(btnShape),
                        scale = tvSurfaceScale(1.02f),
                        colors = tvSelectableSurfaceColors(selected = false),
                        border = tvSelectableSurfaceBorder(selected = false, shape = btnShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.islamic_cal_prev_year),
                                tint = if (canGoPrev) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.35f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${displayedYear - 1}",
                                style = TvTypography.ButtonText.copy(
                                    fontSize = 14.sp,
                                    color = if (canGoPrev) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.35f)
                                )
                            )
                        }
                    }

                    Surface(
                        onClick = { jumpToToday() },
                        shape = ClickableSurfaceDefaults.shape(btnShape),
                        scale = tvSurfaceScale(1.02f),
                        colors = tvSelectableSurfaceColors(selected = displayedYear == currentHijriYear),
                        border = tvSelectableSurfaceBorder(selected = displayedYear == currentHijriYear, shape = btnShape),
                        modifier = Modifier.focusRequester(todayBtnFocus)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = stringResource(R.string.islamic_cal_today_btn),
                                tint = colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.islamic_cal_today_btn),
                                style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                            )
                        }
                    }

                    Surface(
                        onClick = { stepYear(1) },
                        shape = ClickableSurfaceDefaults.shape(btnShape),
                        scale = tvSurfaceScale(1.02f),
                        colors = tvSelectableSurfaceColors(selected = false),
                        border = tvSelectableSurfaceBorder(selected = false, shape = btnShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${displayedYear + 1}",
                                style = TvTypography.ButtonText.copy(
                                    fontSize = 14.sp,
                                    color = if (canGoNext) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.35f)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.islamic_cal_next_year),
                                tint = if (canGoNext) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.35f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onBack,
                        shape = ClickableSurfaceDefaults.shape(btnShape),
                        scale = tvSurfaceScale(1.02f),
                        colors = tvSelectableSurfaceColors(selected = false),
                        border = tvSelectableSurfaceBorder(selected = false, shape = btnShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.btn_close),
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.btn_close),
                                style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                            )
                        }
                    }
                }
            }

            // Moon-sighting note + optional clamping notice
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.islamic_cal_moon_note),
                    style = TvTypography.BodyLabel.copy(
                        fontSize = 13.sp,
                        color = colorScheme.onBackground.copy(alpha = 0.72f)
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (limitNotice != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = limitNotice!!,
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.secondary
                        )
                    )
                }
            }

            // Event list (Left/Right D-pad switches Hijri years cleanly from any row)
            if (classifiedRows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.islamic_cal_empty),
                        style = TvTypography.Subtitle.copy(fontSize = 18.sp, color = colorScheme.onBackground.copy(alpha = 0.75f))
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    Key.DirectionLeft -> {
                                        stepYear(-1)
                                        true
                                    }
                                    Key.DirectionRight -> {
                                        stepYear(1)
                                        true
                                    }
                                    else -> false
                                }
                            } else {
                                false
                            }
                        },
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = classifiedRows,
                        key = { "${it.occurrence.eventId}_${it.occurrence.startDate}" }
                    ) { row ->
                        IslamicCalendarEventRow(
                            row = row,
                            monthNames = monthNames
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IslamicCalendarEventRow(
    row: ClassifiedIslamicEvent,
    monthNames: List<String>
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    val isPast = row.state == IslamicEventRowState.PAST
    val isToday = row.state == IslamicEventRowState.TODAY
    val isNext = row.state == IslamicEventRowState.NEXT

    val containerColor = when {
        isToday -> colorScheme.surfaceVariant
        isPast -> colorScheme.surface.copy(alpha = 0.72f)
        else -> colorScheme.surface
    }

    val unfocusedBorder = when {
        isToday -> Border(border = BorderStroke(2.dp, colorScheme.primary), shape = shape)
        isNext -> Border(border = BorderStroke(2.dp, colorScheme.secondary), shape = shape)
        else -> Border.None
    }

    val textAlpha = if (isPast) 0.62f else 1.0f
    val secondaryAlpha = if (isPast) 0.58f else 0.80f

    Card(
        onClick = {},
        colors = CardDefaults.colors(
            containerColor = containerColor,
            focusedContainerColor = colorScheme.surfaceVariant
        ),
        shape = CardDefaults.shape(shape),
        scale = tvCardScale(1.0f),
        border = CardDefaults.border(
            border = unfocusedBorder,
            focusedBorder = Border(border = BorderStroke(2.5.dp, colorScheme.secondary), shape = shape)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: State icon + Badge + Event Name & Hijri/Gregorian Dates
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
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
                        IslamicEventRowState.PAST -> colorScheme.onSurface.copy(alpha = 0.55f)
                        IslamicEventRowState.UPCOMING -> colorScheme.onSurface.copy(alpha = 0.75f)
                    },
                    modifier = Modifier.size(26.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(islamicEventNameRes(row.occurrence.eventId)),
                            style = TvTypography.CardTitle.copy(
                                fontSize = 20.sp,
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
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = formatHijriEventSpan(row.occurrence, monthNames),
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.secondary.copy(alpha = if (isPast) 0.65f else 1.0f)
                            )
                        )
                        Text(
                            text = "•",
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 15.sp,
                                color = colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        )
                        Text(
                            text = formatGregorianEventSpan(row.occurrence),
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 15.sp,
                                color = colorScheme.onSurface.copy(alpha = secondaryAlpha)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right: Relative label ("Today", "in 12 days", "3 days ago")
            Text(
                text = formatRelativeDaysLabel(row.daysRelativeToToday, context),
                style = TvTypography.BodyLabel.copy(
                    fontSize = 16.sp,
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
