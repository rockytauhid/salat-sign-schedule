package com.salatsign.tv.ui.picker

import android.content.Context
import android.os.Environment
import androidx.core.content.ContextCompat
import com.salatsign.tv.data.importing.ImportKind
import java.io.File
import java.util.Locale

data class StorageDrive(
    val id: String,
    val name: String,
    val rootDir: File,
    val isUsb: Boolean
)

data class StorageItem(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val sizeFormatted: String = "",
    val extension: String = ""
)

object StorageVolumeManager {

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
                    name = "Penyimpanan Internal",
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
                    // Check if already included
                    if (drives.none { it.rootDir.absolutePath == file.absolutePath }) {
                        drives.add(
                            StorageDrive(
                                id = "usb_${file.name}",
                                name = "USB Flashdisk (${file.name})",
                                rootDir = file,
                                isUsb = true
                            )
                        )
                    }
                }
            }
        }

        // 3. Fallback via ContextCompat external files dirs
        try {
            val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
            for (dir in externalDirs) {
                if (dir != null) {
                    val root = findStorageRoot(dir)
                    if (root != null && root.canRead() && drives.none { it.rootDir.absolutePath == root.absolutePath }) {
                        drives.add(
                            StorageDrive(
                                id = "ext_${root.name}",
                                name = "Drive Eksternal (${root.name})",
                                rootDir = root,
                                isUsb = true
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

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

    fun listDirectoryContents(dir: File, kind: ImportKind): List<StorageItem> {
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
                StorageItem(
                    file = folder,
                    name = folder.name,
                    isDirectory = true,
                    sizeFormatted = "$childCount item"
                )
            }

        val files = children
            .filter { it.isFile && !it.name.startsWith(".") && it.extension.lowercase(Locale.ROOT) in allowedExtensions }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
            .map { file ->
                StorageItem(
                    file = file,
                    name = file.name,
                    isDirectory = false,
                    sizeFormatted = formatFileSize(file.length()),
                    extension = file.extension.uppercase(Locale.ROOT)
                )
            }

        return directories + files
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
