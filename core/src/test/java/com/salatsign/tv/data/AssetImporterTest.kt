// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data

import com.salatsign.tv.data.importing.AssetImporter
import com.salatsign.tv.data.importing.AudioValidator
import com.salatsign.tv.data.importing.ImageDownscaler
import com.salatsign.tv.data.importing.ImportKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AssetImporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var baseDir: File
    private lateinit var importer: AssetImporter

    // Fake image downscaler for deterministic JVM unit testing
    private val fakeDownscaler = object : ImageDownscaler {
        override fun downscaleAndSave(sourceBytes: ByteArray, targetFile: File, maxWidth: Int, maxHeight: Int): File {
            targetFile.parentFile?.mkdirs()
            targetFile.writeBytes(sourceBytes)
            return targetFile
        }
    }

    // Fake audio validator
    private var audioValidationResult: Result<Unit> = Result.success(Unit)
    private val fakeAudioValidator = object : AudioValidator {
        override fun validateAudio(file: File): Result<Unit> {
            return audioValidationResult
        }
    }

    @Before
    fun setUp() {
        baseDir = tempFolder.newFolder("asset_importer_test")
        importer = AssetImporter(
            baseDir = baseDir,
            downscaler = fakeDownscaler,
            audioValidator = fakeAudioValidator
        )
    }

    @Test
    fun importSlideImage_createsImageInImagesDirectory() {
        val dummyImage = tempFolder.newFile("sample_poster.jpg")
        dummyImage.writeText("fake image bytes")

        val result = importer.importSlideImage(dummyImage)
        assertTrue("Import must succeed", result.success)
        assertTrue("Must return relative file name", result.relativeFileName != null)
        assertTrue(result.relativeFileName!!.startsWith("slide_image_"))
        assertTrue(result.relativeFileName!!.endsWith(".jpg"))

        val targetFile = File(importer.imagesDir, result.relativeFileName!!)
        assertTrue("Imported file must exist in images directory", targetFile.exists())
        assertEquals("fake image bytes", targetFile.readText())
    }

    @Test
    fun importMosqueLogo_createsLogoInLogoDirectory() {
        val dummyLogo = tempFolder.newFile("logo.png")
        dummyLogo.writeText("logo bytes")

        val result = importer.importMosqueLogo(dummyLogo)
        assertTrue("Import must succeed", result.success)
        assertTrue(result.relativeFileName!!.startsWith("mosque_logo_"))
        assertTrue(result.relativeFileName!!.endsWith(".png"))

        val targetFile = File(importer.logoDir, result.relativeFileName!!)
        assertTrue("Imported file must exist in logo directory", targetFile.exists())
        assertEquals("logo bytes", targetFile.readText())
    }

    @Test
    fun importCustomAdhan_validAudio_savesToAudioDirectory() {
        val dummyAudio = tempFolder.newFile("adhan_makkah.mp3")
        dummyAudio.writeBytes(ByteArray(5000) { 1.toByte() })

        val result = importer.importCustomAdhan(dummyAudio)
        assertTrue("Import must succeed", result.success)
        assertTrue(result.relativeFileName!!.startsWith("adhan_"))
        assertTrue(result.relativeFileName!!.endsWith(".mp3"))

        val targetFile = File(importer.audioDir, result.relativeFileName!!)
        assertTrue("Imported file must exist in audio directory", targetFile.exists())
        assertEquals("adhan_makkah.mp3", result.originalName)
    }

    @Test
    fun importCustomAdhan_sameContentTwice_deduplicatesToSingleFileOnDisk() {
        val bytes = ByteArray(4096) { (it % 127).toByte() }
        val file1 = tempFolder.newFile("Mishary_Adhan.mp3").apply { writeBytes(bytes) }
        val file2 = tempFolder.newFile("Standard_Adhan.mp3").apply { writeBytes(bytes) }

        val res1 = importer.importCustomAdhan(file1)
        val res2 = importer.importCustomAdhan(file2)

        assertTrue(res1.success)
        assertTrue(res2.success)
        assertEquals("Identical audio content must reuse the same stored file", res1.relativeFileName, res2.relativeFileName)
        assertEquals("Mishary_Adhan.mp3", res1.originalName)
        assertEquals("Standard_Adhan.mp3", res2.originalName)
        assertEquals("Only one copy should exist on disk", 1, importer.audioDir.listFiles()?.size ?: 0)
    }

    @Test
    fun importCustomAdhan_differentContentSameName_keepsBothFilesDistinct() {
        val dirA = tempFolder.newFolder("a")
        val dirB = tempFolder.newFolder("b")
        val fileA = File(dirA, "adhan.mp3").apply { writeBytes(ByteArray(3000) { 1 }) }
        val fileB = File(dirB, "adhan.mp3").apply { writeBytes(ByteArray(3000) { 2 }) }

        val resA = importer.importCustomAdhan(fileA)
        val resB = importer.importCustomAdhan(fileB)

        assertTrue(resA.success)
        assertTrue(resB.success)
        assertTrue("Different content must not overwrite each other", resA.relativeFileName != resB.relativeFileName)
        assertEquals(2, importer.audioDir.listFiles()?.size ?: 0)
    }

    @Test
    fun importCustomAdhan_exceeds10MB_isRejected() {
        val dummyLargeAudio = tempFolder.newFile("large_adhan.mp3")
        val elevenMegabytes = 11 * 1024 * 1024
        // Write byte array exceeding 10MB
        val bytes = ByteArray(elevenMegabytes)
        dummyLargeAudio.writeBytes(bytes)

        val result = importer.importCustomAdhan(dummyLargeAudio)
        assertFalse("Import must fail when file > 10MB", result.success)
        assertTrue(result.errorMessage?.contains("10MB") == true || result.errorMessage?.contains("10 MB") == true)
    }

    @Test
    fun importCustomAdhan_audioValidatorFailure_deletesFileAndReturnsError() {
        val dummyAudio = tempFolder.newFile("bad_adhan.wav")
        dummyAudio.writeText("corrupt or >10 min audio")

        audioValidationResult = Result.failure(IllegalArgumentException("Audio duration exceeds 10 minutes"))

        val result = importer.importCustomAdhan(dummyAudio)
        assertFalse("Import must fail when validator reports failure", result.success)
        assertEquals("Audio duration exceeds 10 minutes", result.errorMessage)

        // Ensure temp file was cleaned up and audioDir has no files
        assertEquals(0, importer.audioDir.listFiles()?.size ?: 0)
    }

    @Test
    fun listAndDeleteImportedFiles() {
        val dummy1 = tempFolder.newFile("pic1.jpg").apply { writeText("pic1") }
        val dummy2 = tempFolder.newFile("pic2.jpg").apply { writeText("pic2") }

        val res1 = importer.importSlideImage(dummy1)
        val res2 = importer.importSlideImage(dummy2)

        val files = importer.listImportedFiles(ImportKind.SLIDE_IMAGE)
        assertEquals(2, files.size)

        val deleted = importer.deleteImportedFile(ImportKind.SLIDE_IMAGE, res1.relativeFileName!!)
        assertTrue(deleted)

        val filesAfterDelete = importer.listImportedFiles(ImportKind.SLIDE_IMAGE)
        assertEquals(1, filesAfterDelete.size)
        assertEquals(res2.relativeFileName, filesAfterDelete.first().name)
    }

    @Test
    fun pathTraversalAndDisallowedExtensions_areSanitized() {
        val outside = File(baseDir, "outside.txt").apply { writeText("keep me") }
        assertFalse(importer.deleteImportedFile(ImportKind.SLIDE_IMAGE, "../../outside.txt"))
        assertTrue(outside.exists())

        val weirdExt = importer.importSlideImage("hello".byteInputStream(), "poster.jpg/../../evil")
        assertTrue(weirdExt.success)
        assertTrue(weirdExt.relativeFileName!!.endsWith(".jpg"))
        assertFalse(weirdExt.relativeFileName!!.contains(".."))
    }
}
