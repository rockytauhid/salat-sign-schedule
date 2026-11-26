// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Usb
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.salatschedule.tv.screensaver.ScreensaverPreviewActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.salatschedule.tv.BuildConfig
import com.salatschedule.tv.R as AppR
import com.salatschedule.tv.service.AdhanPreviewPlayer
import com.salatschedule.tv.service.AdhanPreviewState
import com.salatsign.core.tv.R
import com.salatsign.tv.data.importing.ImportKind
import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.ui.common.IslamicEventTogglesSection
import com.salatsign.tv.ui.common.displayLabel
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.common.tvCardScale
import com.salatsign.tv.ui.common.tvSelectableCardBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceColors
import com.salatsign.tv.ui.common.tvSurfaceScale
import com.salatsign.tv.ui.picker.StorageFilePickerScreen
import com.salatsign.tv.ui.settings.SettingsAboutCard
import com.salatsign.tv.ui.settings.SettingsDisplayAndLanguagePane
import com.salatsign.tv.ui.settings.SettingsHubCategory
import com.salatsign.tv.ui.settings.SettingsLocationAndCalculationPane
import com.salatsign.tv.ui.settings.SettingsPrayerOffsetsAndHijriPane
import com.salatsign.tv.ui.settings.TvSettingsHubScaffold
import com.salatsign.tv.ui.theme.TvTypography
import kotlinx.coroutines.delay

private object HomeSettingsCategoryIds {
    const val DISPLAY = "display"
    const val LOCATION = "location"
    const val PRAYER_TIMES = "prayer_times"
    const val ADHAN = "adhan"
    const val CALENDAR = "calendar"
    const val SCREENSAVER = "screensaver"
    const val ABOUT = "about"
}

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
    onSetShowStrikeHighlight: (Boolean) -> Unit = {},
    onSetShowNextEventOnHome: (Boolean) -> Unit = {},
    onToggleIslamicEvent: (String) -> Unit = {},
    onSetScreensaverScheduleEnabled: (Boolean) -> Unit = {},
    onSetScreensaverCalendarEnabled: (Boolean) -> Unit = {},
    onSetScreensaverQuranEnabled: (Boolean) -> Unit = {},
    onSetScreensaverScheduleDuration: (Int) -> Unit = {},
    onSetScreensaverCalendarDuration: (Int) -> Unit = {},
    onRestartSetupWizard: () -> Unit,
    onBack: () -> Unit
) {
    val onClose = onBack
    val context = LocalContext.current

    var selectedCategoryId by remember { mutableStateOf(HomeSettingsCategoryIds.DISPLAY) }
    var activePickerKind by remember { mutableStateOf<ImportKind?>(null) }
    var pickerTargetPrayer by remember { mutableStateOf<Prayer?>(null) }
    var showScreensaverHelp by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val recordInteraction = { lastInteractionTime = System.currentTimeMillis() }

    val adhanPreviewPlayer = remember(context) { AdhanPreviewPlayer(context) }
    val adhanPreviewState by adhanPreviewPlayer.state.collectAsState()
    val adhanPreviewError by adhanPreviewPlayer.errorMessage.collectAsState()
    var selectedTestPrayer by remember { mutableStateOf(Prayer.SUBUH) }

    DisposableEffect(Unit) {
        onDispose { adhanPreviewPlayer.stop() }
    }

    val isPickerOpen = activePickerKind != null
    LaunchedEffect(lastInteractionTime, isPickerOpen, adhanPreviewState) {
        if (adhanPreviewState == AdhanPreviewState.PLAYING) return@LaunchedEffect
        delay(if (isPickerOpen) 300_000L else 120_000L)
        onClose()
    }

    val themeLabel = stringResource(settings.themeMode.labelRes())
    val clockShort = if (settings.is24HourFormat) "24h" else "12h"
    val cityShort = settings.cityName.substringBefore(',').trim()
    val audiblePrayers = listOf(Prayer.SUBUH, Prayer.ZUHUR, Prayer.ASHR, Prayer.MAGRIB, Prayer.ISYA)
        .count { settings.adhanFor(it).type != AdhanAudioType.SILENT }

    val categories = listOf(
        SettingsHubCategory(
            id = HomeSettingsCategoryIds.DISPLAY,
            title = stringResource(R.string.settings_hub_cat_display),
            subtitle = "$themeLabel • $clockShort",
            icon = Icons.Default.DisplaySettings
        ),
        SettingsHubCategory(
            id = HomeSettingsCategoryIds.LOCATION,
            title = stringResource(R.string.settings_hub_cat_location),
            subtitle = cityShort,
            icon = Icons.Default.LocationOn
        ),
        SettingsHubCategory(
            id = HomeSettingsCategoryIds.PRAYER_TIMES,
            title = stringResource(R.string.settings_hub_cat_prayer_times),
            subtitle = "Hijri ${if (settings.hijriOffsetDays >= 0) "+${settings.hijriOffsetDays}" else settings.hijriOffsetDays}",
            icon = Icons.Default.AccessTime
        ),
        SettingsHubCategory(
            id = HomeSettingsCategoryIds.ADHAN,
            title = stringResource(R.string.settings_hub_cat_adhan),
            subtitle = "$audiblePrayers/5 audible • ${if (settings.prePrayerReminderEnabled) "15m On" else "15m Off"}",
            icon = Icons.AutoMirrored.Filled.VolumeUp
        ),
        SettingsHubCategory(
            id = HomeSettingsCategoryIds.CALENDAR,
            title = stringResource(R.string.settings_hub_cat_calendar),
            subtitle = "${settings.enabledIslamicEventIds.size} events",
            icon = Icons.Default.CalendarMonth
        ),
        SettingsHubCategory(
            id = HomeSettingsCategoryIds.SCREENSAVER,
            title = stringResource(AppR.string.settings_hub_cat_screensaver),
            subtitle = "${listOf(settings.screensaverScheduleEnabled, settings.screensaverCalendarEnabled, settings.screensaverQuranEnabled).count { it }}/3 slides",
            icon = Icons.Default.Slideshow
        ),
        SettingsHubCategory(
            id = HomeSettingsCategoryIds.ABOUT,
            title = stringResource(R.string.settings_hub_cat_about),
            subtitle = "v${BuildConfig.VERSION_NAME}",
            icon = Icons.Default.Info
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        TvSettingsHubScaffold(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onSelectCategory = { selectedCategoryId = it },
            onRestartSetupWizard = onRestartSetupWizard,
            onClose = onClose,
            onUserInteraction = recordInteraction
        ) { activeCategory ->
            when (activeCategory.id) {
                HomeSettingsCategoryIds.DISPLAY -> {
                    SettingsDisplayAndLanguagePane(
                        settings = settings,
                        onSetAppLanguage = onSetAppLanguage,
                        onSet24HourFormat = onSet24HourFormat,
                        onSetShowArabicPrayerNames = onSetShowArabicPrayerNames,
                        onSetShowStrikeHighlight = onSetShowStrikeHighlight,
                        onSetThemeMode = onSetThemeMode
                    )
                }

                HomeSettingsCategoryIds.LOCATION -> {
                    SettingsLocationAndCalculationPane(
                        settings = settings,
                        onSelectCity = onSelectCity,
                        onSetCalculationMethod = onSetCalculationMethod,
                        onSetMadhab = onSetMadhab,
                        onSetHighLatitudeRule = onSetHighLatitudeRule
                    )
                }

                HomeSettingsCategoryIds.PRAYER_TIMES -> {
                    SettingsPrayerOffsetsAndHijriPane(
                        settings = settings,
                        onSetPrayerOffset = onSetPrayerOffset,
                        onSetHijriOffset = onSetHijriOffset,
                        onSetHijriChangeAtMidnight = onSetHijriChangeAtMidnight
                    )
                }

                HomeSettingsCategoryIds.ADHAN -> {
                    HomeAdhanAndRemindersPane(
                        settings = settings,
                        adhanPreviewPlayer = adhanPreviewPlayer,
                        adhanPreviewState = adhanPreviewState,
                        adhanPreviewError = adhanPreviewError,
                        selectedTestPrayer = selectedTestPrayer,
                        onSelectTestPrayer = { selectedTestPrayer = it },
                        onToggleReminder = onTogglePrePrayerReminder,
                        onSetAdhanAudio = onSetAdhanAudio,
                        onOpenAudioPicker = { prayer ->
                            pickerTargetPrayer = prayer
                            activePickerKind = ImportKind.CUSTOM_ADHAN
                        },
                        onUserInteraction = recordInteraction
                    )
                }

                HomeSettingsCategoryIds.CALENDAR -> {
                    HomeIslamicCalendarPane(
                        settings = settings,
                        onSetShowNextEventOnHome = onSetShowNextEventOnHome,
                        onToggleIslamicEvent = onToggleIslamicEvent
                    )
                }

                HomeSettingsCategoryIds.SCREENSAVER -> {
                    HomeScreensaverPane(
                        settings = settings,
                        onSetScheduleEnabled = onSetScreensaverScheduleEnabled,
                        onSetCalendarEnabled = onSetScreensaverCalendarEnabled,
                        onSetQuranEnabled = onSetScreensaverQuranEnabled,
                        onSetScheduleDuration = onSetScreensaverScheduleDuration,
                        onSetCalendarDuration = onSetScreensaverCalendarDuration,
                        onOpenPreview = {
                            recordInteraction()
                            context.startActivity(Intent(context, ScreensaverPreviewActivity::class.java))
                        },
                        onOpenHelp = {
                            recordInteraction()
                            showScreensaverHelp = true
                        }
                    )
                }

                HomeSettingsCategoryIds.ABOUT -> {
                    val colorScheme = MaterialTheme.colorScheme
                    SettingsAboutCard(
                        versionName = BuildConfig.VERSION_NAME,
                        versionCode = BuildConfig.VERSION_CODE,
                        extraCredits = {
                            Text(
                                text = stringResource(R.string.about_quran_tanzil),
                                style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.72f))
                            )
                            Text(
                                text = stringResource(R.string.about_quran_en),
                                style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.72f))
                            )
                            Text(
                                text = stringResource(R.string.about_quran_id),
                                style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.72f))
                            )
                        }
                    )
                }
            }
        }

        if (showScreensaverHelp) {
            ScreensaverHelpDialog(onDismiss = { showScreensaverHelp = false })
        }

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
                    recordInteraction()
                    if (activePickerKind == ImportKind.CUSTOM_ADHAN) {
                        val prayer = pickerTargetPrayer
                        if (prayer != null) {
                            onSetCustomAdhan(prayer, fileName, "Adhan (${fileName.take(15)})")
                        }
                    }
                    activePickerKind = null
                    pickerTargetPrayer = null
                },
                onDismiss = {
                    recordInteraction()
                    activePickerKind = null
                    pickerTargetPrayer = null
                },
                onUserInteraction = recordInteraction
            )
        }
    }
}

@Composable
private fun HomeAdhanAndRemindersPane(
    settings: AppSettings,
    adhanPreviewPlayer: AdhanPreviewPlayer,
    adhanPreviewState: AdhanPreviewState,
    adhanPreviewError: String?,
    selectedTestPrayer: Prayer,
    onSelectTestPrayer: (Prayer) -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onSetAdhanAudio: (Prayer, AdhanAudioOption) -> Unit,
    onOpenAudioPicker: (Prayer) -> Unit,
    onUserInteraction: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val prayers = listOf(Prayer.SUBUH, Prayer.ZUHUR, Prayer.ASHR, Prayer.MAGRIB, Prayer.ISYA)

    Text(
        text = stringResource(R.string.settings_section_home_reminders),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    val reminderShape = RoundedCornerShape(10.dp)
    Surface(
        onClick = { onToggleReminder(!settings.prePrayerReminderEnabled) },
        shape = ClickableSurfaceDefaults.shape(reminderShape),
        scale = tvSurfaceScale(1.0f),
        colors = tvSelectableSurfaceColors(selected = settings.prePrayerReminderEnabled),
        border = tvSelectableSurfaceBorder(selected = settings.prePrayerReminderEnabled, shape = reminderShape),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = if (settings.prePrayerReminderEnabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (settings.prePrayerReminderEnabled) stringResource(R.string.settings_reminder_toggle_on) else stringResource(R.string.settings_reminder_toggle_off),
                    style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                )
                Text(
                    text = stringResource(R.string.settings_reminder_toggle_desc),
                    style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.72f))
                )
            }
        }
    }

    Text(
        text = stringResource(R.string.settings_section_home_adhan),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val adhanShape = RoundedCornerShape(10.dp)
        for (prayer in prayers) {
            val currentOption = settings.adhanFor(prayer)
            val isAudible = currentOption.type != AdhanAudioType.SILENT
            Card(
                onClick = {
                    val hasCustom = !currentOption.customUri.isNullOrBlank()
                    val nextOption = when (currentOption.type) {
                        AdhanAudioType.SILENT -> currentOption.copy(type = AdhanAudioType.BEEP)
                        AdhanAudioType.BEEP,
                        AdhanAudioType.DEFAULT_SUBUH,
                        AdhanAudioType.DEFAULT_STANDARD -> {
                            if (hasCustom) {
                                currentOption.copy(type = AdhanAudioType.CUSTOM_FILE)
                            } else {
                                currentOption.copy(type = AdhanAudioType.SILENT)
                            }
                        }
                        AdhanAudioType.CUSTOM_FILE -> currentOption.copy(type = AdhanAudioType.SILENT)
                    }
                    onSetAdhanAudio(prayer, nextOption)
                },
                colors = CardDefaults.colors(
                    containerColor = if (isAudible) colorScheme.surfaceVariant else colorScheme.surface,
                    focusedContainerColor = colorScheme.surfaceVariant
                ),
                shape = CardDefaults.shape(adhanShape),
                scale = tvCardScale(1.02f),
                border = tvSelectableCardBorder(selected = isAudible, shape = adhanShape),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(prayer.labelRes()),
                        style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentOption.displayLabel(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentOption.type == AdhanAudioType.SILENT) Color(0xFFDC2626) else colorScheme.primary
                        )
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (prayer in prayers) {
            val usbShape = RoundedCornerShape(6.dp)
            Surface(
                onClick = { onOpenAudioPicker(prayer) },
                shape = ClickableSurfaceDefaults.shape(usbShape),
                scale = tvSurfaceScale(1.02f),
                colors = tvSelectableSurfaceColors(containerColor = colorScheme.surfaceVariant),
                border = tvSelectableSurfaceBorder(shape = usbShape),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Usb,
                        contentDescription = null,
                        tint = colorScheme.secondary,
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

    // Test Adhan Audio Bar
    val testOption = settings.adhanFor(selectedTestPrayer)
    val isTestSilent = testOption.type == AdhanAudioType.SILENT
    val isPlayingPreview = adhanPreviewState == AdhanPreviewState.PLAYING
    val isPausedPreview = adhanPreviewState == AdhanPreviewState.PAUSED

    LaunchedEffect(selectedTestPrayer, testOption) {
        if (isTestSilent && adhanPreviewState != AdhanPreviewState.STOPPED) {
            adhanPreviewPlayer.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(AppR.string.settings_adhan_test_label),
                            style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                        )
                    }

                    val statusText = when {
                        adhanPreviewError != null -> adhanPreviewError
                        isPlayingPreview -> stringResource(AppR.string.settings_adhan_test_playing, testOption.displayLabel())
                        isPausedPreview -> stringResource(AppR.string.settings_adhan_test_paused, testOption.displayLabel())
                        isTestSilent -> stringResource(AppR.string.settings_adhan_test_silent_hint)
                        else -> "${stringResource(selectedTestPrayer.labelRes())} • ${testOption.displayLabel()}"
                    }
                    Text(
                        text = statusText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isPlayingPreview) FontWeight.SemiBold else FontWeight.Normal,
                            color = when {
                                adhanPreviewError != null -> colorScheme.error
                                isPlayingPreview -> colorScheme.primary
                                isPausedPreview -> colorScheme.secondary
                                else -> colorScheme.onSurface.copy(alpha = 0.72f)
                            }
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val pillShape = RoundedCornerShape(8.dp)
                    for (p in prayers) {
                        val isSelected = selectedTestPrayer == p
                        Surface(
                            onClick = {
                                onUserInteraction()
                                if (selectedTestPrayer != p) {
                                    adhanPreviewPlayer.stop()
                                    onSelectTestPrayer(p)
                                }
                            },
                            shape = ClickableSurfaceDefaults.shape(pillShape),
                            scale = tvSurfaceScale(1.03f),
                            colors = tvSelectableSurfaceColors(selected = isSelected),
                            border = tvSelectableSurfaceBorder(selected = isSelected, shape = pillShape)
                        ) {
                            Text(
                                text = stringResource(p.labelRes()),
                                style = TvTypography.BodyLabel.copy(
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val ctrlShape = RoundedCornerShape(8.dp)
                    Surface(
                        onClick = {
                            onUserInteraction()
                            adhanPreviewPlayer.togglePlayPause(testOption)
                        },
                        enabled = !isTestSilent,
                        shape = ClickableSurfaceDefaults.shape(ctrlShape),
                        scale = tvSurfaceScale(1.03f),
                        colors = tvSelectableSurfaceColors(selected = isPlayingPreview),
                        border = tvSelectableSurfaceBorder(selected = isPlayingPreview, shape = ctrlShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(
                                    if (isPlayingPreview) AppR.string.settings_adhan_btn_pause
                                    else AppR.string.settings_adhan_btn_play
                                ),
                                tint = if (isTestSilent) colorScheme.onSurface.copy(alpha = 0.35f) else colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(
                                    if (isPlayingPreview) AppR.string.settings_adhan_btn_pause
                                    else AppR.string.settings_adhan_btn_play
                                ),
                                style = TvTypography.ButtonText.copy(
                                    fontSize = 13.sp,
                                    color = if (isTestSilent) colorScheme.onSurface.copy(alpha = 0.35f) else colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            onUserInteraction()
                            adhanPreviewPlayer.stop()
                        },
                        shape = ClickableSurfaceDefaults.shape(ctrlShape),
                        scale = tvSurfaceScale(1.03f),
                        colors = tvSelectableSurfaceColors(selected = false),
                        border = tvSelectableSurfaceBorder(selected = false, shape = ctrlShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = stringResource(AppR.string.settings_adhan_btn_stop),
                                tint = if (adhanPreviewState != AdhanPreviewState.STOPPED) Color(0xFFEF4444) else colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(AppR.string.settings_adhan_btn_stop),
                                style = TvTypography.ButtonText.copy(
                                    fontSize = 13.sp,
                                    color = if (adhanPreviewState != AdhanPreviewState.STOPPED) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeIslamicCalendarPane(
    settings: AppSettings,
    onSetShowNextEventOnHome: (Boolean) -> Unit,
    onToggleIslamicEvent: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Text(
        text = stringResource(R.string.islamic_cal_settings_section),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    val nextHomeShape = RoundedCornerShape(12.dp)
    Card(
        onClick = { onSetShowNextEventOnHome(!settings.showNextEventOnHome) },
        colors = CardDefaults.colors(
            containerColor = if (settings.showNextEventOnHome) colorScheme.surfaceVariant else colorScheme.surface,
            focusedContainerColor = colorScheme.surfaceVariant
        ),
        shape = CardDefaults.shape(nextHomeShape),
        scale = tvCardScale(1.0f),
        border = tvSelectableCardBorder(selected = settings.showNextEventOnHome, shape = nextHomeShape),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.islamic_cal_settings_show_home),
                    style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                )
                Text(
                    text = stringResource(R.string.islamic_cal_settings_show_home_desc),
                    style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.78f))
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .background(
                        if (settings.showNextEventOnHome) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.16f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (settings.showNextEventOnHome) "On" else "Off",
                    style = TvTypography.ButtonText.copy(
                        fontSize = 13.sp,
                        color = if (settings.showNextEventOnHome) Color.White else colorScheme.onSurface
                    )
                )
            }
        }
    }

    IslamicEventTogglesSection(
        enabledIds = settings.enabledIslamicEventIds,
        hijriOffsetDays = settings.hijriOffsetDays,
        onToggleEvent = onToggleIslamicEvent
    )
}

@Composable
private fun HomeScreensaverPane(
    settings: AppSettings,
    onSetScheduleEnabled: (Boolean) -> Unit,
    onSetCalendarEnabled: (Boolean) -> Unit,
    onSetQuranEnabled: (Boolean) -> Unit,
    onSetScheduleDuration: (Int) -> Unit,
    onSetCalendarDuration: (Int) -> Unit,
    onOpenPreview: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val durations = listOf(10, 15, 20, 30, 45, 60)
    val cardShape = RoundedCornerShape(12.dp)
    val chipShape = RoundedCornerShape(8.dp)

    Text(
        text = stringResource(AppR.string.settings_screensaver_section),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )
    Text(
        text = stringResource(AppR.string.settings_screensaver_desc),
        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.78f))
    )

    // Action Row: Preview + How to Set as Screensaver
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            onClick = onOpenPreview,
            shape = ClickableSurfaceDefaults.shape(cardShape),
            scale = tvSurfaceScale(1.02f),
            colors = tvSelectableSurfaceColors(selected = true),
            border = tvSelectableSurfaceBorder(selected = true, shape = cardShape),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(AppR.string.settings_screensaver_btn_preview),
                    style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                )
            }
        }

        Surface(
            onClick = onOpenHelp,
            shape = ClickableSurfaceDefaults.shape(cardShape),
            scale = tvSurfaceScale(1.02f),
            colors = tvSelectableSurfaceColors(selected = false),
            border = tvSelectableSurfaceBorder(selected = false, shape = cardShape),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(AppR.string.settings_screensaver_btn_help),
                    style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                )
            }
        }
    }

    // Slide 1: Prayer Schedule toggle + duration
    ScreensaverSlideToggleCard(
        title = stringResource(AppR.string.settings_screensaver_slide_schedule),
        enabled = settings.screensaverScheduleEnabled,
        onToggle = { onSetScheduleEnabled(!settings.screensaverScheduleEnabled) }
    )
    if (settings.screensaverScheduleEnabled) {
        ScreensaverDurationRow(
            selectedSeconds = settings.screensaverScheduleDurationSeconds,
            durations = durations,
            chipShape = chipShape,
            onSelect = onSetScheduleDuration
        )
    }

    // Slide 2: Islamic Calendar toggle + duration
    ScreensaverSlideToggleCard(
        title = stringResource(AppR.string.settings_screensaver_slide_calendar),
        enabled = settings.screensaverCalendarEnabled,
        onToggle = { onSetCalendarEnabled(!settings.screensaverCalendarEnabled) }
    )
    if (settings.screensaverCalendarEnabled) {
        ScreensaverDurationRow(
            selectedSeconds = settings.screensaverCalendarDurationSeconds,
            durations = durations,
            chipShape = chipShape,
            onSelect = onSetCalendarDuration
        )
    }

    // Slide 3: Quran Verse toggle (Auto duration)
    ScreensaverSlideToggleCard(
        title = stringResource(AppR.string.settings_screensaver_slide_quran),
        enabled = settings.screensaverQuranEnabled,
        onToggle = { onSetQuranEnabled(!settings.screensaverQuranEnabled) }
    )

    Text(
        text = stringResource(AppR.string.settings_screensaver_min_one_note),
        style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.65f))
    )
}

@Composable
private fun ScreensaverSlideToggleCard(
    title: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Card(
        onClick = onToggle,
        colors = CardDefaults.colors(
            containerColor = if (enabled) colorScheme.surfaceVariant else colorScheme.surface,
            focusedContainerColor = colorScheme.surfaceVariant
        ),
        shape = CardDefaults.shape(shape),
        scale = tvCardScale(1.0f),
        border = tvSelectableCardBorder(selected = enabled, shape = shape),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
            )
            Box(
                modifier = Modifier
                    .background(
                        if (enabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.16f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (enabled) "On" else "Off",
                    style = TvTypography.ButtonText.copy(
                        fontSize = 12.sp,
                        color = if (enabled) Color.White else colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun ScreensaverDurationRow(
    selectedSeconds: Int,
    durations: List<Int>,
    chipShape: RoundedCornerShape,
    onSelect: (Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = stringResource(AppR.string.settings_screensaver_duration_label),
            style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.75f))
        )
        for (secs in durations) {
            val isSel = selectedSeconds == secs
            Surface(
                onClick = { onSelect(secs) },
                shape = ClickableSurfaceDefaults.shape(chipShape),
                scale = tvSurfaceScale(1.03f),
                colors = tvSelectableSurfaceColors(selected = isSel),
                border = tvSelectableSurfaceBorder(selected = isSel, shape = chipShape)
            ) {
                Text(
                    text = "${secs}s",
                    style = TvTypography.ButtonText.copy(
                        fontSize = 13.sp,
                        color = if (isSel) colorScheme.primary else colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ScreensaverHelpDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val closeFocus = remember { FocusRequester() }
    val canOpenDreamSettings = remember {
        Intent(Settings.ACTION_DREAM_SETTINGS).resolveActivity(context.packageManager) != null
    }

    LaunchedEffect(Unit) {
        runCatching { closeFocus.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(horizontal = 48.dp, vertical = 27.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .background(colorScheme.background, RoundedCornerShape(16.dp))
                    .border(2.dp, colorScheme.primary, RoundedCornerShape(16.dp))
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(AppR.string.settings_screensaver_help_title),
                    style = TvTypography.HeaderTitle.copy(fontSize = 24.sp, color = colorScheme.primary)
                )
                Text(
                    text = stringResource(AppR.string.settings_screensaver_help_android_tv),
                    style = TvTypography.BodyLabel.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                )
                Text(
                    text = stringResource(AppR.string.settings_screensaver_help_google_tv),
                    style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.78f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val btnShape = RoundedCornerShape(8.dp)
                    if (canOpenDreamSettings) {
                        Surface(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_DREAM_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            },
                            shape = ClickableSurfaceDefaults.shape(btnShape),
                            scale = tvSurfaceScale(1.03f),
                            colors = tvSelectableSurfaceColors(selected = true),
                            border = tvSelectableSurfaceBorder(selected = true, shape = btnShape)
                        ) {
                            Text(
                                text = stringResource(AppR.string.settings_screensaver_open_tv_settings),
                                style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.primary),
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Surface(
                        onClick = onDismiss,
                        shape = ClickableSurfaceDefaults.shape(btnShape),
                        scale = tvSurfaceScale(1.03f),
                        colors = tvSelectableSurfaceColors(selected = false),
                        border = tvSelectableSurfaceBorder(selected = false, shape = btnShape),
                        modifier = Modifier.focusRequester(closeFocus)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_close),
                            style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
