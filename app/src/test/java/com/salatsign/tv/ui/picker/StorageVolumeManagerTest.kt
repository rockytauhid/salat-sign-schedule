// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.picker

import com.salatsign.tv.data.importing.ImportKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StorageVolumeManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun formatFileSize_formatsCorrectly() {
        assertEquals("500 B", StorageVolumeManager.formatFileSize(500))
        assertEquals("1.0 KB", StorageVolumeManager.formatFileSize(1024))
        assertEquals("1.5 KB", StorageVolumeManager.formatFileSize(1536))
        assertEquals("1.0 MB", StorageVolumeManager.formatFileSize(1024 * 1024))
        assertEquals("5.2 MB", StorageVolumeManager.formatFileSize((5.2 * 1024 * 1024).toLong()))
    }

    @Test
    fun storageItem_dataModelIntegrity() {
        val fileItem = StorageItem(
            file = File("/dummy/image.jpg"),
            uri = null,
            name = "image.jpg",
            isDirectory = false,
            sizeFormatted = "1.2 MB",
            extension = "JPG"
        )
        assertFalse(fileItem.isDirectory)
        assertEquals("image.jpg", fileItem.name)
        assertEquals("JPG", fileItem.extension)
    }

    @Test
    fun storageDrive_mediaStoreFlag() {
        val drive = StorageDrive(
            id = "mediastore",
            name = "Media Library",
            rootDir = null,
            isUsb = false,
            isMediaStore = true
        )
        assertTrue(drive.isMediaStore)
        assertFalse(drive.isUsb)
    }
}
