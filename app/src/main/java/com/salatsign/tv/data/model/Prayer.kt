// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.model

enum class Prayer(
    val id: String,
    val arabicName: String
) {
    SUBUH(
        id = "subuh",
        arabicName = "الفجر"
    ),
    ZUHUR(
        id = "zuhur",
        arabicName = "الظهر"
    ),
    ASHR(
        id = "ashr",
        arabicName = "العصر"
    ),
    MAGRIB(
        id = "magrib",
        arabicName = "المغرب"
    ),
    ISYA(
        id = "isya",
        arabicName = "العشاء"
    );

    companion object {
        fun fromId(id: String): Prayer {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SUBUH
        }
    }
}
