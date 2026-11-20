// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object TvTypography {
    val ActivePrayerTime = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 72.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary
    )

    val InactivePrayerTime = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        letterSpacing = 0.sp,
        color = TextPrimary
    )

    val HeaderTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        letterSpacing = 0.5.sp,
        color = TextPrimary
    )

    val Subtitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        letterSpacing = 0.25.sp,
        color = TextSecondary
    )

    val CardTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        letterSpacing = 0.15.sp,
        color = TextPrimary
    )

    val ArabicTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        letterSpacing = 0.5.sp,
        color = AccentGold
    )

    val CountdownText = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        letterSpacing = 1.sp,
        color = AccentGold
    )

    val BodyLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        letterSpacing = 0.15.sp,
        color = TextSecondary
    )

    val ButtonText = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.5.sp,
        color = TextPrimary
    )
}
