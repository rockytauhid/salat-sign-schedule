// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.repository

import android.content.Context
import android.util.Log
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.SlideContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate

class RevisionConflictException(val expected: Int, val actual: Int) :
    IllegalStateException("Revision conflict: expected $expected but found $actual")

class ContentRepository(
    private val baseDir: File
) : IContentRepository {

    constructor(context: Context) : this(context.filesDir)

    companion object {
        private const val TAG = "ContentRepository"
        const val MAX_SLIDES = 30
        const val MAX_TITLE_LENGTH = 80
        const val MAX_BODY_LENGTH = 400
        const val MIN_DURATION_SECONDS = 3
        const val MAX_DURATION_SECONDS = 120
    }

    val contentDir = File(baseDir, "content")
    val imagesDir = File(contentDir, "images")
    private val slidesFile = File(contentDir, "slides.json")
    private val backupFile = File(contentDir, "slides.json.bak")
    private val tempFile = File(contentDir, "slides.json.tmp")

    private val mutex = Mutex()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _slidesFlow = MutableStateFlow(readContainerSafely())
    override val slidesFlow: StateFlow<SlideContainer> = _slidesFlow.asStateFlow()

    init {
        contentDir.mkdirs()
        imagesDir.mkdirs()
    }

    /**
     * Replaces all slides with optimistic locking check against expectedRevision.
     */
    override suspend fun replaceAll(expectedRevision: Int, slides: List<AnnouncementSlide>): Result<SlideContainer> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching {
                    val current = _slidesFlow.value
                    if (current.revision != expectedRevision) {
                        throw RevisionConflictException(expected = expectedRevision, actual = current.revision)
                    }

                    validateAll(slides)

                    val newContainer = SlideContainer(
                        revision = current.revision + 1,
                        slides = slides.sortedBy { it.sortOrder }
                    )
                    writeContainerAtomic(newContainer)
                    newContainer
                }
            }
        }

    /**
     * Adds a new slide.
     */
    override suspend fun addSlide(slide: AnnouncementSlide): Result<SlideContainer> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val current = _slidesFlow.value
                val newSlides = current.slides + slide
                validateAll(newSlides)

                val newContainer = SlideContainer(
                    revision = current.revision + 1,
                    slides = newSlides.sortedBy { it.sortOrder }
                )
                writeContainerAtomic(newContainer)
                newContainer
            }
        }
    }

    /**
     * Updates an existing slide matching slide.id.
     */
    override suspend fun updateSlide(slide: AnnouncementSlide): Result<SlideContainer> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val current = _slidesFlow.value
                val exists = current.slides.any { it.id == slide.id }
                if (!exists) {
                    throw NoSuchElementException("Slide with id '${slide.id}' not found")
                }

                val newSlides = current.slides.map { if (it.id == slide.id) slide else it }
                validateAll(newSlides)

                val newContainer = SlideContainer(
                    revision = current.revision + 1,
                    slides = newSlides.sortedBy { it.sortOrder }
                )
                writeContainerAtomic(newContainer)
                newContainer
            }
        }
    }

    /**
     * Deletes a slide matching slideId.
     */
    override suspend fun deleteSlide(slideId: String): Result<SlideContainer> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val current = _slidesFlow.value
                val exists = current.slides.any { it.id == slideId }
                if (!exists) {
                    throw NoSuchElementException("Slide with id '$slideId' not found")
                }

                val newSlides = current.slides.filterNot { it.id == slideId }

                val newContainer = SlideContainer(
                    revision = current.revision + 1,
                    slides = newSlides.sortedBy { it.sortOrder }
                )
                writeContainerAtomic(newContainer)
                newContainer
            }
        }
    }

    /**
     * Filters active slides for signage display (enabled and within date window).
     */
    override fun getActiveSlides(
        slides: List<AnnouncementSlide>,
        today: LocalDate
    ): List<AnnouncementSlide> {
        return slides
            .filter { it.enabled }
            .filter { slide ->
                val afterStart = slide.startDate?.let {
                    runCatching { LocalDate.parse(it) }.getOrNull()?.let { start -> today >= start } ?: true
                } ?: true

                val beforeEnd = slide.endDate?.let {
                    runCatching { LocalDate.parse(it) }.getOrNull()?.let { end -> today <= end } ?: true
                } ?: true

                afterStart && beforeEnd
            }
            .sortedBy { it.sortOrder }
    }

    override fun getImageFile(fileName: String): File {
        return File(imagesDir, fileName)
    }

    fun validate(slide: AnnouncementSlide) {
        require(slide.title.length <= MAX_TITLE_LENGTH) {
            "Slide title length exceeds $MAX_TITLE_LENGTH characters (was ${slide.title.length})"
        }
        require(slide.body.length <= MAX_BODY_LENGTH) {
            "Slide body length exceeds $MAX_BODY_LENGTH characters (was ${slide.body.length})"
        }
        require(slide.durationSeconds in MIN_DURATION_SECONDS..MAX_DURATION_SECONDS) {
            "Slide duration must be between $MIN_DURATION_SECONDS and $MAX_DURATION_SECONDS seconds (was ${slide.durationSeconds})"
        }
        slide.imageFile?.let { file ->
            require(!file.startsWith("http://") && !file.startsWith("https://")) {
                "Slide imageFile must not be a remote URL"
            }
            require(!file.startsWith("content://") && !file.startsWith("file://")) {
                "Slide imageFile must not be a URI scheme"
            }
            require(!file.contains("/") && !file.contains("\\")) {
                "Slide imageFile must be a relative filename only without directories"
            }
        }
    }

    private fun validateAll(slides: List<AnnouncementSlide>) {
        require(slides.size <= MAX_SLIDES) {
            "Total slides count cannot exceed $MAX_SLIDES (was ${slides.size})"
        }
        for (slide in slides) {
            validate(slide)
        }
    }

    private fun writeContainerAtomic(container: SlideContainer) {
        contentDir.mkdirs()
        val jsonString = json.encodeToString(SlideContainer.serializer(), container)

        // 1. Write to temp file
        tempFile.writeText(jsonString)

        // 2. Backup current good file if it exists and decodes successfully (F9)
        if (slidesFile.exists()) {
            val isCurrentFileValid = runCatching {
                json.decodeFromString(SlideContainer.serializer(), slidesFile.readText())
            }.isSuccess

            if (isCurrentFileValid) {
                try {
                    Files.copy(slidesFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to create slides.json.bak", e)
                }
            } else {
                Log.w(TAG, "slides.json is corrupt; preserving existing slides.json.bak")
            }
        }

        // 3. Atomically replace slides.json with tempFile
        try {
            Files.move(
                tempFile.toPath(),
                slidesFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: IOException) {
            // Fallback to standard replace if atomic move fails (e.g. cross-filesystem)
            Files.move(tempFile.toPath(), slidesFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        // 4. Clean up orphaned slide image files (excluding any active logo file)
        cleanupOrphanImages(container)

        _slidesFlow.value = container
    }

    private fun cleanupOrphanImages(container: SlideContainer) {
        if (!imagesDir.exists() || !imagesDir.isDirectory) return
        val referencedImages = container.slides.mapNotNull { it.imageFile }.toSet()
        val logoFiles = File(contentDir, "logo").listFiles()?.filter { it.isFile }?.map { it.name }?.toSet() ?: emptySet()
        imagesDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name !in referencedImages && file.name !in logoFiles) {
                val deleted = file.delete()
                if (!deleted) {
                    Log.w(TAG, "Failed to delete orphaned slide image: ${file.name}")
                }
            }
        }
    }

    fun readContainerSafely(): SlideContainer {
        if (slidesFile.exists()) {
            try {
                val content = slidesFile.readText()
                return json.decodeFromString(SlideContainer.serializer(), content)
            } catch (e: Exception) {
                Log.e(TAG, "Corrupt or unreadable slides.json. Attempting recovery from .bak", e)
            }
        }

        if (backupFile.exists()) {
            try {
                val content = backupFile.readText()
                Log.i(TAG, "Successfully restored slides from slides.json.bak")
                return json.decodeFromString(SlideContainer.serializer(), content)
            } catch (e: Exception) {
                Log.e(TAG, "Corrupt or unreadable slides.json.bak as well.", e)
            }
        }

        return SlideContainer(revision = 0, slides = emptyList())
    }
}
