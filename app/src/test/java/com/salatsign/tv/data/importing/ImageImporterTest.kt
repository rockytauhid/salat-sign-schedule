// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.importing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class ImageImporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var destDir: File

    // Deterministic fake engine for unit tests
    private val fakeEngine = object : ImageProcessingEngine {
        var probeResult: ImageDimensions? = ImageDimensions(2560, 1440)
        var shouldThrow: Boolean = false

        override fun probeDimensions(bytes: ByteArray): ImageDimensions? {
            if (bytes.isEmpty()) return null
            return probeResult
        }

        override fun processAndSave(
            bytes: ByteArray,
            destinationFile: File,
            targetType: ImageTargetType
        ): ProcessedImageResult {
            if (shouldThrow) throw IllegalStateException("Processing failed")
            if (bytes.isEmpty()) throw IllegalArgumentException("Image byte buffer is empty")
            if (bytes.size > AndroidImageProcessingEngine.MAX_IMAGE_FILE_SIZE) {
                throw IllegalArgumentException("Image file size exceeds maximum")
            }

            val dims = probeDimensions(bytes) ?: throw IllegalArgumentException("Invalid bounds")
            if (dims.width > AndroidImageProcessingEngine.MAX_ALLOWED_DIMENSION ||
                dims.height > AndroidImageProcessingEngine.MAX_ALLOWED_DIMENSION) {
                throw IllegalArgumentException("Dimensions exceed maximum allowed")
            }

            // Downscale to target
            val scale = minOf(
                targetType.maxWidth.toFloat() / dims.width,
                targetType.maxHeight.toFloat() / dims.height,
                1.0f
            )
            val finalW = (dims.width * scale).toInt().coerceAtLeast(1)
            val finalH = (dims.height * scale).toInt().coerceAtLeast(1)

            destinationFile.parentFile?.mkdirs()
            destinationFile.writeBytes(bytes)

            return ProcessedImageResult(
                file = destinationFile,
                relativeFileName = destinationFile.name,
                width = finalW,
                height = finalH,
                sizeBytes = destinationFile.length(),
                metadataStripped = true
            )
        }
    }

    @Before
    fun setUp() {
        destDir = tempFolder.newFolder("importer_test_dest")
    }

    @Test
    fun calculateInSampleSize_downscalesLargeDimensions() {
        // 4K (3840x2160) to 1080p (1920x1080)
        val sampleSize4k = ImageImporter.calculateInSampleSize(3840, 2160, 1920, 1080)
        assertEquals(2, sampleSize4k)

        // Exact match
        val sampleSizeExact = ImageImporter.calculateInSampleSize(1920, 1080, 1920, 1080)
        assertEquals(1, sampleSizeExact)

        // Smaller than target
        val sampleSizeSmall = ImageImporter.calculateInSampleSize(800, 600, 1920, 1080)
        assertEquals(1, sampleSizeSmall)

        // Very large image down to logo (512x512)
        val sampleSizeLogo = ImageImporter.calculateInSampleSize(4096, 4096, 512, 512)
        assertTrue("Sample size must be at least 4", sampleSizeLogo >= 4)
    }

    @Test
    fun generateServerFilename_producesValidAndUniqueNames() {
        val importer = ImageImporter(destDir, fakeEngine)

        val slideName1 = importer.generateServerFilename(ImageTargetType.SLIDE)
        val slideName2 = importer.generateServerFilename(ImageTargetType.SLIDE)
        val logoName = importer.generateServerFilename(ImageTargetType.LOGO)

        assertTrue(slideName1.startsWith("slide_"))
        assertTrue(slideName1.endsWith(".jpg"))
        assertTrue(logoName.startsWith("logo_"))
        assertTrue(logoName.endsWith(".png"))

        assertTrue("Generated filenames must be unique", slideName1 != slideName2)
    }

    @Test
    fun importImage_fromByteArray_slideImage_succeeds() {
        val importer = ImageImporter(destDir, fakeEngine)
        val dummyBytes = "JPEG_DATA_STREAM".toByteArray()

        val result = importer.importImage(dummyBytes, ImageTargetType.SLIDE)
        assertTrue(result.isSuccess)

        val processed = result.getOrThrow()
        assertNotNull(processed.file)
        assertTrue(processed.file.exists())
        assertEquals(1920, processed.width)
        assertEquals(1080, processed.height)
        assertTrue(processed.metadataStripped)
        assertTrue(processed.relativeFileName.startsWith("slide_"))
    }

    @Test
    fun importImage_fromInputStream_logoImage_succeeds() {
        fakeEngine.probeResult = ImageDimensions(1024, 1024)
        val importer = ImageImporter(destDir, fakeEngine)
        val dummyStream = ByteArrayInputStream("PNG_DATA_STREAM".toByteArray())

        val result = importer.importImage(dummyStream, ImageTargetType.LOGO)
        assertTrue(result.isSuccess)

        val processed = result.getOrThrow()
        assertTrue(processed.file.exists())
        assertEquals(512, processed.width)
        assertEquals(512, processed.height)
        assertTrue(processed.relativeFileName.startsWith("logo_"))
    }

    @Test
    fun importImage_emptyBytes_returnsFailure() {
        val importer = ImageImporter(destDir, fakeEngine)
        val result = importer.importImage(ByteArray(0), ImageTargetType.SLIDE)
        assertTrue(result.isFailure)
    }

    @Test
    fun importImage_oversizedFile_returnsFailure() {
        val importer = ImageImporter(destDir, fakeEngine)
        // 16MB buffer (exceeds 15MB limit)
        val hugeBytes = ByteArray(16 * 1024 * 1024)
        val result = importer.importImage(hugeBytes, ImageTargetType.SLIDE)
        assertTrue(result.isFailure)
    }

    @Test
    fun importImage_decompressionBombDimensions_returnsFailure() {
        fakeEngine.probeResult = ImageDimensions(9000, 9000)
        val importer = ImageImporter(destDir, fakeEngine)
        val result = importer.importImage("bomb".toByteArray(), ImageTargetType.SLIDE)
        assertTrue(result.isFailure)
    }

    @Test
    fun importImage_corruptedOrUnreadableBounds_returnsFailure() {
        fakeEngine.probeResult = null
        val importer = ImageImporter(destDir, fakeEngine)
        val result = importer.importImage("corrupt".toByteArray(), ImageTargetType.SLIDE)
        assertTrue(result.isFailure)
    }
}
