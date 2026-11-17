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
        assertTrue(result.errorMessage?.contains("10MB") == true)
    }

    @Test
    fun importCustomAdhan_audioValidatorFailure_deletesFileAndReturnsError() {
        val dummyAudio = tempFolder.newFile("bad_adhan.wav")
        dummyAudio.writeText("corrupt or >10 min audio")

        audioValidationResult = Result.failure(IllegalArgumentException("Durasi audio melebihi 10 menit"))

        val result = importer.importCustomAdhan(dummyAudio)
        assertFalse("Import must fail when validator reports failure", result.success)
        assertEquals("Durasi audio melebihi 10 menit", result.errorMessage)

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
}
