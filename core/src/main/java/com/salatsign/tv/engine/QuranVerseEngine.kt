// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.QuranSlideDefaults
import com.salatsign.tv.data.model.QuranTextEntry
import com.salatsign.tv.data.model.QuranVerseItem
import com.salatsign.tv.data.model.SurahMeta
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Pure-Kotlin repository interface for bundled offline Quran verses (no Android types).
 */
interface QuranVerseRepository {
    fun allReferences(): List<String>
    fun getVerse(reference: String, language: AppLanguage): QuranVerseItem?
}

/**
 * Loads the verbatim JSON assets extracted by tools/build_quran_data.py.
 * Language mapping:
 * - EN -> English (Saheeh International)
 * - ID -> Indonesian (Kemenag), falling back to English if Indonesian data is unavailable
 * - AR -> English (since Arabic original is already displayed prominently)
 * - SYSTEM / other -> English
 */
class JsonQuranVerseRepository(
    versesArJson: String,
    translationsEnJson: String,
    translationsIdJson: String?,
    surahsJson: String
) : QuranVerseRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val entryMapSerializer = MapSerializer(String.serializer(), QuranTextEntry.serializer())
    private val surahMapSerializer = MapSerializer(String.serializer(), SurahMeta.serializer())

    private val versesAr: Map<String, QuranTextEntry> =
        json.decodeFromString(entryMapSerializer, versesArJson)
    private val translationsEn: Map<String, QuranTextEntry> =
        json.decodeFromString(entryMapSerializer, translationsEnJson)
    private val translationsId: Map<String, QuranTextEntry> =
        if (!translationsIdJson.isNullOrBlank()) {
            runCatching { json.decodeFromString(entryMapSerializer, translationsIdJson) }
                .getOrDefault(emptyMap())
        } else {
            emptyMap()
        }
    private val surahs: Map<String, SurahMeta> =
        json.decodeFromString(surahMapSerializer, surahsJson)

    private val orderedRefs: List<String> = versesAr.keys.toList()

    override fun allReferences(): List<String> = orderedRefs

    override fun getVerse(reference: String, language: AppLanguage): QuranVerseItem? {
        val arabic = versesAr[reference]?.text ?: return null
        val parts = reference.split(":")
        if (parts.size != 2) return null
        val surahNum = parts[0].toIntOrNull() ?: return null
        val ayahNum = parts[1].toIntOrNull() ?: return null
        val surahMeta = surahs[surahNum.toString()] ?: SurahMeta(arabic = "", transliteration = "Surah $surahNum")

        val english = translationsEn[reference]?.text.orEmpty()
        val translation = when (language) {
            AppLanguage.ID -> translationsId[reference]?.text?.takeIf { it.isNotEmpty() } ?: english
            AppLanguage.EN, AppLanguage.AR, AppLanguage.SYSTEM -> english
        }

        return QuranVerseItem(
            reference = reference,
            surahNumber = surahNum,
            ayahNumber = ayahNum,
            surahArabicName = surahMeta.arabic,
            surahTransliteration = surahMeta.transliteration,
            arabicText = arabic,
            translationText = translation
        )
    }
}

/**
 * Pure shuffle bag guaranteeing every verse appears once per full cycle
 * and never repeats the same verse twice in a row across a reshuffle boundary.
 */
data class QuranShuffleBag(
    val remaining: List<String> = emptyList(),
    val lastShown: String? = null
) {
    fun next(allReferences: List<String>, random: Random = Random.Default): Pair<String, QuranShuffleBag>? {
        val validSet = allReferences.toSet()
        if (validSet.isEmpty()) return null

        val cleanRemaining = remaining.filter { it in validSet }
        val pool = if (cleanRemaining.isNotEmpty()) {
            cleanRemaining
        } else {
            val shuffled = allReferences.shuffled(random).toMutableList()
            if (shuffled.size > 1 && lastShown != null && shuffled.first() == lastShown) {
                val swapIdx = 1 + random.nextInt(shuffled.size - 1)
                val tmp = shuffled[0]
                shuffled[0] = shuffled[swapIdx]
                shuffled[swapIdx] = tmp
            }
            shuffled
        }

        val chosen = pool.first()
        val nextBag = QuranShuffleBag(
            remaining = pool.drop(1),
            lastShown = chosen
        )
        return chosen to nextBag
    }

    fun serialize(): String {
        val last = lastShown.orEmpty()
        val rem = remaining.joinToString(",")
        return "$last|$rem"
    }

    companion object {
        fun deserialize(raw: String?, allReferences: List<String>): QuranShuffleBag {
            if (raw.isNullOrBlank()) return QuranShuffleBag()
            val sepIdx = raw.indexOf('|')
            if (sepIdx < 0) return QuranShuffleBag()
            val validSet = allReferences.toSet()
            val last = raw.substring(0, sepIdx).trim().takeIf { it in validSet }
            val remPart = raw.substring(sepIdx + 1)
            val rem = if (remPart.isBlank()) {
                emptyList()
            } else {
                remPart.split(",").map { it.trim() }.filter { it in validSet }.distinct()
            }
            return QuranShuffleBag(remaining = rem, lastShown = last)
        }
    }
}

object QuranDurationCalculator {
    fun wordCount(text: String?): Int {
        if (text.isNullOrBlank()) return 0
        return text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    }

    /**
     * Auto duration formula: clamp(12 + 0.5 * wordCount, 15, 60) seconds,
     * where wordCount counts Arabic words plus translation words (when shown).
     */
    fun autoDurationSeconds(
        arabicText: String,
        translationText: String?,
        showTranslation: Boolean
    ): Int {
        val totalWords = wordCount(arabicText) +
            if (showTranslation) wordCount(translationText) else 0
        val computed = (12.0 + 0.5 * totalWords).roundToInt()
        return computed.coerceIn(
            QuranSlideDefaults.MIN_AUTO_DURATION_SECONDS,
            QuranSlideDefaults.MAX_AUTO_DURATION_SECONDS
        )
    }

    fun resolveDurationSeconds(
        configuredDurationSeconds: Int,
        arabicText: String,
        translationText: String?,
        showTranslation: Boolean
    ): Int {
        return if (configuredDurationSeconds == QuranSlideDefaults.DURATION_AUTO) {
            autoDurationSeconds(arabicText, translationText, showTranslation)
        } else {
            configuredDurationSeconds.coerceIn(
                QuranSlideDefaults.MIN_FIXED_DURATION_SECONDS,
                QuranSlideDefaults.MAX_FIXED_DURATION_SECONDS
            )
        }
    }
}
