// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.mosque

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

/**
 * Full-screen high-contrast Urgent Notice display (Phase 3c, D24).
 * Overrides Prayer Schedule & Slideshow, but yields to Silent Screen during Jumu'ah / prayer.
 * Strictly visual-only (Decision D18).
 */
@Composable
fun UrgentNoticeScreen(
    notice: UrgentNoticeRecord,
    remainingMinutes: Long
) {
    val bg = Color(0xFF1A1205)
    val cardBg = Color(0xFF261A08)
    val accent = Color(0xFFF59E0B)
    val accentSoft = Color(0xFFFDE68A)
    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .overscanPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(cardBg, cardShape)
                .border(3.dp, accent, cardShape)
                .padding(horizontal = 48.dp, vertical = 40.dp),
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

            Spacer(modifier = Modifier.height(6.dp))

            val remLabel = if (remainingMinutes <= 1L) "Clears in < 1 min" else "Clears in $remainingMinutes min"
            Box(
                modifier = Modifier
                    .background(accent.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                    .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 22.dp, vertical = 8.dp)
            ) {
                Text(
                    text = remLabel,
                    style = TvTypography.BodyLabel.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentSoft
                    )
                )
            }
        }
    }
}
