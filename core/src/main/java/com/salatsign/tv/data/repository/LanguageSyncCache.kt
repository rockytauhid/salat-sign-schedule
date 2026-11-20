// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.repository

import android.content.Context
import com.salatsign.tv.data.model.AppLanguage

/**
 * Synchronous in-process / SharedPreferences cache for [AppLanguage].
 * Allows instant locale retrieval in [android.app.Activity.attachBaseContext]
 * without blocking the main thread on DataStore disk I/O (F6).
 */
object LanguageSyncCache {

    const val PREFS_NAME = "salatsign_sync_cache"
    const val KEY_APP_LANGUAGE = "cached_app_language"

    fun getLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_APP_LANGUAGE, null)
        return parseLanguage(name)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_LANGUAGE, language.name)
            .apply()
    }

    fun parseLanguage(name: String?): AppLanguage {
        if (name.isNullOrBlank()) return AppLanguage.SYSTEM
        return runCatching { AppLanguage.valueOf(name) }.getOrDefault(AppLanguage.SYSTEM)
    }
}
