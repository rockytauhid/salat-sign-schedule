// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data

import com.salatsign.tv.data.migration.LegacySlideMigration
import com.salatsign.tv.data.model.SlideType
import com.salatsign.tv.data.repository.ContentRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LegacySlideMigrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var baseDir: File
    private lateinit var repository: ContentRepository

    @Before
    fun setUp() {
        baseDir = tempFolder.newFolder("migration_test")
        repository = ContentRepository(baseDir)
    }

    @Test
    fun parseLegacySlides_doubleSemicolonAndColonFormat() {
        val raw = "s1::https://example.com/pic1.jpg::Kajian Subuh::15;;s2::file:///sdcard/pic2.png::Tabligh Akbar::20"
        val records = LegacySlideMigration.parseLegacySlides(raw)

        assertEquals(2, records.size)
        assertEquals("s1", records[0].id)
        assertEquals("https://example.com/pic1.jpg", records[0].image)
        assertEquals("Kajian Subuh", records[0].title)
        assertEquals(15, records[0].durationSeconds)

        assertEquals("s2", records[1].id)
        assertEquals("file:///sdcard/pic2.png", records[1].image)
        assertEquals("Tabligh Akbar", records[1].title)
        assertEquals(20, records[1].durationSeconds)
    }

    @Test
    fun parseLegacySlides_pipeAndTildeFormat() {
        val raw = "s1~https://example.com/a.jpg~Title 1~12|s2~/local/path.jpg~Title 2~18"
        val records = LegacySlideMigration.parseLegacySlides(raw)

        assertEquals(2, records.size)
        assertEquals("s1", records[0].id)
        assertEquals("https://example.com/a.jpg", records[0].image)
        assertEquals("Title 1", records[0].title)
        assertEquals(12, records[0].durationSeconds)

        assertEquals("s2", records[1].id)
        assertEquals("/local/path.jpg", records[1].image)
        assertEquals("Title 2", records[1].title)
        assertEquals(18, records[1].durationSeconds)
    }

    @Test
    fun parseLegacySlides_blankOrMalformed_returnsEmpty() {
        assertTrue(LegacySlideMigration.parseLegacySlides("").isEmpty())
        assertTrue(LegacySlideMigration.parseLegacySlides("   ").isEmpty())
        assertTrue(LegacySlideMigration.parseLegacySlides("malformed_token_without_separators").isEmpty())
    }

    @Test
    fun convertRecords_dropsRemoteHttpAndHttpsUrls() {
        val records = listOf(
            LegacySlideMigration.LegacySlideRecord("1", "http://example.com/img1.jpg", "HTTP Slide", 10),
            LegacySlideMigration.LegacySlideRecord("2", "https://example.com/img2.jpg", "HTTPS Slide", 10)
        )

        val converted = LegacySlideMigration.convertRecords(records, repository)
        assertTrue("All remote HTTP/HTTPS slides must be dropped", converted.isEmpty())
    }

    @Test
    fun convertRecords_copiesReadableLocalFileAndCreatesValidSlide() {
        // Create an existing readable file
        val localImage = tempFolder.newFile("sample_slide.jpg")
        localImage.writeText("fake image bytes")

        val records = listOf(
            LegacySlideMigration.LegacySlideRecord("1", localImage.absolutePath, "Valid Local Slide", 15)
        )

        val converted = LegacySlideMigration.convertRecords(records, repository)
        assertEquals(1, converted.size)

        val slide = converted.first()
        assertEquals("1", slide.id)
        assertEquals(SlideType.IMAGE, slide.type)
        assertEquals("Valid Local Slide", slide.title)
        assertEquals(15, slide.durationSeconds)
        assertTrue(slide.imageFile != null)

        val destinationFile = repository.getImageFile(slide.imageFile!!)
        assertTrue("Image file must be copied to repository images directory", destinationFile.exists())
        assertEquals("fake image bytes", destinationFile.readText())
    }

    @Test
    fun convertRecords_dropsMissingOrUnreadableLocalFile() {
        val nonExistentPath = File(baseDir, "ghost_image.jpg").absolutePath

        val records = listOf(
            LegacySlideMigration.LegacySlideRecord("1", nonExistentPath, "Missing Image Slide", 10)
        )

        val converted = LegacySlideMigration.convertRecords(records, repository)
        assertTrue("Slides with missing local files must be dropped", converted.isEmpty())
    }

    @Test
    fun convertRecords_clampsDurationsAndTruncatesTitles() {
        val localImage = tempFolder.newFile("clamp_test.jpg")
        localImage.writeText("clamp bytes")

        val longTitle = "A".repeat(120) // Exceeds MAX_TITLE_LENGTH (80)

        val records = listOf(
            LegacySlideMigration.LegacySlideRecord("1", localImage.absolutePath, longTitle, 1),    // Below min (3)
            LegacySlideMigration.LegacySlideRecord("2", localImage.absolutePath, "Normal", 999)     // Above max (120)
        )

        val converted = LegacySlideMigration.convertRecords(records, repository)
        assertEquals(2, converted.size)

        assertEquals(80, converted[0].title.length)
        assertEquals(ContentRepository.MIN_DURATION_SECONDS, converted[0].durationSeconds)
        assertEquals(ContentRepository.MAX_DURATION_SECONDS, converted[1].durationSeconds)
    }
}
