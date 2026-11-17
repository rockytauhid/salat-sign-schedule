package com.salatsign.tv.data.model

enum class Prayer(
    val id: String,
    val displayName: String,
    val arabicName: String
) {
    SUBUH(
        id = "subuh",
        displayName = "Subuh",
        arabicName = "الفجر"
    ),
    ZUHUR(
        id = "zuhur",
        displayName = "Zuhur",
        arabicName = "الظهر"
    ),
    ASHR(
        id = "ashr",
        displayName = "Ashr",
        arabicName = "العصر"
    ),
    MAGRIB(
        id = "magrib",
        displayName = "Magrib",
        arabicName = "المغرب"
    ),
    ISYA(
        id = "isya",
        displayName = "Isya",
        arabicName = "العشاء"
    );

    companion object {
        fun fromId(id: String): Prayer {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SUBUH
        }
    }
}
