// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.picker

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.salatsign.tv.R
import com.salatsign.tv.data.importing.ImportKind
import java.io.File
import java.util.Locale

data class StorageDrive(
    val id: String,
    val name: String,
    val rootDir: File?,
    val isUsb: Boolean,
    val isMediaStore: Boolean = false
)

data class StorageItem(
    val file: File?,
    val uri: Uri? = null,
    val name: String,
    val isDirectory: Boolean,
    val sizeFormatted: String = "",
    val extension: String = ""
)

object StorageVolumeManager {

    private const val TAG = "StorageVolumeManager"
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "m4a", "ogg", "aac", "flac")

    fun getAvailableDrives(context: Context): List<StorageDrive> {
        val drives = mutableListOf<StorageDrive>()

        // 1. Primary Internal Shared Storage
        val primary = Environment.getExternalStorageDirectory()
        if (primary != null && primary.exists() && primary.canRead()) {
            drives.add(
                StorageDrive(
                    id = "internal",
                    name = context.getString(R.string.storage_internal),
                    rootDir = primary,
                    isUsb = false
                )
            )
        }

        // 2. Discover USB and Removable Drives under /storage
        val storageRoot = File("/storage")
        if (storageRoot.exists() && storageRoot.isDirectory) {
            storageRoot.listFiles()?.forEach { file ->
                val name = file.name.lowercase(Locale.ROOT)
                if (file.isDirectory && file.canRead() &&
                    name != "emulated" && name != "self" && name != "knox" && !name.startsWith(".")
                ) {
                    if (drives.none { it.rootDir?.absolutePath == file.absolutePath }) {
                        drives.add(
                            StorageDrive(
                                id = "usb_${file.name}",
                                name = context.getString(R.string.storage_usb, file.name),
                                rootDir = file,
                                isUsb = true
                            )
                        )
                    }
                }
            }
        }

        // 3. Fallback via ContextCompat external files dirs (Android 8+ / USB mount points)
        try {
            val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
            for (dir in externalDirs) {
                if (dir != null) {
                    val root = findStorageRoot(dir)
                    if (root != null && root.canRead() && drives.none { it.rootDir?.absolutePath == root.absolutePath }) {
                        drives.add(
                            StorageDrive(
                                id = "ext_${root.name}",
                                name = context.getString(R.string.storage_external, root.name),
                                rootDir = root,
                                isUsb = true
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // 4. MediaStore virtual library drive (for Scoped Storage & TV media compatibility)
        drives.add(
            StorageDrive(
                id = "mediastore",
                name = context.getString(R.string.storage_mediastore_title),
                rootDir = null,
                isUsb = false,
                isMediaStore = true
            )
        )

        return drives
    }

    private fun findStorageRoot(file: File): File? {
        var current: File? = file
        while (current != null) {
            val parent = current.parentFile
            if (parent != null && parent.absolutePath == "/storage") {
                return current
            }
            current = parent
        }
        return null
    }

    fun listDirectoryContents(context: Context, dir: File, kind: ImportKind): List<StorageItem> {
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) {
            return emptyList()
        }

        val allowedExtensions = when (kind) {
            ImportKind.SLIDE_IMAGE, ImportKind.MOSQUE_LOGO -> IMAGE_EXTENSIONS
            ImportKind.CUSTOM_ADHAN -> AUDIO_EXTENSIONS
        }

        val children = dir.listFiles() ?: return emptyList()

        val directories = children
            .filter { it.isDirectory && !it.name.startsWith(".") && it.canRead() }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
            .map { folder ->
                val childCount = folder.listFiles()?.size ?: 0
                val countText = if (childCount == 1) {
                    context.getString(R.string.storage_item_single)
                } else {
                    context.getString(R.string.storage_items_count, childCount)
                }
                StorageItem(
                    file = folder,
                    uri = null,
                    name = folder.name,
                    isDirectory = true,
                    sizeFormatted = countText
                )
            }

        val files = children
            .filter { it.isFile && !it.name.startsWith(".") && it.extension.lowercase(Locale.ROOT) in allowedExtensions }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
            .map { file ->
                StorageItem(
                    file = file,
                    uri = null,
                    name = file.name,
                    isDirectory = false,
                    sizeFormatted = formatFileSize(file.length()),
                    extension = file.extension.uppercase(Locale.ROOT)
                )
            }

        return directories + files
    }

    fun queryMediaStore(context: Context, kind: ImportKind): List<StorageItem> {
        val items = mutableListOf<StorageItem>()
        try {
            val collectionUri = when (kind) {
                ImportKind.SLIDE_IMAGE, ImportKind.MOSQUE_LOGO -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                }
                ImportKind.CUSTOM_ADHAN -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    } else {
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                }
            }

            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE
            )

            context.contentResolver.query(
                collectionUri,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)

                while (cursor.moveToNext()) {
                    if (idCol != -1 && nameCol != -1 && sizeCol != -1) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: "item_$id"
                        val size = cursor.getLong(sizeCol)
                        val contentUri = ContentUris.withAppendedId(collectionUri, id)
                        val ext = name.substringAfterLast('.', "").uppercase(Locale.ROOT)

                        items.add(
                            StorageItem(
                                file = null,
                                uri = contentUri,
                                name = name,
                                isDirectory = false,
                                sizeFormatted = formatFileSize(size),
                                extension = ext
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query MediaStore for $kind", e)
        }
        return items
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
