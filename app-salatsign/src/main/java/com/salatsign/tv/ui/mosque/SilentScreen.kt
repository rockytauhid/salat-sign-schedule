// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.mosque

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MobileOff
import androidx.compose.material.icons.filled.VoiceOverOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.salatsign.tv.R
import com.salatsign.tv.data.model.SilentWindowKind
import com.salatsign.tv.ui.common.overscanPadding
import kotlinx.coroutines.delay

/**
 * Centralised palette and typography for [SilentScreen].
 * Always dark (even when the mosque uses Light Theme during the day) so it does not glare
 * or draw unnecessary attention during Jumu'ah or congregational prayer, while preserving
 * >= 7:1 WCAG AAA contrast against the background.
 */
object SilentScreenPalette {
    val Background: Color = Color(0xFF0E1110) // Near-black charcoal, not pure #000
    val TitleColor: Color = Color(0xFFD8DFDC) // ~14.1:1 contrast vs Background
    val IconColor: Color = Color(0xFFC8D2CE)  // ~12.0:1 contrast vs Background
    val LabelColor: Color = Color(0xFFBFC8C4) // ~10.8:1 contrast vs Background

    val IconSize = 96.dp

    val TitleStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 42.sp,
        lineHeight = 50.sp,
        letterSpacing = 0.5.sp,
        color = TitleColor,
        textAlign = TextAlign.Center
    )

    val LabelStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        color = LabelColor,
        textAlign = TextAlign.Center
    )

    // Subtle pixel-level offsets (<= 8 px) cycled every 60 seconds for burn-in / image-retention protection
    val BurnInOffsetsPx: List<IntOffset> = listOf(
        IntOffset(0, 0),
        IntOffset(6, 0),
        IntOffset(6, 6),
        IntOffset(0, 6),
        IntOffset(-6, 6),
        IntOffset(-6, 0),
        IntOffset(-6, -6),
        IntOffset(0, -6)
    )
}

/**
 * Plain, non-distracting Mosque Silent Screen shown during Jumu'ah, configured daily prayers,
 * or manual override. No gradients, clock, logo, ornaments, or animations.
 */
@Composable
fun SilentScreen(
    kind: SilentWindowKind,
    modifier: Modifier = Modifier
) {
    var offsetStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            offsetStep = (offsetStep + 1) % SilentScreenPalette.BurnInOffsetsPx.size
        }
    }

    val pixelOffset = SilentScreenPalette.BurnInOffsetsPx[offsetStep]
    val titleText = when (kind) {
        SilentWindowKind.JUMUAH -> stringResource(R.string.silent_title_jumuah)
        SilentWindowKind.PRAYER, SilentWindowKind.MANUAL -> stringResource(R.string.silent_title_prayer)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SilentScreenPalette.Background)
            .overscanPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { pixelOffset },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = titleText,
                style = SilentScreenPalette.TitleStyle,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(44.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.76f),
                horizontalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top
            ) {
                SilentInstructionItem(
                    icon = Icons.Filled.VoiceOverOff,
                    iconContentDescription = stringResource(R.string.silent_icon_no_talking_desc),
                    label = stringResource(R.string.silent_no_talking),
                    modifier = Modifier.weight(1f)
                )

                SilentInstructionItem(
                    icon = Icons.Filled.MobileOff,
                    iconContentDescription = stringResource(R.string.silent_icon_phone_desc),
                    label = stringResource(R.string.silent_phone),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SilentInstructionItem(
    icon: ImageVector,
    iconContentDescription: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconContentDescription,
            tint = SilentScreenPalette.IconColor,
            modifier = Modifier.size(SilentScreenPalette.IconSize)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = label,
            style = SilentScreenPalette.LabelStyle,
            maxLines = 2
        )
    }
}
