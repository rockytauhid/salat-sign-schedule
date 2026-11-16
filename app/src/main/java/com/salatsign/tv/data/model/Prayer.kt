package com.salatsign.tv.data.model

import androidx.annotation.StringRes
import com.salatsign.tv.R

enum class Prayer(
    val id: String,
    val displayName: String,
    val arabicName: String,
    val defaultRequestCodeAdhan: Int,
    val defaultRequestCodeReminder: Int
) {
    SUBUH(
        id = "subuh",
        displayName = "Subuh",
        arabicName = "الفجر",
        defaultRequestCodeAdhan = 101,
        defaultRequestCodeReminder = 201
    ),
    ZUHUR(
        id = "zuhur",
        displayName = "Zuhur",
        arabicName = "الظهر",
        defaultRequestCodeAdhan = 102,
        defaultRequestCodeReminder = 202
    ),
    ASHR(
        id = "ashr",
        displayName = "Ashr",
        arabicName = "العصر",
        defaultRequestCodeAdhan = 103,
        defaultRequestCodeReminder = 203
    ),
    MAGRIB(
        id = "magrib",
        displayName = "Magrib",
        arabicName = "المغرب",
        defaultRequestCodeAdhan = 104,
        defaultRequestCodeReminder = 204
    ),
    ISYA(
        id = "isya",
        displayName = "Isya",
        arabicName = "العشاء",
        defaultRequestCodeAdhan = 105,
        defaultRequestCodeReminder = 205
    );

    companion object {
        fun fromId(id: String): Prayer {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SUBUH
        }
    }
}
