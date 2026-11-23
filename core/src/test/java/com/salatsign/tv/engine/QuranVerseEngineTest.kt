// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.engine

import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.QuranSlideDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuranVerseEngineTest {

    // Synthetic fixture data for unit tests (real verses live solely in assets extracted by build_quran_data.py)
    private val fixtureAr = """{"2:43":{"text":"ar_one two"},"3:8":{"text":"ar_three four five"}}"""
    private val fixtureEn = """{"2:43":{"text":"en translation one"},"3:8":{"text":"en translation two"}}"""
    private val fixtureId = """{"2:43":{"text":"id translation one"},"3:8":{"text":"id translation two"}}"""
    private val fixtureSurahs = """{"2":{"arabic":"البقرة","transliteration":"Al-Baqara"},"3":{"arabic":"آل عمران","transliteration":"Aal-i-Imraan"}}"""

    @Test
    fun repository_loadsEntries_handlesUnknown_andFollowsLanguageRules() {
        val repoWithId = JsonQuranVerseRepository(fixtureAr, fixtureEn, fixtureId, fixtureSurahs)
        assertEquals(listOf("2:43", "3:8"), repoWithId.allReferences())
        assertNull(repoWithId.getVerse("999:1", AppLanguage.EN))

        val enItem = repoWithId.getVerse("2:43", AppLanguage.EN)
        assertNotNull(enItem)
        assertEquals("en translation one", enItem!!.translationText)
        assertEquals("Al-Baqara", enItem.surahTransliteration)

        // ID -> Indonesian when bundled
        assertEquals("id translation one", repoWithId.getVerse("2:43", AppLanguage.ID)!!.translationText)
        // AR -> English
        assertEquals("en translation one", repoWithId.getVerse("2:43", AppLanguage.AR)!!.translationText)
        // SYSTEM / other -> English
        assertEquals("en translation one", repoWithId.getVerse("2:43", AppLanguage.SYSTEM)!!.translationText)

        // Without Indonesian data, ID falls back to English
        val repoNoId = JsonQuranVerseRepository(fixtureAr, fixtureEn, null, fixtureSurahs)
        assertEquals("en translation one", repoNoId.getVerse("2:43", AppLanguage.ID)!!.translationText)
    }

    @Test
    fun shuffleBag_all63OncePerCycle_noImmediateRepeatAcrossReshuffle_andPersistenceRecovery() {
        val refs = (1..63).map { "2:$it" }
        val rng = Random(42)
        var bag = QuranShuffleBag()
        var previous: String? = null

        // Run 5 full cycles (315 draws)
        repeat(5) { cycle ->
            val seen = mutableSetOf<String>()
            repeat(63) {
                val (picked, nextBag) = bag.next(refs, rng)!!
                if (previous != null) {
                    assertNotEquals("Immediate repeat at cycle $cycle", previous, picked)
                }
                assertTrue("Duplicate in cycle $cycle: $picked", seen.add(picked))
                previous = picked
                bag = nextBag
            }
            assertEquals(63, seen.size)
        }

        // Persist & restore mid-cycle
        val (first, afterFirst) = QuranShuffleBag().next(refs, Random(7))!!
        val serialized = afterFirst.serialize()
        val restored = QuranShuffleBag.deserialize(serialized, refs)
        assertEquals(first, restored.lastShown)
        assertEquals(62, restored.remaining.size)
        val (second, _) = restored.next(refs, Random(7))!!
        assertNotEquals(first, second)

        // Corrupt / empty persisted state starts a fresh bag safely
        val corrupt = QuranShuffleBag.deserialize("not-valid-data-at-all", refs)
        assertTrue(corrupt.remaining.isEmpty())
        assertNull(corrupt.lastShown)
        assertNotNull(corrupt.next(refs, rng))
    }

    @Test
    fun durationFormula_andClamps() {
        // Very short verse -> clamps to minimum 15s
        assertEquals(15, QuranDurationCalculator.autoDurationSeconds("one two", "three four", showTranslation = true))
        // 20 Arabic + 20 English = 40 words -> 12 + 20 = 32s
        val twentyWords = (1..20).joinToString(" ") { "w$it" }
        assertEquals(32, QuranDurationCalculator.autoDurationSeconds(twentyWords, twentyWords, showTranslation = true))
        // Arabic-only mode ignores translation words: 20 words -> 12 + 10 = 22s
        assertEquals(22, QuranDurationCalculator.autoDurationSeconds(twentyWords, twentyWords, showTranslation = false))
        // Very long verse -> clamps to maximum 60s
        val hundredWords = (1..100).joinToString(" ") { "w$it" }
        assertEquals(60, QuranDurationCalculator.autoDurationSeconds(hundredWords, hundredWords, showTranslation = true))

        // Fixed duration clamps to 10..90s
        assertEquals(10, QuranDurationCalculator.resolveDurationSeconds(5, "a", "b", true))
        assertEquals(90, QuranDurationCalculator.resolveDurationSeconds(200, "a", "b", true))
        assertEquals(30, QuranDurationCalculator.resolveDurationSeconds(30, "a", "b", true))
        assertEquals(
            QuranDurationCalculator.autoDurationSeconds(twentyWords, twentyWords, true),
            QuranDurationCalculator.resolveDurationSeconds(QuranSlideDefaults.DURATION_AUTO, twentyWords, twentyWords, true)
        )
    }
}
