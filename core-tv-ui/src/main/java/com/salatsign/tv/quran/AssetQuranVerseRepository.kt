// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.quran

import android.content.Context
import android.util.Log
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.QuranVerseItem
import com.salatsign.tv.engine.JsonQuranVerseRepository
import com.salatsign.tv.engine.QuranVerseRepository

/**
 * Loads the verbatim offline Quran JSON assets once from app-bundled assets/quran/.
 */
class AssetQuranVerseRepository(private val context: Context) : QuranVerseRepository {

    private val delegate: QuranVerseRepository? by lazy {
        runCatching {
            val ar = readAsset("quran/verses_ar.json")
            val en = readAsset("quran/translations_en.json")
            val id = runCatching { readAsset("quran/translations_id.json") }.getOrNull()
            val surahs = readAsset("quran/surahs.json")
            JsonQuranVerseRepository(
                versesArJson = ar,
                translationsEnJson = en,
                translationsIdJson = id,
                surahsJson = surahs
            )
        }.onFailure { err ->
            Log.e("AssetQuranVerseRepo", "Failed to load bundled Quran assets", err)
        }.getOrNull()
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    override fun allReferences(): List<String> = delegate?.allReferences().orEmpty()

    override fun getVerse(reference: String, language: AppLanguage): QuranVerseItem? =
        delegate?.getVerse(reference, language)
}
