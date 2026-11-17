// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.importing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Target category for imported images with bounds constraints.
 */
enum class ImageTargetType(
    val maxWidth: Int,
    val maxHeight: Int,
    val prefix: String,
    val defaultExtension: String
) {
    SLIDE(maxWidth = 1920, maxHeight = 1080, prefix = "slide", defaultExtension = "jpg"),
    LOGO(maxWidth = 512, maxHeight = 512, prefix = "logo", defaultExtension = "png")
}

data class ImageDimensions(
    val width: Int,
    val height: Int
)

data class ProcessedImageResult(
    val file: File,
    val relativeFileName: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val metadataStripped: Boolean = true
)

/**
 * Engine interface for probing image dimensions and downscaling/re-encoding bitmaps.
 */
interface ImageProcessingEngine {
    fun probeDimensions(bytes: ByteArray): ImageDimensions?
    fun processAndSave(bytes: ByteArray, destinationFile: File, targetType: ImageTargetType): ProcessedImageResult
}

/**
 * Android implementation of ImageProcessingEngine using BitmapFactory and Bitmap.
 */
class AndroidImageProcessingEngine : ImageProcessingEngine {

    companion object {
        const val MAX_IMAGE_FILE_SIZE = 15 * 1024 * 1024L // 15MB safety limit
        const val MAX_ALLOWED_DIMENSION = 8192 // Prevents decompression bomb attacks
        const val JPEG_COMPRESSION_QUALITY = 85
    }

    override fun probeDimensions(bytes: ByteArray): ImageDimensions? {
        if (bytes.isEmpty()) return null
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return if (options.outWidth > 0 && options.outHeight > 0) {
            ImageDimensions(width = options.outWidth, height = options.outHeight)
        } else {
            null
        }
    }

    override fun processAndSave(
        bytes: ByteArray,
        destinationFile: File,
        targetType: ImageTargetType
    ): ProcessedImageResult {
        require(bytes.isNotEmpty()) { "Image byte buffer is empty" }
        require(bytes.size <= MAX_IMAGE_FILE_SIZE) {
            "Image file size (${bytes.size} bytes) exceeds maximum allowed ${MAX_IMAGE_FILE_SIZE} bytes"
        }

        val dimensions = probeDimensions(bytes)
            ?: throw IllegalArgumentException("Could not decode image bounds or invalid image format")

        require(dimensions.width <= MAX_ALLOWED_DIMENSION && dimensions.height <= MAX_ALLOWED_DIMENSION) {
            "Image dimensions (${dimensions.width}x${dimensions.height}) exceed maximum allowed dimension $MAX_ALLOWED_DIMENSION"
        }

        val inSampleSize = calculateInSampleSize(
            srcWidth = dimensions.width,
            srcHeight = dimensions.height,
            reqWidth = targetType.maxWidth,
            reqHeight = targetType.maxHeight
        )

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }

        val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            ?: throw IllegalStateException("Failed to decode bitmap bytes into memory")

        // Exact aspect-ratio-preserving downscale if still exceeds maximums
        val scale = minOf(
            targetType.maxWidth.toFloat() / decodedBitmap.width,
            targetType.maxHeight.toFloat() / decodedBitmap.height,
            1.0f
        )

        val finalBitmap = if (scale < 1.0f) {
            val targetW = (decodedBitmap.width * scale).toInt().coerceAtLeast(1)
            val targetH = (decodedBitmap.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true)
            if (scaled != decodedBitmap) {
                decodedBitmap.recycle()
            }
            scaled
        } else {
            decodedBitmap
        }

        destinationFile.parentFile?.mkdirs()
        FileOutputStream(destinationFile).use { out ->
            when (targetType) {
                ImageTargetType.LOGO -> finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                ImageTargetType.SLIDE -> finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, out)
            }
        }

        val outWidth = finalBitmap.width
        val outHeight = finalBitmap.height
        finalBitmap.recycle()

        return ProcessedImageResult(
            file = destinationFile,
            relativeFileName = destinationFile.name,
            width = outWidth,
            height = outHeight,
            sizeBytes = destinationFile.length(),
            metadataStripped = true
        )
    }

    private fun calculateInSampleSize(srcWidth: Int, srcHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (srcHeight > reqHeight || srcWidth > reqWidth) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

/**
 * Image importer utility for local files, remote phone controller uploads, and USB media.
 * Enforces memory safety, downscales images to ≤ 1920×1080 (slides) or ≤ 512×512 (logos),
 * re-encodes to strip EXIF and sensitive metadata, and generates server-safe unique filenames.
 */
class ImageImporter(
    val destinationDir: File,
    private val engine: ImageProcessingEngine = AndroidImageProcessingEngine()
) {

    init {
        destinationDir.mkdirs()
    }

    /**
     * Imports and processes image data from an InputStream.
     */
    fun importImage(
        inputStream: InputStream,
        targetType: ImageTargetType,
        originalFileName: String? = null
    ): Result<ProcessedImageResult> {
        return runCatching {
            val bytes = inputStream.use { it.readBytes() }
            importImage(bytes, targetType, originalFileName).getOrThrow()
        }
    }

    /**
     * Imports and processes image data from a raw byte array.
     */
    fun importImage(
        bytes: ByteArray,
        targetType: ImageTargetType,
        originalFileName: String? = null
    ): Result<ProcessedImageResult> {
        return runCatching {
            val serverFileName = generateServerFilename(targetType, originalFileName)
            val targetFile = File(destinationDir, serverFileName)
            engine.processAndSave(bytes, targetFile, targetType)
        }
    }

    /**
     * Generates a unique, server-safe, predictable filename with timestamp and UUID.
     */
    fun generateServerFilename(targetType: ImageTargetType, originalFileName: String? = null): String {
        val extension = when (targetType) {
            ImageTargetType.LOGO -> "png"
            ImageTargetType.SLIDE -> "jpg"
        }
        val timestamp = System.currentTimeMillis()
        val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(8)
        return "${targetType.prefix}_${timestamp}_${randomSuffix}.$extension"
    }

    companion object {
        /**
         * Calculates memory-efficient inSampleSize (power of 2) for BitmapFactory decoding.
         */
        fun calculateInSampleSize(srcWidth: Int, srcHeight: Int, reqWidth: Int, reqHeight: Int): Int {
            var inSampleSize = 1
            if (srcHeight > reqHeight || srcWidth > reqWidth) {
                val halfHeight = srcHeight / 2
                val halfWidth = srcWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }
}
