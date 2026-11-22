// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import coil3.compose.AsyncImage
import com.salatsign.core.tv.R
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import java.io.File

@Composable
fun MosqueEmblemIcon(
    preset: MosqueIconPreset,
    customLogoUri: String?,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    if (preset == MosqueIconPreset.CUSTOM && !customLogoUri.isNullOrBlank()) {
        val context = LocalContext.current
        val model = remember(customLogoUri) {
            val localFile = File(File(context.filesDir, "content/logo"), customLogoUri)
            if (localFile.exists()) localFile else customLogoUri
        }
        AsyncImage(
            model = model,
            contentDescription = stringResource(R.string.mosque_icon_custom),
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentScale = ContentScale.Crop
        )
    } else {
        val circleBg = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
            Color(0xFF0A241A)
        } else {
            AccentEmerald.copy(alpha = 0.12f)
        }
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(circleBg)
                .border(1.dp, AccentEmerald.copy(alpha = 0.30f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val iconSize = (size.value * 0.58f).dp
            when (preset) {
                MosqueIconPreset.DOME_CRESCENT -> {
                    Image(
                        painter = painterResource(id = R.drawable.ic_mosque_dome),
                        contentDescription = stringResource(preset.labelRes()),
                        modifier = Modifier.size((size.value * 0.72f).dp)
                    )
                }
                MosqueIconPreset.MINARET -> {
                    Image(
                        painter = painterResource(id = R.drawable.ic_mosque_minaret),
                        contentDescription = stringResource(preset.labelRes()),
                        modifier = Modifier.size((size.value * 0.72f).dp)
                    )
                }
                MosqueIconPreset.MIHRAB -> {
                    Image(
                        painter = painterResource(id = R.drawable.ic_mosque_mihrab),
                        contentDescription = stringResource(preset.labelRes()),
                        modifier = Modifier.size((size.value * 0.72f).dp)
                    )
                }
                MosqueIconPreset.CUSTOM -> {
                    if (!customLogoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = customLogoUri,
                            contentDescription = stringResource(R.string.mosque_icon_custom),
                            modifier = Modifier
                                .size(size)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = stringResource(preset.labelRes()),
                            tint = AccentGold,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }
        }
    }
}

fun getMosqueFontFamily(preset: MosqueFontPreset): FontFamily {
    return when (preset) {
        MosqueFontPreset.MODERN_SANS -> FontFamily.SansSerif
        MosqueFontPreset.TRADITIONAL_SERIF -> FontFamily.Serif
        MosqueFontPreset.ELEGANT_DISPLAY -> FontFamily.Serif
        MosqueFontPreset.CLEAN_TECH -> FontFamily.Monospace
    }
}

fun getMosqueTitleStyle(preset: MosqueFontPreset, color: Color): TextStyle {
    return when (preset) {
        MosqueFontPreset.MODERN_SANS -> TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 36.sp,
            letterSpacing = 0.sp,
            color = color
        )
        MosqueFontPreset.TRADITIONAL_SERIF -> TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 38.sp,
            letterSpacing = 0.5.sp,
            color = color
        )
        MosqueFontPreset.ELEGANT_DISPLAY -> TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            fontSize = 36.sp,
            letterSpacing = 1.sp,
            color = color
        )
        MosqueFontPreset.CLEAN_TECH -> TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            letterSpacing = 0.25.sp,
            color = color
        )
    }
}
