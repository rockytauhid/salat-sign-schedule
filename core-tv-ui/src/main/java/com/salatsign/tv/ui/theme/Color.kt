// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.theme

import androidx.compose.ui.graphics.Color

// ================= Dark Palette (OLED / Night) =================
val DarkBackground = Color(0xFF08140E)
val DarkSurface = Color(0xFF11261B)
val DarkSurfaceVariant = Color(0xFF173325)
val DarkTextPrimary = Color(0xFFF9FAFB)
val DarkTextSecondary = Color(0xFF9CA3AF)
val DarkDivider = Color(0xFF1E3A2B)

// ================= Light Palette (Muted Sage-Stone / Low-Glare TV Day) =================
val LightBackground = Color(0xFFC2CFC8)
val LightSurface = Color(0xFFD2DDD6)
val LightSurfaceVariant = Color(0xFFB0C3B8)
val LightTextPrimary = Color(0xFF0A1D15)
val LightTextSecondary = Color(0xFF233B30)
val LightDivider = Color(0xFF93AA9D)

// ================= Brand Accents =================
val AccentEmerald = Color(0xFF10B981)
val AccentEmeraldDark = Color(0xFF046347)
val EmeraldLight = Color(0xFF34D399)
val AccentGold = Color(0xFFFBBF24)
val AccentGoldDark = Color(0xFF734000) // Deep antique bronze (>=4.7:1 contrast on LightSurface & 18% chip tint)
val ActivePrayerPulse = Color(0xFF059669)
val PulseRed = Color(0xFFEF4444)

// Backwards compatibility aliases
val CardBackground = DarkSurface
val CardFocusedBorder = AccentEmerald
val SurfaceVariant = DarkSurfaceVariant
val TextPrimary = DarkTextPrimary
val TextSecondary = DarkTextSecondary
val DividerColor = DarkDivider
