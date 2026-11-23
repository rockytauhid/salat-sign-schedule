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
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
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
import com.salatsign.tv.data.repository.ContentRepository
import com.salatsign.tv.ui.common.TvEditableTextField
import com.salatsign.tv.ui.common.tvSelectableSurfaceBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceColors
import com.salatsign.tv.ui.common.tvSurfaceScale
import com.salatsign.tv.ui.theme.TvTypography

const val MAX_TEXT_SLIDE_LINES = 6

@Composable
fun AddTextSlideDialog(
    canAddEnabledSlide: Boolean,
    existingSlide: AnnouncementSlide? = null,
    onSave: (title: String, body: String, durationSeconds: Int, enabled: Boolean) -> Unit,
    onDismiss: () -> Unit,
    onUserInteraction: () -> Unit = {}
) {
    val isEditing = existingSlide != null
    val colorScheme = MaterialTheme.colorScheme
    var title by remember(existingSlide?.id) { mutableStateOf(existingSlide?.title ?: "") }
    var body by remember(existingSlide?.id) { mutableStateOf(existingSlide?.body ?: "") }
    var durationSeconds by remember(existingSlide?.id) {
        mutableIntStateOf(existingSlide?.durationSeconds ?: 15)
    }
    var validationError by remember { mutableStateOf(false) }

    val titleFocusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        runCatching { titleFocusRequester.requestFocus() }
    }

    val currentLines = if (body.isEmpty()) 1 else body.count { it == '\n' } + 1
    val canAddNewLine = currentLines < MAX_TEXT_SLIDE_LINES && body.length < ContentRepository.MAX_BODY_LENGTH

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
                    .widthIn(max = 820.dp)
                    .fillMaxWidth()
                    .background(colorScheme.background, RoundedCornerShape(16.dp))
                    .border(2.dp, colorScheme.primary, RoundedCornerShape(16.dp))
                    .verticalScroll(scrollState)
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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

                // Multi-line Body input (up to MAX_TEXT_SLIDE_LINES rows)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(AppR.string.settings_text_slide_body_label, MAX_TEXT_SLIDE_LINES),
                            style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.secondary)
                        )

                        val newLineBtnShape = RoundedCornerShape(8.dp)
                        Surface(
                            onClick = {
                                onUserInteraction()
                                if (canAddNewLine) {
                                    body = "$body\n"
                                    if (validationError) validationError = false
                                }
                            },
                            shape = ClickableSurfaceDefaults.shape(newLineBtnShape),
                            scale = tvSurfaceScale(1.03f),
                            colors = tvSelectableSurfaceColors(selected = false),
                            border = tvSelectableSurfaceBorder(selected = false, shape = newLineBtnShape)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SubdirectoryArrowLeft,
                                    contentDescription = null,
                                    tint = if (canAddNewLine) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(AppR.string.settings_text_slide_new_line),
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (canAddNewLine) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(AppR.string.settings_text_slide_body_hint),
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 12.sp,
                            color = colorScheme.onSurface.copy(alpha = 0.72f)
                        )
                    )

                    TvEditableTextField(
                        value = body,
                        onValueChange = {
                            onUserInteraction()
                            body = it
                            if (validationError) validationError = false
                        },
                        placeholder = stringResource(AppR.string.settings_text_slide_body_placeholder),
                        leadingIcon = Icons.Default.Edit,
                        accentUnfocusedBorder = true,
                        maxLength = ContentRepository.MAX_BODY_LENGTH,
                        maxLines = MAX_TEXT_SLIDE_LINES,
                        minLines = 4
                    )
                }

                // Duration presets
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(AppR.string.settings_text_slide_duration_label),
                        style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.secondary)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                        fontSize = 14.sp,
                                        color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                                    ),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
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
                            val cleanBody = body.trim()
                            if (cleanTitle.isEmpty() && cleanBody.isEmpty()) {
                                validationError = true
                            } else {
                                onSave(
                                    cleanTitle,
                                    cleanBody,
                                    durationSeconds,
                                    existingSlide?.enabled ?: canAddEnabledSlide
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
