// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data

import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.SlideContainer
import com.salatsign.tv.data.model.SlideType
import com.salatsign.tv.data.repository.ContentRepository
import com.salatsign.tv.data.repository.RevisionConflictException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class ContentRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var baseDir: File
    private lateinit var repository: ContentRepository

    @Before
    fun setUp() {
        baseDir = tempFolder.newFolder("salatsign_test_files")
        repository = ContentRepository(baseDir)
    }

    @Test
    fun roundTrip_handlesSpecialCharacters_emoji_arabic_andDelimiters() = runBlocking {
        val slide = AnnouncementSlide(
            id = "slide_1",
            type = SlideType.TEXT,
            title = "Kajian;;Rutin::Masjid \"Al-Ikhlas\" 🕌 100% Free",
            body = "بسم الله الرحمن الرحيم\nLine 1::Text with colons\nLine 2;;Text with semicolons\nEmoji test: ✨🌟",
            durationSeconds = 15,
            enabled = true,
            startDate = "2026-11-01",
            endDate = "2026-11-30",
            sortOrder = 1
        )

        val addResult = repository.addSlide(slide)
        assertTrue("Add slide must succeed", addResult.isSuccess)

        // Read directly from disk with a fresh repository instance
        val freshRepo = ContentRepository(baseDir)
        val container = freshRepo.slidesFlow.value

        assertEquals(1, container.revision)
        assertEquals(1, container.slides.size)

        val loaded = container.slides.first()
        assertEquals(slide.id, loaded.id)
        assertEquals(slide.type, loaded.type)
        assertEquals(slide.title, loaded.title)
        assertEquals(slide.body, loaded.body)
        assertEquals(slide.durationSeconds, loaded.durationSeconds)
        assertEquals(slide.enabled, loaded.enabled)
        assertEquals(slide.startDate, loaded.startDate)
        assertEquals(slide.endDate, loaded.endDate)
        assertEquals(slide.sortOrder, loaded.sortOrder)
    }

    @Test
    fun replaceAll_incrementsRevision_andStoresBackup() = runBlocking {
        val slide1 = AnnouncementSlide(id = "s1", title = "First Slide")
        val slide2 = AnnouncementSlide(id = "s2", title = "Second Slide")

        val res1 = repository.replaceAll(0, listOf(slide1))
        assertTrue(res1.isSuccess)
        assertEquals(1, repository.slidesFlow.value.revision)

        val res2 = repository.replaceAll(1, listOf(slide1, slide2))
        assertTrue(res2.isSuccess)
        assertEquals(2, repository.slidesFlow.value.revision)
        assertEquals(2, repository.slidesFlow.value.slides.size)

        // Verify backup file exists and contains previous version (revision 1)
        val backupFile = File(repository.contentDir, "slides.json.bak")
        assertTrue("Backup file must exist after replacement", backupFile.exists())
        val backupContent = backupFile.readText()
        assertTrue("Backup must contain revision 1", backupContent.contains("\"revision\": 1"))
    }

    @Test
    fun replaceAll_detectsRevisionConflict() = runBlocking {
        val slide1 = AnnouncementSlide(id = "s1", title = "Slide 1")
        repository.addSlide(slide1) // Revision becomes 1

        val staleExpectedRevision = 0
        val conflictResult = repository.replaceAll(staleExpectedRevision, listOf(slide1))

        assertTrue("Stale revision must fail", conflictResult.isFailure)
        val exception = conflictResult.exceptionOrNull()
        assertTrue("Must be RevisionConflictException", exception is RevisionConflictException)
        assertEquals(0, (exception as RevisionConflictException).expected)
        assertEquals(1, exception.actual)
    }

    @Test
    fun corruptFileRecovery_fallsBackToBackupFile() = runBlocking {
        val goodSlide = AnnouncementSlide(id = "good", title = "Good Slide")
        repository.addSlide(goodSlide) // Rev 1, no bak
        repository.addSlide(goodSlide.copy(id = "good2")) // Rev 2, bak has Rev 1

        val slidesFile = File(repository.contentDir, "slides.json")
        val backupFile = File(repository.contentDir, "slides.json.bak")
        assertTrue(slidesFile.exists())
        assertTrue(backupFile.exists())

        // Corrupt main slides.json file
        slidesFile.writeText("{ this is invalid json !!! ")

        val freshRepo = ContentRepository(baseDir)
        val recovered = freshRepo.slidesFlow.value

        assertEquals("Must recover from backup with revision 1", 1, recovered.revision)
        assertEquals(1, recovered.slides.size)
        assertEquals("good", recovered.slides.first().id)
    }

    @Test
    fun corruptFileAndBackup_fallsBackToEmptyListWithoutCrashing() {
        val contentDir = File(baseDir, "content")
        contentDir.mkdirs()
        File(contentDir, "slides.json").writeText("{ corrupt }")
        File(contentDir, "slides.json.bak").writeText("{ corrupt backup }")

        val freshRepo = ContentRepository(baseDir)
        val result = freshRepo.slidesFlow.value

        assertEquals(0, result.revision)
        assertTrue(result.slides.isEmpty())
    }

    @Test
    fun activeSlides_filtersByEnabledAndDateRange() {
        val today = LocalDate.of(2026, 11, 17)

        val activeAlways = AnnouncementSlide(id = "1", title = "Always", enabled = true)
        val disabled = AnnouncementSlide(id = "2", title = "Disabled", enabled = false)
        val activeWindow = AnnouncementSlide(
            id = "3",
            title = "In Window",
            enabled = true,
            startDate = "2026-11-10",
            endDate = "2026-11-20"
        )
        val futureSlide = AnnouncementSlide(
            id = "4",
            title = "Future",
            enabled = true,
            startDate = "2026-11-25"
        )
        val expiredSlide = AnnouncementSlide(
            id = "5",
            title = "Expired",
            enabled = true,
            endDate = "2026-11-15"
        )

        val all = listOf(activeAlways, disabled, activeWindow, futureSlide, expiredSlide)
        val active = repository.getActiveSlides(all, today)

        assertEquals(2, active.size)
        assertEquals(listOf("1", "3"), active.map { it.id })
    }

    @Test
    fun validation_enforcesConstraints() {
        // 1. Title length > 80
        val longTitle = "A".repeat(81)
        val res1 = runCatching { repository.validate(AnnouncementSlide(id = "t1", title = longTitle)) }
        assertTrue(res1.isFailure)

        // 2. Body length > 400
        val longBody = "B".repeat(401)
        val res2 = runCatching { repository.validate(AnnouncementSlide(id = "t2", body = longBody)) }
        assertTrue(res2.isFailure)

        // 3. Duration < 3 or > 120
        val res3Short = runCatching { repository.validate(AnnouncementSlide(id = "t3", durationSeconds = 2)) }
        assertTrue(res3Short.isFailure)
        val res3Long = runCatching { repository.validate(AnnouncementSlide(id = "t3", durationSeconds = 121)) }
        assertTrue(res3Long.isFailure)

        // 4. Remote URLs or absolute paths in imageFile
        val res4Url = runCatching { repository.validate(AnnouncementSlide(id = "t4", imageFile = "https://example.com/img.jpg")) }
        assertTrue(res4Url.isFailure)
        val res4Uri = runCatching { repository.validate(AnnouncementSlide(id = "t4", imageFile = "content://media/123")) }
        assertTrue(res4Uri.isFailure)
        val res4Path = runCatching { repository.validate(AnnouncementSlide(id = "t4", imageFile = "subfolder/image.png")) }
        assertTrue(res4Path.isFailure)

        // Valid slide
        val validSlide = AnnouncementSlide(
            id = "valid",
            title = "Valid Title",
            body = "Valid Body",
            imageFile = "slide_123.jpg",
            durationSeconds = 10
        )
        val resValid = runCatching { repository.validate(validSlide) }
        assertTrue(resValid.isSuccess)
    }
}
