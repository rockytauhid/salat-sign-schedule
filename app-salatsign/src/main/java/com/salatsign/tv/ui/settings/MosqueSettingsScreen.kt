// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.VoiceOverOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.salatsign.core.tv.R
import com.salatsign.tv.BuildConfig
import com.salatsign.tv.data.importing.ImportKind
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerSilentConfig
import com.salatsign.tv.data.model.QuranVerseItem
import com.salatsign.tv.data.model.SlideTextAlign
import com.salatsign.tv.data.model.SlideType
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.engine.CopyPrayerSilentResult
import com.salatsign.tv.engine.SlideBudget
import com.salatsign.tv.engine.SlideshowRotationBuilder
import com.salatsign.tv.ui.common.PinDialog
import com.salatsign.tv.ui.common.PinDialogMode
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.mosque.BuiltInSlideRegistry
import com.salatsign.tv.ui.picker.StorageFilePickerScreen
import kotlinx.coroutines.delay

private object MosqueSettingsCategoryIds {
    const val DISPLAY = "display"
    const val LOCATION = "location"
    const val PRAYER_JUMUAH = "prayer_jumuah"
    const val BRANDING = "branding"
    const val SLIDESHOW = "slideshow"
    const val SILENT = "silent"
    const val REMOTE_SECURITY = "remote_security"
    const val ABOUT = "about"
}

@Composable
fun MosqueSettingsScreen(
    settings: AppSettings,
    slides: List<AnnouncementSlide> = emptyList(),
    onSetThemeMode: (ThemeMode) -> Unit,
    onSelectCity: (CityPreset) -> Unit,
    onSetCalculationMethod: (String) -> Unit,
    onSetMadhab: (String) -> Unit,
    onSetHighLatitudeRule: (String?) -> Unit,
    onSetPrayerOffset: (Prayer, Int) -> Unit,
    onSetSunriseOffset: (Int) -> Unit,
    onSetIqamahOffset: (Prayer, Int) -> Unit,
    onSetHijriOffset: (Int) -> Unit,
    onSetHijriChangeAtMidnight: (Boolean) -> Unit,
    onSetShowStrikeHighlight: (Boolean) -> Unit,
    onSetMosqueName: (String) -> Unit,
    onSetMosqueSubtitle: (String) -> Unit,
    onSetMosqueIconPreset: (MosqueIconPreset) -> Unit,
    onSetMosqueFontPreset: (MosqueFontPreset) -> Unit,
    isPhoneControllerRunning: Boolean = false,
    phoneControllerUrl: String? = null,
    onTogglePhoneController: (Boolean) -> Unit = {},
    onSetScheduleDuration: (Int) -> Unit,
    onAddTextSlide: (String, String, Int, Boolean, SlideTextAlign) -> Unit,
    onAddImageSlide: (String, String, Int) -> Unit,
    onUpdateSlide: (AnnouncementSlide) -> Unit = {},
    onToggleSlideEnabled: (String) -> Unit = {},
    onDeleteSlide: (String) -> Unit,
    onClearSlides: () -> Unit,
    slideLimitError: Boolean = false,
    onClearSlideLimitError: () -> Unit = {},
    onSetCustomLogo: (String) -> Unit,
    onChangeAdminPin: (String) -> Unit,
    onDismissDefaultPinPrompt: () -> Unit,
    onSetAppLanguage: (AppLanguage) -> Unit,
    onSet24HourFormat: (Boolean) -> Unit,
    onSetShowArabicPrayerNames: (Boolean) -> Unit,
    onSetJumuahMode: (JumuahMode) -> Unit,
    onSetJumuahTimes: (List<String>) -> Unit,
    onSetSilentJumuahEnabled: (Boolean) -> Unit = {},
    onSetSilentJumuahLeadMinutes: (Int) -> Unit = {},
    onSetSilentJumuahDurationMinutes: (Int) -> Unit = {},
    onSetSilentDailyPrayerConfig: (Prayer, PrayerSilentConfig) -> Unit = { _, _ -> },
    onCopySilentSettingsToAllPrayers: (PrayerSilentConfig) -> CopyPrayerSilentResult = {
        CopyPrayerSilentResult(emptyMap(), 0, emptyList())
    },
    onStartManualSilentScreen: (Int) -> Unit = {},
    onStopManualSilentScreen: () -> Unit = {},
    onSetIslamicCalendarSlideEnabled: (Boolean) -> Unit = {},
    onSetIslamicCalendarSlideDurationSeconds: (Int) -> Unit = {},
    onToggleIslamicEvent: (String) -> Unit = {},
    allQuranVerses: List<QuranVerseItem> = emptyList(),
    onSetQuranVerseEnabled: (Boolean) -> Unit = {},
    onSetQuranShowTranslation: (Boolean) -> Unit = {},
    onSetQuranVerseDurationSeconds: (Int) -> Unit = {},
    onRequestPreviewQuranVerse: () -> QuranVerseItem? = { null },
    onRestartSetupWizard: () -> Unit,
    onBack: () -> Unit
) {
    val onClose = onBack
    val onResetPin: () -> Unit = { onChangeAdminPin("0000") }

    var selectedCategoryId by remember { mutableStateOf(MosqueSettingsCategoryIds.DISPLAY) }
    var activePickerKind by remember { mutableStateOf<ImportKind?>(null) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showAddTextSlideDialog by remember { mutableStateOf(false) }
    var editingTextSlide by remember { mutableStateOf<AnnouncementSlide?>(null) }
    var pendingImageSlideFile by remember { mutableStateOf<String?>(null) }
    var editingImageSlide by remember { mutableStateOf<AnnouncementSlide?>(null) }
    var pickingReplacementForImageEdit by remember { mutableStateOf(false) }
    var addSlideCanEnable by remember { mutableStateOf(true) }

    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val recordInteraction = { lastInteractionTime = System.currentTimeMillis() }
    val isModalOpen = activePickerKind != null ||
        showAddTextSlideDialog ||
        editingTextSlide != null ||
        pendingImageSlideFile != null ||
        editingImageSlide != null ||
        showChangePinDialog

    LaunchedEffect(lastInteractionTime, isModalOpen) {
        delay(if (isModalOpen) 300_000L else 120_000L)
        onClose()
    }

    val builtIns = BuiltInSlideRegistry.registrationsFor(settings)
    val rotationPlan = SlideshowRotationBuilder.build(slides, builtIns)

    val themeLabel = stringResource(settings.themeMode.labelRes())
    val clockShort = if (settings.is24HourFormat) "24h" else "12h"
    val cityShort = settings.cityName.substringBefore(',').trim()
    val sittingsCount = settings.jumuahTimes.size.coerceIn(1, 3)
    val silentSummary = buildString {
        append(if (settings.silentScreen.jumuahEnabled) "Jumu'ah On" else "Jumu'ah Off")
        val dailyOn = settings.silentScreen.dailyPrayers.values.count { it.enabled }
        if (dailyOn > 0) append(" • $dailyOn daily")
    }

    val categories = listOf(
        SettingsHubCategory(
            id = MosqueSettingsCategoryIds.DISPLAY,
            title = stringResource(R.string.settings_hub_cat_display),
            subtitle = "$themeLabel • $clockShort",
            icon = Icons.Default.DisplaySettings
        ),
        SettingsHubCategory(
            id = MosqueSettingsCategoryIds.LOCATION,
            title = stringResource(R.string.settings_hub_cat_location),
            subtitle = cityShort,
            icon = Icons.Default.LocationOn
        ),
        SettingsHubCategory(
            id = MosqueSettingsCategoryIds.PRAYER_JUMUAH,
            title = stringResource(R.string.settings_hub_cat_prayer_jumuah),
            subtitle = "$sittingsCount Jumu'ah • Hijri ${if (settings.hijriOffsetDays >= 0) "+${settings.hijriOffsetDays}" else settings.hijriOffsetDays}",
            icon = Icons.Default.AccessTime
        ),
        SettingsHubCategory(
            id = MosqueSettingsCategoryIds.BRANDING,
            title = stringResource(R.string.settings_hub_cat_branding),
            subtitle = settings.mosqueName.ifBlank { stringResource(R.string.mosque_default_name) },
            icon = Icons.Default.Mosque
        ),
        SettingsHubCategory(
            id = MosqueSettingsCategoryIds.SLIDESHOW,
            title = stringResource(R.string.settings_hub_cat_slideshow),
            subtitle = "${rotationPlan.allocatedCount}/${SlideBudget.MAX_ROTATION_SLIDES} active • ${settings.scheduleDurationSeconds}s",
            icon = Icons.Default.Slideshow
        ),
        SettingsHubCategory(
            id = MosqueSettingsCategoryIds.SILENT,
            title = stringResource(R.string.settings_hub_cat_silent),
            subtitle = silentSummary,
            icon = Icons.Default.VoiceOverOff
        ),
        SettingsHubCategory(
            id = MosqueSettingsCategoryIds.REMOTE_SECURITY,
            title = stringResource(R.string.settings_hub_cat_remote_security),
            subtitle = if (isPhoneControllerRunning) "LAN On" else if (settings.hasCustomPin) "Custom PIN" else "Default PIN",
            icon = Icons.Default.Security,
            badgeWarning = !settings.hasCustomPin
        ),
        SettingsHubCategory(
            id = MosqueSettingsCategoryIds.ABOUT,
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
            onUserInteraction = recordInteraction,
            topBanner = if (!settings.hasCustomPin) {
                { MosqueDefaultPinWarningBanner(onOpenChangePin = { showChangePinDialog = true }) }
            } else {
                null
            }
        ) { activeCategory ->
            when (activeCategory.id) {
                MosqueSettingsCategoryIds.DISPLAY -> {
                    SettingsDisplayAndLanguagePane(
                        settings = settings,
                        onSetAppLanguage = onSetAppLanguage,
                        onSet24HourFormat = onSet24HourFormat,
                        onSetShowArabicPrayerNames = onSetShowArabicPrayerNames,
                        onSetThemeMode = onSetThemeMode,
                        extraContent = {
                            MosqueStrikeHighlightCard(
                                enabled = settings.showStrikeHighlight,
                                onToggle = onSetShowStrikeHighlight
                            )
                        }
                    )
                }

                MosqueSettingsCategoryIds.LOCATION -> {
                    SettingsLocationAndCalculationPane(
                        settings = settings,
                        onSelectCity = onSelectCity,
                        onSetCalculationMethod = onSetCalculationMethod,
                        onSetMadhab = onSetMadhab,
                        onSetHighLatitudeRule = onSetHighLatitudeRule
                    )
                }

                MosqueSettingsCategoryIds.PRAYER_JUMUAH -> {
                    SettingsPrayerOffsetsAndHijriPane(
                        settings = settings,
                        onSetPrayerOffset = onSetPrayerOffset,
                        onSetHijriOffset = onSetHijriOffset,
                        onSetHijriChangeAtMidnight = onSetHijriChangeAtMidnight
                    )
                    MosqueIqamahAndJumuahPane(
                        settings = settings,
                        onSetIqamahOffset = onSetIqamahOffset,
                        onSetJumuahMode = onSetJumuahMode,
                        onSetJumuahTimes = onSetJumuahTimes
                    )
                }

                MosqueSettingsCategoryIds.BRANDING -> {
                    MosqueBrandingPane(
                        settings = settings,
                        onSetMosqueIconPreset = onSetMosqueIconPreset,
                        onOpenLogoPicker = { activePickerKind = ImportKind.MOSQUE_LOGO },
                        onSetMosqueName = onSetMosqueName,
                        onSetMosqueFontPreset = onSetMosqueFontPreset,
                        onSetMosqueSubtitle = onSetMosqueSubtitle,
                        onUserInteraction = recordInteraction
                    )
                }

                MosqueSettingsCategoryIds.SLIDESHOW -> {
                    MosqueSlideshowPane(
                        settings = settings,
                        slides = slides,
                        slideLimitError = slideLimitError,
                        allQuranVerses = allQuranVerses,
                        onUpdateScheduleDuration = onSetScheduleDuration,
                        onAddImageSlideClick = { canEnable ->
                            onClearSlideLimitError()
                            addSlideCanEnable = canEnable
                            pickingReplacementForImageEdit = false
                            activePickerKind = ImportKind.SLIDE_IMAGE
                        },
                        onAddTextSlideClick = { canEnable ->
                            onClearSlideLimitError()
                            addSlideCanEnable = canEnable
                            showAddTextSlideDialog = true
                        },
                        onEditSlide = { slide ->
                            recordInteraction()
                            onClearSlideLimitError()
                            if (slide.type == SlideType.IMAGE) {
                                editingImageSlide = slide
                            } else {
                                editingTextSlide = slide
                            }
                        },
                        onToggleSlideEnabled = onToggleSlideEnabled,
                        onDeleteSlide = onDeleteSlide,
                        onClearSlides = onClearSlides,
                        onSetIslamicCalendarSlideEnabled = onSetIslamicCalendarSlideEnabled,
                        onSetIslamicCalendarSlideDurationSeconds = onSetIslamicCalendarSlideDurationSeconds,
                        onToggleIslamicEvent = onToggleIslamicEvent,
                        onSetQuranVerseEnabled = onSetQuranVerseEnabled,
                        onSetQuranShowTranslation = onSetQuranShowTranslation,
                        onSetQuranVerseDurationSeconds = onSetQuranVerseDurationSeconds,
                        onRequestPreviewQuranVerse = onRequestPreviewQuranVerse
                    )
                }

                MosqueSettingsCategoryIds.SILENT -> {
                    SilentScreenSettingsSection(
                        settings = settings,
                        onSetSilentJumuahEnabled = onSetSilentJumuahEnabled,
                        onSetSilentJumuahLeadMinutes = onSetSilentJumuahLeadMinutes,
                        onSetSilentJumuahDurationMinutes = onSetSilentJumuahDurationMinutes,
                        onSetSilentDailyPrayerConfig = onSetSilentDailyPrayerConfig,
                        onCopySilentSettingsToAllPrayers = onCopySilentSettingsToAllPrayers,
                        onStartManualSilentScreen = onStartManualSilentScreen,
                        onStopManualSilentScreen = onStopManualSilentScreen
                    )
                }

                MosqueSettingsCategoryIds.REMOTE_SECURITY -> {
                    MosqueRemoteSecurityPane(
                        settings = settings,
                        isPhoneControllerRunning = isPhoneControllerRunning,
                        phoneControllerUrl = phoneControllerUrl,
                        onTogglePhoneController = onTogglePhoneController,
                        onOpenChangePin = { showChangePinDialog = true },
                        onResetPin = onResetPin
                    )
                }

                MosqueSettingsCategoryIds.ABOUT -> {
                    MosqueAboutPane()
                }
            }
        }

        // Modal: TV D-pad Storage File Picker
        if (activePickerKind != null) {
            StorageFilePickerScreen(
                kind = activePickerKind!!,
                title = when (activePickerKind) {
                    ImportKind.MOSQUE_LOGO -> stringResource(R.string.storage_title_mosque_logo)
                    ImportKind.SLIDE_IMAGE -> stringResource(R.string.storage_title_slide_image)
                    else -> stringResource(R.string.storage_title_slide_image)
                },
                onFileImported = { fileName ->
                    recordInteraction()
                    when (activePickerKind) {
                        ImportKind.MOSQUE_LOGO -> onSetCustomLogo(fileName)
                        ImportKind.CUSTOM_ADHAN -> {}
                        ImportKind.SLIDE_IMAGE -> {
                            if (pickingReplacementForImageEdit && editingImageSlide != null) {
                                editingImageSlide = editingImageSlide!!.copy(imageFile = fileName)
                                pickingReplacementForImageEdit = false
                            } else {
                                pendingImageSlideFile = fileName
                            }
                        }
                        null -> {}
                    }
                    activePickerKind = null
                },
                onDismiss = {
                    recordInteraction()
                    pickingReplacementForImageEdit = false
                    activePickerKind = null
                },
                onUserInteraction = recordInteraction
            )
        }

        // Modal: Add / Edit Text Announcement Slide Dialog
        if (showAddTextSlideDialog || editingTextSlide != null) {
            val targetEditSlide = editingTextSlide
            AddTextSlideDialog(
                canAddEnabledSlide = addSlideCanEnable,
                existingSlide = targetEditSlide,
                onSave = { title, body, durationSeconds, enabled, textAlign ->
                    recordInteraction()
                    if (targetEditSlide != null) {
                        onUpdateSlide(
                            targetEditSlide.copy(
                                title = title,
                                body = body,
                                durationSeconds = durationSeconds,
                                textAlign = textAlign
                            )
                        )
                    } else {
                        onAddTextSlide(title, body, durationSeconds, enabled, textAlign)
                    }
                    showAddTextSlideDialog = false
                    editingTextSlide = null
                },
                onDismiss = {
                    recordInteraction()
                    showAddTextSlideDialog = false
                    editingTextSlide = null
                },
                onUserInteraction = recordInteraction
            )
        }

        // Modal: Add / Edit Image Slide Dialog
        val activeImageFile = editingImageSlide?.imageFile ?: pendingImageSlideFile
        if (activePickerKind == null && activeImageFile != null && (editingImageSlide != null || pendingImageSlideFile != null)) {
            val targetEditImage = editingImageSlide
            ImageSlideEditorDialog(
                imageFileName = activeImageFile,
                canAddEnabledSlide = addSlideCanEnable,
                existingSlide = targetEditImage,
                onRequestChangeImage = {
                    recordInteraction()
                    pickingReplacementForImageEdit = targetEditImage != null
                    activePickerKind = ImportKind.SLIDE_IMAGE
                },
                onSave = { title, imageFile, durationSeconds ->
                    recordInteraction()
                    if (targetEditImage != null) {
                        onUpdateSlide(
                            targetEditImage.copy(
                                title = title,
                                imageFile = imageFile,
                                durationSeconds = durationSeconds
                            )
                        )
                    } else {
                        onAddImageSlide(title, imageFile, durationSeconds)
                    }
                    pendingImageSlideFile = null
                    editingImageSlide = null
                    pickingReplacementForImageEdit = false
                },
                onDismiss = {
                    recordInteraction()
                    pendingImageSlideFile = null
                    editingImageSlide = null
                    pickingReplacementForImageEdit = false
                },
                onUserInteraction = recordInteraction
            )
        }

        // Modal: Change PIN Dialog
        if (showChangePinDialog) {
            PinDialog(
                mode = PinDialogMode.SET_NEW,
                settings = settings,
                onPinChanged = { newPin ->
                    onChangeAdminPin(newPin)
                    showChangePinDialog = false
                },
                onDismiss = { showChangePinDialog = false }
            )
        }
    }
}
