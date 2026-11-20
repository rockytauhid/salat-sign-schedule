// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.salatschedule.tv.BuildConfig
import com.salatsign.core.tv.R
import com.salatsign.tv.data.importing.ImportKind
import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.FridayAdhanTiming
import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.PopularCityPresets
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.SlideType
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.engine.JumuahCalculator
import com.salatsign.tv.engine.JumuahWarningReason
import com.salatsign.tv.ui.common.MosqueEmblemIcon
import com.salatsign.tv.ui.common.displayLabel
import com.salatsign.tv.ui.common.getMosqueFontFamily
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.tvFocusable
import com.salatsign.tv.ui.picker.StorageFilePickerScreen
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.TvTypography
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HomeSettingsScreen(
    settings: AppSettings,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSelectCity: (CityPreset) -> Unit,
    onSetCalculationMethod: (String) -> Unit,
    onSetMadhab: (String) -> Unit,
    onSetHighLatitudeRule: (String?) -> Unit,
    onSetPrayerOffset: (Prayer, Int) -> Unit,
    onSetSunriseOffset: (Int) -> Unit,
    onSetHijriOffset: (Int) -> Unit,
    onSetHijriChangeAtMidnight: (Boolean) -> Unit,
    onTogglePrePrayerReminder: (Boolean) -> Unit,
    onSetAdhanAudio: (Prayer, AdhanAudioOption) -> Unit,
    onSetCustomAdhan: (Prayer, String, String) -> Unit,
    onSetAppLanguage: (AppLanguage) -> Unit,
    onSet24HourFormat: (Boolean) -> Unit,
    onSetShowArabicPrayerNames: (Boolean) -> Unit,
    onRestartSetupWizard: () -> Unit,
    onBack: () -> Unit
) {
    val onClose = onBack
    val onToggleReminder = onTogglePrePrayerReminder
    val initialFocusRequester = remember { FocusRequester() }
    val verticalScrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    var activePickerKind by remember { mutableStateOf<ImportKind?>(null) }
    var pickerTargetPrayer by remember { mutableStateOf<Prayer?>(null) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Auto-close Settings after 2 minutes of inactivity and return to signage/home
    LaunchedEffect(lastInteractionTime) {
        delay(120_000L)
        onClose()
    }

    LaunchedEffect(Unit) {
        initialFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .overscanPadding()
            .onPreviewKeyEvent {
                lastInteractionTime = System.currentTimeMillis()
                false
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = TvTypography.HeaderTitle.copy(color = colorScheme.onBackground)
                    )
                    Text(
                        text = stringResource(R.string.settings_subtitle_home),
                        style = TvTypography.Subtitle.copy(fontSize = 16.sp, color = colorScheme.onBackground.copy(alpha = 0.7f))
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = onRestartSetupWizard,
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = colorScheme.surfaceVariant,
                            focusedContainerColor = AccentGold
                        ),
                        modifier = Modifier.tvFocusable()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_open_wizard), style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = colorScheme.onSurfaceVariant))
                        }
                    }

                    Surface(
                        onClick = onClose,
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = colorScheme.surface,
                            focusedContainerColor = AccentEmerald
                        ),
                        modifier = Modifier.focusRequester(initialFocusRequester).tvFocusable()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.btn_close),
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_close), style = TvTypography.ButtonText.copy(color = colorScheme.onSurface))
                        }
                    }
                }
            }

            // Section: Display Language Selector (shown only when multiple languages ship - F3)
            if (AppLanguage.isMultiLanguageShipped) {
                Text(
                    text = stringResource(R.string.settings_section_language),
                    style = TvTypography.CardTitle.copy(color = AccentGold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (lang in AppLanguage.entries) {
                        val isSelected = (settings.appLanguage == lang)
                        Card(
                            onClick = { onSetAppLanguage(lang) },
                            colors = CardDefaults.colors(
                                containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface
                            ),
                            shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                            modifier = Modifier
                                .weight(1f)
                                .tvFocusable()
                                .then(
                                    if (isSelected) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(10.dp))
                                    else Modifier
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = if (isSelected) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(lang.labelRes()),
                                    style = TvTypography.CardTitle.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                                )
                            }
                        }
                    }
                }
            }

            // Section: Time Format & Display Preferences
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 12/24 hour toggle
                Card(
                    onClick = { onSet24HourFormat(!settings.is24HourFormat) },
                    colors = CardDefaults.colors(containerColor = colorScheme.surface),
                    shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                    modifier = Modifier
                        .weight(1f)
                        .tvFocusable()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = AccentEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_time_format_toggle),
                                    style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                                )
                                Text(
                                    text = if (settings.is24HourFormat) stringResource(R.string.settings_time_format_24) else stringResource(R.string.settings_time_format_12),
                                    style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = AccentEmerald, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // Show Arabic Names Toggle
                Card(
                    onClick = { onSetShowArabicPrayerNames(!settings.showArabicPrayerNames) },
                    colors = CardDefaults.colors(containerColor = colorScheme.surface),
                    shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                    modifier = Modifier
                        .weight(1f)
                        .tvFocusable()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_arabic_names_toggle),
                                    style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                                )
                                Text(
                                    text = if (settings.showArabicPrayerNames) stringResource(R.string.settings_reminder_toggle_on) else stringResource(R.string.settings_reminder_toggle_off),
                                    style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = if (settings.showArabicPrayerNames) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.6f))
                                )
                            }
                        }
                    }
                }
            }

            // 1. Theme Selection (Shared in both modes)
            Text(
                text = stringResource(R.string.settings_section_theme),
                style = TvTypography.CardTitle.copy(color = AccentGold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                for (mode in ThemeMode.entries) {
                    val isSelected = (settings.themeMode == mode)
                    Card(
                        onClick = { onSetThemeMode(mode) },
                        colors = CardDefaults.colors(
                            containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface
                        ),
                        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                        modifier = Modifier
                            .weight(1f)
                            .tvFocusable()
                            .then(
                                if (isSelected) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    ThemeMode.ALWAYS_DARK -> Icons.Default.DarkMode
                                    ThemeMode.ALWAYS_LIGHT -> Icons.Default.LightMode
                                    ThemeMode.AUTO_DAY_NIGHT -> Icons.Default.BrightnessAuto
                                },
                                contentDescription = null,
                                tint = if (isSelected) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(mode.labelRes()),
                                    style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                                )
                            }
                        }
                    }
                }
            }

            // 2. City & Location Selection (Shared)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.settings_section_location)} (${settings.cityName})",
                    style = TvTypography.CardTitle.copy(color = AccentGold)
                )
            }

            val cityScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(cityScrollState),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (city in PopularCityPresets) {
                    val isSelected = (settings.cityName.startsWith(city.name.split(",")[0]))
                    Surface(
                        onClick = { onSelectCity(city) },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) AccentEmerald else colorScheme.surface,
                            focusedContainerColor = AccentGold
                        ),
                        modifier = Modifier.tvFocusable()
                    ) {
                        Text(
                            text = city.name,
                            style = TvTypography.ButtonText.copy(
                                fontSize = 15.sp,
                                color = if (isSelected) Color.White else colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Timezone Mismatch Warning (if TV clock zone differs from configured location zone)
            val currentZone = remember(settings.zoneId) {
                runCatching { ZoneId.of(settings.zoneId) }.getOrDefault(ZoneId.systemDefault())
            }
            val systemZone = remember { ZoneId.systemDefault() }
            val isTimezoneMismatch = remember(currentZone, systemZone) {
                val now = Instant.now()
                currentZone.rules.getOffset(now) != systemZone.rules.getOffset(now)
            }

            if (isTimezoneMismatch) {
                Card(
                    onClick = {},
                    colors = CardDefaults.colors(containerColor = Color(0xFF1E3A8A).copy(alpha = 0.4f)),
                    shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${stringResource(R.string.settings_current_zone, currentZone.id)} (System: ${systemZone.id})",
                            style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = Color.White)
                        )
                    }
                }
            }

            // 3. Calculation Method
            Text(
                text = "${stringResource(R.string.settings_section_calc_method)} (${settings.calculationMethodName})",
                style = TvTypography.CardTitle.copy(color = AccentGold)
            )

            val methodScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(methodScrollState),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val methods = listOf(
                    "SINGAPORE" to "Singapura / MABIMS",
                    "MUSLIM_WORLD_LEAGUE" to "Muslim World League (MWL)",
                    "UMM_AL_QURA" to "Umm al-Qura (Makkah)",
                    "EGYPTIAN" to "Egyptian Authority",
                    "KARACHI" to "Univ. of Karachi",
                    "NORTH_AMERICA" to "ISNA",
                    "DUBAI" to "Dubai (UAE)"
                )
                for ((methodKey, methodTitle) in methods) {
                    val isSelected = settings.calculationMethodName.equals(methodKey, ignoreCase = true)
                    Surface(
                        onClick = { onSetCalculationMethod(methodKey) },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) AccentEmerald else colorScheme.surface,
                            focusedContainerColor = AccentGold
                        ),
                        modifier = Modifier.tvFocusable()
                    ) {
                        Text(
                            text = methodTitle,
                            style = TvTypography.ButtonText.copy(
                                fontSize = 14.sp,
                                color = if (isSelected) Color.White else colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // 4. Madhab Selection (Shafi'i/Standard vs Hanafi)
            Text(
                text = stringResource(R.string.settings_section_madhab),
                style = TvTypography.CardTitle.copy(color = AccentGold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val isHanafi = settings.madhab.equals("HANAFI", ignoreCase = true)
                Card(
                    onClick = { onSetMadhab("SHAFI") },
                    colors = CardDefaults.colors(
                        containerColor = if (!isHanafi) colorScheme.surfaceVariant else colorScheme.surface
                    ),
                    shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                    modifier = Modifier
                        .weight(1f)
                        .tvFocusable()
                        .then(
                            if (!isHanafi) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(12.dp)) else Modifier
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_madhab_standard),
                            style = TvTypography.CardTitle.copy(fontSize = 18.sp, color = colorScheme.onSurface)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_madhab_standard_desc),
                            style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.75f))
                        )
                    }
                }

                Card(
                    onClick = { onSetMadhab("HANAFI") },
                    colors = CardDefaults.colors(
                        containerColor = if (isHanafi) colorScheme.surfaceVariant else colorScheme.surface
                    ),
                    shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                    modifier = Modifier
                        .weight(1f)
                        .tvFocusable()
                        .then(
                            if (isHanafi) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(12.dp)) else Modifier
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_madhab_hanafi),
                            style = TvTypography.CardTitle.copy(fontSize = 18.sp, color = colorScheme.onSurface)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_madhab_hanafi_desc),
                            style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.75f))
                        )
                    }
                }
            }

            // 5. High-Latitude Rule (Polar / High Latitude Regions)
            Text(
                text = stringResource(R.string.settings_section_high_latitude),
                style = TvTypography.CardTitle.copy(color = AccentGold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val rules = listOf(
                    null to stringResource(R.string.settings_high_latitude_none),
                    "MIDDLE_OF_THE_NIGHT" to stringResource(R.string.settings_high_latitude_motn),
                    "SEVENTH_OF_THE_NIGHT" to stringResource(R.string.settings_high_latitude_sotn),
                    "TWILIGHT_ANGLE" to stringResource(R.string.settings_high_latitude_angle)
                )
                for ((ruleKey, ruleTitle) in rules) {
                    val isSelected = (settings.highLatitudeRule?.uppercase() == ruleKey)
                    Surface(
                        onClick = { onSetHighLatitudeRule(ruleKey) },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) AccentEmerald else colorScheme.surface,
                            focusedContainerColor = AccentGold
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .tvFocusable()
                    ) {
                        Text(
                            text = ruleTitle,
                            style = TvTypography.ButtonText.copy(
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // 6. Per-Prayer Time Offsets (-30m to +30m)
            Text(
                text = stringResource(R.string.settings_section_offsets),
                style = TvTypography.CardTitle.copy(color = AccentGold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val prayerList = listOf(
                    Prayer.SUBUH to settings.offsetSubuh,
                    Prayer.ZUHUR to settings.offsetZuhur,
                    Prayer.ASHR to settings.offsetAshr,
                    Prayer.MAGRIB to settings.offsetMagrib,
                    Prayer.ISYA to settings.offsetIsya
                )
                for ((prayer, currentOffset) in prayerList) {
                    Card(
                        onClick = {},
                        colors = CardDefaults.colors(containerColor = colorScheme.surface),
                        shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(prayer.labelRes()),
                                style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                            )
                            Text(
                                text = if (currentOffset > 0) "+$currentOffset m" else "$currentOffset m",
                                style = TvTypography.BodyLabel.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentOffset != 0) AccentGold else AccentEmerald
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    onClick = { onSetPrayerOffset(prayer, (currentOffset - 1).coerceAtLeast(-30)) },
                                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
                                    colors = ClickableSurfaceDefaults.colors(containerColor = colorScheme.surfaceVariant),
                                    modifier = Modifier.tvFocusable()
                                ) {
                                    Text("-1", style = TvTypography.ButtonText.copy(fontSize = 11.sp), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Surface(
                                    onClick = { onSetPrayerOffset(prayer, 0) },
                                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
                                    colors = ClickableSurfaceDefaults.colors(containerColor = colorScheme.surfaceVariant),
                                    modifier = Modifier.tvFocusable()
                                ) {
                                    Text("0", style = TvTypography.ButtonText.copy(fontSize = 11.sp), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Surface(
                                    onClick = { onSetPrayerOffset(prayer, (currentOffset + 1).coerceAtMost(30)) },
                                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
                                    colors = ClickableSurfaceDefaults.colors(containerColor = colorScheme.surfaceVariant),
                                    modifier = Modifier.tvFocusable()
                                ) {
                                    Text("+1", style = TvTypography.ButtonText.copy(fontSize = 11.sp), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 8. Hijriyah Date Adjustments & Rollover
            Text(
                text = stringResource(R.string.settings_section_hijri_offset),
                style = TvTypography.CardTitle.copy(color = AccentGold)
            )

            // Day offset (-2..+2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val offsets = listOf(-2, -1, 0, 1, 2)
                for (offset in offsets) {
                    val isSelected = (settings.hijriOffsetDays == offset)
                    Surface(
                        onClick = { onSetHijriOffset(offset) },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) AccentEmerald else colorScheme.surface,
                            focusedContainerColor = AccentGold
                        ),
                        modifier = Modifier.tvFocusable()
                    ) {
                        Text(
                            text = if (offset > 0) "+$offset" else "$offset",
                            style = TvTypography.ButtonText.copy(
                                fontSize = 15.sp,
                                color = if (isSelected) Color.White else colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Maghrib vs Midnight Rollover
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    onClick = { onSetHijriChangeAtMidnight(false) },
                    colors = CardDefaults.colors(
                        containerColor = if (!settings.hijriChangeAtMidnight) colorScheme.surfaceVariant else colorScheme.surface
                    ),
                    shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                    modifier = Modifier
                        .weight(1f)
                        .tvFocusable()
                        .then(
                            if (!settings.hijriChangeAtMidnight) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(10.dp)) else Modifier
                        )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.settings_hijri_rollover_magrib),
                            style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                        )
                    }
                }

                Card(
                    onClick = { onSetHijriChangeAtMidnight(true) },
                    colors = CardDefaults.colors(
                        containerColor = if (settings.hijriChangeAtMidnight) colorScheme.surfaceVariant else colorScheme.surface
                    ),
                    shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                    modifier = Modifier
                        .weight(1f)
                        .tvFocusable()
                        .then(
                            if (settings.hijriChangeAtMidnight) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(10.dp)) else Modifier
                        )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.settings_hijri_rollover_midnight),
                            style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                        )
                    }
                }
            }

                Text(
                    text = stringResource(R.string.settings_section_home_reminders),
                    style = TvTypography.CardTitle.copy(color = AccentGold)
                )

                // 15-min Reminder Toggle
                Surface(
                    onClick = { onToggleReminder(!settings.prePrayerReminderEnabled) },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (settings.prePrayerReminderEnabled) colorScheme.surfaceVariant else colorScheme.surface
                    ),
                    modifier = Modifier.tvFocusable()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (settings.prePrayerReminderEnabled) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (settings.prePrayerReminderEnabled) stringResource(R.string.settings_reminder_toggle_on) else stringResource(R.string.settings_reminder_toggle_off),
                                style = TvTypography.ButtonText.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                            )
                            Text(
                                text = stringResource(R.string.settings_reminder_toggle_desc),
                                style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                            )
                        }
                    }
                }

                // Per-Prayer Adhan Sound Selector
                Text(
                    text = stringResource(R.string.settings_section_home_adhan),
                    style = TvTypography.CardTitle.copy(color = AccentGold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val prayers = listOf(Prayer.SUBUH, Prayer.ZUHUR, Prayer.ASHR, Prayer.MAGRIB, Prayer.ISYA)
                    for (prayer in prayers) {
                        val currentOption = settings.adhanFor(prayer)
                        Card(
                            onClick = {
                                val nextOption = when (currentOption.type) {
                                    AdhanAudioType.DEFAULT_SUBUH, AdhanAudioType.DEFAULT_STANDARD -> AdhanAudioOption.Silent
                                    AdhanAudioType.SILENT -> if (prayer == Prayer.SUBUH) AdhanAudioOption.DefaultSubuh else AdhanAudioOption.DefaultStandard
                                    AdhanAudioType.CUSTOM_FILE -> AdhanAudioOption.DefaultStandard
                                }
                                onSetAdhanAudio(prayer, nextOption)
                            },
                            colors = CardDefaults.colors(containerColor = colorScheme.surface),
                            shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                            modifier = Modifier
                                .weight(1f)
                                .tvFocusable()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(prayer.labelRes()),
                                    style = TvTypography.CardTitle.copy(fontSize = 18.sp, color = colorScheme.onSurface)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentOption.displayLabel(),
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (currentOption.type == AdhanAudioType.SILENT) Color(0xFFEF4444) else AccentEmerald
                                    )
                                )
                            }
                        }
                    }
                }

                // Row to browse & select custom audio for each prayer from USB/TV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val prayers = listOf(Prayer.SUBUH, Prayer.ZUHUR, Prayer.ASHR, Prayer.MAGRIB, Prayer.ISYA)
                    for (prayer in prayers) {
                        Surface(
                            onClick = {
                                pickerTargetPrayer = prayer
                                activePickerKind = ImportKind.CUSTOM_ADHAN
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = colorScheme.surfaceVariant,
                                focusedContainerColor = AccentGold
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .tvFocusable()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Usb,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.settings_btn_usb_file),
                                    style = TvTypography.BodyLabel.copy(fontSize = 11.sp, color = colorScheme.onSurface)
                                )
                            }
                        }
                    }
                }

            // ================= SECTION: ABOUT & CREDITS (Phase 7.1) =================
            Text(
                text = stringResource(R.string.settings_section_about),
                style = TvTypography.CardTitle.copy(color = AccentGold)
            )

            Card(
                onClick = {},
                colors = CardDefaults.colors(containerColor = colorScheme.surface),
                shape = CardDefaults.shape(RoundedCornerShape(14.dp)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = TvTypography.HeaderTitle.copy(fontSize = 22.sp, color = colorScheme.onSurface)
                            )
                            Text(
                                text = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                                style = TvTypography.Subtitle.copy(fontSize = 14.sp, color = AccentGold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.about_credit),
                        style = TvTypography.BodyLabel.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentEmerald)
                    )

                    Text(
                        text = stringResource(R.string.settings_about_license),
                        style = TvTypography.BodyLabel.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                    )

                    Text(
                        text = stringResource(R.string.settings_about_copyright),
                        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
                    )

                    Text(
                        text = stringResource(R.string.settings_about_source_code),
                        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
                    )

                    Text(
                        text = stringResource(R.string.settings_about_trademarks),
                        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                    )

                    Text(
                        text = stringResource(R.string.settings_about_libraries),
                        style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.6f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Overlay: TV D-pad Storage File Picker
        if (activePickerKind != null) {
            StorageFilePickerScreen(
                kind = activePickerKind!!,
                title = when (activePickerKind) {
                    ImportKind.MOSQUE_LOGO -> stringResource(R.string.storage_title_mosque_logo)
                    ImportKind.CUSTOM_ADHAN -> stringResource(R.string.storage_title_custom_adhan)
                    ImportKind.SLIDE_IMAGE -> stringResource(R.string.storage_title_slide_image)
                    else -> stringResource(R.string.storage_title_slide_image)
                },
                onFileImported = { fileName ->
                    when (activePickerKind) {
                        ImportKind.MOSQUE_LOGO -> {}
                        ImportKind.CUSTOM_ADHAN -> {
                            val prayer = pickerTargetPrayer
                            if (prayer != null) {
                                onSetCustomAdhan(prayer, fileName, "Adhan (${fileName.take(15)})")
                            }
                        }
                        ImportKind.SLIDE_IMAGE -> {}
                        null -> {}
                    }
                    activePickerKind = null
                    pickerTargetPrayer = null
                },
                onDismiss = {
                    activePickerKind = null
                    pickerTargetPrayer = null
                }
            )
        }

    }
}
