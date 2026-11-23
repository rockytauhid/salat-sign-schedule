// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.mosque

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.salatsign.tv.R
import com.salatsign.tv.data.model.QuranVerseItem
import com.salatsign.tv.ui.common.overscanPadding

private const val TAG = "QuranVerseSlide"

val AmiriQuranFontFamily: FontFamily = FontFamily(
    Font(resId = R.font.amiri_quran, weight = FontWeight.Normal)
)

data class QuranVerseFitLayout(
    val fits: Boolean,
    val arabicFontSp: Float,
    val translationFontSp: Float
)

private val ARABIC_CANDIDATE_SP = listOf(56f, 52f, 48f, 44f, 40f)
private val TRANSLATION_CANDIDATE_SP = listOf(32f, 29f, 26f, 24f)
private const val REFERENCE_FONT_SP = 24f

/**
 * Measures the largest (arabicSp, translationSp) pair that fits completely inside [maxWidthPx] x [maxHeightPx]
 * without ever truncating, ellipsising, or shrinking below the minimum 40sp Arabic / 24sp translation bounds.
 */
fun measureQuranVerseFit(
    textMeasurer: TextMeasurer,
    verse: QuranVerseItem,
    referenceLabel: String,
    showTranslation: Boolean,
    maxWidthPx: Int,
    maxHeightPx: Int,
    arabicGapPx: Int,
    refGapPx: Int
): QuranVerseFitLayout {
    if (maxWidthPx <= 0 || maxHeightPx <= 0) {
        return QuranVerseFitLayout(fits = true, arabicFontSp = 48f, translationFontSp = 28f)
    }

    val widthConstraints = Constraints(maxWidth = maxWidthPx)
    val refHeightPx = textMeasurer.measure(
        text = referenceLabel,
        style = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = REFERENCE_FONT_SP.sp,
            lineHeight = (REFERENCE_FONT_SP * 1.3f).sp,
            textAlign = TextAlign.Center
        ),
        constraints = widthConstraints
    ).size.height

    val hasTranslation = showTranslation && verse.translationText.isNotEmpty()
    val translationCandidates = if (hasTranslation) TRANSLATION_CANDIDATE_SP else listOf(0f)

    // Try largest balanced pairs first; never go below 40sp Arabic or 24sp translation
    val pairs = buildList {
        val maxIdx = maxOf(ARABIC_CANDIDATE_SP.lastIndex, translationCandidates.lastIndex)
        for (step in 0..maxIdx) {
            val ar = ARABIC_CANDIDATE_SP[step.coerceAtMost(ARABIC_CANDIDATE_SP.lastIndex)]
            val tr = translationCandidates[step.coerceAtMost(translationCandidates.lastIndex)]
            add(ar to tr)
        }
        // Final exhaustive pass over smaller combinations down to (40sp, 24sp)
        for (ar in ARABIC_CANDIDATE_SP) {
            for (tr in translationCandidates) {
                if ((ar to tr) !in this) add(ar to tr)
            }
        }
    }

    for ((arSp, trSp) in pairs) {
        val arHeight = textMeasurer.measure(
            text = verse.arabicText,
            style = TextStyle(
                fontFamily = AmiriQuranFontFamily,
                fontSize = arSp.sp,
                lineHeight = (arSp * 1.85f).sp,
                textAlign = TextAlign.Center,
                textDirection = TextDirection.Rtl
            ),
            constraints = widthConstraints
        ).size.height

        val trHeight = if (hasTranslation) {
            textMeasurer.measure(
                text = verse.translationText,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = trSp.sp,
                    lineHeight = (trSp * 1.38f).sp,
                    textAlign = TextAlign.Center,
                    textDirection = TextDirection.Ltr
                ),
                constraints = widthConstraints
            ).size.height
        } else {
            0
        }

        val totalHeight = arHeight +
            (if (hasTranslation) arabicGapPx + trHeight else 0) +
            refGapPx +
            refHeightPx

        if (totalHeight <= maxHeightPx) {
            return QuranVerseFitLayout(fits = true, arabicFontSp = arSp, translationFontSp = trSp)
        }
    }

    return QuranVerseFitLayout(fits = false, arabicFontSp = 40f, translationFontSp = 24f)
}

/**
 * Plain, reverent single-screen Quran verse display for SalatSign.
 * Shows ONLY the verbatim Arabic verse (Amiri Quran font, RTL), optional translation (LTR),
 * and Surah reference line. Never truncates or ellipsises; skips via [onOverflowSkip] if a verse
 * cannot fit at minimum 40sp / 24sp sizes on the display.
 */
@Composable
fun QuranVerseSlide(
    verse: QuranVerseItem,
    showTranslation: Boolean,
    onOverflowSkip: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val referenceLabel = stringResource(
        R.string.quran_surah_reference,
        verse.surahTransliteration,
        verse.reference
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .overscanPadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val contentWidthPx = with(density) { (maxWidth * 0.92f).roundToPx() }
            val contentHeightPx = with(density) { maxHeight.roundToPx() }
            val arabicGapPx = with(density) { 24.dp.roundToPx() }
            val refGapPx = with(density) { 22.dp.roundToPx() }

            val fit = remember(verse.reference, showTranslation, contentWidthPx, contentHeightPx, referenceLabel) {
                measureQuranVerseFit(
                    textMeasurer = textMeasurer,
                    verse = verse,
                    referenceLabel = referenceLabel,
                    showTranslation = showTranslation,
                    maxWidthPx = contentWidthPx,
                    maxHeightPx = contentHeightPx,
                    arabicGapPx = arabicGapPx,
                    refGapPx = refGapPx
                )
            }

            if (!fit.fits) {
                LaunchedEffect(verse.reference) {
                    Log.w(TAG, "Skipping Quran verse ${verse.reference}: does not fit on one screen at minimum 40sp/24sp sizes")
                    onOverflowSkip()
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = verse.arabicText,
                            style = TextStyle(
                                fontFamily = AmiriQuranFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = fit.arabicFontSp.sp,
                                lineHeight = (fit.arabicFontSp * 1.85f).sp,
                                color = colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                textDirection = TextDirection.Rtl
                            )
                        )
                    }

                    if (showTranslation && verse.translationText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = verse.translationText,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = fit.translationFontSp.sp,
                                    lineHeight = (fit.translationFontSp * 1.38f).sp,
                                    color = colorScheme.onBackground.copy(alpha = 0.88f),
                                    textAlign = TextAlign.Center,
                                    textDirection = TextDirection.Ltr
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = referenceLabel,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = REFERENCE_FONT_SP.sp,
                            lineHeight = (REFERENCE_FONT_SP * 1.3f).sp,
                            color = colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}
