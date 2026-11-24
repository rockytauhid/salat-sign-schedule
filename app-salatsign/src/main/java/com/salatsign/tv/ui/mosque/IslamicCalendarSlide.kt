// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.mosque

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.salatsign.tv.data.model.ClassifiedIslamicEvent
import com.salatsign.tv.ui.slides.SharedIslamicCalendarSlide

@Composable
fun IslamicCalendarSlide(
    rows: List<ClassifiedIslamicEvent>,
    uiState: MosqueSignageUiState,
    modifier: Modifier = Modifier
) {
    val prayerTimes = uiState.prayerTimesDay
    SharedIslamicCalendarSlide(
        rows = rows,
        hijriDate = prayerTimes?.hijriDate.orEmpty(),
        gregorianDate = prayerTimes?.gregorianDate.orEmpty(),
        modifier = modifier
    )
}
