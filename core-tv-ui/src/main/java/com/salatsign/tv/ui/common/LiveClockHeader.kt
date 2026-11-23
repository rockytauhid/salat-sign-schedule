// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.salatsign.core.tv.R
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.TvTypography
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LiveClockHeader(
    isMosqueMode: Boolean,
    gregorianDate: String,
    hijriDate: String,
    cityName: String,
    zoneId: String = ZoneId.systemDefault().id,
    mosqueName: String = "",
    mosqueSubtitle: String = "",
    mosqueLogoUri: String? = null,
    mosqueIconPreset: MosqueIconPreset = MosqueIconPreset.DOME_CRESCENT,
    mosqueFontPreset: MosqueFontPreset = MosqueFontPreset.MODERN_SANS,
    isViewingToday: Boolean = true,
    is24HourFormat: Boolean = false,
    onPreviousDay: () -> Unit = {},
    onNextDay: () -> Unit = {},
    onTodayClick: () -> Unit = {},
    onCalendarClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    initialFocusRequester: FocusRequester? = null
) {
    val zone = remember(zoneId) {
        runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault())
    }
    var currentTime by remember(zone) { mutableStateOf(ZonedDateTime.now(zone).toLocalTime()) }

    LaunchedEffect(zone) {
        while (true) {
            currentTime = ZonedDateTime.now(zone).toLocalTime()
            delay(1000)
        }
    }

    val timeFormatted = remember(currentTime, is24HourFormat) {
        if (is24HourFormat) {
            currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US))
        } else {
            currentTime.format(DateTimeFormatter.ofPattern("h:mm:ss a", Locale.forLanguageTag("en-AU")))
                .lowercase(Locale.ENGLISH)
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val shortCityName = remember(cityName) { formatHeaderCity(cityName) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Branding / Identity
        if (isMosqueMode) {
            // White-Label Mosque Branding (Zero App Logos or App Titles)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MosqueEmblemIcon(
                    preset = mosqueIconPreset,
                    customLogoUri = mosqueLogoUri,
                    size = 68.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val displayName = mosqueName.ifBlank { stringResource(R.string.mosque_default_name) }
                    val baseTitleStyle = getMosqueTitleStyle(mosqueFontPreset, colorScheme.onBackground)
                    val adaptiveNameSize = when {
                        displayName.length <= 16 -> baseTitleStyle.fontSize
                        displayName.length <= 24 -> 27.sp
                        displayName.length <= 31 -> 23.sp
                        else -> 20.sp
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = baseTitleStyle.copy(fontSize = adaptiveNameSize),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (shortCityName.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = shortCityName,
                                    maxLines = 1,
                                    softWrap = false,
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 13.sp,
                                        color = colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    if (mosqueSubtitle.isNotBlank()) {
                        Text(
                            text = mosqueSubtitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TvTypography.Subtitle.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.secondary
                            )
                        )
                    }
                }
            }
        } else {
            // Home / Personal Mode: App Name & City + D-pad Date Traversal Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val context = LocalContext.current
                val appIconBitmap = remember(context) {
                    runCatching {
                        context.packageManager
                            .getApplicationIcon(context.applicationInfo)
                            .toBitmap(width = 128, height = 128)
                            .asImageBitmap()
                    }.getOrNull()
                }
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = stringResource(R.string.desc_app_logo),
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = TvTypography.HeaderTitle.copy(fontSize = 32.sp, color = colorScheme.onBackground)
                        )
                        Box(
                            modifier = Modifier
                                .background(colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = shortCityName,
                                maxLines = 1,
                                softWrap = false,
                                style = TvTypography.BodyLabel.copy(
                                    fontSize = 14.sp,
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Date navigation shortcuts
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            onClick = onPreviousDay,
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = colorScheme.surface,
                                focusedContainerColor = AccentEmerald
                            ),
                            modifier = Modifier
                                .then(if (initialFocusRequester != null) Modifier.focusRequester(initialFocusRequester) else Modifier)
                                .tvFocusable()
                                .pointerInput(onPreviousDay) { detectTapGestures { onPreviousDay() } }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.header_nav_prev_day),
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.header_nav_prev_day_short),
                                    style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                                )
                            }
                        }

                        if (!isViewingToday) {
                            Surface(
                                onClick = onTodayClick,
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = AccentGold,
                                    focusedContainerColor = AccentEmerald
                                ),
                                modifier = Modifier
                                    .tvFocusable()
                                    .pointerInput(onTodayClick) { detectTapGestures { onTodayClick() } }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Today,
                                        contentDescription = stringResource(R.string.header_nav_today),
                                        tint = colorScheme.onSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        stringResource(R.string.header_nav_today),
                                        style = TvTypography.BodyLabel.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSecondary
                                        )
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = onNextDay,
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = colorScheme.surface,
                                focusedContainerColor = AccentEmerald
                            ),
                            modifier = Modifier
                                .tvFocusable()
                                .pointerInput(onNextDay) { detectTapGestures { onNextDay() } }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.header_nav_next_day_short),
                                    style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = stringResource(R.string.header_nav_next_day),
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Islamic Calendar Button in Home Mode
                        if (onCalendarClick != null) {
                            val calShape = RoundedCornerShape(8.dp)
                            Surface(
                                onClick = onCalendarClick,
                                shape = ClickableSurfaceDefaults.shape(calShape),
                                scale = tvSurfaceScale(1.03f),
                                colors = tvSelectableSurfaceColors(selected = false),
                                border = tvSelectableSurfaceBorder(selected = false, shape = calShape),
                                modifier = Modifier.pointerInput(onCalendarClick) { detectTapGestures { onCalendarClick() } }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = stringResource(R.string.islamic_cal_button),
                                        tint = colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = stringResource(R.string.islamic_cal_button),
                                        style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                                    )
                                }
                            }
                        }

                        // Settings Icon Button in Home Mode
                        Surface(
                            onClick = onSettingsClick,
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = colorScheme.surface,
                                focusedContainerColor = AccentEmerald
                            ),
                            modifier = Modifier
                                .tvFocusable()
                                .pointerInput(onSettingsClick) { detectTapGestures { onSettingsClick() } }
                        ) {
                            Box(modifier = Modifier.padding(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.header_settings),
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Column: Dual Date & Live Digital Clock
        Column(
            horizontalAlignment = Alignment.End
        ) {
            // Live Clock (sized proportionally to match Gregorian/Hijri date width below)
            Text(
                text = timeFormatted,
                style = TvTypography.HeaderTitle.copy(
                    fontSize = 36.sp,
                    letterSpacing = 0.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground
                )
            )

            // Gregorian Date
            Text(
                text = gregorianDate,
                style = TvTypography.Subtitle.copy(fontSize = 20.sp, color = colorScheme.onBackground)
            )

            // Hijri Date with Theme-Aware Gold/Bronze Accent
            Text(
                text = hijriDate,
                style = TvTypography.Subtitle.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.secondary
                )
            )
        }
    }
}

/**
 * Strips the trailing country segment for concise header pills:
 * - "Brisbane, QLD, Australia" -> "Brisbane, QLD"
 * - "Jakarta, Indonesia" -> "Jakarta"
 * - "Singapore" -> "Singapore"
 */
private fun formatHeaderCity(fullCityName: String): String {
    val trimmed = fullCityName.trim()
    val lastComma = trimmed.lastIndexOf(',')
    return if (lastComma > 0) {
        trimmed.substring(0, lastComma).trim().ifBlank { trimmed }
    } else {
        trimmed
    }
}
