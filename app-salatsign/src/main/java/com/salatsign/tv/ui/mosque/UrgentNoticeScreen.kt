// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.mosque

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.salatsign.tv.security.UrgentNoticeRecord
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.theme.TvTypography
import java.util.Locale

/**
 * Full-screen high-contrast Urgent Notice display (Phase 3c, D24).
 * Overrides Prayer Schedule & Slideshow, but yields to Silent Screen during Jumu'ah / prayer.
 * Strictly visual-only (Decision D18).
 */
@Composable
fun UrgentNoticeScreen(
    notice: UrgentNoticeRecord,
    remainingSeconds: Long
) {
    val cardBg = Color(0xFF261A08)
    val accent = Color(0xFFF59E0B)
    val dismissTextColor = Color(0xFFE5E7EB)
    val cardShape = RoundedCornerShape(24.dp)

    val clampedSec = remainingSeconds.coerceAtLeast(0L)
    val mins = clampedSec / 60L
    val secs = clampedSec % 60L
    val countdownLabel = String.format(Locale.US, "Dismiss in %d:%02d", mins, secs)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .overscanPadding()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.88f)
                .background(cardBg, cardShape)
                .border(3.dp, accent, cardShape)
                .padding(horizontal = 48.dp, vertical = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = notice.title.ifBlank { "Urgent Notice" }.uppercase(),
                    style = TvTypography.HeaderTitle.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        color = accent
                    )
                )
            }

            Text(
                text = notice.message,
                style = TvTypography.ActivePrayerTime.copy(
                    fontSize = 46.sp,
                    lineHeight = 58.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )
        }

        // Countdown in the bottom-right corner outside the yellow border
        Text(
            text = countdownLabel,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 2.dp),
            style = TvTypography.BodyLabel.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = dismissTextColor
            )
        )
    }
}
