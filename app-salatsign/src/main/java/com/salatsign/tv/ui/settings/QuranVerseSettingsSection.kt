// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.salatsign.core.tv.R
import com.salatsign.tv.R as AppR
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.QuranSlideDefaults
import com.salatsign.tv.data.model.QuranVerseItem
import com.salatsign.tv.ui.common.tvCardScale
import com.salatsign.tv.ui.common.tvSelectableCardBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceColors
import com.salatsign.tv.ui.common.tvSurfaceScale
import com.salatsign.tv.ui.slides.QuranVerseSlide
import com.salatsign.tv.ui.slides.measureQuranVerseFit
import com.salatsign.tv.ui.theme.TvTypography

@Composable
fun QuranVerseSettingsSection(
    settings: AppSettings,
    allVerses: List<QuranVerseItem>,
    onSetQuranVerseEnabled: (Boolean) -> Unit,
    onSetQuranShowTranslation: (Boolean) -> Unit,
    onSetQuranVerseDurationSeconds: (Int) -> Unit,
    onRequestPreviewVerse: () -> QuranVerseItem?
) {
    val colorScheme = MaterialTheme.colorScheme
    var previewVerse by remember { mutableStateOf<QuranVerseItem?>(null) }
    var showFitCheckDialog by remember { mutableStateOf(false) }

    Text(
        text = stringResource(AppR.string.settings_section_quran_slide),
        style = TvTypography.CardTitle.copy(color = colorScheme.secondary)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Enable toggle (1 slot of 6)
            val toggleShape = RoundedCornerShape(10.dp)
            Card(
                onClick = { onSetQuranVerseEnabled(!settings.quranVerseEnabled) },
                colors = CardDefaults.colors(
                    containerColor = if (settings.quranVerseEnabled) colorScheme.surfaceVariant else colorScheme.background,
                    focusedContainerColor = colorScheme.surfaceVariant
                ),
                shape = CardDefaults.shape(toggleShape),
                scale = tvCardScale(1.0f),
                border = tvSelectableCardBorder(selected = settings.quranVerseEnabled, shape = toggleShape),
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
                            text = stringResource(AppR.string.settings_quran_enable_title),
                            style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                        )
                        Text(
                            text = stringResource(AppR.string.settings_quran_enable_desc),
                            style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.78f))
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (settings.quranVerseEnabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.16f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(
                                if (settings.quranVerseEnabled) AppR.string.settings_silent_enabled
                                else AppR.string.settings_silent_disabled
                            ),
                            style = TvTypography.ButtonText.copy(
                                fontSize = 13.sp,
                                color = if (settings.quranVerseEnabled) Color.White else colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // Show Translation toggle
            Card(
                onClick = { onSetQuranShowTranslation(!settings.quranShowTranslation) },
                colors = CardDefaults.colors(
                    containerColor = if (settings.quranShowTranslation) colorScheme.surfaceVariant else colorScheme.background,
                    focusedContainerColor = colorScheme.surfaceVariant
                ),
                shape = CardDefaults.shape(toggleShape),
                scale = tvCardScale(1.0f),
                border = tvSelectableCardBorder(selected = settings.quranShowTranslation, shape = toggleShape),
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
                            text = stringResource(AppR.string.settings_quran_show_translation_title),
                            style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                        )
                        Text(
                            text = stringResource(AppR.string.settings_quran_show_translation_desc),
                            style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.78f))
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (settings.quranShowTranslation) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.16f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (settings.quranShowTranslation) "ON" else "OFF",
                            style = TvTypography.ButtonText.copy(
                                fontSize = 13.sp,
                                color = if (settings.quranShowTranslation) Color.White else colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // Duration selector (Auto / 15s / 20s / 30s / 45s / 60s / 90s)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(AppR.string.settings_quran_duration_label),
                    style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val durShape = RoundedCornerShape(8.dp)
                    for (secs in QuranSlideDefaults.DURATION_PRESETS) {
                        val selected = settings.quranVerseDurationSeconds == secs
                        val label = if (secs == QuranSlideDefaults.DURATION_AUTO) {
                            stringResource(AppR.string.settings_quran_duration_auto)
                        } else {
                            "${secs}s"
                        }
                        Surface(
                            onClick = { onSetQuranVerseDurationSeconds(secs) },
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
                                    text = label,
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

            // Admin tools: Preview a verse now + Check verse fit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val actionShape = RoundedCornerShape(10.dp)
                Surface(
                    onClick = { previewVerse = onRequestPreviewVerse() },
                    shape = ClickableSurfaceDefaults.shape(actionShape),
                    scale = tvSurfaceScale(1.01f),
                    colors = tvSelectableSurfaceColors(selected = false, containerColor = colorScheme.surfaceVariant),
                    border = tvSelectableSurfaceBorder(selected = false, shape = actionShape),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(AppR.string.settings_quran_preview_btn),
                            style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                        )
                    }
                }

                Surface(
                    onClick = { showFitCheckDialog = true },
                    shape = ClickableSurfaceDefaults.shape(actionShape),
                    scale = tvSurfaceScale(1.01f),
                    colors = tvSelectableSurfaceColors(selected = false, containerColor = colorScheme.surfaceVariant),
                    border = tvSelectableSurfaceBorder(selected = false, shape = actionShape),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(AppR.string.settings_quran_fit_check_btn),
                            style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                        )
                    }
                }
            }
        }
    }

    // Full-screen Preview Overlay
    previewVerse?.let { verse ->
        Dialog(
            onDismissRequest = { previewVerse = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { ev ->
                        if (ev.type == KeyEventType.KeyDown &&
                            (ev.key == Key.Back || ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                        ) {
                            previewVerse = null
                            true
                        } else {
                            false
                        }
                    }
            ) {
                QuranVerseSlide(
                    verse = verse,
                    showTranslation = settings.quranShowTranslation
                )
            }
        }
    }

    // Check Verse Fit Diagnostic Dialog (measures all 63 verses at this TV's exact overscan-safe dimensions)
    if (showFitCheckDialog) {
        QuranVerseFitCheckDialog(
            allVerses = allVerses,
            showTranslation = settings.quranShowTranslation,
            onDismiss = { showFitCheckDialog = false }
        )
    }
}

@Composable
private fun QuranVerseFitCheckDialog(
    allVerses: List<QuranVerseItem>,
    showTranslation: Boolean,
    onDismiss: () -> Unit
) {
    val resources = LocalResources.current
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val closeFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { closeFocus.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center
        ) {
            // Exact usable dimensions matching QuranVerseSlide (5% overscan: 48dp H, 27dp V)
            val usableWidthPx = with(density) { ((maxWidth - 96.dp).coerceAtLeast(100.dp) * 0.92f).roundToPx() }
            val usableHeightPx = with(density) { (maxHeight - 54.dp).coerceAtLeast(100.dp).roundToPx() }
            val arabicGapPx = with(density) { 24.dp.roundToPx() }
            val refGapPx = with(density) { 22.dp.roundToPx() }

            val nonFittingRefs = remember(allVerses, showTranslation, usableWidthPx, usableHeightPx) {
                allVerses.mapNotNull { verse ->
                    val refLabel = resources.getString(
                        R.string.quran_surah_reference,
                        verse.surahTransliteration,
                        verse.reference
                    )
                    val fit = measureQuranVerseFit(
                        textMeasurer = textMeasurer,
                        verse = verse,
                        referenceLabel = refLabel,
                        showTranslation = showTranslation,
                        maxWidthPx = usableWidthPx,
                        maxHeightPx = usableHeightPx,
                        arabicGapPx = arabicGapPx,
                        refGapPx = refGapPx
                    )
                    if (!fit.fits) verse.reference else null
                }
            }

            val dialogShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .background(colorScheme.background, dialogShape)
                    .border(1.5.dp, colorScheme.primary.copy(alpha = 0.55f), dialogShape)
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(AppR.string.settings_quran_fit_check_title),
                        style = TvTypography.CardTitle.copy(fontSize = 20.sp, color = colorScheme.secondary)
                    )

                    if (nonFittingRefs.isEmpty()) {
                        Text(
                            text = stringResource(AppR.string.settings_quran_fit_all_ok, allVerses.size),
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.primary
                            )
                        )
                    } else {
                        Text(
                            text = stringResource(
                                AppR.string.settings_quran_fit_overflow,
                                nonFittingRefs.size,
                                nonFittingRefs.joinToString(", ")
                            ),
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 14.sp,
                                color = Color(0xFFDC2626)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val btnShape = RoundedCornerShape(10.dp)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(closeFocus)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
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
