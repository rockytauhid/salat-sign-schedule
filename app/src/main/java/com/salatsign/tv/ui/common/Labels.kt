// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.common

import com.salatsign.tv.R
import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.ThemeMode

fun Prayer.labelRes(isFriday: Boolean = false): Int {
    return when (this) {
        Prayer.SUBUH -> R.string.prayer_subuh
        Prayer.ZUHUR -> if (isFriday) R.string.prayer_jumuah else R.string.prayer_zuhur
        Prayer.ASHR -> R.string.prayer_ashr
        Prayer.MAGRIB -> R.string.prayer_magrib
        Prayer.ISYA -> R.string.prayer_isya
    }
}

fun Prayer.arabicLabel(): String = arabicName

fun ThemeMode.labelRes(): Int {
    return when (this) {
        ThemeMode.ALWAYS_DARK -> R.string.theme_always_dark
        ThemeMode.ALWAYS_LIGHT -> R.string.theme_always_light
        ThemeMode.AUTO_DAY_NIGHT -> R.string.theme_auto_day_night
    }
}

fun MosqueIconPreset.labelRes(): Int {
    return when (this) {
        MosqueIconPreset.DOME_CRESCENT -> R.string.mosque_icon_dome_crescent
        MosqueIconPreset.MINARET -> R.string.mosque_icon_minaret
        MosqueIconPreset.GEOMETRIC_STAR -> R.string.mosque_icon_geometric_star
        MosqueIconPreset.MIHRAB -> R.string.mosque_icon_mihrab
        MosqueIconPreset.CUSTOM -> R.string.mosque_icon_custom
    }
}

fun MosqueFontPreset.labelRes(): Int {
    return when (this) {
        MosqueFontPreset.MODERN_SANS -> R.string.mosque_font_modern_sans
        MosqueFontPreset.TRADITIONAL_SERIF -> R.string.mosque_font_traditional_serif
        MosqueFontPreset.ELEGANT_DISPLAY -> R.string.mosque_font_elegant_display
        MosqueFontPreset.CLEAN_TECH -> R.string.mosque_font_clean_tech
    }
}

fun AdhanAudioType.labelRes(): Int {
    return when (this) {
        AdhanAudioType.DEFAULT_STANDARD -> R.string.adhan_standard
        AdhanAudioType.DEFAULT_SUBUH -> R.string.adhan_subuh_special
        AdhanAudioType.CUSTOM_FILE -> R.string.adhan_custom_file
        AdhanAudioType.SILENT -> R.string.adhan_silent
    }
}

fun AdhanAudioOption.labelRes(): Int = type.labelRes()

fun AppLanguage.labelRes(): Int {
    return when (this) {
        AppLanguage.SYSTEM -> R.string.lang_system
        AppLanguage.EN -> R.string.lang_en
        AppLanguage.ID -> R.string.lang_id
        AppLanguage.AR -> R.string.lang_ar
    }
}
