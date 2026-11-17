package com.salatsign.tv.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.salatsign.tv.R
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
    is24HourFormat: Boolean = true,
    onPreviousDay: () -> Unit = {},
    onNextDay: () -> Unit = {},
    onTodayClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
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
        val pattern = if (is24HourFormat) "HH:mm:ss" else "hh:mm:ss a"
        currentTime.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
    }

    val colorScheme = MaterialTheme.colorScheme

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MosqueEmblemIcon(
                    preset = mosqueIconPreset,
                    customLogoUri = mosqueLogoUri,
                    size = 64.dp
                )

                Column {
                    val displayName = mosqueName.ifBlank { stringResource(R.string.mosque_default_name) }
                    Text(
                        text = displayName,
                        style = getMosqueTitleStyle(mosqueFontPreset, colorScheme.onBackground)
                    )
                    if (mosqueSubtitle.isNotBlank()) {
                        Text(
                            text = mosqueSubtitle,
                            style = TvTypography.Subtitle.copy(fontSize = 20.sp, color = AccentGold)
                        )
                    } else {
                        Text(
                            text = cityName,
                            style = TvTypography.HeaderTitle.copy(fontSize = 20.sp, color = colorScheme.onBackground.copy(alpha = 0.7f))
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
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = stringResource(R.string.desc_app_logo),
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                )

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
                                text = cityName,
                                style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = AccentEmerald, fontWeight = FontWeight.Bold)
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
                            modifier = Modifier.tvFocusable()
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
                                modifier = Modifier.tvFocusable()
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
                            modifier = Modifier.tvFocusable()
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

                        // Settings Icon Button in Home Mode
                        Surface(
                            onClick = onSettingsClick,
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = colorScheme.surface,
                                focusedContainerColor = AccentEmerald
                            ),
                            modifier = Modifier.tvFocusable()
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
            // Live Clock
            Text(
                text = timeFormatted,
                style = TvTypography.HeaderTitle.copy(
                    fontSize = 46.sp,
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

            // Hijri Date with Gold Accent
            Text(
                text = hijriDate,
                style = TvTypography.Subtitle.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentGold
                )
            )
        }
    }
}
