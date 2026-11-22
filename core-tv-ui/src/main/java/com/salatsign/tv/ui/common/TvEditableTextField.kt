// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.TvTypography

/**
 * Android TV D-pad safe editable text field.
 *
 * In browse mode (default), renders as a normal TV Surface so D-pad Up/Down/Left/Right
 * glides right past it without popping up the soft keyboard or trapping focus.
 * Pressing OK/Enter (or clicking) enters edit mode, focuses the BasicTextField, and
 * shows the on-screen keyboard. Pressing Done or Back exits edit mode cleanly.
 */
@Composable
fun TvEditableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector = Icons.Default.Edit,
    accentUnfocusedBorder: Boolean = false,
    maxLength: Int? = null,
    textStyle: TextStyle = TvTypography.BodyLabel.copy(fontSize = 16.sp),
    trailingContent: (@Composable () -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    var isEditing by remember { mutableStateOf(false) }
    var restoreSurfaceFocus by remember { mutableStateOf(false) }
    val surfaceFocusRequester = remember { FocusRequester() }
    val textFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    if (isEditing) {
        LaunchedEffect(Unit) {
            runCatching { textFocusRequester.requestFocus() }
            keyboardController?.show()
        }

        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(colorScheme.surfaceVariant, shape)
                .border(2.5.dp, AccentGold, shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = AccentGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 14.sp,
                            color = colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = { next ->
                        onValueChange(if (maxLength != null) next.take(maxLength) else next)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            restoreSurfaceFocus = true
                            isEditing = false
                        }
                    ),
                    textStyle = textStyle.copy(color = colorScheme.onSurface),
                    cursorBrush = SolidColor(AccentGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(textFocusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown &&
                                (event.key == Key.Back || event.key == Key.Escape)
                            ) {
                                keyboardController?.hide()
                                restoreSurfaceFocus = true
                                isEditing = false
                                true
                            } else {
                                false
                            }
                        }
                )
            }
            if (maxLength != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${value.length}/$maxLength",
                    style = TvTypography.BodyLabel.copy(
                        fontSize = 12.sp,
                        color = if (value.length >= maxLength) AccentGold else colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            }
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingContent()
            }
        }
    } else {
        LaunchedEffect(restoreSurfaceFocus) {
            if (restoreSurfaceFocus) {
                restoreSurfaceFocus = false
                runCatching { surfaceFocusRequester.requestFocus() }
            }
        }

        val unfocusedBorderColor = if (accentUnfocusedBorder) {
            AccentEmerald.copy(alpha = 0.65f)
        } else {
            colorScheme.onSurface.copy(alpha = 0.25f)
        }

        Surface(
            onClick = { isEditing = true },
            shape = ClickableSurfaceDefaults.shape(shape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
            colors = tvSelectableSurfaceColors(selected = false),
            border = ClickableSurfaceDefaults.border(
                border = Border(
                    border = BorderStroke(if (accentUnfocusedBorder) 1.5.dp else 1.dp, unfocusedBorderColor),
                    shape = shape
                ),
                focusedBorder = Border(border = BorderStroke(2.5.dp, AccentGold), shape = shape)
            ),
            modifier = modifier
                .fillMaxWidth()
                .focusRequester(surfaceFocusRequester)
                .pointerInput(Unit) { detectTapGestures { isEditing = true } }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (accentUnfocusedBorder) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.65f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 14.sp,
                                color = colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        )
                    } else {
                        Text(
                            text = value,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = textStyle.copy(color = colorScheme.onSurface)
                        )
                    }
                }
                if (maxLength != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${value.length}/$maxLength",
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 12.sp,
                            color = if (value.length >= maxLength) AccentGold else colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    )
                }
                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingContent()
                }
            }
        }
    }
}
