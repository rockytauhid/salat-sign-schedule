package com.salatsign.tv.ui.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.salatsign.tv.data.importing.AssetImporter
import com.salatsign.tv.data.importing.ImportKind
import com.salatsign.tv.data.importing.ImportResult
import com.salatsign.tv.ui.common.tvFocusable
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.DarkBackground
import com.salatsign.tv.ui.theme.DarkSurface
import com.salatsign.tv.ui.theme.DarkSurfaceVariant
import com.salatsign.tv.ui.theme.TvTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun StorageFilePickerScreen(
    kind: ImportKind,
    title: String,
    onFileImported: (relativeFileName: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val importer = remember { AssetImporter(context) }

    var drives by remember { mutableStateOf<List<StorageDrive>>(emptyList()) }
    var selectedDrive by remember { mutableStateOf<StorageDrive?>(null) }
    var currentDir by remember { mutableStateOf<File?>(null) }
    var items by remember { mutableStateOf<List<StorageItem>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load available drives on launch
    LaunchedEffect(Unit) {
        val available = withContext(Dispatchers.IO) {
            StorageVolumeManager.getAvailableDrives(context)
        }
        drives = available
        if (available.isNotEmpty()) {
            val defaultDrive = available.firstOrNull { it.isUsb } ?: available.first()
            selectedDrive = defaultDrive
            currentDir = defaultDrive.rootDir
        }
    }

    // Load folder contents when currentDir changes
    LaunchedEffect(currentDir, kind) {
        val dir = currentDir
        if (dir != null) {
            items = withContext(Dispatchers.IO) {
                StorageVolumeManager.listDirectoryContents(dir, kind)
            }
        } else {
            items = emptyList()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.96f))
            .padding(horizontal = 48.dp, vertical = 27.dp) // 10-foot TV overscan safety
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        style = TvTypography.HeaderTitle.copy(fontSize = 28.sp, color = AccentEmerald)
                    )
                    Text(
                        text = when (kind) {
                            ImportKind.SLIDE_IMAGE -> "Mendukung JPG, PNG, WEBP (Maksimal resolusi Full HD 1080p)"
                            ImportKind.MOSQUE_LOGO -> "Mendukung JPG, PNG (Maksimal resolusi 512x512)"
                            ImportKind.CUSTOM_ADHAN -> "Mendukung MP3, WAV, M4A, OGG (Maksimal 10MB / 10 menit)"
                        },
                        style = TvTypography.Subtitle.copy(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Surface(
                    onClick = onDismiss,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = DarkSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.tvFocusable()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = "Batal", style = TvTypography.BodyLabel.copy(color = MaterialTheme.colorScheme.onSurface))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Drive Selector Tabs
            if (drives.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(drives) { drive ->
                        val isSelected = drive.id == selectedDrive?.id
                        Surface(
                            onClick = {
                                selectedDrive = drive
                                currentDir = drive.rootDir
                                errorMessage = null
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isSelected) AccentGold.copy(alpha = 0.2f) else DarkSurface,
                                focusedContainerColor = AccentGold
                            ),
                            modifier = (if (isSelected) Modifier.border(1.dp, AccentGold, RoundedCornerShape(8.dp)) else Modifier)
                                .tvFocusable()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (drive.isUsb) Icons.Default.Usb else Icons.Default.Storage,
                                    contentDescription = drive.name,
                                    tint = if (isSelected) AccentGold else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = drive.name,
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) AccentGold else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Directory Breadcrumb
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lokasi: ${currentDir?.absolutePath ?: "-"}",
                    style = TvTypography.BodyLabel.copy(
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error banner if any
            if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = TvTypography.BodyLabel.copy(color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // File & Directory List
            val isAtRoot = currentDir?.absolutePath == selectedDrive?.rootDir?.absolutePath

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // "Go to parent folder" item
                if (!isAtRoot && currentDir?.parentFile != null) {
                    item {
                        Surface(
                            onClick = {
                                currentDir = currentDir?.parentFile
                                errorMessage = null
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = DarkSurface,
                                focusedContainerColor = AccentEmerald
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusable()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali ke folder atas",
                                    tint = AccentGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = ".. [Folder Atas]",
                                    style = TvTypography.BodyLabel.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AccentGold)
                                )
                            }
                        }
                    }
                }

                if (items.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.SdCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tidak ada file media yang didukung di folder ini.",
                                    style = TvTypography.Subtitle.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = "Pastikan USB flashdisk terpasang atau salin file ke folder Download / Internal.",
                                    style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                )
                            }
                        }
                    }
                } else {
                    items(items) { item ->
                        Surface(
                            onClick = {
                                if (item.isDirectory) {
                                    currentDir = item.file
                                    errorMessage = null
                                } else if (!isProcessing) {
                                    isProcessing = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            when (kind) {
                                                ImportKind.SLIDE_IMAGE -> importer.importSlideImage(item.file)
                                                ImportKind.MOSQUE_LOGO -> importer.importMosqueLogo(item.file)
                                                ImportKind.CUSTOM_ADHAN -> importer.importCustomAdhan(item.file)
                                            }
                                        }
                                        isProcessing = false
                                        if (result.success && result.relativeFileName != null) {
                                            onFileImported(result.relativeFileName)
                                            onDismiss()
                                        } else {
                                            errorMessage = result.errorMessage ?: "Gagal mengimpor file"
                                        }
                                    }
                                }
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = DarkSurface,
                                focusedContainerColor = AccentEmerald
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusable()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = when {
                                            item.isDirectory -> Icons.Default.Folder
                                            kind == ImportKind.CUSTOM_ADHAN -> Icons.Default.Audiotrack
                                            else -> Icons.Default.Image
                                        },
                                        contentDescription = null,
                                        tint = if (item.isDirectory) AccentGold else AccentEmerald,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = item.name,
                                        style = TvTypography.BodyLabel.copy(
                                            fontSize = 15.sp,
                                            fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = item.sizeFormatted,
                                        style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
