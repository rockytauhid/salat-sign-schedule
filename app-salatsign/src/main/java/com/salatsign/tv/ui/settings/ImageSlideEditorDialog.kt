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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Usb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import coil3.compose.AsyncImage
import com.salatsign.core.tv.R
import com.salatsign.tv.R as AppR
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.repository.ContentRepository
import com.salatsign.tv.ui.common.TvEditableTextField
import com.salatsign.tv.ui.common.tvSelectableSurfaceBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceColors
import com.salatsign.tv.ui.common.tvSurfaceScale
import com.salatsign.tv.ui.theme.TvTypography
import java.io.File

@Composable
fun ImageSlideEditorDialog(
    imageFileName: String,
    canAddEnabledSlide: Boolean,
    existingSlide: AnnouncementSlide? = null,
    onRequestChangeImage: () -> Unit,
    onSave: (title: String, imageFile: String, durationSeconds: Int) -> Unit,
    onDismiss: () -> Unit,
    onUserInteraction: () -> Unit = {}
) {
    val isEditing = existingSlide != null
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val defaultTitle = remember(existingSlide?.id, imageFileName) {
        existingSlide?.title?.ifBlank { null }
            ?: imageFileName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim().ifBlank { "Poster" }
    }
    var title by remember(existingSlide?.id) { mutableStateOf(defaultTitle) }
    var durationSeconds by remember(existingSlide?.id) {
        mutableIntStateOf(existingSlide?.durationSeconds ?: 15)
    }

    val titleFocusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        runCatching { titleFocusRequester.requestFocus() }
    }

    val previewFile = remember(imageFileName) {
        File(context.filesDir, "content/images/$imageFileName")
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
                .padding(horizontal = 48.dp, vertical = 27.dp)
                .onPreviewKeyEvent {
                    onUserInteraction()
                    false
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 780.dp)
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
                        imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = stringResource(
                            if (isEditing) AppR.string.settings_image_slide_edit_title
                            else AppR.string.settings_image_slide_dialog_title
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

                // Image Preview & Change Image button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorScheme.surface)
                            .border(1.dp, colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewFile.exists()) {
                            AsyncImage(
                                model = previewFile,
                                contentDescription = title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = imageFileName,
                            maxLines = 2,
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 13.sp,
                                color = colorScheme.onSurface.copy(alpha = 0.78f)
                            )
                        )

                        val changeBtnShape = RoundedCornerShape(8.dp)
                        Surface(
                            onClick = {
                                onUserInteraction()
                                onRequestChangeImage()
                            },
                            shape = ClickableSurfaceDefaults.shape(changeBtnShape),
                            scale = tvSurfaceScale(1.03f),
                            colors = tvSelectableSurfaceColors(selected = false),
                            border = tvSelectableSurfaceBorder(selected = false, shape = changeBtnShape)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Usb,
                                    contentDescription = null,
                                    tint = colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(AppR.string.settings_image_slide_change_file),
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                // Title input
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
                        },
                        placeholder = stringResource(AppR.string.settings_image_slide_title_placeholder),
                        leadingIcon = Icons.Default.Edit,
                        accentUnfocusedBorder = true,
                        maxLength = ContentRepository.MAX_TITLE_LENGTH,
                        maxLines = 1,
                        modifier = Modifier.focusRequester(titleFocusRequester)
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
                            val cleanTitle = title.trim().ifBlank { "Poster" }
                            onSave(cleanTitle, imageFileName, durationSeconds)
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
