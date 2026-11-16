package com.salatsign.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val TvColorScheme: ColorScheme = darkColorScheme(
    primary = AccentEmerald,
    onPrimary = DarkBackground,
    primaryContainer = CardBackground,
    onPrimaryContainer = TextPrimary,
    secondary = AccentGold,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    border = CardFocusedBorder
)

@Composable
fun SalatSignTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvColorScheme,
        content = content
    )
}
