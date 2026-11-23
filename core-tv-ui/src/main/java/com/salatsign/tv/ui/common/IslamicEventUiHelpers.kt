// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.salatsign.core.tv.R
import com.salatsign.tv.data.model.EventOccurrence
import com.salatsign.tv.data.model.HijriDay
import com.salatsign.tv.data.model.IslamicEventDefinitions
import com.salatsign.tv.data.model.IslamicEventGroup
import com.salatsign.tv.ui.theme.TvTypography
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@StringRes
fun islamicEventNameRes(eventId: String): Int = when (eventId) {
    "new_year" -> R.string.islamic_event_new_year
    "ashura" -> R.string.islamic_event_ashura
    "ramadan_start" -> R.string.islamic_event_ramadan_start
    "eid_fitr" -> R.string.islamic_event_eid_fitr
    "arafah" -> R.string.islamic_event_arafah
    "eid_adha" -> R.string.islamic_event_eid_adha
    "mawlid" -> R.string.islamic_event_mawlid
    "isra_miraj" -> R.string.islamic_event_isra_miraj
    "nisfu_shaban" -> R.string.islamic_event_nisfu_shaban
    "nuzul_quran" -> R.string.islamic_event_nuzul_quran
    "last_ten_ramadan" -> R.string.islamic_event_last_ten_ramadan
    "tasua" -> R.string.islamic_event_tasua
    "six_shawwal" -> R.string.islamic_event_six_shawwal
    "white_days" -> R.string.islamic_event_white_days
    "first_ten_dhulhijjah" -> R.string.islamic_event_first_ten_dhulhijjah
    "tashreeq" -> R.string.islamic_event_tashreeq
    else -> R.string.islamic_event_new_year
}

@StringRes
fun islamicEventGroupTitleRes(group: IslamicEventGroup): Int = when (group) {
    IslamicEventGroup.MAJOR -> R.string.islamic_cal_group_major
    IslamicEventGroup.COMMONLY_OBSERVED -> R.string.islamic_cal_group_common
    IslamicEventGroup.FASTING_AND_OTHER -> R.string.islamic_cal_group_fasting
}

fun formatHijriEventSpan(occ: EventOccurrence, monthNames: List<String>): String {
    val startMonth = monthNames.getOrNull(occ.hijriStart.month - 1) ?: "Month ${occ.hijriStart.month}"
    return if (!occ.isRange) {
        "${occ.hijriStart.day} $startMonth ${occ.hijriStart.year} AH"
    } else if (occ.hijriStart.month == occ.hijriEnd.month && occ.hijriStart.year == occ.hijriEnd.year) {
        "${occ.hijriStart.day}–${occ.hijriEnd.day} $startMonth ${occ.hijriStart.year} AH"
    } else {
        val endMonth = monthNames.getOrNull(occ.hijriEnd.month - 1) ?: "Month ${occ.hijriEnd.month}"
        "${occ.hijriStart.day} $startMonth – ${occ.hijriEnd.day} $endMonth ${occ.hijriEnd.year} AH"
    }
}

fun formatHijriDayShort(day: HijriDay, monthNames: List<String>): String {
    val monthName = monthNames.getOrNull(day.month - 1) ?: "Month ${day.month}"
    return "${day.day} $monthName"
}

private val gregSingleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.forLanguageTag("en-AU"))
private val gregShortFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.forLanguageTag("en-AU"))
private val gregSpanFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("en-AU"))

fun formatGregorianEventSpan(occ: EventOccurrence): String {
    return if (!occ.isRange) {
        occ.startDate.format(gregSingleFormatter)
    } else if (occ.startDate.year == occ.endDate.year) {
        "${occ.startDate.format(gregShortFormatter)} – ${occ.endDate.format(gregSingleFormatter)}"
    } else {
        "${occ.startDate.format(gregSingleFormatter)} – ${occ.endDate.format(gregSingleFormatter)}"
    }
}

fun formatGregorianYearSpan(start: LocalDate, end: LocalDate): String {
    return "${start.format(gregSpanFormatter)} – ${end.format(gregSpanFormatter)}"
}

fun formatRelativeDaysLabel(days: Long, context: Context): String = when {
    days == 0L -> context.getString(R.string.islamic_cal_rel_today)
    days == 1L -> context.getString(R.string.islamic_cal_rel_tomorrow)
    days > 1L -> context.getString(R.string.islamic_cal_rel_in_days, days.toInt())
    days == -1L -> context.getString(R.string.islamic_cal_rel_yesterday)
    else -> context.getString(R.string.islamic_cal_rel_days_ago, (-days).toInt())
}

/**
 * Shared D-pad-friendly grouped Islamic Calendar Event toggles used in both SalatSchedule (Home)
 * and SalatSign (Mosque) Settings screens to guarantee visual & copy parity.
 */
@Composable
fun IslamicEventTogglesSection(
    enabledIds: Set<String>,
    hijriOffsetDays: Int,
    onToggleEvent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val offsetDisplay = if (hijriOffsetDays > 0) "+$hijriOffsetDays" else "$hijriOffsetDays"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.islamic_cal_settings_hijri_hint, offsetDisplay),
            style = TvTypography.BodyLabel.copy(
                fontSize = 13.sp,
                color = colorScheme.onSurface.copy(alpha = 0.78f)
            )
        )

        for (group in IslamicEventGroup.entries) {
            val defs = IslamicEventDefinitions.byGroup(group)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(islamicEventGroupTitleRes(group)),
                    style = TvTypography.BodyLabel.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.secondary
                    )
                )

                defs.chunked(2).forEach { rowDefs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (def in rowDefs) {
                            val checked = def.id in enabledIds
                            val shape = RoundedCornerShape(10.dp)
                            Card(
                                onClick = { onToggleEvent(def.id) },
                                colors = CardDefaults.colors(
                                    containerColor = if (checked) colorScheme.surfaceVariant else colorScheme.surface,
                                    focusedContainerColor = colorScheme.surfaceVariant
                                ),
                                shape = CardDefaults.shape(shape),
                                scale = tvCardScale(1.01f),
                                border = tvSelectableCardBorder(selected = checked, shape = shape),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = if (checked) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.55f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(islamicEventNameRes(def.id)),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = TvTypography.BodyLabel.copy(
                                            fontSize = 14.sp,
                                            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                                            color = colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                        if (rowDefs.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
