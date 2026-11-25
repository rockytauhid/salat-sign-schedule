// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.settings

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.salatsign.core.tv.R
import com.salatsign.tv.R as AppR
import com.salatsign.tv.security.ActivePairingCode
import com.salatsign.tv.security.RemoteRole
import com.salatsign.tv.ui.common.tvSelectableSurfaceBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceColors
import com.salatsign.tv.ui.common.tvSurfaceScale
import com.salatsign.tv.ui.theme.TvTypography
import kotlinx.coroutines.delay

@Composable
fun PairPhoneDialog(
    phoneControllerUrl: String?,
    pairedDeviceCount: Int,
    onGenerateCode: (RemoteRole) -> ActivePairingCode,
    onCancelCode: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedRole by remember { mutableStateOf(RemoteRole.MANAGE) }
    var activeCode by remember { mutableStateOf(onGenerateCode(RemoteRole.MANAGE)) }
    var secondsLeft by remember { mutableIntStateOf(120) }
    val initialPairedCount = remember { pairedDeviceCount }
    val closeFocusRequester = remember { FocusRequester() }

    // Auto-close as soon as a new phone finishes pairing
    LaunchedEffect(pairedDeviceCount) {
        if (pairedDeviceCount > initialPairedCount) {
            onDismiss()
        }
    }

    LaunchedEffect(activeCode) {
        while (true) {
            val rem = ((activeCode.expiresAtEpochMillis - System.currentTimeMillis() + 999L) / 1000L)
                .toInt()
                .coerceAtLeast(0)
            secondsLeft = rem
            if (rem == 0) break
            delay(1000L)
        }
    }

    LaunchedEffect(Unit) {
        runCatching { closeFocusRequester.requestFocus() }
    }

    DisposableEffect(Unit) {
        onDispose { onCancelCode() }
    }

    val qrUrl = remember(phoneControllerUrl, activeCode.code) {
        phoneControllerUrl?.let { "$it/#pair=${activeCode.code}" }
    }
    val qrMatrix: BitMatrix? = remember(qrUrl) {
        qrUrl?.let { url ->
            runCatching {
                QRCodeWriter().encode(
                    url,
                    BarcodeFormat.QR_CODE,
                    0,
                    0,
                    mapOf(EncodeHintType.MARGIN to 1)
                )
            }.getOrNull()
        }
    }

    val formattedCode = remember(activeCode.code) {
        if (activeCode.code.length == 6) {
            "${activeCode.code.substring(0, 3)} ${activeCode.code.substring(3)}"
        } else {
            activeCode.code
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f)),
            contentAlignment = Alignment.Center
        ) {
            val shape = RoundedCornerShape(18.dp)
            Column(
                modifier = Modifier
                    .width(760.dp)
                    .background(colorScheme.background, shape)
                    .border(1.5.dp, colorScheme.primary.copy(alpha = 0.6f), shape)
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = stringResource(AppR.string.pair_dialog_title),
                    style = TvTypography.HeaderTitle.copy(fontSize = 24.sp, color = colorScheme.onBackground)
                )

                // Role selector (Manage vs Notify)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(AppR.string.pair_role_label),
                        style = TvTypography.BodyLabel.copy(fontSize = 15.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
                    )
                    listOf(
                        RemoteRole.MANAGE to stringResource(AppR.string.pair_role_manage),
                        RemoteRole.NOTIFY to stringResource(AppR.string.pair_role_notify)
                    ).forEach { (role, label) ->
                        val isSelected = selectedRole == role
                        val chipShape = RoundedCornerShape(10.dp)
                        Surface(
                            onClick = {
                                if (selectedRole != role) {
                                    selectedRole = role
                                    activeCode = onGenerateCode(role)
                                }
                            },
                            shape = ClickableSurfaceDefaults.shape(chipShape),
                            scale = tvSurfaceScale(1.02f),
                            colors = tvSelectableSurfaceColors(selected = isSelected),
                            border = tvSelectableSurfaceBorder(selected = isSelected, shape = chipShape)
                        ) {
                            Text(
                                text = label,
                                style = TvTypography.ButtonText.copy(fontSize = 14.sp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Offline QR Code
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrMatrix != null) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cols = qrMatrix.width
                                val rows = qrMatrix.height
                                val cellW = size.width / cols.toFloat()
                                val cellH = size.height / rows.toFloat()
                                for (y in 0 until rows) {
                                    for (x in 0 until cols) {
                                        if (qrMatrix[x, y]) {
                                            drawRect(
                                                color = Color.Black,
                                                topLeft = Offset(x * cellW, y * cellH),
                                                size = Size(cellW + 0.5f, cellH + 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.settings_phone_controller_no_wifi),
                                style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = Color.DarkGray)
                            )
                        }
                    }

                    // Code & instructions
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(AppR.string.pair_dialog_instructions),
                            style = TvTypography.BodyLabel.copy(fontSize = 15.sp, color = colorScheme.onSurface.copy(alpha = 0.85f))
                        )
                        if (!phoneControllerUrl.isNullOrBlank()) {
                            Text(
                                text = phoneControllerUrl,
                                style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.secondary)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedCode,
                            style = TvTypography.ActivePrayerTime.copy(
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp,
                                color = if (secondsLeft > 0) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        )
                        Text(
                            text = if (secondsLeft > 0) {
                                stringResource(AppR.string.pair_code_expires_in, secondsLeft)
                            } else {
                                stringResource(AppR.string.pair_code_expired)
                            },
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 14.sp,
                                color = if (secondsLeft > 0) colorScheme.onSurface.copy(alpha = 0.75f) else colorScheme.secondary
                            )
                        )
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    val btnShape = RoundedCornerShape(10.dp)
                    Surface(
                        onClick = { activeCode = onGenerateCode(selectedRole) },
                        shape = ClickableSurfaceDefaults.shape(btnShape),
                        scale = tvSurfaceScale(1.02f),
                        colors = tvSelectableSurfaceColors(selected = false),
                        border = tvSelectableSurfaceBorder(selected = false, shape = btnShape)
                    ) {
                        Text(
                            text = stringResource(AppR.string.pair_btn_new_code),
                            style = TvTypography.ButtonText.copy(fontSize = 14.sp),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                    Surface(
                        onClick = onDismiss,
                        shape = ClickableSurfaceDefaults.shape(btnShape),
                        scale = tvSurfaceScale(1.02f),
                        colors = tvSelectableSurfaceColors(selected = true),
                        border = tvSelectableSurfaceBorder(selected = true, shape = btnShape),
                        modifier = Modifier.focusRequester(closeFocusRequester)
                    ) {
                        Text(
                            text = stringResource(AppR.string.pair_btn_close),
                            style = TvTypography.ButtonText.copy(fontSize = 14.sp),
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
