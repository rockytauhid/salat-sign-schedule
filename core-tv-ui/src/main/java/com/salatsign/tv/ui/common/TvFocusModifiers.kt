// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.CardFocusedBorder

/**
 * Android TV 10-foot UI Overscan Padding:
 * Horizontal: 48.dp
 * Vertical: 27.dp
 */
fun Modifier.overscanPadding(): Modifier =
    this.padding(
        horizontal = 48.dp,
        vertical = 27.dp
    )

/**
 * Standard TV focusable element scaling (1.05x) with high-contrast border.
 */
fun Modifier.tvFocusable(
    shape: Shape = RoundedCornerShape(12.dp),
    focusedScale: Float = 1.05f,
    focusedBorderColor: Color = CardFocusedBorder,
    focusedBorderWidth: Dp = 2.5.dp
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "tv_focus_scale"
    )

    this
        .onFocusChanged { isFocused = it.isFocused }
        .scale(scale)
        .then(
            if (isFocused) {
                Modifier.border(width = focusedBorderWidth, color = focusedBorderColor, shape = shape)
            } else {
                Modifier
            }
        )
}

/**
 * Pulsing glow border modifier for active prayer highlight.
 */
@Composable
fun Modifier.pulsingGlow(
    shape: Shape = RoundedCornerShape(16.dp),
    glowColor: Color = AccentEmerald
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    return this.border(
        width = 3.dp,
        color = glowColor.copy(alpha = alpha),
        shape = shape
    )
}
