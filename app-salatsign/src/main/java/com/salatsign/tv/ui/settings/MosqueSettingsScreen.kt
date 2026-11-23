// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.settings

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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
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
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.salatsign.tv.BuildConfig
import com.salatsign.core.tv.R
import com.salatsign.tv.R as AppR
import com.salatsign.tv.data.importing.ImportKind
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.MAX_MOSQUE_NAME_LENGTH
import com.salatsign.tv.data.model.MAX_MOSQUE_TAGLINE_LENGTH
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.PopularCityPresets
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerSilentConfig
import com.salatsign.tv.data.model.QuranVerseItem
import com.salatsign.tv.data.model.SlideType
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.engine.CopyPrayerSilentResult
import com.salatsign.tv.engine.JumuahCalculator
import com.salatsign.tv.engine.JumuahWarningReason
import com.salatsign.tv.engine.SlideBudget
import com.salatsign.tv.engine.SlideshowRotationBuilder
import com.salatsign.tv.ui.mosque.BuiltInSlideRegistry
import com.salatsign.tv.ui.common.CountryCitySelector
import com.salatsign.tv.ui.common.IslamicEventTogglesSection
import com.salatsign.tv.ui.common.MosqueEmblemIcon
import com.salatsign.tv.ui.common.PinDialog
import com.salatsign.tv.ui.common.PinDialogMode
import com.salatsign.tv.ui.common.TvEditableTextField
import com.salatsign.tv.ui.common.getMosqueFontFamily
import com.salatsign.tv.ui.common.horizontalScrollBleed
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.tvCardScale
import com.salatsign.tv.ui.common.tvFocusable
import com.salatsign.tv.ui.common.tvSelectableCardBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceColors
import com.salatsign.tv.ui.common.tvSurfaceScale
import com.salatsign.tv.ui.picker.StorageFilePickerScreen
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.TvTypography
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
    onAddTextSlide: (String, String, Int, Boolean) -> Unit,
    onAddImageSlide: (String, String, Int) -> Unit,
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
    val onUpdateScheduleDuration = onSetScheduleDuration
    val onSetCustomPin = onChangeAdminPin
    val onResetPin: () -> Unit = { onChangeAdminPin("0000") }
    val onAddSampleSlide: () -> Unit = {}
    val initialFocusRequester = remember { FocusRequester() }
    val verticalScrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    var activePickerKind by remember { mutableStateOf<ImportKind?>(null) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showAddTextSlideDialog by remember { mutableStateOf(false) }
    var addTextSlideCanEnable by remember { mutableStateOf(true) }

    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val recordInteraction = { lastInteractionTime = System.currentTimeMillis() }
    val isModalOpen = activePickerKind != null || showAddTextSlideDialog || showChangePinDialog

    // Auto-close Settings after inactivity and return to signage/home.
    // Extended to 5 minutes (and reset on every keystroke/remote press inside dialogs) while composing or picking files.
    LaunchedEffect(lastInteractionTime, isModalOpen) {
        delay(if (isModalOpen) 300_000L else 120_000L)
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
                .verticalScroll(verticalScrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = TvTypography.HeaderTitle.copy(color = colorScheme.onBackground)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val headerBtnShape = RoundedCornerShape(10.dp)
                    Surface(
                        onClick = onRestartSetupWizard,
                        shape = ClickableSurfaceDefaults.shape(headerBtnShape),
                        scale = tvSurfaceScale(1.03f),
                        colors = tvSelectableSurfaceColors(selected = false),
                        border = tvSelectableSurfaceBorder(selected = false, shape = headerBtnShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.settings_open_wizard),
                                style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                            )
                        }
                    }

                    Surface(
                        onClick = onClose,
                        shape = ClickableSurfaceDefaults.shape(headerBtnShape),
                        scale = tvSurfaceScale(1.03f),
                        colors = tvSelectableSurfaceColors(selected = false),
                        border = tvSelectableSurfaceBorder(selected = false, shape = headerBtnShape),
                        modifier = Modifier.focusRequester(initialFocusRequester)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.btn_close),
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_close),
                                style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                            )
                        }
                    }
                }
            }

            // Security Warning Banner for Default PIN
            if (!settings.hasCustomPin) {
                val warnShape = RoundedCornerShape(12.dp)
                val warnRed = Color(0xFFDC2626)
                Card(
                    onClick = { showChangePinDialog = true },
                    colors = CardDefaults.colors(
                        containerColor = Color(0xFFEF4444).copy(alpha = 0.14f),
                        focusedContainerColor = Color(0xFFEF4444).copy(alpha = 0.24f)
                    ),
                    shape = CardDefaults.shape(warnShape),
                    scale = tvCardScale(1.0f),
                    border = CardDefaults.border(
                        border = androidx.tv.material3.Border(
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, warnRed.copy(alpha = 0.55f)),
                            shape = warnShape
                        ),
                        focusedBorder = androidx.tv.material3.Border(
                            border = androidx.compose.foundation.BorderStroke(2.5.dp, colorScheme.secondary),
                            shape = warnShape
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = warnRed,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_pin_warning_title),
                                    style = TvTypography.CardTitle.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = stringResource(R.string.settings_pin_warning_desc),
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 13.sp,
                                        color = colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(warnRed, RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.btn_change_pin),
                                style = TvTypography.ButtonText.copy(fontSize = 13.sp, color = Color.White)
                            )
                        }
                    }
                }
            }

            // Section: Display Language Selector (shown only when multiple languages ship - F3)
            if (AppLanguage.isMultiLanguageShipped) {
                Text(
                    text = stringResource(R.string.settings_section_language),
                    style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (lang in AppLanguage.entries) {
                        val isSelected = (settings.appLanguage == lang)
                        val langShape = RoundedCornerShape(10.dp)
                        Card(
                            onClick = { onSetAppLanguage(lang) },
                            colors = CardDefaults.colors(
                                containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface,
                                focusedContainerColor = colorScheme.surfaceVariant
                            ),
                            shape = CardDefaults.shape(langShape),
                            border = tvSelectableCardBorder(selected = isSelected, shape = langShape),
                            modifier = Modifier.weight(1f)
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
                val prefShape = RoundedCornerShape(10.dp)
                // 12/24 hour toggle
                Card(
                    onClick = { onSet24HourFormat(!settings.is24HourFormat) },
                    colors = CardDefaults.colors(
                        containerColor = colorScheme.surface,
                        focusedContainerColor = colorScheme.surfaceVariant
                    ),
                    shape = CardDefaults.shape(prefShape),
                    scale = tvCardScale(1.01f),
                    border = tvSelectableCardBorder(selected = false, shape = prefShape),
                    modifier = Modifier.weight(1f)
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
                    colors = CardDefaults.colors(
                        containerColor = if (settings.showArabicPrayerNames) colorScheme.surfaceVariant else colorScheme.surface,
                        focusedContainerColor = colorScheme.surfaceVariant
                    ),
                    shape = CardDefaults.shape(prefShape),
                    scale = tvCardScale(1.01f),
                    border = tvSelectableCardBorder(selected = settings.showArabicPrayerNames, shape = prefShape),
                    modifier = Modifier.weight(1f)
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
                                    text = if (settings.showArabicPrayerNames) "Active" else "Hidden",
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
                style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                        scale = tvCardScale(1.02f),
                        border = tvSelectableCardBorder(selected = isSelected, shape = themeShape),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
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
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(mode.labelRes()),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                style = TvTypography.CardTitle.copy(fontSize = 12.5.sp, color = colorScheme.onSurface)
                            )
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
                    style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
                )
            }

            CountryCitySelector(
                selectedCityName = settings.cityName,
                onSelectCity = onSelectCity,
                scrollableCityGrid = false
            )

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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E3A8A).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
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

            // 4. Madhab Selection (Shafi'i/Standard vs Hanafi)
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_madhab_standard),
                            style = TvTypography.CardTitle.copy(fontSize = 18.sp, color = colorScheme.onSurface)
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_madhab_hanafi),
                            style = TvTypography.CardTitle.copy(fontSize = 18.sp, color = colorScheme.onSurface)
                        )
                    }
                }
            }

            // 5. High-Latitude Rule (Polar / High Latitude Regions)
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
                                color = if (isSelected) AccentEmerald else colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // 6. Per-Prayer Time Offsets (-30m to +30m)
            Text(
                text = stringResource(R.string.settings_section_offsets),
                style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
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
                            val stepShape = RoundedCornerShape(6.dp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    onClick = { onSetPrayerOffset(prayer, (currentOffset - 1).coerceAtLeast(-30)) },
                                    shape = ClickableSurfaceDefaults.shape(stepShape),
                                    scale = tvSurfaceScale(1.04f),
                                    colors = tvSelectableSurfaceColors(selected = false, containerColor = colorScheme.surfaceVariant),
                                    border = tvSelectableSurfaceBorder(selected = false, shape = stepShape)
                                ) {
                                    Text("-1", style = TvTypography.ButtonText.copy(fontSize = 12.sp, color = colorScheme.onSurface), modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                                }
                                Surface(
                                    onClick = { onSetPrayerOffset(prayer, 0) },
                                    shape = ClickableSurfaceDefaults.shape(stepShape),
                                    scale = tvSurfaceScale(1.04f),
                                    colors = tvSelectableSurfaceColors(selected = false, containerColor = colorScheme.surfaceVariant),
                                    border = tvSelectableSurfaceBorder(selected = false, shape = stepShape)
                                ) {
                                    Text("0", style = TvTypography.ButtonText.copy(fontSize = 12.sp, color = colorScheme.onSurface), modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                                }
                                Surface(
                                    onClick = { onSetPrayerOffset(prayer, (currentOffset + 1).coerceAtMost(30)) },
                                    shape = ClickableSurfaceDefaults.shape(stepShape),
                                    scale = tvSurfaceScale(1.04f),
                                    colors = tvSelectableSurfaceColors(selected = false, containerColor = colorScheme.surfaceVariant),
                                    border = tvSelectableSurfaceBorder(selected = false, shape = stepShape)
                                ) {
                                    Text("+1", style = TvTypography.ButtonText.copy(fontSize = 12.sp, color = colorScheme.onSurface), modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 7. Per-Prayer Iqamah Offsets (Minutes After Adhan)
            Text(
                text = stringResource(R.string.settings_section_iqamah),
                style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val iqamahList = listOf(
                    Prayer.SUBUH to settings.iqamahSubuh,
                    Prayer.ZUHUR to settings.iqamahZuhur,
                    Prayer.ASHR to settings.iqamahAshr,
                    Prayer.MAGRIB to settings.iqamahMagrib,
                    Prayer.ISYA to settings.iqamahIsya
                )
                for ((prayer, currentIqamah) in iqamahList) {
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
                                style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                            )
                            Text(
                                text = if (currentIqamah == 0) stringResource(R.string.settings_iqamah_disabled) else stringResource(R.string.settings_iqamah_minutes, currentIqamah),
                                style = TvTypography.BodyLabel.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentIqamah > 0) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val iqShape = RoundedCornerShape(6.dp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val steps = listOf(0, 5, 10, 15)
                                for (step in steps) {
                                    val isStepSelected = (currentIqamah == step)
                                    Surface(
                                        onClick = { onSetIqamahOffset(prayer, step) },
                                        shape = ClickableSurfaceDefaults.shape(iqShape),
                                        scale = tvSurfaceScale(1.04f),
                                        colors = tvSelectableSurfaceColors(selected = isStepSelected, containerColor = colorScheme.surfaceVariant),
                                        border = tvSelectableSurfaceBorder(selected = isStepSelected, shape = iqShape)
                                    ) {
                                        Text(
                                            text = "$step",
                                            style = TvTypography.ButtonText.copy(
                                                fontSize = 11.sp,
                                                color = if (isStepSelected) AccentEmerald else colorScheme.onSurface
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. Hijriyah Date Adjustments & Rollover
            Text(
                text = stringResource(R.string.settings_section_hijri_offset),
                style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
            )

            // Day offset (-2..+2)
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
                                color = if (isSelected) AccentEmerald else colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Maghrib vs Midnight Rollover
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
                            style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.onSurface)
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
                            style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                // Visual Strike Highlight toggle (full width -> 1.0f scale so it never overflows)
                val strikeShape = RoundedCornerShape(10.dp)
                Card(
                    onClick = { onSetShowStrikeHighlight(!settings.showStrikeHighlight) },
                    colors = CardDefaults.colors(
                        containerColor = if (settings.showStrikeHighlight) colorScheme.surfaceVariant else colorScheme.surface,
                        focusedContainerColor = colorScheme.surfaceVariant
                    ),
                    shape = CardDefaults.shape(strikeShape),
                    scale = tvCardScale(1.0f),
                    border = tvSelectableCardBorder(selected = settings.showStrikeHighlight, shape = strikeShape),
                    modifier = Modifier.weight(1f)
                ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = if (settings.showStrikeHighlight) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (settings.showStrikeHighlight) stringResource(R.string.settings_mosque_strike_on) else stringResource(R.string.settings_mosque_strike_off),
                                    style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                                )
                            }
                        }
                    }
                }
                // Jumu'ah (Friday) Schedule - Mosque Mode only (D16)
                Text(
                    text = stringResource(R.string.settings_section_jumuah),
                    style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
                )
                Text(
                    text = stringResource(R.string.settings_jumuah_desc),
                    style = TvTypography.BodyLabel.copy(color = colorScheme.onSurface.copy(alpha = 0.7f))
                )

                // 1. Calculation Mode (FIXED vs AFTER_ZUHR)
                Text(
                    text = stringResource(R.string.settings_jumuah_mode_title),
                    style = TvTypography.Subtitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val modes = listOf(JumuahMode.FIXED, JumuahMode.AFTER_ZUHR)
                    for (mode in modes) {
                        val isSelected = (settings.jumuahMode == mode)
                        val modeShape = RoundedCornerShape(10.dp)
                        Card(
                            onClick = {
                                onSetJumuahMode(mode)
                                if (settings.jumuahTimes.isEmpty()) {
                                    onSetJumuahTimes(if (mode == JumuahMode.FIXED) listOf("13:00") else listOf("45"))
                                }
                            },
                            colors = CardDefaults.colors(
                                containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface,
                                focusedContainerColor = colorScheme.surfaceVariant
                            ),
                            shape = CardDefaults.shape(modeShape),
                            scale = tvCardScale(1.01f),
                            border = tvSelectableCardBorder(selected = isSelected, shape = modeShape),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = stringResource(mode.labelRes()),
                                    style = TvTypography.CardTitle.copy(
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) AccentEmerald else colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (mode == JumuahMode.FIXED) stringResource(R.string.settings_jumuah_mode_fixed_desc) else stringResource(R.string.settings_jumuah_mode_after_zuhr_desc),
                                    style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                )
                            }
                        }
                    }
                }

                // 2. Number of Sittings (1, 2, or 3)
                Text(
                    text = stringResource(R.string.settings_jumuah_sittings_count_title),
                    style = TvTypography.Subtitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val counts = listOf(1, 2, 3)
                    val currentCount = settings.jumuahTimes.size.coerceIn(1, 3)
                    for (c in counts) {
                        val isCountSelected = (currentCount == c)
                        val countLabel = when (c) {
                            1 -> stringResource(R.string.settings_jumuah_sittings_1)
                            2 -> stringResource(R.string.settings_jumuah_sittings_2)
                            else -> stringResource(R.string.settings_jumuah_sittings_3)
                        }
                        val countShape = RoundedCornerShape(8.dp)
                        Card(
                            onClick = {
                                val newTimes = when (settings.jumuahMode) {
                                    JumuahMode.FIXED -> {
                                        when (c) {
                                            1 -> listOf("13:00")
                                            2 -> listOf("12:30", "13:30")
                                            else -> listOf("12:15", "13:00", "13:45")
                                        }
                                    }
                                    JumuahMode.AFTER_ZUHR -> {
                                        when (c) {
                                            1 -> listOf("45")
                                            2 -> listOf("45", "90")
                                            else -> listOf("30", "60", "90")
                                        }
                                    }
                                }
                                onSetJumuahTimes(newTimes)
                            },
                            colors = CardDefaults.colors(
                                containerColor = if (isCountSelected) colorScheme.surfaceVariant else colorScheme.surface,
                                focusedContainerColor = colorScheme.surfaceVariant
                            ),
                            shape = CardDefaults.shape(countShape),
                            scale = tvCardScale(1.02f),
                            border = tvSelectableCardBorder(selected = isCountSelected, shape = countShape),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = countLabel,
                                    style = TvTypography.ButtonText.copy(
                                        fontSize = 14.sp,
                                        fontWeight = if (isCountSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCountSelected) AccentEmerald else colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. Sitting Time Adjusters
                val currentTimes = if (settings.jumuahTimes.isEmpty()) {
                    if (settings.jumuahMode == JumuahMode.FIXED) listOf("13:00") else listOf("45")
                } else settings.jumuahTimes

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    currentTimes.forEachIndexed { index, timeValue ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(colorScheme.surface, RoundedCornerShape(10.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_jumuah_sitting_n, index + 1),
                                    style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = AccentGold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (settings.jumuahMode == JumuahMode.FIXED) timeValue else "+$timeValue min",
                                    style = TvTypography.ActivePrayerTime.copy(fontSize = 24.sp, color = AccentEmerald)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val presetOptions = if (settings.jumuahMode == JumuahMode.FIXED) {
                                    listOf("12:15", "12:30", "12:45", "13:00", "13:15", "13:30", "13:45", "14:00")
                                } else {
                                    listOf("0", "15", "30", "45", "60", "75", "90", "120")
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val currentIndex = presetOptions.indexOf(timeValue).coerceAtLeast(0)
                                    val stepShape = RoundedCornerShape(6.dp)
                                    Surface(
                                        onClick = {
                                            if (currentIndex > 0) {
                                                val mutable = currentTimes.toMutableList()
                                                mutable[index] = presetOptions[currentIndex - 1]
                                                onSetJumuahTimes(mutable)
                                            }
                                        },
                                        shape = ClickableSurfaceDefaults.shape(stepShape),
                                        scale = tvSurfaceScale(1.04f),
                                        colors = tvSelectableSurfaceColors(selected = false),
                                        border = tvSelectableSurfaceBorder(selected = false, shape = stepShape)
                                    ) {
                                        Text("-15m", style = TvTypography.ButtonText.copy(fontSize = 13.sp, color = colorScheme.onSurface), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                    }

                                    Surface(
                                        onClick = {
                                            if (currentIndex < presetOptions.lastIndex) {
                                                val mutable = currentTimes.toMutableList()
                                                mutable[index] = presetOptions[currentIndex + 1]
                                                onSetJumuahTimes(mutable)
                                            }
                                        },
                                        shape = ClickableSurfaceDefaults.shape(stepShape),
                                        scale = tvSurfaceScale(1.04f),
                                        colors = tvSelectableSurfaceColors(selected = false),
                                        border = tvSelectableSurfaceBorder(selected = false, shape = stepShape)
                                    ) {
                                        Text("+15m", style = TvTypography.ButtonText.copy(fontSize = 13.sp, color = colorScheme.onSurface), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. 53-Friday Validation Status
                val validation = remember(settings.jumuahMode, settings.jumuahTimes, settings.latitude, settings.longitude, settings.zoneId, settings.calculationMethodName) {
                    JumuahCalculator.validateSittings(
                        startDate = LocalDate.now(),
                        config = settings.prayerConfig(),
                        jumuahMode = settings.jumuahMode,
                        jumuahTimes = settings.jumuahTimes
                    )
                }

                val firstWarn = validation.firstWarning
                if (firstWarn != null) {
                    val isInvalid = (firstWarn.reason == JumuahWarningReason.SITTING_AFTER_ASR)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isInvalid) colorScheme.error.copy(alpha = 0.2f) else AccentGold.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isInvalid) {
                                    stringResource(R.string.settings_jumuah_warning_after_asr, firstWarn.sittingTime, firstWarn.ashrTime, firstWarn.date)
                                } else {
                                    stringResource(R.string.settings_jumuah_warning_before_zuhr, firstWarn.sittingTime, firstWarn.zuhurTime, firstWarn.date)
                                },
                                style = TvTypography.BodyLabel.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isInvalid) colorScheme.error else AccentGold
                                )
                            )
                        }
                    }
                }

                // 4. Mosque Icon & Emblem Preset Selector (Tidy Square Tiles)
                Text(
                    text = stringResource(R.string.settings_section_mosque_symbol),
                    style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (preset in MosqueIconPreset.entries) {
                        val isSelected = (settings.mosqueIconPreset == preset)
                        val iconShape = RoundedCornerShape(14.dp)
                        Card(
                            onClick = {
                                onSetMosqueIconPreset(preset)
                                if (preset == MosqueIconPreset.CUSTOM) {
                                    activePickerKind = ImportKind.MOSQUE_LOGO
                                }
                            },
                            colors = CardDefaults.colors(
                                containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface,
                                focusedContainerColor = colorScheme.surfaceVariant
                            ),
                            shape = CardDefaults.shape(iconShape),
                            scale = tvCardScale(1.03f),
                            border = tvSelectableCardBorder(selected = isSelected, shape = iconShape),
                            modifier = Modifier.size(132.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                MosqueEmblemIcon(preset = preset, customLogoUri = settings.mosqueLogoUri, size = 54.dp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = stringResource(preset.labelRes()),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                // 5. Mosque Name (Custom editable input, default Masjiduna) & Font Selector
                Text(
                    text = stringResource(R.string.mosque_label_name),
                    style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
                )

                val defaultMosqueName = stringResource(R.string.mosque_default_name)
                var mosqueNameInput by remember(settings.mosqueName) {
                    mutableStateOf(settings.mosqueName.ifBlank { defaultMosqueName })
                }

                TvEditableTextField(
                    value = mosqueNameInput,
                    onValueChange = {
                        recordInteraction()
                        mosqueNameInput = it
                        onSetMosqueName(it.trim().ifBlank { defaultMosqueName })
                    },
                    placeholder = defaultMosqueName,
                    accentUnfocusedBorder = true,
                    maxLength = MAX_MOSQUE_NAME_LENGTH,
                    textStyle = TvTypography.CardTitle.copy(fontSize = 17.sp)
                )

                Text(
                    text = stringResource(R.string.settings_section_mosque_font),
                    style = TvTypography.CardTitle.copy(fontSize = 18.sp, color = colorScheme.onSurface)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (font in MosqueFontPreset.entries) {
                        val isSelected = (settings.mosqueFontPreset == font)
                        val fontShape = RoundedCornerShape(10.dp)
                        Card(
                            onClick = { onSetMosqueFontPreset(font) },
                            colors = CardDefaults.colors(
                                containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface,
                                focusedContainerColor = colorScheme.surfaceVariant
                            ),
                            shape = CardDefaults.shape(fontShape),
                            scale = tvCardScale(1.02f),
                            border = tvSelectableCardBorder(selected = isSelected, shape = fontShape),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = settings.mosqueName.ifBlank { stringResource(R.string.mosque_default_name) },
                                    fontFamily = getMosqueFontFamily(font),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    color = colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(font.labelRes()),
                                    textAlign = TextAlign.Center,
                                    style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                )
                            }
                        }
                    }
                }

                // 6. Mosque Tagline (Editable input with example hint)
                var subtitleInput by remember(settings.mosqueSubtitle) { mutableStateOf(settings.mosqueSubtitle) }

                Text(
                    text = stringResource(R.string.mosque_label_tagline),
                    style = TvTypography.CardTitle.copy(fontSize = 18.sp, color = colorScheme.onSurface)
                )

                TvEditableTextField(
                    value = subtitleInput,
                    onValueChange = {
                        recordInteraction()
                        subtitleInput = it
                        onSetMosqueSubtitle(it)
                    },
                    placeholder = stringResource(R.string.mosque_tagline_hint),
                    accentUnfocusedBorder = false,
                    maxLength = MAX_MOSQUE_TAGLINE_LENGTH,
                    textStyle = TvTypography.BodyLabel.copy(fontSize = 15.sp)
                )

                // 7. Slideshow & Signage Poster Timing
                Text(
                    text = stringResource(R.string.settings_section_schedule_duration),
                    style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val durations = listOf(30, 60, 90, 120)
                    val durShape = RoundedCornerShape(8.dp)
                    for (dur in durations) {
                        val isSelected = (settings.scheduleDurationSeconds == dur)
                        Surface(
                            onClick = { onUpdateScheduleDuration(dur) },
                            shape = ClickableSurfaceDefaults.shape(durShape),
                            scale = tvSurfaceScale(1.03f),
                            colors = tvSelectableSurfaceColors(selected = isSelected),
                            border = tvSelectableSurfaceBorder(selected = isSelected, shape = durShape)
                        ) {
                            Text(
                                text = "${dur}s",
                                style = TvTypography.ButtonText.copy(
                                    fontSize = 15.sp,
                                    color = if (isSelected) AccentEmerald else colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                val builtIns = BuiltInSlideRegistry.registrationsFor(settings)
                val rotationPlan = SlideshowRotationBuilder.build(slides, builtIns)
                val enabledBuiltInCount = rotationPlan.enabledBuiltInCount
                val canAddEnabledSlide = SlideBudget.canEnableOrAddAnnouncement(
                    enabledAnnouncements = rotationPlan.enabledAnnouncementCount,
                    enabledBuiltIns = enabledBuiltInCount
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(
                            AppR.string.settings_slides_in_rotation,
                            rotationPlan.allocatedCount,
                            SlideBudget.MAX_ROTATION_SLIDES
                        ),
                        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
                    )
                    val breakdownParts = buildList {
                        if (rotationPlan.enabledAnnouncementCount == 1) {
                            add(stringResource(AppR.string.settings_slides_breakdown_announcement_one))
                        } else if (rotationPlan.enabledAnnouncementCount > 1) {
                            add(stringResource(AppR.string.settings_slides_breakdown_announcements, rotationPlan.enabledAnnouncementCount))
                        }
                        for (bi in builtIns.filter { it.enabled }) {
                            add(
                                when (bi.id) {
                                    com.salatsign.tv.engine.BuiltInSlideId.ISLAMIC_CALENDAR ->
                                        stringResource(AppR.string.settings_slides_builtin_islamic_calendar)
                                    com.salatsign.tv.engine.BuiltInSlideId.QURAN_VERSE ->
                                        stringResource(AppR.string.settings_slides_builtin_quran_verse)
                                }
                            )
                        }
                    }
                    val breakdownText = if (breakdownParts.isEmpty()) {
                        stringResource(AppR.string.settings_slides_breakdown_none)
                    } else {
                        breakdownParts.joinToString(", ")
                    }
                    Text(
                        text = "$breakdownText  •  ${stringResource(AppR.string.settings_slides_stored_library, slides.size, SlideBudget.MAX_STORED_ANNOUNCEMENTS)}",
                        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.78f))
                    )
                }

                if (slideLimitError) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEF4444).copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFDC2626).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = stringResource(AppR.string.settings_slides_limit_refused),
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                        )
                    }
                }

                if (rotationPlan.isOverLimit) {
                    val hiddenNames = (rotationPlan.hiddenAnnouncements.map { it.title.ifBlank { "(Untitled)" } } +
                        rotationPlan.hiddenBuiltIns.map { it.displayName }).joinToString(", ")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorScheme.secondary.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            .border(1.dp, colorScheme.secondary.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = stringResource(
                                AppR.string.settings_slides_over_limit_warning,
                                rotationPlan.hiddenCount,
                                hiddenNames
                            ),
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                        )
                    }
                }

                val slideActionShape = RoundedCornerShape(8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        onClick = {
                            onClearSlideLimitError()
                            activePickerKind = ImportKind.SLIDE_IMAGE
                        },
                        shape = ClickableSurfaceDefaults.shape(slideActionShape),
                        scale = tvSurfaceScale(1.03f),
                        colors = tvSelectableSurfaceColors(selected = false),
                        border = tvSelectableSurfaceBorder(selected = false, shape = slideActionShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_btn_add_image_slide), style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = colorScheme.onSurface))
                        }
                    }

                    Surface(
                        onClick = {
                            onClearSlideLimitError()
                            addTextSlideCanEnable = canAddEnabledSlide
                            showAddTextSlideDialog = true
                        },
                        shape = ClickableSurfaceDefaults.shape(slideActionShape),
                        scale = tvSurfaceScale(1.03f),
                        colors = tvSelectableSurfaceColors(selected = false),
                        border = tvSelectableSurfaceBorder(selected = false, shape = slideActionShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                                contentDescription = null,
                                tint = colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    if (canAddEnabledSlide) R.string.settings_btn_add_text_slide
                                    else AppR.string.settings_slides_add_disabled
                                ),
                                style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                            )
                        }
                    }

                    if (slides.isNotEmpty()) {
                        Surface(
                            onClick = onClearSlides,
                            shape = ClickableSurfaceDefaults.shape(slideActionShape),
                            scale = tvSurfaceScale(1.03f),
                            colors = tvSelectableSurfaceColors(selected = false),
                            border = tvSelectableSurfaceBorder(selected = false, shape = slideActionShape)
                        ) {
                            Text(
                                text = "${stringResource(R.string.settings_btn_clear_slides)} (${slides.size})",
                                style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = Color(0xFFEF4444)),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (slides.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        slides.forEachIndexed { index, slide ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (slide.enabled) colorScheme.surfaceVariant else colorScheme.surface,
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(colorScheme.background, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "#${index + 1} ${if (slide.type == SlideType.IMAGE) "IMG" else "TXT"}",
                                                style = TvTypography.BodyLabel.copy(
                                                    fontSize = 12.sp,
                                                    color = if (slide.enabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.55f),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                        Text(
                                            text = slide.title.ifBlank { "(Untitled)" },
                                            style = TvTypography.BodyLabel.copy(
                                                fontSize = 15.sp,
                                                color = if (slide.enabled) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        )
                                        Text(
                                            text = "(${slide.durationSeconds}s)",
                                            style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.6f))
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val btnShape = RoundedCornerShape(6.dp)
                                        Surface(
                                            onClick = { onToggleSlideEnabled(slide.id) },
                                            shape = ClickableSurfaceDefaults.shape(btnShape),
                                            scale = tvSurfaceScale(1.03f),
                                            colors = tvSelectableSurfaceColors(selected = slide.enabled),
                                            border = tvSelectableSurfaceBorder(selected = slide.enabled, shape = btnShape)
                                        ) {
                                            Text(
                                                text = stringResource(
                                                    if (slide.enabled) AppR.string.settings_slides_status_enabled
                                                    else AppR.string.settings_slides_status_disabled
                                                ),
                                                style = TvTypography.BodyLabel.copy(
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (slide.enabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.7f)
                                                ),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }

                                        Surface(
                                            onClick = { onDeleteSlide(slide.id) },
                                            shape = ClickableSurfaceDefaults.shape(btnShape),
                                            scale = tvSurfaceScale(1.03f),
                                            colors = tvSelectableSurfaceColors(selected = false),
                                            border = tvSelectableSurfaceBorder(selected = false, shape = btnShape)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = stringResource(R.string.btn_delete),
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = stringResource(R.string.btn_delete),
                                                    style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = Color(0xFFEF4444))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 7b. Islamic Calendar Slide (Built-in Slide, 1 slot of 6)
                Text(
                    text = stringResource(R.string.islamic_cal_slide_section),
                    style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val calToggleShape = RoundedCornerShape(10.dp)
                        Card(
                            onClick = { onSetIslamicCalendarSlideEnabled(!settings.islamicCalendarSlideEnabled) },
                            colors = CardDefaults.colors(
                                containerColor = if (settings.islamicCalendarSlideEnabled) colorScheme.surfaceVariant else colorScheme.background,
                                focusedContainerColor = colorScheme.surfaceVariant
                            ),
                            shape = CardDefaults.shape(calToggleShape),
                            scale = tvCardScale(1.0f),
                            border = tvSelectableCardBorder(selected = settings.islamicCalendarSlideEnabled, shape = calToggleShape),
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
                                        text = stringResource(R.string.islamic_cal_slide_enable_title),
                                        style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                                    )
                                    Text(
                                        text = stringResource(R.string.islamic_cal_slide_enable_desc),
                                        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.78f))
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (settings.islamicCalendarSlideEnabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.16f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = stringResource(
                                            if (settings.islamicCalendarSlideEnabled) AppR.string.settings_silent_enabled
                                            else AppR.string.settings_silent_disabled
                                        ),
                                        style = TvTypography.ButtonText.copy(
                                            fontSize = 13.sp,
                                            color = if (settings.islamicCalendarSlideEnabled) Color.White else colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }

                        if (settings.islamicCalendarSlideEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.islamic_cal_slide_duration_label),
                                    style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val durShape = RoundedCornerShape(8.dp)
                                    for (secs in listOf(10, 15, 20, 30, 45, 60)) {
                                        val selected = settings.islamicCalendarSlideDurationSeconds == secs
                                        Surface(
                                            onClick = { onSetIslamicCalendarSlideDurationSeconds(secs) },
                                            shape = ClickableSurfaceDefaults.shape(durShape),
                                            scale = tvSurfaceScale(1.01f),
                                            colors = tvSelectableSurfaceColors(selected = selected, containerColor = colorScheme.background),
                                            border = tvSelectableSurfaceBorder(selected = selected, shape = durShape),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${secs}s",
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

                // 7c. Islamic Calendar Events (Shared toggles)
                Text(
                    text = stringResource(R.string.islamic_cal_settings_section),
                    style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
                )
                IslamicEventTogglesSection(
                    enabledIds = settings.enabledIslamicEventIds,
                    hijriOffsetDays = settings.hijriOffsetDays,
                    onToggleEvent = onToggleIslamicEvent
                )

                // 7c2. Offline Quran Verse Slide (Built-in Slide, 1 slot of 6)
                QuranVerseSettingsSection(
                    settings = settings,
                    allVerses = allQuranVerses,
                    onSetQuranVerseEnabled = onSetQuranVerseEnabled,
                    onSetQuranShowTranslation = onSetQuranShowTranslation,
                    onSetQuranVerseDurationSeconds = onSetQuranVerseDurationSeconds,
                    onRequestPreviewVerse = onRequestPreviewQuranVerse
                )

                // 7d. Silent Screen ("Jumu'ah / Prayer in progress")
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

                // 8. Mosque Admin PIN Security
                Text(
                    text = stringResource(R.string.settings_section_security),
                    style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
                )

                val pinCardShape = RoundedCornerShape(12.dp)
                Card(
                    onClick = { showChangePinDialog = true },
                    colors = CardDefaults.colors(
                        containerColor = colorScheme.surface,
                        focusedContainerColor = colorScheme.surfaceVariant
                    ),
                    shape = CardDefaults.shape(pinCardShape),
                    scale = tvCardScale(1.0f),
                    border = tvSelectableCardBorder(selected = false, shape = pinCardShape),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (settings.hasCustomPin) colorScheme.primary else colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (settings.hasCustomPin) stringResource(R.string.settings_custom_pin_active) else stringResource(R.string.settings_using_default_pin),
                                    style = TvTypography.CardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                                )
                                Text(
                                    text = if (settings.hasCustomPin)
                                        stringResource(R.string.settings_custom_pin_desc)
                                    else
                                        stringResource(R.string.settings_pin_warning_desc),
                                    style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))
                        Box(
                            modifier = Modifier
                                .background(colorScheme.primary, RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_btn_change_admin_pin),
                                style = TvTypography.ButtonText.copy(fontSize = 13.sp, color = Color.White)
                            )
                        }
                    }
                }

                if (settings.hasCustomPin) {
                    val resetBtnShape = RoundedCornerShape(8.dp)
                    Surface(
                        onClick = onResetPin,
                        shape = ClickableSurfaceDefaults.shape(resetBtnShape),
                        scale = tvSurfaceScale(1.02f),
                        colors = tvSelectableSurfaceColors(selected = false),
                        border = tvSelectableSurfaceBorder(selected = false, shape = resetBtnShape)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_reset_pin),
                            style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

            // ================= SECTION: LOCAL PHONE CONTROLLER (LAN) =================
            Text(
                text = stringResource(R.string.settings_section_phone_controller),
                style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
            )

            val phoneShape = RoundedCornerShape(12.dp)
            Surface(
                onClick = { onTogglePhoneController(!isPhoneControllerRunning) },
                shape = ClickableSurfaceDefaults.shape(phoneShape),
                scale = tvSurfaceScale(1.0f),
                colors = tvSelectableSurfaceColors(
                    selected = isPhoneControllerRunning,
                    containerColor = if (isPhoneControllerRunning) AccentEmerald.copy(alpha = 0.2f) else colorScheme.surface
                ),
                border = tvSelectableSurfaceBorder(selected = isPhoneControllerRunning, shape = phoneShape),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isPhoneControllerRunning) {
                            stringResource(R.string.settings_phone_controller_on)
                        } else {
                            stringResource(R.string.settings_phone_controller_off)
                        },
                        style = TvTypography.CardTitle.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPhoneControllerRunning) AccentEmerald else colorScheme.onSurface
                        )
                    )
                    Text(
                        text = stringResource(R.string.settings_phone_controller_desc),
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 13.sp,
                            color = colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    )
                    if (isPhoneControllerRunning) {
                        Text(
                            text = if (!phoneControllerUrl.isNullOrBlank()) {
                                stringResource(R.string.settings_phone_controller_url, phoneControllerUrl)
                            } else {
                                stringResource(R.string.settings_phone_controller_no_wifi)
                            },
                            style = TvTypography.CardTitle.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                        )
                    }
                }
            }

            // ================= SECTION: ABOUT & CREDITS (Phase 7.1) =================
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
                                text = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                                style = TvTypography.Subtitle.copy(fontSize = 14.sp, color = colorScheme.secondary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.about_credit),
                        style = TvTypography.BodyLabel.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
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
                        text = stringResource(R.string.settings_about_libraries),
                        style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.65f))
                    )

                    Text(
                        text = stringResource(AppR.string.about_quran_tanzil),
                        style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.72f))
                    )
                    Text(
                        text = stringResource(AppR.string.about_quran_en),
                        style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.72f))
                    )
                    Text(
                        text = stringResource(AppR.string.about_quran_id),
                        style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.72f))
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
                                        ImportKind.SLIDE_IMAGE -> stringResource(R.string.storage_title_slide_image)
                    else -> stringResource(R.string.storage_title_slide_image)
                },
                onFileImported = { fileName ->
                    when (activePickerKind) {
                        ImportKind.MOSQUE_LOGO -> onSetCustomLogo(fileName)
                        ImportKind.CUSTOM_ADHAN -> {}
                        ImportKind.SLIDE_IMAGE -> {
                            onAddImageSlide("Poster", fileName, 15)
                        }
                        null -> {}
                    }
                    activePickerKind = null
                                    },
                onDismiss = {
                    recordInteraction()
                    activePickerKind = null
                },
                onUserInteraction = recordInteraction
            )
        }

        // Overlay: Add Text Announcement Slide Dialog
        if (showAddTextSlideDialog) {
            AddTextSlideDialog(
                canAddEnabledSlide = addTextSlideCanEnable,
                onSave = { title, body, durationSeconds, enabled ->
                    recordInteraction()
                    onAddTextSlide(title, body, durationSeconds, enabled)
                    showAddTextSlideDialog = false
                },
                onDismiss = {
                    recordInteraction()
                    showAddTextSlideDialog = false
                },
                onUserInteraction = recordInteraction
            )
        }

        // Overlay: Change PIN Dialog
        if (showChangePinDialog) {
            PinDialog(
                mode = PinDialogMode.SET_NEW,
                settings = settings,
                onPinChanged = { newPin ->
                    onSetCustomPin(newPin)
                    showChangePinDialog = false
                },
                onDismiss = { showChangePinDialog = false }
            )
        }
    }
}
