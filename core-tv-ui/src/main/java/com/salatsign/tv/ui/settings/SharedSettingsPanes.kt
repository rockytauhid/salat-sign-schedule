// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.salatsign.core.tv.R
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.ui.common.CountryCitySelector
import com.salatsign.tv.ui.common.horizontalScrollBleed
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.common.tvCardScale
import com.salatsign.tv.ui.common.tvSelectableCardBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceColors
import com.salatsign.tv.ui.common.tvSurfaceScale
import com.salatsign.tv.ui.theme.TvTypography
import java.time.Instant
import java.time.ZoneId

/**
 * Shared Category 1: Display & Language (identical in SalatSign and SalatSchedule).
 */
@Composable
fun SettingsDisplayAndLanguagePane(
    settings: AppSettings,
    onSetAppLanguage: (AppLanguage) -> Unit,
    onSet24HourFormat: (Boolean) -> Unit,
    onSetShowArabicPrayerNames: (Boolean) -> Unit,
    onSetShowStrikeHighlight: (Boolean) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    bannerDescriptionRes: Int = R.string.settings_prayer_alert_banner_desc,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    if (AppLanguage.isMultiLanguageShipped) {
        Text(
            text = stringResource(R.string.settings_section_language),
            style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (lang in AppLanguage.ShippedLanguages) {
                val isSelected = (settings.appLanguage == lang) ||
                    (settings.appLanguage == AppLanguage.SYSTEM && lang == AppLanguage.EN)
                val langShape = RoundedCornerShape(10.dp)
                Card(
                    onClick = { onSetAppLanguage(lang) },
                    colors = CardDefaults.colors(
                        containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface,
                        focusedContainerColor = colorScheme.surfaceVariant
                    ),
                    shape = CardDefaults.shape(langShape),
                    scale = tvCardScale(1.01f),
                    border = tvSelectableCardBorder(selected = isSelected, shape = langShape),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = if (isSelected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(lang.labelRes()),
                            style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                        )
                    }
                }
            }
        }
    }

    // Time & Display Preferences (each option on its own full-width row)
    Text(
        text = stringResource(R.string.settings_section_time_display),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val prefShape = RoundedCornerShape(10.dp)
        Card(
            onClick = { onSet24HourFormat(!settings.is24HourFormat) },
            colors = CardDefaults.colors(
                containerColor = colorScheme.surface,
                focusedContainerColor = colorScheme.surfaceVariant
            ),
            shape = CardDefaults.shape(prefShape),
            scale = tvCardScale(1.01f),
            border = tvSelectableCardBorder(selected = false, shape = prefShape),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = colorScheme.primary,
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
                        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.primary, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Card(
            onClick = { onSetShowArabicPrayerNames(!settings.showArabicPrayerNames) },
            colors = CardDefaults.colors(
                containerColor = if (settings.showArabicPrayerNames) colorScheme.surfaceVariant else colorScheme.surface,
                focusedContainerColor = colorScheme.surfaceVariant
            ),
            shape = CardDefaults.shape(prefShape),
            scale = tvCardScale(1.01f),
            border = tvSelectableCardBorder(selected = settings.showArabicPrayerNames, shape = prefShape),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.settings_arabic_names_toggle),
                        style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                    )
                    Text(
                        text = if (settings.showArabicPrayerNames) "Active" else "Hidden",
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 13.sp,
                            color = if (settings.showArabicPrayerNames) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }

        PrayerTimeAlertBannerCard(
            enabled = settings.showStrikeHighlight,
            descriptionRes = bannerDescriptionRes,
            onToggle = onSetShowStrikeHighlight
        )

        if (extraContent != null) {
            extraContent()
        }
    }

    // Theme Selection
    Text(
        text = stringResource(R.string.settings_section_theme),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (mode in ThemeMode.entries) {
            val isSelected = (settings.themeMode == mode)
            val themeShape = RoundedCornerShape(12.dp)
            Card(
                onClick = { onSetThemeMode(mode) },
                colors = CardDefaults.colors(
                    containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface,
                    focusedContainerColor = colorScheme.surfaceVariant
                ),
                shape = CardDefaults.shape(themeShape),
                scale = tvCardScale(1.01f),
                border = tvSelectableCardBorder(selected = isSelected, shape = themeShape),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (mode) {
                            ThemeMode.ALWAYS_DARK -> Icons.Default.DarkMode
                            ThemeMode.ALWAYS_LIGHT -> Icons.Default.LightMode
                            ThemeMode.AUTO_DAY_NIGHT -> Icons.Default.BrightnessAuto
                        },
                        contentDescription = null,
                        tint = if (isSelected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(mode.labelRes()),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                    )
                }
            }
        }
    }
}

@Composable
fun PrayerTimeAlertBannerCard(
    enabled: Boolean,
    descriptionRes: Int = R.string.settings_prayer_alert_banner_desc,
    onToggle: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val strikeShape = RoundedCornerShape(10.dp)
    Card(
        onClick = { onToggle(!enabled) },
        colors = CardDefaults.colors(
            containerColor = if (enabled) colorScheme.surfaceVariant else colorScheme.surface,
            focusedContainerColor = colorScheme.surfaceVariant
        ),
        shape = CardDefaults.shape(strikeShape),
        scale = tvCardScale(1.01f),
        border = tvSelectableCardBorder(selected = enabled, shape = strikeShape),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = if (enabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (enabled) {
                        stringResource(R.string.settings_prayer_alert_banner_on)
                    } else {
                        stringResource(R.string.settings_prayer_alert_banner_off)
                    },
                    style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                )
                Text(
                    text = stringResource(descriptionRes),
                    style = TvTypography.BodyLabel.copy(
                        fontSize = 12.sp,
                        color = colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                )
            }
        }
    }
}

/**
 * Shared Category 2: Location & Calculation Method (identical in SalatSign and SalatSchedule).
 */
@Composable
fun SettingsLocationAndCalculationPane(
    settings: AppSettings,
    onSelectCity: (CityPreset) -> Unit,
    onSetCalculationMethod: (String) -> Unit,
    onSetMadhab: (String) -> Unit,
    onSetHighLatitudeRule: (String?) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Text(
        text = "${stringResource(R.string.settings_section_location)} (${settings.cityName})",
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    CountryCitySelector(
        selectedCityName = settings.cityName,
        onSelectCity = onSelectCity,
        scrollableCityGrid = false
    )

    val currentZone = remember(settings.zoneId) {
        runCatching { ZoneId.of(settings.zoneId) }.getOrDefault(ZoneId.systemDefault())
    }
    val systemZone = remember { ZoneId.systemDefault() }
    val isTimezoneMismatch = remember(currentZone, systemZone) {
        val now = Instant.now()
        currentZone.rules.getOffset(now) != systemZone.rules.getOffset(now)
    }

    if (isTimezoneMismatch) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E3A8A).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
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
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${stringResource(R.string.settings_current_zone, currentZone.id)} (System: ${systemZone.id})",
                    style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface)
                )
            }
        }
    }

    Text(
        text = stringResource(R.string.settings_section_calc_method),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    val methodScrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScrollBleed(6.dp)
            .horizontalScroll(methodScrollState)
            .padding(horizontal = 6.dp, vertical = 4.dp),
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
            val methodShape = RoundedCornerShape(8.dp)
            Surface(
                onClick = { onSetCalculationMethod(methodKey) },
                shape = ClickableSurfaceDefaults.shape(methodShape),
                scale = tvSurfaceScale(1.0f),
                colors = tvSelectableSurfaceColors(selected = isSelected),
                border = tvSelectableSurfaceBorder(selected = isSelected, shape = methodShape)
            ) {
                Text(
                    text = methodTitle,
                    style = TvTypography.ButtonText.copy(
                        fontSize = 14.sp,
                        color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }

    Text(
        text = stringResource(R.string.settings_section_madhab),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val isHanafi = settings.madhab.equals("HANAFI", ignoreCase = true)
        val madhabShape = RoundedCornerShape(12.dp)
        Card(
            onClick = { onSetMadhab("SHAFI") },
            colors = CardDefaults.colors(
                containerColor = if (!isHanafi) colorScheme.surfaceVariant else colorScheme.surface,
                focusedContainerColor = colorScheme.surfaceVariant
            ),
            shape = CardDefaults.shape(madhabShape),
            scale = tvCardScale(1.01f),
            border = tvSelectableCardBorder(selected = !isHanafi, shape = madhabShape),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.settings_madhab_standard),
                    style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                )
            }
        }

        Card(
            onClick = { onSetMadhab("HANAFI") },
            colors = CardDefaults.colors(
                containerColor = if (isHanafi) colorScheme.surfaceVariant else colorScheme.surface,
                focusedContainerColor = colorScheme.surfaceVariant
            ),
            shape = CardDefaults.shape(madhabShape),
            scale = tvCardScale(1.01f),
            border = tvSelectableCardBorder(selected = isHanafi, shape = madhabShape),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.settings_madhab_hanafi),
                    style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                )
            }
        }
    }

    Text(
        text = stringResource(R.string.settings_section_high_latitude),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
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
            val ruleShape = RoundedCornerShape(8.dp)
            Surface(
                onClick = { onSetHighLatitudeRule(ruleKey) },
                shape = ClickableSurfaceDefaults.shape(ruleShape),
                scale = tvSurfaceScale(1.02f),
                colors = tvSelectableSurfaceColors(selected = isSelected),
                border = tvSelectableSurfaceBorder(selected = isSelected, shape = ruleShape),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = ruleTitle,
                    textAlign = TextAlign.Center,
                    style = TvTypography.ButtonText.copy(
                        fontSize = 13.sp,
                        color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                )
            }
        }
    }
}

/**
 * Shared Category 3: Prayer Time Offsets & Hijri Adjustments (identical in both apps).
 */
@Composable
fun SettingsPrayerOffsetsAndHijriPane(
    settings: AppSettings,
    onSetPrayerOffset: (Prayer, Int) -> Unit,
    onSetHijriOffset: (Int) -> Unit,
    onSetHijriChangeAtMidnight: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Text(
        text = stringResource(R.string.settings_section_offsets),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val prayerList = listOf(
            Prayer.SUBUH to settings.offsetSubuh,
            Prayer.ZUHUR to settings.offsetZuhur,
            Prayer.ASHR to settings.offsetAshr,
            Prayer.MAGRIB to settings.offsetMagrib,
            Prayer.ISYA to settings.offsetIsya
        )
        for ((prayer, currentOffset) in prayerList) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(colorScheme.surface, RoundedCornerShape(10.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(prayer.labelRes()),
                        style = TvTypography.CardTitle.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                    )
                    Text(
                        text = if (currentOffset > 0) "+$currentOffset m" else "$currentOffset m",
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentOffset != 0) colorScheme.secondary else colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val stepShape = RoundedCornerShape(6.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            onClick = { onSetPrayerOffset(prayer, (currentOffset - 1).coerceAtLeast(-30)) },
                            shape = ClickableSurfaceDefaults.shape(stepShape),
                            scale = tvSurfaceScale(1.04f),
                            colors = tvSelectableSurfaceColors(selected = false, containerColor = colorScheme.surfaceVariant),
                            border = tvSelectableSurfaceBorder(selected = false, shape = stepShape)
                        ) {
                            Text("-1", style = TvTypography.ButtonText.copy(fontSize = 11.sp, color = colorScheme.onSurface), modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                        }
                        Surface(
                            onClick = { onSetPrayerOffset(prayer, 0) },
                            shape = ClickableSurfaceDefaults.shape(stepShape),
                            scale = tvSurfaceScale(1.04f),
                            colors = tvSelectableSurfaceColors(selected = false, containerColor = colorScheme.surfaceVariant),
                            border = tvSelectableSurfaceBorder(selected = false, shape = stepShape)
                        ) {
                            Text("0", style = TvTypography.ButtonText.copy(fontSize = 11.sp, color = colorScheme.onSurface), modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                        }
                        Surface(
                            onClick = { onSetPrayerOffset(prayer, (currentOffset + 1).coerceAtMost(30)) },
                            shape = ClickableSurfaceDefaults.shape(stepShape),
                            scale = tvSurfaceScale(1.04f),
                            colors = tvSelectableSurfaceColors(selected = false, containerColor = colorScheme.surfaceVariant),
                            border = tvSelectableSurfaceBorder(selected = false, shape = stepShape)
                        ) {
                            Text("+1", style = TvTypography.ButtonText.copy(fontSize = 11.sp, color = colorScheme.onSurface), modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }

    Text(
        text = stringResource(R.string.settings_section_hijri_offset),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )
    Text(
        text = stringResource(R.string.settings_hijri_calendar_note),
        style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.72f))
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val offsets = listOf(-2, -1, 0, 1, 2)
        val offsetShape = RoundedCornerShape(8.dp)
        for (offset in offsets) {
            val isSelected = (settings.hijriOffsetDays == offset)
            Surface(
                onClick = { onSetHijriOffset(offset) },
                shape = ClickableSurfaceDefaults.shape(offsetShape),
                scale = tvSurfaceScale(1.03f),
                colors = tvSelectableSurfaceColors(selected = isSelected),
                border = tvSelectableSurfaceBorder(selected = isSelected, shape = offsetShape)
            ) {
                Text(
                    text = if (offset > 0) "+$offset" else "$offset",
                    style = TvTypography.ButtonText.copy(
                        fontSize = 15.sp,
                        color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val rolloverShape = RoundedCornerShape(10.dp)
        Card(
            onClick = { onSetHijriChangeAtMidnight(false) },
            colors = CardDefaults.colors(
                containerColor = if (!settings.hijriChangeAtMidnight) colorScheme.surfaceVariant else colorScheme.surface,
                focusedContainerColor = colorScheme.surfaceVariant
            ),
            shape = CardDefaults.shape(rolloverShape),
            scale = tvCardScale(1.01f),
            border = tvSelectableCardBorder(selected = !settings.hijriChangeAtMidnight, shape = rolloverShape),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.settings_hijri_rollover_magrib),
                    style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                )
            }
        }

        Card(
            onClick = { onSetHijriChangeAtMidnight(true) },
            colors = CardDefaults.colors(
                containerColor = if (settings.hijriChangeAtMidnight) colorScheme.surfaceVariant else colorScheme.surface,
                focusedContainerColor = colorScheme.surfaceVariant
            ),
            shape = CardDefaults.shape(rolloverShape),
            scale = tvCardScale(1.01f),
            border = tvSelectableCardBorder(selected = settings.hijriChangeAtMidnight, shape = rolloverShape),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.settings_hijri_rollover_midnight),
                    style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                )
            }
        }
    }
}

/**
 * Shared About & Credits Card.
 */
@Composable
fun SettingsAboutCard(
    versionName: String,
    versionCode: Int,
    extraCredits: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    Text(
        text = stringResource(R.string.settings_section_about),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surface, RoundedCornerShape(14.dp))
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
                    tint = colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = TvTypography.HeaderTitle.copy(fontSize = 22.sp, color = colorScheme.onSurface)
                    )
                    Text(
                        text = stringResource(R.string.settings_about_version, versionName, versionCode),
                        style = TvTypography.Subtitle.copy(fontSize = 14.sp, color = colorScheme.secondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.about_credit, stringResource(R.string.app_name)),
                style = TvTypography.BodyLabel.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
            )
            Text(
                text = stringResource(R.string.settings_about_license),
                style = TvTypography.BodyLabel.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
            )
            Text(
                text = stringResource(R.string.settings_about_copyright, stringResource(R.string.app_name)),
                style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
            )
            Text(
                text = stringResource(R.string.settings_about_source_code),
                style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
            )
            Text(
                text = stringResource(R.string.settings_about_libraries),
                style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.65f))
            )

            if (extraCredits != null) {
                extraCredits()
            }
        }
    }
}
