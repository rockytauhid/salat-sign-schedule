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
import androidx.compose.foundation.BorderStroke
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
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.MaterialTheme
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
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
 * Standard TV focusable modifier hook.
 * Note: androidx.tv.material3 Surface, Card, and Button already apply native TV focus
 * scaling and a single shape-matched focus border; adding a second border/scale here
 * causes double borders on focused panels.
 */
fun Modifier.tvFocusable(
    shape: Shape = RoundedCornerShape(12.dp),
    focusedScale: Float = 1.05f,
    focusedBorderColor: Color = CardFocusedBorder,
    focusedBorderWidth: Dp = 2.5.dp
): Modifier = this

/**
 * Single-border focus/selection styling for androidx.tv.material3.Card.
 * Prevents double borders when a Card is both selected and focused.
 */
@Composable
fun tvSelectableCardBorder(
    selected: Boolean,
    shape: Shape = RoundedCornerShape(12.dp)
): CardBorder = CardDefaults.border(
    border = if (selected) {
        Border(border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape = shape)
    } else {
        Border.None
    },
    focusedBorder = Border(border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.secondary), shape = shape)
)

/**
 * Unified single-border focus/selection styling for androidx.tv.material3.Surface.
 */
@Composable
fun tvSelectableSurfaceBorder(
    selected: Boolean = false,
    shape: Shape = RoundedCornerShape(10.dp)
): ClickableSurfaceBorder = ClickableSurfaceDefaults.border(
    border = if (selected) {
        Border(border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape = shape)
    } else {
        Border.None
    },
    focusedBorder = Border(border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.secondary), shape = shape)
)

/**
 * Controlled focus scale for TV Cards (overrides androidx.tv.material3's aggressive 1.1x default
 * so cards never enlarge outside the viewport or clip into neighbors).
 * Use 1.0f for full-width banners and 1.02f–1.03f for multi-column tiles.
 */
@Composable
fun tvCardScale(focusedScale: Float = 1.02f): CardScale =
    CardDefaults.scale(focusedScale = focusedScale)

/**
 * Controlled focus scale for TV Surfaces so buttons/chips never enlarge off-screen.
 */
@Composable
fun tvSurfaceScale(focusedScale: Float = 1.03f): ClickableSurfaceScale =
    ClickableSurfaceDefaults.scale(focusedScale = focusedScale)

/**
 * Unified high-contrast colors for androidx.tv.material3.Surface in Settings/Wizards.
 * Keeps focused surfaces dark emerald (surfaceVariant) so white/light text stays 100% legible.
 */
@Composable
fun tvSelectableSurfaceColors(
    selected: Boolean = false,
    containerColor: Color? = null
): ClickableSurfaceColors {
    val scheme = MaterialTheme.colorScheme
    val base = containerColor ?: if (selected) scheme.surfaceVariant else scheme.surface
    return ClickableSurfaceDefaults.colors(
        containerColor = base,
        focusedContainerColor = scheme.surfaceVariant,
        contentColor = scheme.onSurface,
        focusedContentColor = scheme.onSurface
    )
}

/**
 * Single-border styling for Prayer Cards:
 * - Active prayer: pulsing emerald border when unfocused, single gold border when focused.
 * - Next prayer: crisp gold border when unfocused, single brighter gold border when focused.
 * Never stacks a second border on top of Card's native focus border.
 */
@Composable
fun tvPrayerCardBorder(
    isActive: Boolean,
    isNext: Boolean,
    shape: Shape = RoundedCornerShape(16.dp)
): CardBorder {
    val pulseAlpha = if (isActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "prayer_card_pulse")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "prayer_card_pulse_alpha"
        )
        animated
    } else {
        1.0f
    }

    val unfocusedBorder = when {
        isActive -> Border(border = BorderStroke(3.dp, AccentEmerald.copy(alpha = pulseAlpha)), shape = shape)
        isNext -> Border(border = BorderStroke(2.dp, AccentGold.copy(alpha = 0.85f)), shape = shape)
        else -> Border.None
    }

    return CardDefaults.border(
        border = unfocusedBorder,
        focusedBorder = Border(border = BorderStroke(2.5.dp, AccentGold), shape = shape)
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
