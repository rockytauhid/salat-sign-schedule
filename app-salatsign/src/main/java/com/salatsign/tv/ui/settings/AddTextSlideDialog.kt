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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatListNumbered
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.salatsign.core.tv.R
import com.salatsign.tv.R as AppR
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.SlideTextAlign
import com.salatsign.tv.data.model.SlideTextLine
import com.salatsign.tv.data.repository.ContentRepository
import com.salatsign.tv.ui.common.TvEditableTextField
import com.salatsign.tv.ui.common.tvSelectableSurfaceBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceColors
import com.salatsign.tv.ui.common.tvSurfaceScale
import com.salatsign.tv.ui.theme.TvTypography

const val MAX_TEXT_SLIDE_LINES = 10
private const val MAX_LINE_CHARS = 100

private fun nextAlignment(current: SlideTextAlign): SlideTextAlign = when (current) {
    SlideTextAlign.CENTER -> SlideTextAlign.LEFT
    SlideTextAlign.LEFT -> SlideTextAlign.RIGHT
    SlideTextAlign.RIGHT -> SlideTextAlign.CENTER
}

@Composable
fun AddTextSlideDialog(
    canAddEnabledSlide: Boolean,
    existingSlide: AnnouncementSlide? = null,
    onSave: (
        title: String,
        body: String,
        durationSeconds: Int,
        enabled: Boolean,
        textAlign: SlideTextAlign,
        lines: List<SlideTextLine>
    ) -> Unit,
    onDismiss: () -> Unit,
    onUserInteraction: () -> Unit = {}
) {
    val isEditing = existingSlide != null
    val colorScheme = MaterialTheme.colorScheme
    var title by remember(existingSlide?.id) { mutableStateOf(existingSlide?.title ?: "") }
    var lines by remember(existingSlide?.id) {
        val initial = existingSlide?.effectiveLines()?.take(MAX_TEXT_SLIDE_LINES)
            ?.ifEmpty { listOf(SlideTextLine()) }
            ?: listOf(SlideTextLine())
        mutableStateOf(initial)
    }
    var durationSeconds by remember(existingSlide?.id) {
        mutableIntStateOf(existingSlide?.durationSeconds ?: 15)
    }
    var validationError by remember { mutableStateOf(false) }

    val titleFocusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        runCatching { titleFocusRequester.requestFocus() }
    }

    val canAddRow = lines.size < MAX_TEXT_SLIDE_LINES

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
                .padding(horizontal = 48.dp, vertical = 27.dp)
                .onPreviewKeyEvent {
                    onUserInteraction()
                    false
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .background(colorScheme.background, RoundedCornerShape(16.dp))
                    .border(2.dp, colorScheme.primary, RoundedCornerShape(16.dp))
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Edit else Icons.AutoMirrored.Filled.NoteAdd,
                        contentDescription = null,
                        tint = colorScheme.secondary,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = stringResource(
                            if (isEditing) AppR.string.settings_text_slide_edit_title
                            else AppR.string.settings_text_slide_dialog_title
                        ),
                        style = TvTypography.HeaderTitle.copy(fontSize = 24.sp, color = colorScheme.primary)
                    )
                }

                if (!isEditing && !canAddEnabledSlide) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorScheme.secondary.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                            .border(1.dp, colorScheme.secondary.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(AppR.string.settings_text_slide_limit_note),
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                        )
                    }
                }

                // Title input (1 row)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(AppR.string.settings_text_slide_title_label),
                        style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.secondary)
                    )
                    TvEditableTextField(
                        value = title,
                        onValueChange = {
                            onUserInteraction()
                            title = it
                            if (validationError) validationError = false
                        },
                        placeholder = stringResource(AppR.string.settings_text_slide_title_placeholder),
                        leadingIcon = Icons.Default.Edit,
                        accentUnfocusedBorder = true,
                        maxLength = ContentRepository.MAX_TITLE_LENGTH,
                        maxLines = 1,
                        modifier = Modifier.focusRequester(titleFocusRequester)
                    )
                }

                // Per-row Message Editor (up to 10 individually editable & alignable rows)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(AppR.string.settings_text_slide_body_label, MAX_TEXT_SLIDE_LINES),
                            style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.secondary)
                        )

                        val toolBtnShape = RoundedCornerShape(8.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                onClick = {
                                    onUserInteraction()
                                    if (canAddRow) {
                                        val lastAlign = lines.lastOrNull()?.align ?: SlideTextAlign.CENTER
                                        lines = lines + SlideTextLine("", lastAlign)
                                    }
                                },
                                enabled = canAddRow,
                                shape = ClickableSurfaceDefaults.shape(toolBtnShape),
                                scale = tvSurfaceScale(1.03f),
                                colors = tvSelectableSurfaceColors(selected = false),
                                border = tvSelectableSurfaceBorder(selected = false, shape = toolBtnShape)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (canAddRow) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = stringResource(AppR.string.settings_text_slide_new_line),
                                        style = TvTypography.BodyLabel.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (canAddRow) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    onUserInteraction()
                                    if (lines.lastOrNull()?.text?.isBlank() == true) {
                                        lines = lines.dropLast(1) + SlideTextLine("• ", SlideTextAlign.LEFT)
                                    } else if (canAddRow) {
                                        lines = lines + SlideTextLine("• ", SlideTextAlign.LEFT)
                                    }
                                    if (validationError) validationError = false
                                },
                                enabled = canAddRow || lines.lastOrNull()?.text?.isBlank() == true,
                                shape = ClickableSurfaceDefaults.shape(toolBtnShape),
                                scale = tvSurfaceScale(1.03f),
                                colors = tvSelectableSurfaceColors(selected = false),
                                border = tvSelectableSurfaceBorder(selected = false, shape = toolBtnShape)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = stringResource(AppR.string.settings_text_slide_bullet),
                                        style = TvTypography.BodyLabel.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    onUserInteraction()
                                    val rx = Regex("""^\s*(\d+)\.""")
                                    val nextNum = lines.mapNotNull { rx.find(it.text)?.groupValues?.get(1)?.toIntOrNull() }
                                        .maxOrNull()?.plus(1) ?: 1
                                    if (lines.lastOrNull()?.text?.isBlank() == true) {
                                        lines = lines.dropLast(1) + SlideTextLine("$nextNum. ", SlideTextAlign.LEFT)
                                    } else if (canAddRow) {
                                        lines = lines + SlideTextLine("$nextNum. ", SlideTextAlign.LEFT)
                                    }
                                    if (validationError) validationError = false
                                },
                                enabled = canAddRow || lines.lastOrNull()?.text?.isBlank() == true,
                                shape = ClickableSurfaceDefaults.shape(toolBtnShape),
                                scale = tvSurfaceScale(1.03f),
                                colors = tvSelectableSurfaceColors(selected = false),
                                border = tvSelectableSurfaceBorder(selected = false, shape = toolBtnShape)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatListNumbered,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = stringResource(AppR.string.settings_text_slide_numbered),
                                        style = TvTypography.BodyLabel.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                                    )
                                }
                            }
                        }
                    }

                    val rowBtnShape = RoundedCornerShape(8.dp)
                    lines.forEachIndexed { idx, line ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${idx + 1}",
                                textAlign = TextAlign.Center,
                                style = TvTypography.BodyLabel.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.secondary
                                ),
                                modifier = Modifier.width(22.dp)
                            )

                            TvEditableTextField(
                                value = line.text,
                                onValueChange = { newText ->
                                    onUserInteraction()
                                    val clean = newText.replace("\n", " ")
                                    lines = lines.mapIndexed { i, item ->
                                        if (i == idx) item.copy(text = clean) else item
                                    }
                                    if (validationError) validationError = false
                                },
                                placeholder = stringResource(AppR.string.settings_text_slide_body_placeholder),
                                accentUnfocusedBorder = true,
                                maxLength = MAX_LINE_CHARS,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )

                            // Per-row alignment cycle button (Left -> Right -> Center)
                            val alignIcon = when (line.align) {
                                SlideTextAlign.LEFT -> Icons.AutoMirrored.Filled.FormatAlignLeft
                                SlideTextAlign.CENTER -> Icons.Default.FormatAlignCenter
                                SlideTextAlign.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight
                            }
                            val alignLabelRes = when (line.align) {
                                SlideTextAlign.LEFT -> AppR.string.settings_text_slide_align_left
                                SlideTextAlign.CENTER -> AppR.string.settings_text_slide_align_center
                                SlideTextAlign.RIGHT -> AppR.string.settings_text_slide_align_right
                            }
                            Surface(
                                onClick = {
                                    onUserInteraction()
                                    lines = lines.mapIndexed { i, item ->
                                        if (i == idx) item.copy(align = nextAlignment(item.align)) else item
                                    }
                                },
                                shape = ClickableSurfaceDefaults.shape(rowBtnShape),
                                scale = tvSurfaceScale(1.03f),
                                colors = tvSelectableSurfaceColors(selected = false),
                                border = tvSelectableSurfaceBorder(selected = false, shape = rowBtnShape)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = alignIcon,
                                        contentDescription = stringResource(alignLabelRes),
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(alignLabelRes),
                                        style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface)
                                    )
                                }
                            }

                            if (lines.size > 1) {
                                Surface(
                                    onClick = {
                                        onUserInteraction()
                                        lines = lines.filterIndexed { i, _ -> i != idx }
                                    },
                                    shape = ClickableSurfaceDefaults.shape(rowBtnShape),
                                    scale = tvSurfaceScale(1.03f),
                                    colors = tvSelectableSurfaceColors(selected = false),
                                    border = tvSelectableSurfaceBorder(selected = false, shape = rowBtnShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.btn_delete),
                                        tint = colorScheme.error,
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Duration presets
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(AppR.string.settings_text_slide_duration_label),
                        style = TvTypography.CardTitle.copy(fontSize = 15.sp, color = colorScheme.secondary)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val durations = listOf(10, 15, 20, 30, 45, 60)
                        val durShape = RoundedCornerShape(8.dp)
                        for (dur in durations) {
                            val isSelected = durationSeconds == dur
                            Surface(
                                onClick = {
                                    onUserInteraction()
                                    durationSeconds = dur
                                },
                                shape = ClickableSurfaceDefaults.shape(durShape),
                                scale = tvSurfaceScale(1.03f),
                                colors = tvSelectableSurfaceColors(selected = isSelected),
                                border = tvSelectableSurfaceBorder(selected = isSelected, shape = durShape)
                            ) {
                                Text(
                                    text = "${dur}s",
                                    style = TvTypography.ButtonText.copy(
                                        fontSize = 13.sp,
                                        color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                if (validationError) {
                    Text(
                        text = stringResource(AppR.string.settings_text_slide_empty_error),
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.error
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val actionShape = RoundedCornerShape(8.dp)
                    Surface(
                        onClick = onDismiss,
                        shape = ClickableSurfaceDefaults.shape(actionShape),
                        scale = tvSurfaceScale(1.03f),
                        colors = tvSelectableSurfaceColors(selected = false),
                        border = tvSelectableSurfaceBorder(selected = false, shape = actionShape)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_cancel),
                            style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = colorScheme.onSurface),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        onClick = {
                            val cleanTitle = title.trim()
                            val cleanLines = lines
                                .map { it.copy(text = it.text.trimEnd()) }
                                .filter { it.text.isNotBlank() }
                            val joinedBody = cleanLines.joinToString("\n") { it.text }
                            val primaryAlign = cleanLines.firstOrNull()?.align ?: SlideTextAlign.CENTER
                            if (cleanTitle.isEmpty() && cleanLines.isEmpty()) {
                                validationError = true
                            } else {
                                onSave(
                                    cleanTitle,
                                    joinedBody,
                                    durationSeconds,
                                    existingSlide?.enabled ?: canAddEnabledSlide,
                                    primaryAlign,
                                    cleanLines
                                )
                            }
                        },
                        shape = ClickableSurfaceDefaults.shape(actionShape),
                        scale = tvSurfaceScale(1.03f),
                        colors = tvSelectableSurfaceColors(selected = true),
                        border = tvSelectableSurfaceBorder(selected = true, shape = actionShape)
                    ) {
                        Text(
                            text = stringResource(
                                if (isEditing) AppR.string.settings_slide_save_changes
                                else AppR.string.settings_text_slide_save
                            ),
                            style = TvTypography.ButtonText.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
