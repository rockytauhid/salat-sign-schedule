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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
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
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.PopularCityPresets
import com.salatsign.tv.ui.common.MosqueEmblemIcon
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.tvFocusable
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.TvTypography

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
                        // Step 1: Location / City
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(R.string.wizard_step_location_title, 1, settings.cityName),
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = AccentGold)
                            )
                            val scrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                PopularCityPresets.forEachIndexed { idx, city ->
                                    val selected = settings.cityName == city.name
                                    Card(
                                        onClick = { onSelectCity(city) },
                                        colors = CardDefaults.colors(
                                            containerColor = if (selected) colorScheme.surfaceVariant else colorScheme.surface
                                        ),
                                        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                                        modifier = Modifier
                                            .then(if (idx == 0) Modifier.focusRequester(initialFocusRequester) else Modifier)
                                            .tvFocusable()
                                            .then(if (selected) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(12.dp)) else Modifier)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = city.name,
                                                style = TvTypography.CardTitle.copy(fontSize = 17.sp, color = colorScheme.onSurface)
                                            )
                                            Text(
                                                text = city.zoneId,
                                                style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                            )
                                        }
                                    }
                                }
                            }
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
                            val scrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                methods.forEachIndexed { idx, (key, label) ->
                                    val selected = settings.calculationMethodName.equals(key, ignoreCase = true)
                                    Card(
                                        onClick = { onSetCalculationMethod(key) },
                                        colors = CardDefaults.colors(
                                            containerColor = if (selected) colorScheme.surfaceVariant else colorScheme.surface
                                        ),
                                        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                                        modifier = Modifier
                                            .then(if (idx == 0) Modifier.focusRequester(initialFocusRequester) else Modifier)
                                            .tvFocusable()
                                            .then(if (selected) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(12.dp)) else Modifier)
                                    ) {
                                        Text(
                                            text = label,
                                            style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.onSurface),
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.settings_section_madhab),
                                style = TvTypography.Subtitle.copy(fontSize = 18.sp, color = AccentGold)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf("SHAFI" to stringResource(R.string.settings_madhab_standard), "HANAFI" to stringResource(R.string.settings_madhab_hanafi)).forEach { (key, label) ->
                                    val selected = settings.madhab.equals(key, ignoreCase = true)
                                    Card(
                                        onClick = { onSetMadhab(key) },
                                        colors = CardDefaults.colors(
                                            containerColor = if (selected) colorScheme.surfaceVariant else colorScheme.surface
                                        ),
                                        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .tvFocusable()
                                            .then(if (selected) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(12.dp)) else Modifier)
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
                        // Step 3: Mosque Branding
                        val namePresets = stringArrayResource(R.array.mosque_name_presets)
                        val subtitlePresets = stringArrayResource(R.array.mosque_subtitle_presets)
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = stringResource(R.string.wizard_step_mosque_title, 3),
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = AccentGold)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                namePresets.forEachIndexed { idx, name ->
                                    val selected = settings.mosqueName == name
                                    Card(
                                        onClick = { onSetMosqueName(name) },
                                        colors = CardDefaults.colors(
                                            containerColor = if (selected) colorScheme.surfaceVariant else colorScheme.surface
                                        ),
                                        shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                                        modifier = Modifier
                                            .then(if (idx == 0) Modifier.focusRequester(initialFocusRequester) else Modifier)
                                            .tvFocusable()
                                            .then(if (selected) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(10.dp)) else Modifier)
                                    ) {
                                        Text(
                                            text = name,
                                            style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = colorScheme.onSurface),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MosqueIconPreset.entries.forEach { preset ->
                                    val selected = settings.mosqueIconPreset == preset
                                    Card(
                                        onClick = { onSelectMosqueIcon(preset) },
                                        colors = CardDefaults.colors(
                                            containerColor = if (selected) colorScheme.surfaceVariant else colorScheme.surface
                                        ),
                                        shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                                        modifier = Modifier
                                            .tvFocusable()
                                            .then(if (selected) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(10.dp)) else Modifier)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            MosqueEmblemIcon(preset = preset, customLogoUri = settings.mosqueLogoUri, size = 32.dp)
                                            Text(
                                                text = stringResource(preset.labelRes()),
                                                style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface)
                                            )
                                        }
                                    }
                                }
                            }

                            if (subtitlePresets.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    subtitlePresets.forEach { sub ->
                                        val selected = settings.mosqueSubtitle == sub
                                        Card(
                                            onClick = { onSetMosqueSubtitle(sub) },
                                            colors = CardDefaults.colors(
                                                containerColor = if (selected) colorScheme.surfaceVariant else colorScheme.surface
                                            ),
                                            shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                                            modifier = Modifier
                                                .tvFocusable()
                                                .then(if (selected) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(10.dp)) else Modifier)
                                        ) {
                                            Text(
                                                text = sub,
                                                style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // Step 4: Jumu'ah Sittings (Optional)
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(R.string.wizard_step_jumuah_title, 4),
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = AccentGold)
                            )
                            Text(
                                text = stringResource(R.string.settings_jumuah_desc),
                                style = TvTypography.BodyLabel.copy(color = colorScheme.onSurface.copy(alpha = 0.75f))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val presets = listOf(
                                    Triple("1:00 pm", JumuahMode.FIXED, listOf("13:00")),
                                    Triple("12:30 pm & 1:30 pm", JumuahMode.FIXED, listOf("12:30", "13:30")),
                                    Triple("+30 min", JumuahMode.AFTER_ZUHR, listOf("30")),
                                    Triple(stringResource(R.string.wizard_btn_skip_jumuah), JumuahMode.FIXED, emptyList())
                                )
                                presets.forEachIndexed { idx, (label, mode, times) ->
                                    val selected = settings.jumuahMode == mode && settings.jumuahTimes == times
                                    Card(
                                        onClick = {
                                            onSetJumuahMode(mode)
                                            onSetJumuahTimes(times)
                                        },
                                        colors = CardDefaults.colors(
                                            containerColor = if (selected) colorScheme.surfaceVariant else colorScheme.surface
                                        ),
                                        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(if (idx == 0) Modifier.focusRequester(initialFocusRequester) else Modifier)
                                            .tvFocusable()
                                            .then(if (selected) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(12.dp)) else Modifier)
                                    ) {
                                        Text(
                                            text = label,
                                            style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface),
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
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
