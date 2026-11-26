// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.salatsign.core.tv.R
import com.salatsign.tv.BuildConfig
import com.salatsign.tv.R as AppR
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.MAX_MOSQUE_NAME_LENGTH
import com.salatsign.tv.data.model.MAX_MOSQUE_TAGLINE_LENGTH
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.QuranVerseItem
import com.salatsign.tv.data.model.SlideType
import com.salatsign.tv.engine.BuiltInSlideId
import com.salatsign.tv.engine.JumuahCalculator
import com.salatsign.tv.engine.JumuahWarningReason
import com.salatsign.tv.engine.SlideBudget
import com.salatsign.tv.engine.SlideshowRotationBuilder
import com.salatsign.tv.ui.common.IslamicEventTogglesSection
import com.salatsign.tv.ui.common.MosqueEmblemIcon
import com.salatsign.tv.ui.common.TvEditableTextField
import com.salatsign.tv.ui.common.getMosqueFontFamily
import com.salatsign.tv.ui.common.labelRes
import com.salatsign.tv.ui.common.tvCardScale
import com.salatsign.tv.ui.common.tvSelectableCardBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceColors
import com.salatsign.tv.ui.common.tvSurfaceScale
import com.salatsign.tv.ui.mosque.BuiltInSlideRegistry
import com.salatsign.tv.ui.theme.TvTypography
import java.time.LocalDate

@Composable
fun MosqueDefaultPinWarningBanner(onOpenChangePin: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val warnShape = RoundedCornerShape(12.dp)
    val warnRed = Color(0xFFDC2626)

    Card(
        onClick = onOpenChangePin,
        colors = CardDefaults.colors(
            containerColor = Color(0xFFEF4444).copy(alpha = 0.14f),
            focusedContainerColor = Color(0xFFEF4444).copy(alpha = 0.24f)
        ),
        shape = CardDefaults.shape(warnShape),
        scale = tvCardScale(1.0f),
        border = CardDefaults.border(
            border = Border(border = BorderStroke(1.5.dp, warnRed.copy(alpha = 0.55f)), shape = warnShape),
            focusedBorder = Border(border = BorderStroke(2.5.dp, colorScheme.secondary), shape = warnShape)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
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
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.settings_pin_warning_title),
                        style = TvTypography.CardTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                    )
                    Text(
                        text = stringResource(R.string.settings_pin_warning_desc),
                        style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.82f))
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .background(warnRed, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_change_pin),
                    style = TvTypography.ButtonText.copy(fontSize = 12.sp, color = Color.White)
                )
            }
        }
    }
}

@Composable
fun MosqueIqamahAndJumuahPane(
    settings: AppSettings,
    onSetIqamahOffset: (Prayer, Int) -> Unit,
    onSetJumuahMode: (JumuahMode) -> Unit,
    onSetJumuahTimes: (List<String>) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    // Per-Prayer Iqamah Offsets
    Text(
        text = stringResource(R.string.settings_section_iqamah),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        style = TvTypography.CardTitle.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (currentIqamah == 0) stringResource(R.string.settings_iqamah_disabled) else stringResource(R.string.settings_iqamah_minutes, currentIqamah),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentIqamah > 0) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val iqShape = RoundedCornerShape(6.dp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            onClick = { onSetIqamahOffset(prayer, (currentIqamah - 5).coerceAtLeast(0)) },
                            shape = ClickableSurfaceDefaults.shape(iqShape),
                            scale = tvSurfaceScale(1.04f),
                            colors = tvSelectableSurfaceColors(selected = false, containerColor = colorScheme.surfaceVariant),
                            border = tvSelectableSurfaceBorder(selected = false, shape = iqShape)
                        ) {
                            Text(
                                text = "-5",
                                style = TvTypography.ButtonText.copy(fontSize = 12.sp, color = colorScheme.onSurface),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            onClick = { onSetIqamahOffset(prayer, (currentIqamah + 5).coerceAtMost(30)) },
                            shape = ClickableSurfaceDefaults.shape(iqShape),
                            scale = tvSurfaceScale(1.04f),
                            colors = tvSelectableSurfaceColors(selected = false, containerColor = colorScheme.surfaceVariant),
                            border = tvSelectableSurfaceBorder(selected = false, shape = iqShape)
                        ) {
                            Text(
                                text = "+5",
                                style = TvTypography.ButtonText.copy(fontSize = 12.sp, color = colorScheme.onSurface),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Friday Jumu'ah Schedule
    Text(
        text = stringResource(R.string.settings_section_jumuah),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )
    Text(
        text = stringResource(R.string.settings_jumuah_desc),
        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.75f))
    )

    Text(
        text = stringResource(R.string.settings_jumuah_mode_title),
        style = TvTypography.Subtitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (mode in listOf(JumuahMode.FIXED, JumuahMode.AFTER_ZUHR)) {
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
                            color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (mode == JumuahMode.FIXED) stringResource(R.string.settings_jumuah_mode_fixed_desc) else stringResource(R.string.settings_jumuah_mode_after_zuhr_desc),
                        style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.72f))
                    )
                }
            }
        }
    }

    Text(
        text = stringResource(R.string.settings_jumuah_sittings_count_title),
        style = TvTypography.Subtitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val currentCount = settings.jumuahTimes.size.coerceIn(1, 3)
        for (c in listOf(1, 2, 3)) {
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
                        JumuahMode.FIXED -> when (c) {
                            1 -> listOf("13:00")
                            2 -> listOf("12:30", "13:30")
                            else -> listOf("12:15", "13:00", "13:45")
                        }
                        JumuahMode.AFTER_ZUHR -> when (c) {
                            1 -> listOf("45")
                            2 -> listOf("45", "90")
                            else -> listOf("30", "60", "90")
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
                            color = if (isCountSelected) colorScheme.primary else colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }

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
                        style = TvTypography.CardTitle.copy(fontSize = 14.sp, color = colorScheme.secondary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (settings.jumuahMode == JumuahMode.FIXED) timeValue else "+$timeValue min",
                        style = TvTypography.ActivePrayerTime.copy(fontSize = 22.sp, color = colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val presetOptions = if (settings.jumuahMode == JumuahMode.FIXED) {
                        listOf("12:15", "12:30", "12:45", "13:00", "13:15", "13:30", "13:45", "14:00")
                    } else {
                        listOf("0", "15", "30", "45", "60", "75", "90", "120")
                    }
                    val currentIndex = presetOptions.indexOf(timeValue).coerceAtLeast(0)
                    val stepShape = RoundedCornerShape(6.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            Text("-15m", style = TvTypography.ButtonText.copy(fontSize = 12.sp, color = colorScheme.onSurface), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
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
                            Text("+15m", style = TvTypography.ButtonText.copy(fontSize = 12.sp, color = colorScheme.onSurface), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        }
    }

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
                    if (isInvalid) colorScheme.error.copy(alpha = 0.2f) else colorScheme.secondary.copy(alpha = 0.15f),
                    RoundedCornerShape(8.dp)
                )
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
                    color = if (isInvalid) colorScheme.error else colorScheme.secondary
                ),
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun MosqueBrandingPane(
    settings: AppSettings,
    onSetMosqueIconPreset: (MosqueIconPreset) -> Unit,
    onOpenLogoPicker: () -> Unit,
    onSetMosqueName: (String) -> Unit,
    onSetMosqueFontPreset: (MosqueFontPreset) -> Unit,
    onSetMosqueSubtitle: (String) -> Unit,
    onUserInteraction: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Text(
        text = stringResource(R.string.settings_section_mosque_symbol),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (preset in MosqueIconPreset.entries) {
            val isSelected = (settings.mosqueIconPreset == preset)
            val iconShape = RoundedCornerShape(14.dp)
            Card(
                onClick = {
                    onSetMosqueIconPreset(preset)
                    if (preset == MosqueIconPreset.CUSTOM) {
                        onOpenLogoPicker()
                    }
                },
                colors = CardDefaults.colors(
                    containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface,
                    focusedContainerColor = colorScheme.surfaceVariant
                ),
                shape = CardDefaults.shape(iconShape),
                scale = tvCardScale(1.03f),
                border = tvSelectableCardBorder(selected = isSelected, shape = iconShape),
                modifier = Modifier
                    .weight(1f)
                    .height(126.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    MosqueEmblemIcon(preset = preset, customLogoUri = settings.mosqueLogoUri, size = 50.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(preset.labelRes()),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }

    Text(
        text = stringResource(R.string.mosque_label_name),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    val fallbackSampleName = stringResource(R.string.mosque_default_name)
    var mosqueNameInput by remember(settings.mosqueName) {
        mutableStateOf(settings.mosqueName)
    }
    var mosqueNameError by remember { mutableStateOf(false) }

    TvEditableTextField(
        value = mosqueNameInput,
        onValueChange = {
            onUserInteraction()
            mosqueNameInput = it
            val trimmed = it.trim()
            if (trimmed.isEmpty()) {
                mosqueNameError = true
            } else {
                mosqueNameError = false
                onSetMosqueName(trimmed)
            }
        },
        placeholder = stringResource(R.string.mosque_name_placeholder),
        accentUnfocusedBorder = true,
        maxLength = MAX_MOSQUE_NAME_LENGTH,
        textStyle = TvTypography.CardTitle.copy(fontSize = 17.sp)
    )
    if (mosqueNameError) {
        Text(
            text = stringResource(R.string.mosque_name_required_error),
            style = TvTypography.BodyLabel.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.error
            )
        )
    }

    Text(
        text = stringResource(R.string.settings_section_mosque_font),
        style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.onSurface)
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
                        text = settings.mosqueName.ifBlank { fallbackSampleName },
                        fontFamily = getMosqueFontFamily(font),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
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

    var subtitleInput by remember(settings.mosqueSubtitle) { mutableStateOf(settings.mosqueSubtitle) }

    Text(
        text = stringResource(R.string.mosque_label_tagline),
        style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.onSurface)
    )

    TvEditableTextField(
        value = subtitleInput,
        onValueChange = {
            onUserInteraction()
            subtitleInput = it
            onSetMosqueSubtitle(it)
        },
        placeholder = stringResource(R.string.mosque_tagline_hint),
        accentUnfocusedBorder = false,
        maxLength = MAX_MOSQUE_TAGLINE_LENGTH,
        textStyle = TvTypography.BodyLabel.copy(fontSize = 15.sp)
    )
}

@Composable
fun MosqueSlideshowPane(
    settings: AppSettings,
    slides: List<AnnouncementSlide>,
    slideLimitError: Boolean,
    allQuranVerses: List<QuranVerseItem>,
    onUpdateScheduleDuration: (Int) -> Unit,
    onAddImageSlideClick: (canAddEnabled: Boolean) -> Unit,
    onAddTextSlideClick: (canAddEnabled: Boolean) -> Unit,
    onEditSlide: (AnnouncementSlide) -> Unit,
    onToggleSlideEnabled: (String) -> Unit,
    onDeleteSlide: (String) -> Unit,
    onClearSlides: () -> Unit,
    onSetIslamicCalendarSlideEnabled: (Boolean) -> Unit,
    onSetIslamicCalendarSlideDurationSeconds: (Int) -> Unit,
    onToggleIslamicEvent: (String) -> Unit,
    onSetQuranVerseEnabled: (Boolean) -> Unit,
    onSetQuranShowTranslation: (Boolean) -> Unit,
    onSetQuranVerseDurationSeconds: (Int) -> Unit,
    onRequestPreviewQuranVerse: () -> QuranVerseItem?
) {
    val colorScheme = MaterialTheme.colorScheme

    Text(
        text = stringResource(R.string.settings_section_schedule_duration),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val durShape = RoundedCornerShape(8.dp)
        for (dur in listOf(30, 60, 90, 120)) {
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
                        color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    val builtIns = BuiltInSlideRegistry.registrationsFor(settings)
    val rotationPlan = SlideshowRotationBuilder.build(slides, builtIns)
    val canAddEnabledSlide = SlideBudget.canEnableOrAddAnnouncement(
        enabledAnnouncements = rotationPlan.enabledAnnouncementCount,
        enabledBuiltIns = rotationPlan.enabledBuiltInCount
    )

    val addImageFocusRequester = remember { FocusRequester() }
    val slideActionShape = RoundedCornerShape(8.dp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
                            BuiltInSlideId.ISLAMIC_CALENDAR -> stringResource(AppR.string.settings_slides_builtin_islamic_calendar)
                            BuiltInSlideId.QURAN_VERSE -> stringResource(AppR.string.settings_slides_builtin_quran_verse)
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

        if (slides.isNotEmpty()) {
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                onClick = {
                    // Move focus inside the Slideshow pane before removing this button so focus never jumps to the Left Rail
                    runCatching { addImageFocusRequester.requestFocus() }
                    onClearSlides()
                },
                shape = ClickableSurfaceDefaults.shape(slideActionShape),
                scale = tvSurfaceScale(1.03f),
                colors = tvSelectableSurfaceColors(selected = false),
                border = tvSelectableSurfaceBorder(selected = false, shape = slideActionShape)
            ) {
                Text(
                    text = "${stringResource(R.string.settings_btn_clear_slides)} (${slides.size})",
                    maxLines = 1,
                    softWrap = false,
                    style = TvTypography.ButtonText.copy(fontSize = 13.sp, color = Color(0xFFEF4444)),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
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
                style = TvTypography.BodyLabel.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
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
                text = stringResource(AppR.string.settings_slides_over_limit_warning, rotationPlan.hiddenCount, hiddenNames),
                style = TvTypography.BodyLabel.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            onClick = { onAddImageSlideClick(canAddEnabledSlide) },
            shape = ClickableSurfaceDefaults.shape(slideActionShape),
            scale = tvSurfaceScale(1.02f),
            colors = tvSelectableSurfaceColors(selected = false),
            border = tvSelectableSurfaceBorder(selected = false, shape = slideActionShape),
            modifier = Modifier
                .weight(1f)
                .focusRequester(addImageFocusRequester)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.settings_btn_add_image_slide),
                    maxLines = 1,
                    softWrap = false,
                    style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                )
            }
        }

        Surface(
            onClick = { onAddTextSlideClick(canAddEnabledSlide) },
            shape = ClickableSurfaceDefaults.shape(slideActionShape),
            scale = tvSurfaceScale(1.02f),
            colors = tvSelectableSurfaceColors(selected = false),
            border = tvSelectableSurfaceBorder(selected = false, shape = slideActionShape),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
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
                    maxLines = 1,
                    softWrap = false,
                    style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface)
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
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(colorScheme.background, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "#${index + 1} ${if (slide.type == SlideType.IMAGE) "IMG" else "TXT"}",
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 11.sp,
                                        color = if (slide.enabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.55f),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Text(
                                text = slide.title.ifBlank { "(Untitled)" },
                                style = TvTypography.BodyLabel.copy(
                                    fontSize = 14.sp,
                                    color = if (slide.enabled) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            Text(
                                text = "(${slide.durationSeconds}s)",
                                style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.6f))
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val btnShape = RoundedCornerShape(6.dp)
                            Surface(
                                onClick = { onEditSlide(slide) },
                                shape = ClickableSurfaceDefaults.shape(btnShape),
                                scale = tvSurfaceScale(1.03f),
                                colors = tvSelectableSurfaceColors(selected = false),
                                border = tvSelectableSurfaceBorder(selected = false, shape = btnShape)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(AppR.string.settings_slide_edit_btn),
                                        tint = colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(AppR.string.settings_slide_edit_btn),
                                        style = TvTypography.BodyLabel.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                                    )
                                }
                            }

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
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
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
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.btn_delete),
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(15.dp)
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

    // Islamic Calendar Built-in Slide
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

    Text(
        text = stringResource(R.string.islamic_cal_settings_section),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )
    IslamicEventTogglesSection(
        enabledIds = settings.enabledIslamicEventIds,
        hijriOffsetDays = settings.hijriOffsetDays,
        onToggleEvent = onToggleIslamicEvent
    )

    // Offline Quran Verse Slide
    QuranVerseSettingsSection(
        settings = settings,
        allVerses = allQuranVerses,
        onSetQuranVerseEnabled = onSetQuranVerseEnabled,
        onSetQuranShowTranslation = onSetQuranShowTranslation,
        onSetQuranVerseDurationSeconds = onSetQuranVerseDurationSeconds,
        onRequestPreviewVerse = onRequestPreviewQuranVerse
    )
}

@Composable
fun MosqueRemoteSecurityPane(
    settings: AppSettings,
    isPhoneControllerRunning: Boolean,
    phoneControllerUrl: String?,
    onTogglePhoneController: (Boolean) -> Unit,
    onSetPhoneControllerAlwaysOn: (Boolean) -> Unit = {},
    onOpenPairPhone: () -> Unit = {},
    onRevokePairedDevice: (String) -> Unit = {},
    onSetLanPinLoginEnabled: (Boolean) -> Unit = {},
    onSetPairedTokenExpiryDays: (Int) -> Unit = {},
    onOpenChangePin: () -> Unit,
    onResetPin: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Text(
        text = stringResource(R.string.settings_section_security),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    val pinCardShape = RoundedCornerShape(12.dp)
    Card(
        onClick = onOpenChangePin,
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
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (settings.hasCustomPin) stringResource(R.string.settings_custom_pin_active) else stringResource(R.string.settings_using_default_pin),
                        style = TvTypography.CardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                    )
                    Text(
                        text = if (settings.hasCustomPin) stringResource(R.string.settings_custom_pin_desc) else stringResource(R.string.settings_pin_warning_desc),
                        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))
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
            containerColor = if (isPhoneControllerRunning) colorScheme.primary.copy(alpha = 0.16f) else colorScheme.surface
        ),
        border = tvSelectableSurfaceBorder(selected = isPhoneControllerRunning, shape = phoneShape),
        modifier = Modifier.fillMaxWidth()
    ) {
        val activeDevicesCount = settings.pairedDevices.count { !it.isRevoked }
        val tvDisplayName = settings.deviceName.ifBlank { stringResource(AppR.string.tv_identity_unnamed) }
            .let { n -> if (settings.roomGroup.isNotBlank()) "$n (${settings.roomGroup})" else n }
        val shortId = settings.deviceId.take(8).ifBlank { "pending" }
        val keyFingerprint = remember(settings.deviceEcPublicKeyBase64) {
            com.salatsign.tv.security.PeerJoinCrypto.keyFingerprint(settings.deviceEcPublicKeyBase64)
        }
        val urlQrMatrix = remember(isPhoneControllerRunning, phoneControllerUrl) {
            if (isPhoneControllerRunning && !phoneControllerUrl.isNullOrBlank()) {
                runCatching {
                    com.google.zxing.qrcode.QRCodeWriter().encode(
                        phoneControllerUrl,
                        com.google.zxing.BarcodeFormat.QR_CODE,
                        0,
                        0,
                        mapOf(com.google.zxing.EncodeHintType.MARGIN to 1)
                    )
                }.getOrNull()
            } else null
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
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
                        color = if (isPhoneControllerRunning) colorScheme.primary else colorScheme.onSurface
                    )
                )
                Text(
                    text = stringResource(AppR.string.tv_identity_summary, tvDisplayName, shortId, activeDevicesCount),
                    style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.85f))
                )
                if (keyFingerprint.isNotBlank()) {
                    Text(
                        text = "Key Fingerprint: $keyFingerprint",
                        style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.secondary)
                    )
                }
                Text(
                    text = stringResource(R.string.settings_phone_controller_desc),
                    style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.78f))
                )
                if (isPhoneControllerRunning) {
                    Text(
                        text = if (!phoneControllerUrl.isNullOrBlank()) {
                            stringResource(R.string.settings_phone_controller_url, phoneControllerUrl)
                        } else {
                            stringResource(R.string.settings_phone_controller_no_wifi)
                        },
                        style = TvTypography.CardTitle.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.secondary
                        )
                    )
                }
            }
            if (urlQrMatrix != null) {
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .padding(6.dp)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val cols = urlQrMatrix.width
                        val rows = urlQrMatrix.height
                        val cellW = size.width / cols.toFloat()
                        val cellH = size.height / rows.toFloat()
                        for (y in 0 until rows) {
                            for (x in 0 until cols) {
                                if (urlQrMatrix[x, y]) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = androidx.compose.ui.geometry.Offset(x * cellW, y * cellH),
                                        size = androidx.compose.ui.geometry.Size(cellW + 0.5f, cellH + 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Keep Phone Controller Always On (survives TV reboot via boot launch)
    val alwaysOnShape = RoundedCornerShape(12.dp)
    Surface(
        onClick = { onSetPhoneControllerAlwaysOn(!settings.phoneControllerAlwaysOn) },
        shape = ClickableSurfaceDefaults.shape(alwaysOnShape),
        scale = tvSurfaceScale(1.0f),
        colors = tvSelectableSurfaceColors(selected = settings.phoneControllerAlwaysOn),
        border = tvSelectableSurfaceBorder(selected = settings.phoneControllerAlwaysOn, shape = alwaysOnShape),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(
                    if (settings.phoneControllerAlwaysOn) AppR.string.always_on_title_on else AppR.string.always_on_title_off
                ),
                style = TvTypography.CardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
            )
            Text(
                text = stringResource(AppR.string.always_on_desc),
                style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.78f))
            )
        }
    }

    // Pair Phone (QR & one-time code)
    val pairBtnShape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onOpenPairPhone,
        shape = ClickableSurfaceDefaults.shape(pairBtnShape),
        scale = tvSurfaceScale(1.0f),
        colors = tvSelectableSurfaceColors(
            selected = false,
            containerColor = colorScheme.primary.copy(alpha = 0.14f)
        ),
        border = tvSelectableSurfaceBorder(selected = false, shape = pairBtnShape),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(AppR.string.pair_btn_open),
                style = TvTypography.CardTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
            )
            Text(
                text = stringResource(AppR.string.pair_btn_open_desc),
                style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
            )
        }
    }

    // Allow PIN login over LAN toggle
    val lanPinShape = RoundedCornerShape(12.dp)
    Surface(
        onClick = { onSetLanPinLoginEnabled(!settings.lanPinLoginEnabled) },
        shape = ClickableSurfaceDefaults.shape(lanPinShape),
        scale = tvSurfaceScale(1.0f),
        colors = tvSelectableSurfaceColors(selected = settings.lanPinLoginEnabled),
        border = tvSelectableSurfaceBorder(selected = settings.lanPinLoginEnabled, shape = lanPinShape),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(
                    if (settings.lanPinLoginEnabled) AppR.string.pair_pin_login_on else AppR.string.pair_pin_login_off
                ),
                style = TvTypography.CardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
            )
            Text(
                text = stringResource(AppR.string.pair_pin_login_desc),
                style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.78f))
            )
        }
    }

    // Inactivity auto-expiry chips
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(AppR.string.pair_expiry_label),
            style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.85f))
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                0 to stringResource(AppR.string.pair_expiry_never),
                30 to stringResource(AppR.string.pair_expiry_30d),
                90 to stringResource(AppR.string.pair_expiry_90d)
            ).forEach { (days, label) ->
                val isSelected = settings.pairedTokenExpiryDays == days
                val chipShape = RoundedCornerShape(10.dp)
                Surface(
                    onClick = { onSetPairedTokenExpiryDays(days) },
                    shape = ClickableSurfaceDefaults.shape(chipShape),
                    scale = tvSurfaceScale(1.02f),
                    colors = tvSelectableSurfaceColors(selected = isSelected),
                    border = tvSelectableSurfaceBorder(selected = isSelected, shape = chipShape)
                ) {
                    Text(
                        text = label,
                        style = TvTypography.ButtonText.copy(fontSize = 14.sp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    // Active Paired Devices list with instant Revoke
    val activeDevices = settings.pairedDevices.filterNot { it.isRevoked }
    Text(
        text = stringResource(AppR.string.pair_devices_title, activeDevices.size),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )
    if (activeDevices.isEmpty()) {
        Text(
            text = stringResource(AppR.string.pair_devices_empty),
            style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.72f))
        )
    } else {
        activeDevices.forEach { device ->
            val rowShape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surface, rowShape)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.deviceName,
                        style = TvTypography.CardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                    )
                    Text(
                        text = if (device.role == com.salatsign.tv.security.RemoteRole.MANAGE) {
                            stringResource(AppR.string.pair_role_manage)
                        } else {
                            stringResource(AppR.string.pair_role_notify)
                        },
                        style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.primary)
                    )
                }
                val revokeShape = RoundedCornerShape(8.dp)
                Surface(
                    onClick = { onRevokePairedDevice(device.deviceId) },
                    shape = ClickableSurfaceDefaults.shape(revokeShape),
                    scale = tvSurfaceScale(1.02f),
                    colors = tvSelectableSurfaceColors(selected = false),
                    border = tvSelectableSurfaceBorder(selected = false, shape = revokeShape)
                ) {
                    Text(
                        text = stringResource(AppR.string.pair_btn_revoke),
                        style = TvTypography.ButtonText.copy(fontSize = 13.sp, color = Color(0xFFFCA5A5)),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun MosqueAboutPane() {
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
