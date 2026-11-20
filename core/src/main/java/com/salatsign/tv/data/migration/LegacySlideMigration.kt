// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.migration

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.SlideType
import com.salatsign.tv.data.repository.ContentRepository
import com.salatsign.tv.data.repository.dataStore
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.UUID

object LegacySlideMigration {

    val SLIDES_RAW_KEY = stringPreferencesKey("slides_raw")

    data class LegacySlideRecord(
        val id: String,
        val image: String,
        val title: String,
        val durationSeconds: Int
    )

    /**
     * Parses legacy raw slide strings supporting both ";;" / "::" and "|" / "~" delimiters.
     */
    fun parseLegacySlides(raw: String): List<LegacySlideRecord> {
        if (raw.isBlank()) return emptyList()
        val outerDelimiter = if (raw.contains(";;")) ";;" else if (raw.contains("|")) "|" else "\n"
        val items = raw.split(outerDelimiter).filter { it.isNotBlank() }

        return items.mapNotNull { item ->
            val innerDelimiter = if (item.contains("::")) "::" else if (item.contains("~")) "~" else null
            if (innerDelimiter != null) {
                val tokens = item.split(innerDelimiter)
                if (tokens.size >= 4) {
                    LegacySlideRecord(
                        id = tokens[0].trim(),
                        image = tokens[1].trim(),
                        title = tokens[2].trim(),
                        durationSeconds = tokens[3].trim().toIntOrNull() ?: 10
                    )
                } else null
            } else null
        }
    }

    /**
     * Converts legacy slide records into valid AnnouncementSlides.
     * - Drops remote HTTP/HTTPS URLs.
     * - Copies readable local image files into contentRepository.imagesDir.
     * - Drops slides with unreadable or missing local image files.
     */
    fun convertRecords(
        records: List<LegacySlideRecord>,
        contentRepository: ContentRepository
    ): List<AnnouncementSlide> {
        val validSlides = mutableListOf<AnnouncementSlide>()

        for ((index, record) in records.withIndex()) {
            val imageUri = record.image

            // 1. Drop remote HTTP/HTTPS URLs
            if (imageUri.startsWith("http://", ignoreCase = true) ||
                imageUri.startsWith("https://", ignoreCase = true)
            ) {
                continue
            }

            // 2. Validate local file
            if (imageUri.isBlank()) {
                continue
            }

            val filePath = if (imageUri.startsWith("file://")) {
                imageUri.removePrefix("file://")
            } else {
                imageUri
            }

            val sourceFile = File(filePath)
            if (!sourceFile.exists() || !sourceFile.isFile || !sourceFile.canRead()) {
                // Drop unreadable or missing file
                continue
            }

            // 3. Copy to app-private content/images directory
            val ext = sourceFile.extension.ifBlank { "jpg" }
            val targetFilename = "migrated_${UUID.randomUUID()}.$ext"
            val targetFile = contentRepository.getImageFile(targetFilename)

            try {
                sourceFile.copyTo(targetFile, overwrite = true)
                validSlides.add(
                    AnnouncementSlide(
                        id = if (record.id.isNotBlank()) record.id else UUID.randomUUID().toString(),
                        type = SlideType.IMAGE,
                        title = record.title.take(ContentRepository.MAX_TITLE_LENGTH),
                        body = "",
                        imageFile = targetFilename,
                        durationSeconds = record.durationSeconds.coerceIn(
                            ContentRepository.MIN_DURATION_SECONDS,
                            ContentRepository.MAX_DURATION_SECONDS
                        ),
                        enabled = true,
                        sortOrder = index
                    )
                )
            } catch (_: Exception) {
                // Drop if file copy fails
                continue
            }
        }

        return validSlides
    }

    /**
     * Performs one-time migration from DataStore's slides_raw key into ContentRepository.
     * Removes the slides_raw key from DataStore when complete.
     */
    suspend fun migrateIfNeeded(context: Context, contentRepository: ContentRepository) {
        val prefs = try {
            context.dataStore.data.first()
        } catch (_: Exception) {
            return
        }

        val raw = prefs[SLIDES_RAW_KEY] ?: return

        if (raw.isNotBlank()) {
            val records = parseLegacySlides(raw)
            val converted = convertRecords(records, contentRepository)

            // Only populate if contentRepository is currently empty
            if (converted.isNotEmpty() && contentRepository.slidesFlow.value.slides.isEmpty()) {
                val currentRev = contentRepository.slidesFlow.value.revision
                contentRepository.replaceAll(currentRev, converted.take(ContentRepository.MAX_SLIDES))
            }
        }

        // Always remove the legacy key once processed
        try {
            context.dataStore.edit { preferences ->
                preferences.remove(SLIDES_RAW_KEY)
            }
        } catch (_: Exception) {
            // Ignore DataStore edit failure
        }
    }
}
