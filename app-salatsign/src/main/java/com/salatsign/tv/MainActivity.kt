// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.lifecycleScope
import com.salatsign.core.tv.R as CoreR
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.data.repository.LanguageSyncCache
import com.salatsign.tv.ui.common.LocaleHelper
import com.salatsign.tv.ui.common.PinDialog
import com.salatsign.tv.ui.common.PinDialogMode
import com.salatsign.tv.ui.mosque.MosqueSignageScreen
import com.salatsign.tv.ui.mosque.MosqueSlideshowViewModel
import com.salatsign.tv.ui.settings.MosqueSettingsScreen
import com.salatsign.tv.ui.settings.MosqueSettingsViewModel
import com.salatsign.tv.ui.setup.MosqueSetupWizardScreen
import com.salatsign.tv.ui.theme.SalatSignTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * SalatSign MainActivity — Strictly silent Mosque Digital Signage TV experience.
 * Always keeps screen on (FLAG_KEEP_SCREEN_ON) and protects Settings with Admin PIN.
 */
class MainActivity : ComponentActivity() {

    private val mosqueViewModel: MosqueSlideshowViewModel by viewModels()
    private val settingsViewModel: MosqueSettingsViewModel by viewModels()

    private var dpadCenterHoldJob: Job? = null
    private val isSettingsOpen = mutableStateOf(false)
    private val isPinDialogOpen = mutableStateOf(false)

    override fun attachBaseContext(newBase: Context) {
        val language = LanguageSyncCache.getLanguage(newBase)
        super.attachBaseContext(LocaleHelper.wrap(newBase, language))
    }

    private fun requestOpenSettings() {
        isPinDialogOpen.value = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appliedLanguage = LanguageSyncCache.getLanguage(this)

        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            val mosqueUiState by mosqueViewModel.uiState.collectAsState()

            var activeLanguage by remember { mutableStateOf(appliedLanguage) }
            LaunchedEffect(settings.appLanguage) {
                if (settings.appLanguage != activeLanguage) {
                    activeLanguage = settings.appLanguage
                    recreate()
                }
            }

            // Resolve Dark / Light Theme
            val isDark = remember(settings.themeMode, mosqueUiState.prayerTimesDay) {
                when (settings.themeMode) {
                    ThemeMode.ALWAYS_DARK -> true
                    ThemeMode.ALWAYS_LIGHT -> false
                    ThemeMode.AUTO_DAY_NIGHT -> {
                        val now = LocalTime.now()
                        val prayerTimes = mosqueUiState.prayerTimesDay
                        if (prayerTimes != null) {
                            val sunrise = prayerTimes.sunrise
                            val magrib = prayerTimes.prayers.firstOrNull { it.prayer == Prayer.MAGRIB }?.time
                                ?: LocalTime.of(18, 0)
                            if (sunrise != null) {
                                !(now.isAfter(sunrise) && now.isBefore(magrib))
                            } else {
                                now.hour < 6 || now.hour >= 18
                            }
                        } else {
                            now.hour < 6 || now.hour >= 18
                        }
                    }
                }
            }

            SalatSignTheme(isDark = isDark) {
                SalatSignMainContent(
                    mosqueViewModel = mosqueViewModel,
                    settingsViewModel = settingsViewModel,
                    isSettingsOpen = isSettingsOpen.value,
                    isPinDialogOpen = isPinDialogOpen.value,
                    onRequestOpenSettings = { requestOpenSettings() },
                    onOpenSettingsDirect = { isSettingsOpen.value = true },
                    onCloseSettings = { isSettingsOpen.value = false },
                    onClosePinDialog = { isPinDialogOpen.value = false }
                )
            }
        }
    }

    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val settings = settingsViewModel.settings.value

        // When signage screen is live (not in wizard, settings, or PIN dialog)
        if (settings.isSetupCompleted && !isSettingsOpen.value && !isPinDialogOpen.value) {
            if (event.keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_DOWN) {
                requestOpenSettings()
                return true
            }

            if (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER) {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    dpadCenterHoldJob?.cancel()
                    dpadCenterHoldJob = lifecycleScope.launch {
                        delay(3000)
                        requestOpenSettings()
                    }
                    return true
                } else if (event.action == KeyEvent.ACTION_UP) {
                    dpadCenterHoldJob?.cancel()
                    dpadCenterHoldJob = null
                    return true
                }
            }

            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                Toast.makeText(
                    this,
                    getString(CoreR.string.mosque_hold_ok_toast),
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        dpadCenterHoldJob?.cancel()
    }
}

@Composable
private fun SalatSignMainContent(
    mosqueViewModel: MosqueSlideshowViewModel,
    settingsViewModel: MosqueSettingsViewModel,
    isSettingsOpen: Boolean,
    isPinDialogOpen: Boolean,
    onRequestOpenSettings: () -> Unit,
    onOpenSettingsDirect: () -> Unit,
    onCloseSettings: () -> Unit,
    onClosePinDialog: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val mosqueUiState by mosqueViewModel.uiState.collectAsState()
    val slideContainer by settingsViewModel.slides.collectAsState()
    val view = LocalView.current

    // Always keep screen awake for Mosque Digital Signage
    DisposableEffect(Unit) {
        val window = (view.context as? ComponentActivity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    if (isSettingsOpen) {
        BackHandler { onCloseSettings() }
    }

    when {
        !settings.isSetupCompleted -> {
            MosqueSetupWizardScreen(
                settings = settings,
                onSelectCity = { settingsViewModel.selectCity(it) },
                onSetCalculationMethod = { settingsViewModel.setCalculationMethod(it) },
                onSetMadhab = { settingsViewModel.setMadhab(it) },
                onSetMosqueName = { settingsViewModel.setMosqueName(it) },
                onSetMosqueSubtitle = { settingsViewModel.setMosqueSubtitle(it) },
                onSelectMosqueIcon = { settingsViewModel.setMosqueIconPreset(it) },
                onSetJumuahMode = { settingsViewModel.setJumuahMode(it) },
                onSetJumuahTimes = { settingsViewModel.setJumuahTimes(it) },
                onCompleteSetup = { settingsViewModel.completeSetup() }
            )
        }
        isSettingsOpen -> {
            MosqueSettingsScreen(
                settings = settings,
                slides = slideContainer.slides,
                onSetThemeMode = { settingsViewModel.setThemeMode(it) },
                onSelectCity = { settingsViewModel.selectCity(it) },
                onSetCalculationMethod = { settingsViewModel.setCalculationMethod(it) },
                onSetMadhab = { settingsViewModel.setMadhab(it) },
                onSetHighLatitudeRule = { settingsViewModel.setHighLatitudeRule(it) },
                onSetPrayerOffset = { prayer, mins -> settingsViewModel.setPrayerOffset(prayer, mins) },
                onSetSunriseOffset = { mins -> settingsViewModel.setSunriseOffset(mins) },
                onSetIqamahOffset = { prayer, mins -> settingsViewModel.setIqamahOffset(prayer, mins) },
                onSetHijriOffset = { settingsViewModel.setHijriOffset(it) },
                onSetHijriChangeAtMidnight = { settingsViewModel.setHijriChangeAtMidnight(it) },
                onSetShowStrikeHighlight = { settingsViewModel.setShowStrikeHighlight(it) },
                onSetMosqueName = { settingsViewModel.setMosqueName(it) },
                onSetMosqueSubtitle = { settingsViewModel.setMosqueSubtitle(it) },
                onSetMosqueIconPreset = { settingsViewModel.setMosqueIconPreset(it) },
                onSetMosqueFontPreset = { settingsViewModel.setMosqueFontPreset(it) },
                onSetScheduleDuration = { settingsViewModel.setScheduleDuration(it) },
                onAddTextSlide = { title, body, dur -> settingsViewModel.addTextSlide(title, body, dur) },
                onAddImageSlide = { title, imgFile, dur -> settingsViewModel.addImageSlide(title, imgFile, dur) },
                onDeleteSlide = { settingsViewModel.deleteSlide(it) },
                onClearSlides = { settingsViewModel.clearSlides() },
                onSetCustomLogo = { settingsViewModel.setCustomLogo(it) },
                onChangeAdminPin = { settingsViewModel.setAdminPin(it) },
                onDismissDefaultPinPrompt = { settingsViewModel.dismissDefaultPinPrompt() },
                onSetAppLanguage = { settingsViewModel.setAppLanguage(it) },
                onSet24HourFormat = { settingsViewModel.set24HourFormat(it) },
                onSetShowArabicPrayerNames = { settingsViewModel.setShowArabicPrayerNames(it) },
                onSetJumuahMode = { settingsViewModel.setJumuahMode(it) },
                onSetJumuahTimes = { settingsViewModel.setJumuahTimes(it) },
                onRestartSetupWizard = {
                    onCloseSettings()
                    settingsViewModel.restartSetupWizard()
                },
                onBack = onCloseSettings
            )
        }
        else -> {
            MosqueSignageScreen(
                uiState = mosqueUiState,
                onOpenSettings = onRequestOpenSettings
            )
        }
    }

    if (isPinDialogOpen) {
        PinDialog(
            mode = PinDialogMode.VERIFY,
            settings = settings,
            onPinVerified = {
                onClosePinDialog()
                onOpenSettingsDirect()
            },
            onPinFailure = { newAttempts, lockedUntil ->
                settingsViewModel.recordPinFailure(newAttempts, lockedUntil)
            },
            onPinSuccess = {
                settingsViewModel.recordPinSuccess()
            },
            onDismiss = onClosePinDialog
        )
    }
}
