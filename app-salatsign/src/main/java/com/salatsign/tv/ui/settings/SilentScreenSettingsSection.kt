// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.salatsign.core.tv.R as CoreR
import com.salatsign.tv.R as AppR
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerSilentConfig
import com.salatsign.tv.data.model.SilentAnchor
import com.salatsign.tv.data.model.SilentScreenConfig
import com.salatsign.tv.engine.CopyPrayerSilentResult
import com.salatsign.tv.engine.SilentScreenCalculator
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.common.tvCardScale
import com.salatsign.tv.ui.common.tvSelectableCardBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceColors
import com.salatsign.tv.ui.common.tvSurfaceScale
import com.salatsign.tv.ui.theme.TvTypography
import kotlinx.coroutines.delay

@Composable
fun SilentScreenSettingsSection(
    settings: AppSettings,
    onSetSilentJumuahEnabled: (Boolean) -> Unit,
    onSetSilentJumuahLeadMinutes: (Int) -> Unit,
    onSetSilentJumuahDurationMinutes: (Int) -> Unit,
    onSetSilentDailyPrayerConfig: (Prayer, PrayerSilentConfig) -> Unit,
    onCopySilentSettingsToAllPrayers: (PrayerSilentConfig) -> CopyPrayerSilentResult,
    onStartManualSilentScreen: (Int) -> Unit,
    onStopManualSilentScreen: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val silent = settings.silentScreen
    var editingPrayer by remember { mutableStateOf<Prayer?>(null) }
    var copyStatusMessage by remember { mutableStateOf<String?>(null) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(silent.manualSilentUntilEpochMillis) {
        nowMillis = System.currentTimeMillis()
        while (silent.manualSilentUntilEpochMillis > nowMillis) {
            delay(15_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val manualRemainingMinutes = if (silent.manualSilentUntilEpochMillis > nowMillis) {
        (((silent.manualSilentUntilEpochMillis - nowMillis) + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
    } else {
        0
    }
    val hasConfiguredJumuah = settings.jumuahTimes.any { it.isNotBlank() }

    Text(
        text = stringResource(AppR.string.settings_section_silent_screen),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    // 1. Manual Control (Start 15 / 30 / 45 min, Stop Now)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(AppR.string.settings_silent_manual_title),
                        style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                    )
                    Text(
                        text = stringResource(AppR.string.settings_silent_manual_desc),
                        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.78f))
                    )
                }

                if (manualRemainingMinutes > 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                    val stopShape = RoundedCornerShape(8.dp)
                    Surface(
                        onClick = onStopManualSilentScreen,
                        shape = ClickableSurfaceDefaults.shape(stopShape),
                        scale = tvSurfaceScale(1.02f),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color(0xFFDC2626),
                            focusedContainerColor = Color(0xFFB91C1C),
                            contentColor = Color.White,
                            focusedContentColor = Color.White
                        ),
                        border = tvSelectableSurfaceBorder(selected = false, shape = stopShape)
                    ) {
                        Text(
                            text = stringResource(AppR.string.settings_silent_stop_now),
                            style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = Color.White),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            if (manualRemainingMinutes > 0) {
                Text(
                    text = stringResource(AppR.string.settings_silent_manual_active, manualRemainingMinutes),
                    style = TvTypography.BodyLabel.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val btnShape = RoundedCornerShape(10.dp)
                for (mins in SilentScreenConfig.MANUAL_DURATION_OPTIONS_MINUTES) {
                    Surface(
                        onClick = { onStartManualSilentScreen(mins) },
                        shape = ClickableSurfaceDefaults.shape(btnShape),
                        scale = tvSurfaceScale(1.01f),
                        colors = tvSelectableSurfaceColors(selected = false, containerColor = colorScheme.surfaceVariant),
                        border = tvSelectableSurfaceBorder(selected = false, shape = btnShape),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(AppR.string.settings_silent_show_now_mins, mins),
                                style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                            )
                        }
                    }
                }
            }
        }
    }

    // 2. Friday Jumu'ah Silent Screen (default ON)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val toggleShape = RoundedCornerShape(10.dp)
            Card(
                onClick = { onSetSilentJumuahEnabled(!silent.jumuahEnabled) },
                colors = CardDefaults.colors(
                    containerColor = if (silent.jumuahEnabled) colorScheme.surfaceVariant else colorScheme.background,
                    focusedContainerColor = colorScheme.surfaceVariant
                ),
                shape = CardDefaults.shape(toggleShape),
                scale = tvCardScale(1.0f),
                border = tvSelectableCardBorder(selected = silent.jumuahEnabled, shape = toggleShape),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(AppR.string.settings_silent_jumuah_title),
                            style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                        )
                        Text(
                            text = if (hasConfiguredJumuah) {
                                stringResource(AppR.string.settings_silent_jumuah_desc)
                            } else {
                                stringResource(AppR.string.settings_silent_jumuah_unconfigured)
                            },
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 13.sp,
                                color = if (hasConfiguredJumuah) colorScheme.onSurface.copy(alpha = 0.78f) else colorScheme.secondary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (silent.jumuahEnabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.16f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(if (silent.jumuahEnabled) AppR.string.settings_silent_enabled else AppR.string.settings_silent_disabled),
                            style = TvTypography.ButtonText.copy(
                                fontSize = 13.sp,
                                color = if (silent.jumuahEnabled) Color.White else colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            if (silent.jumuahEnabled) {
                // Lead minutes (0 / 5 / 10 / 15)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(AppR.string.settings_silent_lead_label),
                        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val chipShape = RoundedCornerShape(8.dp)
                        for (lead in SilentScreenConfig.LEAD_PRESET_OPTIONS_MINUTES) {
                            val selected = silent.jumuahLeadMinutes == lead
                            Surface(
                                onClick = { onSetSilentJumuahLeadMinutes(lead) },
                                shape = ClickableSurfaceDefaults.shape(chipShape),
                                scale = tvSurfaceScale(1.01f),
                                colors = tvSelectableSurfaceColors(selected = selected, containerColor = colorScheme.background),
                                border = tvSelectableSurfaceBorder(selected = selected, shape = chipShape),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (lead == 0) "0 min" else "-$lead min",
                                        style = TvTypography.ButtonText.copy(
                                            fontSize = 13.sp,
                                            color = if (selected) colorScheme.primary else colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Duration minutes (30 / 45 / 60 / 75 / 90 / 120)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(AppR.string.settings_silent_duration_label),
                        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val chipShape = RoundedCornerShape(8.dp)
                        for (dur in SilentScreenConfig.JUMUAH_DURATION_PRESET_MINUTES) {
                            val selected = silent.jumuahDurationMinutes == dur
                            Surface(
                                onClick = { onSetSilentJumuahDurationMinutes(dur) },
                                shape = ClickableSurfaceDefaults.shape(chipShape),
                                scale = tvSurfaceScale(1.01f),
                                colors = tvSelectableSurfaceColors(selected = selected, containerColor = colorScheme.background),
                                border = tvSelectableSurfaceBorder(selected = selected, shape = chipShape),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dur min",
                                        style = TvTypography.ButtonText.copy(
                                            fontSize = 13.sp,
                                            color = if (selected) colorScheme.primary else colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 3. Daily Prayers (Optional, compact 5-column cards -> opens D-pad editor dialog)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(AppR.string.settings_silent_daily_title),
            style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.onSurface)
        )
        Text(
            text = stringResource(AppR.string.settings_silent_daily_desc),
            style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.78f))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val prayerCardShape = RoundedCornerShape(10.dp)
            for (prayer in Prayer.entries) {
                val cfg = silent.forPrayer(prayer)
                val iqamahMins = settings.iqamahFor(prayer)
                val effectivelyOn = cfg.enabled && SilentScreenCalculator.canEnableDailySilent(cfg.anchor, iqamahMins)
                val anchorLabel = stringResource(
                    if (cfg.anchor == SilentAnchor.IQAMAH) AppR.string.settings_silent_anchor_iqamah
                    else AppR.string.settings_silent_anchor_adhan
                )
                Card(
                    onClick = {
                        copyStatusMessage = null
                        editingPrayer = prayer
                    },
                    colors = CardDefaults.colors(
                        containerColor = if (effectivelyOn) colorScheme.surfaceVariant else colorScheme.surface,
                        focusedContainerColor = colorScheme.surfaceVariant
                    ),
                    shape = CardDefaults.shape(prayerCardShape),
                    scale = tvCardScale(1.01f),
                    border = tvSelectableCardBorder(selected = effectivelyOn, shape = prayerCardShape),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(prayer.labelRes()),
                            style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (effectivelyOn) {
                                stringResource(AppR.string.settings_silent_summary_on, anchorLabel, cfg.durationMinutes)
                            } else {
                                stringResource(AppR.string.settings_silent_summary_off)
                            },
                            maxLines = 1,
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 12.sp,
                                fontWeight = if (effectivelyOn) FontWeight.Bold else FontWeight.Normal,
                                color = if (effectivelyOn) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        )
                    }
                }
            }
        }
    }

    editingPrayer?.let { prayer ->
        DailyPrayerSilentEditorDialog(
            prayer = prayer,
            config = silent.forPrayer(prayer),
            iqamahMinutes = settings.iqamahFor(prayer),
            copyStatusMessage = copyStatusMessage,
            onUpdateConfig = { updated ->
                copyStatusMessage = null
                onSetSilentDailyPrayerConfig(prayer, updated)
            },
            onCopyToAll = { currentConfig ->
                val result = onCopySilentSettingsToAllPrayers(currentConfig)
                copyStatusMessage = if (result.skippedWithoutIqamah.isEmpty()) {
                    null // Replaced via string resource below
                } else {
                    ""
                }
                result
            },
            onSetCopyStatusMessage = { msg -> copyStatusMessage = msg },
            onDismiss = { editingPrayer = null }
        )
    }
}

@Composable
private fun DailyPrayerSilentEditorDialog(
    prayer: Prayer,
    config: PrayerSilentConfig,
    iqamahMinutes: Int,
    copyStatusMessage: String?,
    onUpdateConfig: (PrayerSilentConfig) -> Unit,
    onCopyToAll: (PrayerSilentConfig) -> CopyPrayerSilentResult,
    onSetCopyStatusMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val canEnable = SilentScreenCalculator.canEnableDailySilent(config.anchor, iqamahMinutes)
    val initialFocus = remember { FocusRequester() }
    val resources = LocalResources.current
    val copiedAllText = stringResource(AppR.string.settings_silent_copied_all)

    LaunchedEffect(Unit) {
        runCatching { initialFocus.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogShape = RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .background(colorScheme.background, dialogShape)
                .border(1.5.dp, colorScheme.primary.copy(alpha = 0.55f), dialogShape)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "${stringResource(prayer.labelRes())} — ${stringResource(AppR.string.settings_section_silent_screen)}",
                    style = TvTypography.CardTitle.copy(fontSize = 20.sp, color = colorScheme.secondary)
                )

                // Enable / Disable toggle
                val toggleShape = RoundedCornerShape(10.dp)
                Card(
                    onClick = {
                        if (canEnable) {
                            onUpdateConfig(config.copy(enabled = !config.enabled))
                        }
                    },
                    colors = CardDefaults.colors(
                        containerColor = if (config.enabled && canEnable) colorScheme.surfaceVariant else colorScheme.surface,
                        focusedContainerColor = colorScheme.surfaceVariant
                    ),
                    shape = CardDefaults.shape(toggleShape),
                    scale = tvCardScale(1.0f),
                    border = tvSelectableCardBorder(selected = config.enabled && canEnable, shape = toggleShape),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(initialFocus)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(
                                    if (config.enabled && canEnable) AppR.string.settings_silent_enabled
                                    else AppR.string.settings_silent_disabled
                                ),
                                style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                            )
                            if (!canEnable) {
                                Text(
                                    text = stringResource(AppR.string.settings_silent_iqamah_required),
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFDC2626)
                                    )
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (config.enabled && canEnable) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.16f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (config.enabled && canEnable) "ON" else "OFF",
                                style = TvTypography.ButtonText.copy(
                                    fontSize = 13.sp,
                                    color = if (config.enabled && canEnable) Color.White else colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                // Anchor: IQAMAH vs ADHAN
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(AppR.string.settings_silent_anchor_label),
                        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val chipShape = RoundedCornerShape(8.dp)
                        for (anchor in SilentAnchor.entries) {
                            val selected = config.anchor == anchor
                            val label = stringResource(
                                if (anchor == SilentAnchor.IQAMAH) AppR.string.settings_silent_anchor_iqamah
                                else AppR.string.settings_silent_anchor_adhan
                            )
                            Surface(
                                onClick = {
                                    val nextCanEnable = SilentScreenCalculator.canEnableDailySilent(anchor, iqamahMinutes)
                                    onUpdateConfig(config.copy(anchor = anchor, enabled = if (nextCanEnable) config.enabled else false))
                                },
                                shape = ClickableSurfaceDefaults.shape(chipShape),
                                scale = tvSurfaceScale(1.01f),
                                colors = tvSelectableSurfaceColors(selected = selected),
                                border = tvSelectableSurfaceBorder(selected = selected, shape = chipShape),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = TvTypography.ButtonText.copy(
                                            fontSize = 14.sp,
                                            color = if (selected) colorScheme.primary else colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Lead minutes (0 / 5 / 10 / 15)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(AppR.string.settings_silent_lead_label),
                        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val chipShape = RoundedCornerShape(8.dp)
                        for (lead in SilentScreenConfig.LEAD_PRESET_OPTIONS_MINUTES) {
                            val selected = config.leadMinutes == lead
                            Surface(
                                onClick = { onUpdateConfig(config.copy(leadMinutes = lead)) },
                                shape = ClickableSurfaceDefaults.shape(chipShape),
                                scale = tvSurfaceScale(1.01f),
                                colors = tvSelectableSurfaceColors(selected = selected),
                                border = tvSelectableSurfaceBorder(selected = selected, shape = chipShape),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (lead == 0) "0 min" else "-$lead min",
                                        style = TvTypography.ButtonText.copy(
                                            fontSize = 13.sp,
                                            color = if (selected) colorScheme.primary else colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Duration minutes (5 / 10 / 15 / 20 / 30 / 45 / 60)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(AppR.string.settings_silent_duration_label),
                        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chipShape = RoundedCornerShape(8.dp)
                        for (dur in SilentScreenConfig.DAILY_DURATION_PRESET_MINUTES) {
                            val selected = config.durationMinutes == dur
                            Surface(
                                onClick = { onUpdateConfig(config.copy(durationMinutes = dur)) },
                                shape = ClickableSurfaceDefaults.shape(chipShape),
                                scale = tvSurfaceScale(1.01f),
                                colors = tvSelectableSurfaceColors(selected = selected),
                                border = tvSelectableSurfaceBorder(selected = selected, shape = chipShape),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${dur}m",
                                        style = TvTypography.ButtonText.copy(
                                            fontSize = 13.sp,
                                            color = if (selected) colorScheme.primary else colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (!copyStatusMessage.isNullOrBlank()) {
                    Text(
                        text = copyStatusMessage,
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.primary
                        )
                    )
                }

                // Actions: Copy to all prayers + Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val btnShape = RoundedCornerShape(10.dp)
                    Surface(
                        onClick = {
                            val result = onCopyToAll(config)
                            val msg = if (result.skippedWithoutIqamah.isEmpty()) {
                                copiedAllText
                            } else {
                                resources.getString(
                                    AppR.string.settings_silent_copied_partial,
                                    result.enabledCount,
                                    result.skippedWithoutIqamah.size
                                )
                            }
                            onSetCopyStatusMessage(msg)
                        },
                        shape = ClickableSurfaceDefaults.shape(btnShape),
                        scale = tvSurfaceScale(1.01f),
                        colors = tvSelectableSurfaceColors(selected = false),
                        border = tvSelectableSurfaceBorder(selected = false, shape = btnShape),
                        modifier = Modifier.weight(1.6f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 11.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(AppR.string.settings_silent_copy_all),
                                textAlign = TextAlign.Center,
                                style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                            )
                        }
                    }

                    Surface(
                        onClick = onDismiss,
                        shape = ClickableSurfaceDefaults.shape(btnShape),
                        scale = tvSurfaceScale(1.01f),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = colorScheme.primary,
                            focusedContainerColor = colorScheme.primary,
                            contentColor = Color.White,
                            focusedContentColor = Color.White
                        ),
                        border = tvSelectableSurfaceBorder(selected = false, shape = btnShape),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(AppR.string.settings_silent_done),
                                style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}
