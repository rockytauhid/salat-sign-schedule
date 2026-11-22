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

// ================= Light Palette (Warm Soft Sage / Low-Glare Day) =================
val LightBackground = Color(0xFFE4ECE7)
val LightSurface = Color(0xFFF1F5F2)
val LightSurfaceVariant = Color(0xFFD2E2D8)
val LightTextPrimary = Color(0xFF0B1F17)
val LightTextSecondary = Color(0xFF3A5248)
val LightDivider = Color(0xFFB8CEC1)

// ================= Brand Accents =================
val AccentEmerald = Color(0xFF10B981)
val AccentEmeraldDark = Color(0xFF047857)
val EmeraldLight = Color(0xFF34D399)
val AccentGold = Color(0xFFFBBF24)
val AccentGoldDark = Color(0xFF9A5B04) // Rich antique bronze (>5.5:1 contrast on light surfaces)
val ActivePrayerPulse = Color(0xFF059669)
val PulseRed = Color(0xFFEF4444)

// Backwards compatibility aliases
val CardBackground = DarkSurface
val CardFocusedBorder = AccentEmerald
val SurfaceVariant = DarkSurfaceVariant
val TextPrimary = DarkTextPrimary
val TextSecondary = DarkTextSecondary
val DividerColor = DarkDivider
