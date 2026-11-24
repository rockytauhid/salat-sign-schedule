// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.ui.setup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import com.salatsign.tv.data.model.PopularCityPresets
import com.salatsign.tv.ui.common.CountryCitySelector
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.tvCardScale
import com.salatsign.tv.ui.common.tvFocusable
import com.salatsign.tv.ui.common.tvSelectableCardBorder
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.TvTypography

/**
 * First-run setup wizard dedicated to SalatSchedule (Home Prayer Times TV app).
 * Steps: 1. Location -> 2. Calculation Method & Madhab -> 3. Reminders & Notifications
 */
@Composable
fun HomeSetupWizardScreen(
    settings: AppSettings,
    onSetAppLanguage: (AppLanguage) -> Unit = {},
    onSelectCity: (CityPreset) -> Unit,
    onSetCalculationMethod: (String) -> Unit,
    onSetMadhab: (String) -> Unit,
    onTogglePrePrayerReminder: (Boolean) -> Unit,
    onCompleteSetup: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val maxSteps = 3
    val initialFocusRequester = remember { FocusRequester() }
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onCompleteSetup()
    }

    val handleCompleteSetup: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onCompleteSetup()
            }
        } else {
            onCompleteSetup()
        }
    }

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
                        text = stringResource(R.string.app_name),
                        style = TvTypography.HeaderTitle.copy(fontSize = 36.sp, color = colorScheme.onBackground)
                    )
                    Text(
                        text = stringResource(R.string.wizard_step_indicator, currentStep, maxSteps),
                        style = TvTypography.Subtitle.copy(fontSize = 18.sp, color = colorScheme.onBackground.copy(alpha = 0.7f))
                    )
                }

                Surface(
                    onClick = handleCompleteSetup,
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
                        // Step 1: Language & Country -> City Location
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (AppLanguage.isMultiLanguageShipped) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_section_language),
                                        style = TvTypography.BodyLabel.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorScheme.secondary
                                        )
                                    )
                                    for (lang in AppLanguage.ShippedLanguages) {
                                        val isSelected = settings.appLanguage == lang ||
                                            (settings.appLanguage == AppLanguage.SYSTEM && lang == AppLanguage.EN)
                                        val langShape = RoundedCornerShape(10.dp)
                                        Card(
                                            onClick = { onSetAppLanguage(lang) },
                                            colors = CardDefaults.colors(
                                                containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface,
                                                focusedContainerColor = colorScheme.surfaceVariant
                                            ),
                                            shape = CardDefaults.shape(langShape),
                                            scale = tvCardScale(1.02f),
                                            border = tvSelectableCardBorder(selected = isSelected, shape = langShape)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Language,
                                                    contentDescription = null,
                                                    tint = if (isSelected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = stringResource(lang.labelRes()),
                                                    style = TvTypography.CardTitle.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Text(
                                text = stringResource(R.string.wizard_step_location_title, 1, settings.cityName),
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = colorScheme.secondary)
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
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = colorScheme.secondary)
                            )
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                methods.chunked(2).forEachIndexed { rowIdx, rowMethods ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowMethods.forEachIndexed { colIdx, (key, label) ->
                                            val idx = rowIdx * 2 + colIdx
                                            val selected = settings.calculationMethodName.equals(key, ignoreCase = true)
                                            val shape = RoundedCornerShape(12.dp)
                                            Card(
                                                onClick = { onSetCalculationMethod(key) },
                                                colors = CardDefaults.colors(
                                                    containerColor = if (selected) colorScheme.surfaceVariant else colorScheme.surface
                                                ),
                                                shape = CardDefaults.shape(shape),
                                                scale = tvCardScale(1.02f),
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
                                style = TvTypography.Subtitle.copy(fontSize = 18.sp, color = colorScheme.secondary)
                            )
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
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
                                        scale = tvCardScale(1.02f),
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
                        // Step 3: 15-Minute Pre-Prayer Reminders
                        val shape = RoundedCornerShape(14.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(R.string.settings_section_home_reminders),
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = colorScheme.secondary)
                            )
                            Card(
                                onClick = { onTogglePrePrayerReminder(!settings.prePrayerReminderEnabled) },
                                colors = CardDefaults.colors(
                                    containerColor = if (settings.prePrayerReminderEnabled) colorScheme.surfaceVariant else colorScheme.surface
                                ),
                                shape = CardDefaults.shape(shape),
                                scale = tvCardScale(1.0f),
                                border = tvSelectableCardBorder(selected = settings.prePrayerReminderEnabled, shape = shape),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(initialFocusRequester)
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = if (settings.prePrayerReminderEnabled) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = if (settings.prePrayerReminderEnabled) stringResource(R.string.settings_reminder_toggle_on) else stringResource(R.string.settings_reminder_toggle_off),
                                            style = TvTypography.CardTitle.copy(fontSize = 18.sp, color = colorScheme.onSurface)
                                        )
                                        Text(
                                            text = stringResource(R.string.settings_reminder_toggle_desc),
                                            style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.75f))
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
                        if (currentStep < maxSteps) currentStep += 1 else handleCompleteSetup()
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
