// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.common

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.salatsign.core.tv.R
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.security.PinHasher
import com.salatsign.tv.security.PinVerifyResult
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.TvTypography
import kotlinx.coroutines.delay

enum class PinDialogMode {
    VERIFY,
    SET_NEW
}

private enum class SetPinStep {
    ENTER_NEW,
    CONFIRM_NEW
}

@Composable
fun PinDialog(
    mode: PinDialogMode,
    settings: AppSettings,
    onPinVerified: () -> Unit = {},
    onPinChanged: (String) -> Unit = {},
    onPinFailure: (newFailedAttempts: Int, lockedUntilEpochMillis: Long) -> Unit = { _, _ -> },
    onPinSuccess: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val keypadFocusRequesters = remember { List(12) { FocusRequester() } }
    val cancelFocusRequester = remember { FocusRequester() }

    val isResetRequired = remember(mode, settings.hasCustomPin, settings.pinSaltHex, settings.pinHashHex) {
        mode == PinDialogMode.VERIFY && PinHasher.isStoredPinCorrupted(
            hasCustomPin = settings.hasCustomPin,
            saltHex = settings.pinSaltHex,
            hashHex = settings.pinHashHex
        )
    }

    var enteredPin by remember { mutableStateOf("") }
    var setPinStep by remember { mutableStateOf(SetPinStep.ENTER_NEW) }
    var firstEnteredPin by remember { mutableStateOf("") }
    var statusMessage by remember(isResetRequired) {
        mutableStateOf(if (isResetRequired) context.getString(R.string.pin_err_reset_required) else null)
    }
    var isError by remember(isResetRequired) { mutableStateOf(isResetRequired) }

    // Lockout countdown timer
    var lockoutSeconds by remember(settings.pinLockedUntilEpochMillis) {
        val now = System.currentTimeMillis()
        val diff = ((settings.pinLockedUntilEpochMillis - now + 999) / 1000).toInt()
        mutableIntStateOf(diff.coerceAtLeast(0))
    }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000)
            lockoutSeconds -= 1
        }
    }

    LaunchedEffect(Unit) {
        val target = if (lockoutSeconds > 0) cancelFocusRequester else keypadFocusRequesters[0]
        runCatching { target.requestFocus() }
    }

    fun submitPin() {
        if (isResetRequired || lockoutSeconds > 0) return

        if (!PinHasher.validatePinFormat(enteredPin)) {
            isError = true
            statusMessage = context.getString(R.string.pin_err_format)
            return
        }

        when (mode) {
            PinDialogMode.VERIFY -> {
                val now = System.currentTimeMillis()
                val result = PinHasher.verifyPin(
                    enteredPin = enteredPin,
                    hasCustomPin = settings.hasCustomPin,
                    saltHex = settings.pinSaltHex,
                    hashHex = settings.pinHashHex,
                    nowEpochMillis = now,
                    currentFailedAttempts = settings.pinFailedAttempts,
                    lockedUntilEpochMillis = settings.pinLockedUntilEpochMillis
                )

                when (result) {
                    is PinVerifyResult.Success -> {
                        isError = false
                        statusMessage = null
                        onPinSuccess()
                        onPinVerified()
                    }
                    is PinVerifyResult.Incorrect -> {
                        val lockout = PinHasher.calculateLockout(settings.pinFailedAttempts, now)
                        onPinFailure(lockout.newFailedAttempts, lockout.lockedUntilEpochMillis)
                        enteredPin = ""
                        isError = true
                        if (lockout.lockedUntilEpochMillis > now) {
                            val sec = ((lockout.lockedUntilEpochMillis - now + 999) / 1000).toInt()
                            lockoutSeconds = sec
                            statusMessage = context.getString(R.string.pin_err_locked_sec, sec)
                        } else {
                            statusMessage = context.getString(R.string.pin_err_incorrect_remaining, result.attemptsLeftBeforeLockout)
                        }
                    }
                    is PinVerifyResult.LockedOut -> {
                        isError = true
                        lockoutSeconds = result.remainingSeconds
                        statusMessage = context.getString(R.string.pin_err_locked_retry, result.remainingSeconds)
                    }
                    is PinVerifyResult.InvalidFormat -> {
                        isError = true
                        statusMessage = context.getString(R.string.pin_err_format)
                    }
                    is PinVerifyResult.ResetRequired -> {
                        enteredPin = ""
                        isError = true
                        statusMessage = context.getString(R.string.pin_err_reset_required)
                    }
                }
            }

            PinDialogMode.SET_NEW -> {
                when (setPinStep) {
                    SetPinStep.ENTER_NEW -> {
                        firstEnteredPin = enteredPin
                        enteredPin = ""
                        setPinStep = SetPinStep.CONFIRM_NEW
                        isError = false
                        statusMessage = context.getString(R.string.pin_msg_confirm_new)
                    }
                    SetPinStep.CONFIRM_NEW -> {
                        if (enteredPin == firstEnteredPin) {
                            onPinChanged(enteredPin)
                            onDismiss()
                        } else {
                            isError = true
                            statusMessage = context.getString(R.string.pin_err_mismatch)
                            enteredPin = ""
                            firstEnteredPin = ""
                            setPinStep = SetPinStep.ENTER_NEW
                        }
                    }
                }
            }
        }
    }

    fun handleDigitInput(char: Char) {
        if (lockoutSeconds > 0) return
        if (enteredPin.length < PinHasher.MAX_PIN_LENGTH) {
            enteredPin += char
            isError = false
            statusMessage = null
        }
    }

    fun handleDelete() {
        if (lockoutSeconds > 0) return
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            isError = false
            statusMessage = null
        }
    }

    // Guard against stray KeyUp / repeat KeyDown from the OK hold that opened the dialog
    var pressedConfirmKeys by remember { mutableStateOf(emptySet<Key>()) }

    // Modal Dialog window traps D-pad focus so it can never escape to the background screen
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .onPreviewKeyEvent { event ->
                val isConfirmKey = event.key == Key.Enter ||
                    event.key == Key.NumPadEnter ||
                    event.key == Key.DirectionCenter

                if (isConfirmKey) {
                    return@onPreviewKeyEvent when (event.type) {
                        KeyEventType.KeyDown -> {
                            if (event.nativeKeyEvent.repeatCount > 0 && event.key !in pressedConfirmKeys) {
                                true
                            } else {
                                pressedConfirmKeys = pressedConfirmKeys + event.key
                                false
                            }
                        }
                        KeyEventType.KeyUp -> {
                            if (event.key !in pressedConfirmKeys) {
                                // Swallow release of a confirm key that was already held before dialog opened
                                true
                            } else {
                                pressedConfirmKeys = pressedConfirmKeys - event.key
                                false
                            }
                        }
                        else -> false
                    }
                }

                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Zero, Key.NumPad0 -> { handleDigitInput('0'); true }
                        Key.One, Key.NumPad1 -> { handleDigitInput('1'); true }
                        Key.Two, Key.NumPad2 -> { handleDigitInput('2'); true }
                        Key.Three, Key.NumPad3 -> { handleDigitInput('3'); true }
                        Key.Four, Key.NumPad4 -> { handleDigitInput('4'); true }
                        Key.Five, Key.NumPad5 -> { handleDigitInput('5'); true }
                        Key.Six, Key.NumPad6 -> { handleDigitInput('6'); true }
                        Key.Seven, Key.NumPad7 -> { handleDigitInput('7'); true }
                        Key.Eight, Key.NumPad8 -> { handleDigitInput('8'); true }
                        Key.Nine, Key.NumPad9 -> { handleDigitInput('9'); true }
                        Key.Backspace, Key.Delete -> { handleDelete(); true }
                        Key.Back -> {
                            onDismiss()
                            true
                        }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            colors = SurfaceDefaults.colors(containerColor = colorScheme.surface),
            modifier = Modifier
                .width(360.dp)
                .border(1.5.dp, colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon & Title
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (lockoutSeconds > 0) Color(0xFFEF4444) else colorScheme.secondary,
                    modifier = Modifier.size(34.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val titleText = when (mode) {
                    PinDialogMode.VERIFY -> stringResource(R.string.pin_title_verify)
                    PinDialogMode.SET_NEW -> if (setPinStep == SetPinStep.ENTER_NEW) stringResource(R.string.pin_title_set_new) else stringResource(R.string.pin_title_confirm_new)
                }

                Text(
                    text = titleText,
                    style = TvTypography.CardTitle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                )

                val subtitleText = when (mode) {
                    PinDialogMode.VERIFY -> {
                        if (!settings.hasCustomPin) stringResource(R.string.pin_subtitle_default) else stringResource(R.string.pin_subtitle_enter)
                    }
                    PinDialogMode.SET_NEW -> {
                        if (setPinStep == SetPinStep.ENTER_NEW) stringResource(R.string.pin_subtitle_digits) else stringResource(R.string.pin_subtitle_repeat)
                    }
                }

                Text(
                    text = subtitleText,
                    style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Masked PIN Display Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayCount = maxOf(4, enteredPin.length)
                    for (i in 0 until displayCount) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) (if (isError) Color(0xFFEF4444) else colorScheme.primary)
                                    else colorScheme.onSurface.copy(alpha = 0.15f)
                                )
                                .border(
                                    1.dp,
                                    if (isFilled) Color.Transparent else colorScheme.onSurface.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status / Error / Lockout Banner
                if (lockoutSeconds > 0) {
                    Text(
                        text = stringResource(R.string.pin_err_locked_banner, lockoutSeconds),
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF4444)
                        )
                    )
                } else if (statusMessage != null) {
                    Text(
                        text = statusMessage ?: "",
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isError) Color(0xFFEF4444) else colorScheme.primary
                        )
                    )
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3x4 On-Screen Numeric Keypad
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("CLEAR", "0", "OK")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val keyShape = RoundedCornerShape(10.dp)
                    for ((rowIndex, row) in keypadRows.withIndex()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for ((colIndex, key) in row.withIndex()) {
                                val flatIndex = rowIndex * 3 + colIndex
                                val isOkKey = (key == "OK")

                                Surface(
                                    onClick = {
                                        when (key) {
                                            "CLEAR" -> handleDelete()
                                            "OK" -> submitPin()
                                            else -> handleDigitInput(key[0])
                                        }
                                    },
                                    enabled = lockoutSeconds == 0,
                                    shape = ClickableSurfaceDefaults.shape(keyShape),
                                    scale = tvSurfaceScale(1.04f),
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = if (isOkKey) colorScheme.primary else colorScheme.surfaceVariant,
                                        focusedContainerColor = if (isOkKey) colorScheme.primary else colorScheme.surfaceVariant
                                    ),
                                    border = tvSelectableSurfaceBorder(selected = false, shape = keyShape),
                                    modifier = Modifier
                                        .size(width = 90.dp, height = 48.dp)
                                        .focusRequester(keypadFocusRequesters[flatIndex])
                                        .focusProperties {
                                            if (colIndex == 2) {
                                                right = if (flatIndex < 11) {
                                                    keypadFocusRequesters[flatIndex + 1]
                                                } else {
                                                    cancelFocusRequester
                                                }
                                            }
                                            if (colIndex == 0 && flatIndex > 0) {
                                                left = keypadFocusRequesters[flatIndex - 1]
                                            }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (key) {
                                            "CLEAR" -> Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = stringResource(R.string.btn_delete),
                                                tint = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            "OK" -> Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = stringResource(R.string.btn_done),
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            else -> Text(
                                                text = key,
                                                style = TvTypography.CardTitle.copy(
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.onSurface
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dismiss / Cancel Button
                val cancelShape = RoundedCornerShape(8.dp)
                Surface(
                    onClick = onDismiss,
                    shape = ClickableSurfaceDefaults.shape(cancelShape),
                    scale = tvSurfaceScale(1.03f),
                    colors = tvSelectableSurfaceColors(selected = false),
                    border = tvSelectableSurfaceBorder(selected = false, shape = cancelShape),
                    modifier = Modifier.focusRequester(cancelFocusRequester)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = colorScheme.onSurface.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_cancel),
                            style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.85f))
                        )
                    }
                }
            }
        }
    }
    }
}
