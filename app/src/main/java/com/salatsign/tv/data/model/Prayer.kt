// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.model

enum class Prayer(
    val id: String,
    val displayName: String,
    val arabicName: String
) {
    SUBUH(
        id = "subuh",
        displayName = "Fajr",
        arabicName = "الفجر"
    ),
    ZUHUR(
        id = "zuhur",
        displayName = "Dhuhr",
        arabicName = "الظهر"
    ),
    ASHR(
        id = "ashr",
        displayName = "Asr",
        arabicName = "العصر"
    ),
    MAGRIB(
        id = "magrib",
        displayName = "Maghrib",
        arabicName = "المغرب"
    ),
    ISYA(
        id = "isya",
        displayName = "Isha",
        arabicName = "العشاء"
    );

    companion object {
        fun fromId(id: String): Prayer {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SUBUH
        }
    }
}
