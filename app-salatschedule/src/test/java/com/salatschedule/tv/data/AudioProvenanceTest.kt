// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.data

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.util.Locale

class AudioProvenanceTest {

    @Test
    fun allRawAudioFiles_areDocumentedInAudioSourcesMdWithValidChecksums() {
        val docsFile = listOf(
            File("docs/AUDIO_SOURCES.md"),
            File("../docs/AUDIO_SOURCES.md"),
            File("../../docs/AUDIO_SOURCES.md")
        ).firstOrNull { it.exists() }

        assertNotNull("docs/AUDIO_SOURCES.md must exist in repository", docsFile)
        val docContent = docsFile!!.readText().lowercase(Locale.ROOT)

        val rawDir = listOf(
            File("src/main/res/raw"),
            File("src/main/res/raw"),
            File("../src/main/res/raw")
        ).firstOrNull { it.exists() && it.isDirectory }

        assertNotNull("res/raw directory must exist", rawDir)
        val rawAudioFiles = rawDir!!.listFiles()?.filter { it.isFile } ?: emptyList()

        assertTrue("res/raw must contain bundled audio files", rawAudioFiles.isNotEmpty())

        for (file in rawAudioFiles) {
            val fileName = file.name.lowercase(Locale.ROOT)
            assertTrue(
                "Audio file $fileName must be documented in docs/AUDIO_SOURCES.md",
                docContent.contains(fileName)
            )

            // Compute SHA-256 of the audio file
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(file.readBytes())
            val hexHash = hashBytes.joinToString("") { "%02x".format(it) }.lowercase(Locale.ROOT)

            assertTrue(
                "SHA-256 checksum ($hexHash) of $fileName must be documented in docs/AUDIO_SOURCES.md",
                docContent.contains(hexHash)
            )
        }
    }
}
