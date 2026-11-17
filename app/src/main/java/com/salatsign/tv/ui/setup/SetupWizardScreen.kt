// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.setup

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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mosque
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
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
import com.salatsign.tv.R
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.PopularCityPresets
import com.salatsign.tv.ui.common.MosqueEmblemIcon
import com.salatsign.tv.ui.common.PinDialog
import com.salatsign.tv.ui.common.PinDialogMode
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.tvFocusable
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.TvTypography

@Composable
fun SetupWizardScreen(
    settings: AppSettings,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onSelectMode: (Boolean) -> Unit,
    onSelectCity: (CityPreset) -> Unit,
    onSelectMethod: (String) -> Unit = {},
    onSelectMadhab: (String) -> Unit = {},
    onSelectMosqueName: (String) -> Unit,
    onSelectMosqueIcon: (MosqueIconPreset) -> Unit,
    onSetAdminPin: (String) -> Unit = {},
    onCompleteSetup: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var showPinDialog by remember { mutableStateOf(false) }
    var customPinSetSuccess by remember { mutableStateOf(settings.hasCustomPin) }
    val initialFocusRequester = remember { FocusRequester() }
    val colorScheme = MaterialTheme.colorScheme

    val maxSteps = if (settings.isMosqueMode) 6 else 5
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onCompleteSetup()
    }

    val handleCompleteSetup: () -> Unit = {
        if (!settings.isMosqueMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
            // Header with Steps Indicator & Skip Button
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

                // Quick Skip Button
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

            // Step Content Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (currentStep) {
                    1 -> {
                        // Step 1: Language Chooser
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(R.string.wizard_step_lang_title),
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = AccentGold)
                            )
                            Text(
                                text = stringResource(R.string.wizard_step_lang_desc),
                                style = TvTypography.BodyLabel.copy(fontSize = 15.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                for ((idx, lang) in AppLanguage.entries.withIndex()) {
                                    val isSelected = (settings.appLanguage == lang)
                                    Card(
                                        onClick = { onSelectLanguage(lang) },
                                        colors = CardDefaults.colors(
                                            containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface
                                        ),
                                        shape = CardDefaults.shape(RoundedCornerShape(14.dp)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .tvFocusable()
                                            .then(if (idx == 0) Modifier.focusRequester(initialFocusRequester) else Modifier)
                                            .then(
                                                if (isSelected) Modifier.border(2.5.dp, AccentEmerald, RoundedCornerShape(14.dp))
                                                else Modifier
                                            )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = null,
                                                tint = if (isSelected) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.6f),
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = stringResource(lang.labelRes()),
                                                style = TvTypography.CardTitle.copy(fontSize = 18.sp, color = colorScheme.onSurface)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Step 2: City & Timezone Selection
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(R.string.wizard_step_location_title, 2, settings.cityName),
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = AccentGold)
                            )

                            val scrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for ((index, city) in PopularCityPresets.withIndex()) {
                                    val isSelected = (settings.cityName.startsWith(city.name.split(",")[0]))
                                    Card(
                                        onClick = { onSelectCity(city) },
                                        colors = CardDefaults.colors(
                                            containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface
                                        ),
                                        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                                        modifier = Modifier
                                            .tvFocusable()
                                            .then(
                                                if (index == 0) Modifier.focusRequester(initialFocusRequester) else Modifier
                                            )
                                            .then(
                                                if (isSelected) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(12.dp))
                                                else Modifier
                                            )
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                                            Text(
                                                text = city.name,
                                                style = TvTypography.CardTitle.copy(fontSize = 18.sp, color = colorScheme.onSurface)
                                            )
                                            Text(
                                                text = stringResource(R.string.settings_current_zone, city.zoneId ?: "Auto"),
                                                style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Step 3: Method & Madhab
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(R.string.wizard_step_method_title, 3),
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = AccentGold)
                            )

                            Text(
                                text = stringResource(R.string.settings_section_calc_method),
                                style = TvTypography.BodyLabel.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                            )

                            val methodScroll = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(methodScroll),
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
                                for ((key, label) in methods) {
                                    val isSelected = settings.calculationMethodName.equals(key, ignoreCase = true)
                                    Surface(
                                        onClick = { onSelectMethod(key) },
                                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                        colors = ClickableSurfaceDefaults.colors(
                                            containerColor = if (isSelected) AccentEmerald else colorScheme.surface,
                                            focusedContainerColor = AccentGold
                                        ),
                                        modifier = Modifier.tvFocusable()
                                    ) {
                                        Text(
                                            text = label,
                                            style = TvTypography.ButtonText.copy(
                                                fontSize = 14.sp,
                                                color = if (isSelected) Color.White else colorScheme.onSurface
                                            ),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = stringResource(R.string.settings_section_madhab),
                                style = TvTypography.BodyLabel.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val isHanafi = settings.madhab.equals("HANAFI", ignoreCase = true)
                                Card(
                                    onClick = { onSelectMadhab("SHAFI") },
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
                                            style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                        )
                                    }
                                }

                                Card(
                                    onClick = { onSelectMadhab("HANAFI") },
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
                                            style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // Step 4: Mode Selection
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(R.string.wizard_step_mode_title, 4),
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = AccentGold)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                // Home Mode
                                Card(
                                    onClick = { onSelectMode(false) },
                                    colors = CardDefaults.colors(
                                        containerColor = if (!settings.isMosqueMode) colorScheme.surfaceVariant else colorScheme.surface
                                    ),
                                    shape = CardDefaults.shape(RoundedCornerShape(16.dp)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(initialFocusRequester)
                                        .tvFocusable()
                                        .then(
                                            if (!settings.isMosqueMode) Modifier.border(2.5.dp, AccentEmerald, RoundedCornerShape(16.dp))
                                            else Modifier
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(24.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = if (!settings.isMosqueMode) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = stringResource(R.string.wizard_mode_home_title),
                                            style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = colorScheme.onSurface)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = stringResource(R.string.wizard_mode_home_desc),
                                            style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                        )
                                    }
                                }

                                // Mosque Mode
                                Card(
                                    onClick = { onSelectMode(true) },
                                    colors = CardDefaults.colors(
                                        containerColor = if (settings.isMosqueMode) colorScheme.surfaceVariant else colorScheme.surface
                                    ),
                                    shape = CardDefaults.shape(RoundedCornerShape(16.dp)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .tvFocusable()
                                        .then(
                                            if (settings.isMosqueMode) Modifier.border(2.5.dp, AccentEmerald, RoundedCornerShape(16.dp))
                                            else Modifier
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(24.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Mosque,
                                            contentDescription = null,
                                            tint = if (settings.isMosqueMode) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = stringResource(R.string.wizard_mode_mosque_title),
                                            style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = colorScheme.onSurface)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = stringResource(R.string.wizard_mode_mosque_desc),
                                            style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    5 -> {
                        // Step 5: Admin PIN
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(R.string.wizard_step_pin_title, 5),
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = AccentGold)
                            )

                            Text(
                                text = stringResource(R.string.settings_pin_warning_desc),
                                style = TvTypography.BodyLabel.copy(fontSize = 15.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Default PIN Card
                                Card(
                                    onClick = {
                                        customPinSetSuccess = false
                                    },
                                    colors = CardDefaults.colors(
                                        containerColor = if (!customPinSetSuccess) colorScheme.surfaceVariant else colorScheme.surface
                                    ),
                                    shape = CardDefaults.shape(RoundedCornerShape(14.dp)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(initialFocusRequester)
                                        .tvFocusable()
                                        .then(
                                            if (!customPinSetSuccess) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(14.dp))
                                            else Modifier
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = if (!customPinSetSuccess) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.5f),
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.wizard_pin_default_title),
                                                style = TvTypography.CardTitle.copy(fontSize = 18.sp, color = colorScheme.onSurface)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.wizard_pin_default_desc),
                                            style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                        )
                                    }
                                }

                                // Set Custom PIN Card
                                Card(
                                    onClick = {
                                        showPinDialog = true
                                    },
                                    colors = CardDefaults.colors(
                                        containerColor = if (customPinSetSuccess) colorScheme.surfaceVariant else colorScheme.surface
                                    ),
                                    shape = CardDefaults.shape(RoundedCornerShape(14.dp)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .tvFocusable()
                                        .then(
                                            if (customPinSetSuccess) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(14.dp))
                                            else Modifier
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Key,
                                                contentDescription = null,
                                                tint = if (customPinSetSuccess) AccentEmerald else AccentGold,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (customPinSetSuccess) stringResource(R.string.wizard_pin_custom_active) else stringResource(R.string.wizard_pin_custom_title),
                                                style = TvTypography.CardTitle.copy(fontSize = 18.sp, color = colorScheme.onSurface)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.wizard_pin_custom_desc),
                                            style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    6 -> {
                        // Step 6: Mosque Name & Icon (Only if Mosque Mode)
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(R.string.wizard_step_mosque_title, 6),
                                style = TvTypography.CardTitle.copy(fontSize = 22.sp, color = AccentGold)
                            )

                            Text(
                                text = stringResource(R.string.settings_section_mosque_symbol),
                                style = TvTypography.BodyLabel.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for ((idx, iconPreset) in MosqueIconPreset.entries.withIndex()) {
                                    val isSelected = (settings.mosqueIconPreset == iconPreset)
                                    Card(
                                        onClick = { onSelectMosqueIcon(iconPreset) },
                                        colors = CardDefaults.colors(
                                            containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface
                                        ),
                                        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .tvFocusable()
                                            .then(
                                                if (idx == 0) Modifier.focusRequester(initialFocusRequester) else Modifier
                                            )
                                            .then(
                                                if (isSelected) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(12.dp))
                                                else Modifier
                                            )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            MosqueEmblemIcon(preset = iconPreset, customLogoUri = settings.mosqueLogoUri, size = 44.dp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = stringResource(iconPreset.labelRes()),
                                                style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface)
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = stringResource(R.string.settings_section_mosque_identity),
                                style = TvTypography.BodyLabel.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                            )

                            val namePresets = stringArrayResource(R.array.mosque_name_presets)
                            val nameScroll = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(nameScroll),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (name in namePresets) {
                                    val isSelected = (settings.mosqueName == name)
                                    Surface(
                                        onClick = { onSelectMosqueName(name) },
                                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                        colors = ClickableSurfaceDefaults.colors(
                                            containerColor = if (isSelected) AccentEmerald else colorScheme.surface,
                                            focusedContainerColor = AccentGold
                                        ),
                                        modifier = Modifier.tvFocusable()
                                    ) {
                                        Text(
                                            text = name,
                                            style = TvTypography.ButtonText.copy(
                                                fontSize = 15.sp,
                                                color = if (isSelected) Color.White else colorScheme.onSurface
                                            ),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    Surface(
                        onClick = { currentStep-- },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                        colors = ClickableSurfaceDefaults.colors(containerColor = colorScheme.surface),
                        modifier = Modifier.tvFocusable()
                    ) {
                        Text(
                            text = stringResource(R.string.btn_back),
                            style = TvTypography.ButtonText.copy(color = colorScheme.onSurface),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Surface(
                    onClick = {
                        if (currentStep < maxSteps) {
                            currentStep++
                        } else {
                            handleCompleteSetup()
                        }
                    },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = AccentEmerald,
                        focusedContainerColor = AccentGold
                    ),
                    modifier = Modifier.tvFocusable()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isFinalStep = (currentStep == maxSteps)

                        Text(
                            text = if (isFinalStep) stringResource(R.string.btn_finish) else stringResource(R.string.btn_next),
                            style = TvTypography.ButtonText.copy(color = Color.White)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isFinalStep) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (showPinDialog) {
            PinDialog(
                mode = PinDialogMode.SET_NEW,
                settings = settings,
                onPinChanged = { pin ->
                    onSetAdminPin(pin)
                    customPinSetSuccess = true
                    showPinDialog = false
                },
                onDismiss = { showPinDialog = false }
            )
        }
    }
}
