// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.common

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.salatsign.tv.data.model.AppLanguage
import java.util.Locale

object LocaleHelper {

    fun getLocale(language: AppLanguage): Locale {
        return when (language) {
            AppLanguage.SYSTEM -> {
                val sysLoc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val defaultList = LocaleList.getDefault()
                    if (defaultList.isEmpty) Locale.getDefault() else defaultList[0]
                } else {
                    Locale.getDefault()
                }
                when (sysLoc.language.lowercase()) {
                    "in", "id" -> Locale("in", "ID")
                    "ar" -> Locale.forLanguageTag("ar-u-nu-latn")
                    else -> Locale.forLanguageTag("en-AU")
                }
            }
            AppLanguage.EN -> Locale.forLanguageTag("en-AU")
            AppLanguage.ID -> Locale("in", "ID")
            AppLanguage.AR -> Locale.forLanguageTag("ar-u-nu-latn")
        }
    }

    fun wrap(context: Context, language: AppLanguage): Context {
        val locale = getLocale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
