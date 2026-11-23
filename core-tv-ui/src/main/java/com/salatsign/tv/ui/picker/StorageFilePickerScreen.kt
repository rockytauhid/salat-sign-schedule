// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.picker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PermDeviceInformation
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdateAlt
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.salatsign.core.tv.R
import com.salatsign.tv.data.importing.AssetImporter
import com.salatsign.tv.data.importing.ImportKind
import com.salatsign.tv.data.importing.ImportResult
import com.salatsign.tv.ui.common.tvSelectableSurfaceBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceColors
import com.salatsign.tv.ui.common.tvSurfaceScale
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
    modifier: Modifier = Modifier,
    onUserInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val importer = remember { AssetImporter(context) }
    val colorScheme = MaterialTheme.colorScheme

    var drives by remember { mutableStateOf<List<StorageDrive>>(emptyList()) }
    var selectedDrive by remember { mutableStateOf<StorageDrive?>(null) }
    var currentDir by remember { mutableStateOf<File?>(null) }
    var items by remember { mutableStateOf<List<StorageItem>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var didFocusInitialDrive by remember { mutableStateOf(false) }

    val closeFocusRequester = remember { FocusRequester() }
    val firstDriveFocusRequester = remember { FocusRequester() }

    // Check runtime permission for API <= 32
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStoragePermission = granted
        if (granted) {
            errorMessage = null
            coroutineScope.launch {
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
        } else {
            errorMessage = context.getString(R.string.storage_permission_denied)
        }
    }

    // System file picker launcher via SAF (ACTION_OPEN_DOCUMENT)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            errorMessage = null
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    importer.importFromUri(context, uri, kind)
                }
                isProcessing = false
                val importedName = result.relativeFileName
                if (result.success && importedName != null) {
                    onFileImported(importedName)
                    onDismiss()
                } else {
                    errorMessage = result.errorMessage ?: context.getString(R.string.storage_import_failed)
                }
            }
        }
    }

    val isSystemPickerAvailable = remember {
        val testIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = when (kind) {
                ImportKind.SLIDE_IMAGE, ImportKind.MOSQUE_LOGO -> "image/*"
                ImportKind.CUSTOM_ADHAN -> "audio/*"
            }
        }
        testIntent.resolveActivity(context.packageManager) != null
    }

    // Request permission on launch if needed (API <= 32)
    LaunchedEffect(Unit) {
        runCatching { closeFocusRequester.requestFocus() }
        if (!hasStoragePermission && Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
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
    }

    // Move focus to first drive tab once drives finish loading initially
    LaunchedEffect(drives) {
        if (drives.isNotEmpty() && !didFocusInitialDrive) {
            didFocusInitialDrive = true
            runCatching { firstDriveFocusRequester.requestFocus() }
        }
    }

    // Load folder / MediaStore contents when selection changes
    LaunchedEffect(selectedDrive, currentDir, kind, hasStoragePermission) {
        val drive = selectedDrive
        if (drive != null && drive.isMediaStore) {
            items = withContext(Dispatchers.IO) {
                StorageVolumeManager.queryMediaStore(context, kind)
            }
        } else {
            val dir = currentDir
            if (dir != null) {
                items = withContext(Dispatchers.IO) {
                    StorageVolumeManager.listDirectoryContents(context, dir, kind)
                }
            } else {
                items = emptyList()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(horizontal = 48.dp, vertical = 27.dp) // 10-foot TV overscan safety
                .onPreviewKeyEvent {
                    onUserInteraction()
                    false
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header: Title, Optional System Picker Button, and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = TvTypography.HeaderTitle.copy(fontSize = 28.sp, color = colorScheme.primary)
                        )
                        Text(
                            text = when (kind) {
                                ImportKind.SLIDE_IMAGE -> stringResource(R.string.storage_hint_slide_image)
                                ImportKind.MOSQUE_LOGO -> stringResource(R.string.storage_hint_mosque_logo)
                                ImportKind.CUSTOM_ADHAN -> stringResource(R.string.storage_hint_custom_adhan)
                            },
                            style = TvTypography.Subtitle.copy(
                                fontSize = 14.sp,
                                color = colorScheme.onSurface.copy(alpha = 0.78f)
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val headerBtnShape = RoundedCornerShape(8.dp)
                        if (isSystemPickerAvailable) {
                            Surface(
                                onClick = {
                                    val mimes = when (kind) {
                                        ImportKind.SLIDE_IMAGE, ImportKind.MOSQUE_LOGO -> arrayOf("image/*")
                                        ImportKind.CUSTOM_ADHAN -> arrayOf("audio/*")
                                    }
                                    openDocumentLauncher.launch(mimes)
                                },
                                shape = ClickableSurfaceDefaults.shape(headerBtnShape),
                                scale = tvSurfaceScale(1.03f),
                                colors = tvSelectableSurfaceColors(selected = false),
                                border = tvSelectableSurfaceBorder(selected = false, shape = headerBtnShape)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdateAlt,
                                        contentDescription = stringResource(R.string.storage_system_picker),
                                        tint = colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.storage_system_picker),
                                        style = TvTypography.BodyLabel.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorScheme.secondary
                                        )
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = onDismiss,
                            shape = ClickableSurfaceDefaults.shape(headerBtnShape),
                            scale = tvSurfaceScale(1.03f),
                            colors = tvSelectableSurfaceColors(selected = false),
                            border = tvSelectableSurfaceBorder(selected = false, shape = headerBtnShape),
                            modifier = Modifier.focusRequester(closeFocusRequester)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.btn_close),
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.btn_cancel),
                                    style = TvTypography.BodyLabel.copy(color = colorScheme.onSurface)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Permission Request Banner if not granted (API <= 32)
                if (!hasStoragePermission && Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    val permShape = RoundedCornerShape(10.dp)
                    Surface(
                        onClick = { permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE) },
                        shape = ClickableSurfaceDefaults.shape(permShape),
                        scale = tvSurfaceScale(1.01f),
                        colors = tvSelectableSurfaceColors(selected = true),
                        border = tvSelectableSurfaceBorder(selected = true, shape = permShape),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PermDeviceInformation,
                                    contentDescription = null,
                                    tint = colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = stringResource(R.string.storage_permission_rationale),
                                    style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.storage_grant_permission),
                                style = TvTypography.BodyLabel.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.secondary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Drive Selector Tabs
                if (drives.isNotEmpty()) {
                    val tabShape = RoundedCornerShape(8.dp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(drives) { index, drive ->
                            val isSelected = drive.id == selectedDrive?.id
                            Surface(
                                onClick = {
                                    selectedDrive = drive
                                    currentDir = drive.rootDir
                                    errorMessage = null
                                },
                                shape = ClickableSurfaceDefaults.shape(tabShape),
                                scale = tvSurfaceScale(1.03f),
                                colors = tvSelectableSurfaceColors(selected = isSelected),
                                border = tvSelectableSurfaceBorder(selected = isSelected, shape = tabShape),
                                modifier = if (index == 0) {
                                    Modifier.focusRequester(firstDriveFocusRequester)
                                } else {
                                    Modifier
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = when {
                                            drive.isUsb -> Icons.Default.Usb
                                            drive.isMediaStore -> Icons.Default.SdCard
                                            else -> Icons.Default.Storage
                                        },
                                        contentDescription = drive.name,
                                        tint = if (isSelected) colorScheme.primary else colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = drive.name,
                                        style = TvTypography.BodyLabel.copy(
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Current Directory / Location Breadcrumb
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val locationDisplay = when {
                        selectedDrive?.isMediaStore == true -> stringResource(R.string.storage_mediastore_title)
                        else -> currentDir?.absolutePath ?: "-"
                    }
                    Text(
                        text = stringResource(R.string.storage_location_prefix, locationDisplay),
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colorScheme.onSurface.copy(alpha = 0.82f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Error banner if any
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorScheme.error.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                            .border(1.dp, colorScheme.error, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = TvTypography.BodyLabel.copy(color = colorScheme.error, fontSize = 14.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // File & Directory List
                val isAtRoot = currentDir?.absolutePath == selectedDrive?.rootDir?.absolutePath
                val rowShape = RoundedCornerShape(8.dp)

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // "Go to parent folder" item (only for file system drives)
                    if (selectedDrive?.isMediaStore != true && !isAtRoot && currentDir?.parentFile != null) {
                        item {
                            Surface(
                                onClick = {
                                    currentDir = currentDir?.parentFile
                                    errorMessage = null
                                },
                                shape = ClickableSurfaceDefaults.shape(rowShape),
                                scale = tvSurfaceScale(1.01f),
                                colors = tvSelectableSurfaceColors(selected = false),
                                border = tvSelectableSurfaceBorder(selected = false, shape = rowShape),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.storage_parent_folder),
                                        tint = colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.storage_parent_folder),
                                        style = TvTypography.BodyLabel.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorScheme.secondary
                                        )
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
                                    .background(colorScheme.surface, RoundedCornerShape(12.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.SdCard,
                                        contentDescription = null,
                                        tint = colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.storage_empty_folder),
                                        style = TvTypography.Subtitle.copy(
                                            fontSize = 16.sp,
                                            color = colorScheme.onSurface.copy(alpha = 0.85f)
                                        )
                                    )
                                    Text(
                                        text = stringResource(R.string.storage_empty_hint),
                                        style = TvTypography.BodyLabel.copy(
                                            fontSize = 13.sp,
                                            color = colorScheme.onSurface.copy(alpha = 0.65f)
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        items(items) { item ->
                            Surface(
                                onClick = {
                                    if (item.isDirectory && item.file != null) {
                                        currentDir = item.file
                                        errorMessage = null
                                    } else if (!isProcessing) {
                                        isProcessing = true
                                        errorMessage = null
                                        coroutineScope.launch {
                                            val result = withContext(Dispatchers.IO) {
                                                if (item.uri != null) {
                                                    importer.importFromUri(context, item.uri, kind, item.name)
                                                } else if (item.file != null) {
                                                    when (kind) {
                                                        ImportKind.SLIDE_IMAGE -> importer.importSlideImage(item.file)
                                                        ImportKind.MOSQUE_LOGO -> importer.importMosqueLogo(item.file)
                                                        ImportKind.CUSTOM_ADHAN -> importer.importCustomAdhan(item.file)
                                                    }
                                                } else {
                                                    ImportResult(success = false, errorMessage = "No file or URI available")
                                                }
                                            }
                                            isProcessing = false
                                            val importedName = result.relativeFileName
                                            if (result.success && importedName != null) {
                                                onFileImported(importedName)
                                                onDismiss()
                                            } else {
                                                errorMessage = result.errorMessage ?: context.getString(R.string.storage_import_failed)
                                            }
                                        }
                                    }
                                },
                                shape = ClickableSurfaceDefaults.shape(rowShape),
                                scale = tvSurfaceScale(1.01f),
                                colors = tvSelectableSurfaceColors(selected = false),
                                border = tvSelectableSurfaceBorder(selected = false, shape = rowShape),
                                modifier = Modifier.fillMaxWidth()
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
                                            tint = if (item.isDirectory) colorScheme.secondary else colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = item.name,
                                            style = TvTypography.BodyLabel.copy(
                                                fontSize = 15.sp,
                                                fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                                                color = colorScheme.onSurface
                                            )
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = item.sizeFormatted,
                                            style = TvTypography.BodyLabel.copy(
                                                fontSize = 12.sp,
                                                color = colorScheme.onSurface.copy(alpha = 0.78f)
                                            )
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
}
