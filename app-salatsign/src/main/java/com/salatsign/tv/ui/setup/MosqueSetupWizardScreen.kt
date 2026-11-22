// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.setup

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import androidx.tv.material3.Text
import com.salatsign.core.tv.R
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.MAX_MOSQUE_NAME_LENGTH
import com.salatsign.tv.data.model.MAX_MOSQUE_TAGLINE_LENGTH
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.PopularCityPresets
import com.salatsign.tv.ui.common.CountryCitySelector
import com.salatsign.tv.ui.common.MosqueEmblemIcon
import com.salatsign.tv.ui.common.TvEditableTextField
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.tvFocusable
import com.salatsign.tv.ui.common.tvSelectableCardBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceColors
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.TvTypography
import java.util.Locale

/**
 * First-run setup wizard dedicated to SalatSign (strictly silent Mosque Digital Signage).
 * Steps: 1. Location -> 2. Calculation Method & Madhab -> 3. Mosque Branding -> 4. Jumu'ah Sittings (Optional)
 */
@Composable
fun MosqueSetupWizardScreen(
    settings: AppSettings,
    onSelectCity: (CityPreset) -> Unit,
    onSetCalculationMethod: (String) -> Unit,
    onSetMadhab: (String) -> Unit,
    onSetMosqueName: (String) -> Unit,
    onSetMosqueSubtitle: (String) -> Unit,
    onSelectMosqueIcon: (MosqueIconPreset) -> Unit,
    onSetJumuahMode: (JumuahMode) -> Unit,
    onSetJumuahTimes: (List<String>) -> Unit,
    onCompleteSetup: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val maxSteps = 4
    val initialFocusRequester = remember { FocusRequester() }
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(currentStep) {
        initialFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .overscanPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.wizard_welcome),
                        style = TvTypography.HeaderTitle.copy(fontSize = 36.sp, color = colorScheme.onBackground)
                    )
                    Text(
                        text = stringResource(R.string.wizard_step_indicator, currentStep, maxSteps),
                        style = TvTypography.Subtitle.copy(fontSize = 18.sp, color = colorScheme.onBackground.copy(alpha = 0.7f))
                    )
                }

                Surface(
                    onClick = onCompleteSetup,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = colorScheme.surfaceVariant,
                        focusedContainerColor = AccentGold
                    ),
                    modifier = Modifier.tvFocusable()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_skip),
                            style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }

            // Step Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (currentStep) {
                    1 -> {
                        // Step 1: Country -> City Location
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(R.string.wizard_step_location_title, 1, settings.cityName),
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = AccentGold)
                            )
                            CountryCitySelector(
                                selectedCityName = settings.cityName,
                                onSelectCity = onSelectCity,
                                initialFocusRequester = initialFocusRequester
                            )
                        }
                    }

                    2 -> {
                        // Step 2: Calculation Method & Madhab
                        val methods = listOf(
                            "MUSLIM_WORLD_LEAGUE" to "Muslim World League",
                            "SINGAPORE" to "Singapore / JAKIM / Kemenag",
                            "UMM_AL_QURA" to "Umm Al-Qura",
                            "EGYPTIAN" to "Egyptian General Authority",
                            "KARACHI" to "University of Islamic Sciences, Karachi",
                            "NORTH_AMERICA" to "ISNA (North America)"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(R.string.wizard_step_method_title, 2),
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = AccentGold)
                            )
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                methods.chunked(3).forEachIndexed { rowIdx, rowMethods ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowMethods.forEachIndexed { colIdx, (key, label) ->
                                            val idx = rowIdx * 3 + colIdx
                                            val selected = settings.calculationMethodName.equals(key, ignoreCase = true)
                                            val shape = RoundedCornerShape(12.dp)
                                            Card(
                                                onClick = { onSetCalculationMethod(key) },
                                                colors = CardDefaults.colors(
                                                    containerColor = if (selected) colorScheme.surfaceVariant else colorScheme.surface
                                                ),
                                                shape = CardDefaults.shape(shape),
                                                border = tvSelectableCardBorder(selected = selected, shape = shape),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .then(if (idx == 0) Modifier.focusRequester(initialFocusRequester) else Modifier)
                                            ) {
                                                Text(
                                                    text = label,
                                                    maxLines = 2,
                                                    style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface),
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.settings_section_madhab),
                                style = TvTypography.Subtitle.copy(fontSize = 18.sp, color = AccentGold)
                            )
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                listOf("SHAFI" to stringResource(R.string.settings_madhab_standard), "HANAFI" to stringResource(R.string.settings_madhab_hanafi)).forEach { (key, label) ->
                                    val selected = settings.madhab.equals(key, ignoreCase = true)
                                    val shape = RoundedCornerShape(12.dp)
                                    Card(
                                        onClick = { onSetMadhab(key) },
                                        colors = CardDefaults.colors(
                                            containerColor = if (selected) colorScheme.surfaceVariant else colorScheme.surface
                                        ),
                                        shape = CardDefaults.shape(shape),
                                        border = tvSelectableCardBorder(selected = selected, shape = shape),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = label,
                                            style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.onSurface),
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Step 3: Mosque Branding (Mosque Name, Mosque Icon, Mosque Tagline)
                        val defaultMosqueName = stringResource(R.string.mosque_default_name)
                        var nameText by remember(settings.mosqueName) {
                            mutableStateOf(settings.mosqueName.ifBlank { defaultMosqueName })
                        }
                        var subtitleText by remember(settings.mosqueSubtitle) {
                            mutableStateOf(settings.mosqueSubtitle)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(R.string.wizard_step_mosque_title, 3),
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = AccentGold)
                            )

                            // Label + Editable Input: Mosque Name (defaults to Masjiduna)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stringResource(R.string.mosque_label_name),
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSurface.copy(alpha = 0.85f)
                                    )
                                )
                                TvEditableTextField(
                                    value = nameText,
                                    onValueChange = {
                                        nameText = it
                                        onSetMosqueName(it.trim().ifBlank { defaultMosqueName })
                                    },
                                    placeholder = defaultMosqueName,
                                    accentUnfocusedBorder = true,
                                    maxLength = MAX_MOSQUE_NAME_LENGTH,
                                    textStyle = TvTypography.CardTitle.copy(fontSize = 17.sp)
                                )
                            }

                            // Label + 4 Mosque Emblem Icons
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stringResource(R.string.mosque_label_icon),
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSurface.copy(alpha = 0.85f)
                                    )
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    MosqueIconPreset.entries.forEachIndexed { idx, preset ->
                                        val selected = settings.mosqueIconPreset == preset
                                        val shape = RoundedCornerShape(10.dp)
                                        Card(
                                            onClick = { onSelectMosqueIcon(preset) },
                                            colors = CardDefaults.colors(
                                                containerColor = if (selected) colorScheme.surfaceVariant else colorScheme.surface,
                                                focusedContainerColor = colorScheme.surfaceVariant
                                            ),
                                            shape = CardDefaults.shape(shape),
                                            border = tvSelectableCardBorder(selected = selected, shape = shape),
                                            modifier = Modifier
                                                .weight(1f)
                                                .then(if (idx == 0) Modifier.focusRequester(initialFocusRequester) else Modifier)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                MosqueEmblemIcon(preset = preset, customLogoUri = settings.mosqueLogoUri, size = 36.dp)
                                                Text(
                                                    text = stringResource(preset.labelRes()),
                                                    maxLines = 2,
                                                    style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Label + Editable Input: Mosque Tagline (with examples)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stringResource(R.string.mosque_label_tagline),
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSurface.copy(alpha = 0.85f)
                                    )
                                )
                                TvEditableTextField(
                                    value = subtitleText,
                                    onValueChange = {
                                        subtitleText = it
                                        onSetMosqueSubtitle(it)
                                    },
                                    placeholder = stringResource(R.string.mosque_tagline_hint),
                                    accentUnfocusedBorder = false,
                                    maxLength = MAX_MOSQUE_TAGLINE_LENGTH,
                                    textStyle = TvTypography.BodyLabel.copy(fontSize = 15.sp)
                                )
                            }
                        }
                    }

                    4 -> {
                        // Step 4: Friday Jumu'ah Schedule (interactive -15m / +15m stepper, defaults to 1:00 PM)
                        var jumuahDisabled by remember { mutableStateOf(false) }

                        LaunchedEffect(currentStep) {
                            if (settings.jumuahTimes.isEmpty() && !jumuahDisabled) {
                                onSetJumuahMode(JumuahMode.FIXED)
                                onSetJumuahTimes(listOf("13:00"))
                            }
                        }

                        val activeTimes = if (jumuahDisabled) {
                            emptyList()
                        } else if (settings.jumuahMode == JumuahMode.FIXED && settings.jumuahTimes.isNotEmpty()) {
                            settings.jumuahTimes
                        } else {
                            listOf("13:00")
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = stringResource(R.string.wizard_step_jumuah_title, 4),
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = AccentGold)
                            )
                            Text(
                                text = stringResource(R.string.settings_jumuah_desc),
                                style = TvTypography.BodyLabel.copy(color = colorScheme.onSurface.copy(alpha = 0.75f))
                            )

                            if (!jumuahDisabled) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    activeTimes.forEachIndexed { idx, timeStr ->
                                        val mins = parseTimeToMinutes(timeStr, defaultMinutes = 13 * 60)
                                        val formattedDisplay = formatJumuahDisplay(mins, settings.is24HourFormat)

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(colorScheme.surface, RoundedCornerShape(14.dp))
                                                .border(1.5.dp, AccentEmerald.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                                .padding(horizontal = 20.dp, vertical = 16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = if (activeTimes.size > 1) {
                                                    stringResource(R.string.settings_jumuah_sitting_n, idx + 1)
                                                } else {
                                                    stringResource(R.string.prayer_jumuah)
                                                },
                                                style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = AccentGold)
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                                            ) {
                                                val btnShape = RoundedCornerShape(10.dp)
                                                Surface(
                                                    onClick = {
                                                        val updated = (mins - 15).coerceIn(11 * 60, 16 * 60)
                                                        val nextList = activeTimes.toMutableList()
                                                        nextList[idx] = formatMinutesHHmm(updated)
                                                        onSetJumuahMode(JumuahMode.FIXED)
                                                        onSetJumuahTimes(nextList)
                                                    },
                                                    shape = ClickableSurfaceDefaults.shape(btnShape),
                                                    colors = tvSelectableSurfaceColors(selected = false),
                                                    border = tvSelectableSurfaceBorder(selected = false, shape = btnShape),
                                                    modifier = if (idx == 0) Modifier.focusRequester(initialFocusRequester) else Modifier
                                                ) {
                                                    Text(
                                                        text = "-15 min",
                                                        style = TvTypography.ButtonText.copy(fontSize = 16.sp, color = colorScheme.onSurface),
                                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                                    )
                                                }

                                                Text(
                                                    text = formattedDisplay,
                                                    style = TvTypography.ActivePrayerTime.copy(
                                                        fontSize = 32.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AccentEmerald
                                                    )
                                                )

                                                Surface(
                                                    onClick = {
                                                        val updated = (mins + 15).coerceIn(11 * 60, 16 * 60)
                                                        val nextList = activeTimes.toMutableList()
                                                        nextList[idx] = formatMinutesHHmm(updated)
                                                        onSetJumuahMode(JumuahMode.FIXED)
                                                        onSetJumuahTimes(nextList)
                                                    },
                                                    shape = ClickableSurfaceDefaults.shape(btnShape),
                                                    colors = tvSelectableSurfaceColors(selected = false),
                                                    border = tvSelectableSurfaceBorder(selected = false, shape = btnShape)
                                                ) {
                                                    Text(
                                                        text = "+15 min",
                                                        style = TvTypography.ButtonText.copy(fontSize = 16.sp, color = colorScheme.onSurface),
                                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Secondary Actions: Add/Remove 2nd Sitting + Skip / Re-enable Jumu'ah
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val actionShape = RoundedCornerShape(10.dp)
                                if (!jumuahDisabled) {
                                    Surface(
                                        onClick = {
                                            if (activeTimes.size < 2) {
                                                val firstMins = parseTimeToMinutes(activeTimes.first(), 13 * 60)
                                                val secondMins = (firstMins + 45).coerceAtMost(16 * 60)
                                                onSetJumuahMode(JumuahMode.FIXED)
                                                onSetJumuahTimes(listOf(activeTimes.first(), formatMinutesHHmm(secondMins)))
                                            } else {
                                                onSetJumuahMode(JumuahMode.FIXED)
                                                onSetJumuahTimes(listOf(activeTimes.first()))
                                            }
                                        },
                                        shape = ClickableSurfaceDefaults.shape(actionShape),
                                        colors = tvSelectableSurfaceColors(selected = activeTimes.size >= 2),
                                        border = tvSelectableSurfaceBorder(selected = activeTimes.size >= 2, shape = actionShape)
                                    ) {
                                        Text(
                                            text = if (activeTimes.size < 2) {
                                                stringResource(R.string.jumuah_add_second_sitting)
                                            } else {
                                                stringResource(R.string.jumuah_remove_second_sitting)
                                            },
                                            style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = {
                                        if (jumuahDisabled) {
                                            jumuahDisabled = false
                                            onSetJumuahMode(JumuahMode.FIXED)
                                            onSetJumuahTimes(listOf("13:00"))
                                        } else {
                                            jumuahDisabled = true
                                            onSetJumuahMode(JumuahMode.FIXED)
                                            onSetJumuahTimes(emptyList())
                                        }
                                    },
                                    shape = ClickableSurfaceDefaults.shape(actionShape),
                                    colors = tvSelectableSurfaceColors(selected = jumuahDisabled),
                                    border = tvSelectableSurfaceBorder(selected = jumuahDisabled, shape = actionShape),
                                    modifier = if (jumuahDisabled) Modifier.focusRequester(initialFocusRequester) else Modifier
                                ) {
                                    Text(
                                        text = if (jumuahDisabled) {
                                            stringResource(R.string.jumuah_enable_schedule)
                                        } else {
                                            stringResource(R.string.jumuah_no_schedule)
                                        },
                                        style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    Surface(
                        onClick = { currentStep -= 1 },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = colorScheme.surface,
                            focusedContainerColor = AccentGold
                        ),
                        modifier = Modifier.tvFocusable()
                    ) {
                        Text(
                            text = stringResource(R.string.btn_back),
                            style = TvTypography.ButtonText.copy(color = colorScheme.onSurface),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Surface(
                    onClick = {
                        if (currentStep < maxSteps) currentStep += 1 else onCompleteSetup()
                    },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = AccentEmerald,
                        focusedContainerColor = AccentGold
                    ),
                    modifier = Modifier.tvFocusable()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentStep < maxSteps) stringResource(R.string.btn_next) else stringResource(R.string.btn_finish),
                            style = TvTypography.ButtonText.copy(color = Color.White)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (currentStep < maxSteps) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun parseTimeToMinutes(hhmm: String, defaultMinutes: Int): Int {
    val parts = hhmm.trim().split(":")
    if (parts.size != 2) return defaultMinutes
    val h = parts[0].toIntOrNull() ?: return defaultMinutes
    val m = parts[1].toIntOrNull() ?: return defaultMinutes
    return (h * 60 + m).coerceIn(0, 23 * 60 + 59)
}

private fun formatMinutesHHmm(totalMinutes: Int): String {
    val h = (totalMinutes / 60).coerceIn(0, 23)
    val m = (totalMinutes % 60).coerceIn(0, 59)
    return String.format(Locale.US, "%02d:%02d", h, m)
}

private fun formatJumuahDisplay(totalMinutes: Int, is24Hour: Boolean): String {
    val h24 = (totalMinutes / 60).coerceIn(0, 23)
    val m = (totalMinutes % 60).coerceIn(0, 59)
    if (is24Hour) {
        return String.format(Locale.US, "%02d:%02d", h24, m)
    }
    val amPm = if (h24 >= 12) "PM" else "AM"
    val h12 = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    return String.format(Locale.US, "%d:%02d %s", h12, m, amPm)
}
