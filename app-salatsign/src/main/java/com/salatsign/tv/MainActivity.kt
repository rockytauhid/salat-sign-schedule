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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
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
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * SalatSign MainActivity — Strictly silent Mosque Digital Signage TV experience.
 * Always keeps screen on (FLAG_KEEP_SCREEN_ON) and protects Settings with Admin PIN.
 */
class MainActivity : ComponentActivity() {

    private val mosqueViewModel: MosqueSlideshowViewModel by viewModels()
    private val settingsViewModel: MosqueSettingsViewModel by viewModels()

    private var dpadCenterHoldJob: Job? = null
    private var backHoldJob: Job? = null
    private var ignoreNextConfirmKeyUp = false
    private val isSettingsOpen = mutableStateOf(false)
    private val isPinDialogOpen = mutableStateOf(false)
    private val adminHintTrigger = mutableLongStateOf(0L)
    private val exitHintTrigger = mutableLongStateOf(0L)

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

            var themeTick by remember { mutableLongStateOf(0L) }
            LaunchedEffect(settings.themeMode) {
                if (settings.themeMode == ThemeMode.AUTO_DAY_NIGHT) {
                    while (true) {
                        delay(60_000L)
                        themeTick++
                    }
                }
            }

            // Resolve Dark / Light Theme (re-evaluates every minute for Sunrise/Maghrib transitions)
            val isDark = remember(settings.themeMode, mosqueUiState.prayerTimesDay, settings.zoneId, themeTick) {
                when (settings.themeMode) {
                    ThemeMode.ALWAYS_DARK -> true
                    ThemeMode.ALWAYS_LIGHT -> false
                    ThemeMode.AUTO_DAY_NIGHT -> {
                        val zone = runCatching { ZoneId.of(settings.zoneId) }.getOrDefault(ZoneId.systemDefault())
                        val now = ZonedDateTime.now(zone).toLocalTime()
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
                    adminHintTrigger = adminHintTrigger.longValue,
                    exitHintTrigger = exitHintTrigger.longValue,
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
        if (ignoreNextConfirmKeyUp &&
            (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                event.keyCode == KeyEvent.KEYCODE_ENTER ||
                event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
        ) {
            if (event.action == KeyEvent.ACTION_UP) {
                ignoreNextConfirmKeyUp = false
                return true
            }
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount > 0) {
                return true
            }
            ignoreNextConfirmKeyUp = false
        }

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
                        dpadCenterHoldJob = null
                        ignoreNextConfirmKeyUp = true
                        requestOpenSettings()
                    }
                    return true
                } else if (event.action == KeyEvent.ACTION_UP) {
                    val wasStillPending = dpadCenterHoldJob?.isActive == true
                    dpadCenterHoldJob?.cancel()
                    dpadCenterHoldJob = null
                    if (wasStillPending) {
                        adminHintTrigger.longValue = System.currentTimeMillis()
                    }
                    return true
                }
            }

            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    backHoldJob?.cancel()
                    backHoldJob = lifecycleScope.launch {
                        delay(3000)
                        backHoldJob = null
                        finish()
                    }
                    return true
                } else if (event.action == KeyEvent.ACTION_DOWN) {
                    return true
                } else if (event.action == KeyEvent.ACTION_UP) {
                    val wasStillPending = backHoldJob?.isActive == true
                    backHoldJob?.cancel()
                    backHoldJob = null
                    if (wasStillPending) {
                        exitHintTrigger.longValue = System.currentTimeMillis()
                    }
                    return true
                }
            }

            if (event.action == KeyEvent.ACTION_DOWN && (
                    event.keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                )
            ) {
                adminHintTrigger.longValue = System.currentTimeMillis()
                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        dpadCenterHoldJob?.cancel()
        backHoldJob?.cancel()
    }
}

@Composable
private fun SalatSignMainContent(
    mosqueViewModel: MosqueSlideshowViewModel,
    settingsViewModel: MosqueSettingsViewModel,
    isSettingsOpen: Boolean,
    isPinDialogOpen: Boolean,
    adminHintTrigger: Long,
    exitHintTrigger: Long,
    onRequestOpenSettings: () -> Unit,
    onOpenSettingsDirect: () -> Unit,
    onCloseSettings: () -> Unit,
    onClosePinDialog: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val isSettingsLoaded by settingsViewModel.isSettingsLoaded.collectAsState()
    val mosqueUiState by mosqueViewModel.uiState.collectAsState()
    val slideContainer by settingsViewModel.slides.collectAsState()
    val slideLimitError by settingsViewModel.slideLimitError.collectAsState()
    val isPhoneControllerRunning by settingsViewModel.isPhoneControllerRunning.collectAsState()
    val phoneControllerUrl by settingsViewModel.phoneControllerUrl.collectAsState()
    val view = LocalView.current
    val app = view.context.applicationContext as? SalatSignApp
    val pairedDeviceNotice by (app?.lastPairedDeviceNotice?.collectAsState() ?: remember { mutableStateOf(null) })

    LaunchedEffect(pairedDeviceNotice) {
        if (pairedDeviceNotice != null) {
            delay(6_000L)
            app?.clearPairedDeviceNotice()
        }
    }

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
        !isSettingsLoaded -> {
            // Hold calm dark background briefly while DataStore loads saved setup state
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .background(com.salatsign.tv.ui.theme.DarkBackground)
            )
        }
        !settings.isSetupCompleted -> {
            MosqueSetupWizardScreen(
                settings = settings,
                onSetAppLanguage = { settingsViewModel.setAppLanguage(it) },
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
                onAddTextSlide = { title, body, dur, enabled, align, lines -> settingsViewModel.addTextSlide(title, body, dur, enabled, align, lines) },
                onAddImageSlide = { title, imgFile, dur -> settingsViewModel.addImageSlide(title, imgFile, dur) },
                onUpdateSlide = { settingsViewModel.updateSlide(it) },
                onToggleSlideEnabled = { settingsViewModel.toggleSlideEnabled(it) },
                onDeleteSlide = { settingsViewModel.deleteSlide(it) },
                onClearSlides = { settingsViewModel.clearSlides() },
                slideLimitError = slideLimitError,
                onClearSlideLimitError = { settingsViewModel.clearSlideLimitError() },
                onSetCustomLogo = { settingsViewModel.setCustomLogo(it) },
                onChangeAdminPin = { settingsViewModel.setAdminPin(it) },
                onDismissDefaultPinPrompt = { settingsViewModel.dismissDefaultPinPrompt() },
                onSetAppLanguage = { settingsViewModel.setAppLanguage(it) },
                onSet24HourFormat = { settingsViewModel.set24HourFormat(it) },
                onSetShowArabicPrayerNames = { settingsViewModel.setShowArabicPrayerNames(it) },
                onSetJumuahMode = { settingsViewModel.setJumuahMode(it) },
                onSetJumuahTimes = { settingsViewModel.setJumuahTimes(it) },
                onSetSilentJumuahEnabled = { settingsViewModel.setSilentJumuahEnabled(it) },
                onSetSilentJumuahLeadMinutes = { settingsViewModel.setSilentJumuahLeadMinutes(it) },
                onSetSilentJumuahDurationMinutes = { settingsViewModel.setSilentJumuahDurationMinutes(it) },
                onSetSilentDailyPrayerConfig = { prayer, cfg -> settingsViewModel.setSilentDailyPrayerConfig(prayer, cfg) },
                onCopySilentSettingsToAllPrayers = { settingsViewModel.copySilentSettingsToAllPrayers(it) },
                onStartManualSilentScreen = { mins ->
                    settingsViewModel.startManualSilentScreen(mins)
                    onCloseSettings()
                },
                onStopManualSilentScreen = { settingsViewModel.stopManualSilentScreen() },
                onSetIslamicCalendarSlideEnabled = { settingsViewModel.setIslamicCalendarSlideEnabled(it) },
                onSetIslamicCalendarSlideDurationSeconds = { settingsViewModel.setIslamicCalendarSlideDurationSeconds(it) },
                onToggleIslamicEvent = { settingsViewModel.toggleIslamicEvent(it) },
                allQuranVerses = remember(settings.appLanguage) { settingsViewModel.getAllQuranVerses() },
                onSetQuranVerseEnabled = { settingsViewModel.setQuranVerseEnabled(it) },
                onSetQuranShowTranslation = { settingsViewModel.setQuranShowTranslation(it) },
                onSetQuranVerseDurationSeconds = { settingsViewModel.setQuranVerseDurationSeconds(it) },
                onRequestPreviewQuranVerse = { settingsViewModel.drawPreviewQuranVerse() },
                isPhoneControllerRunning = isPhoneControllerRunning,
                phoneControllerUrl = phoneControllerUrl,
                onTogglePhoneController = { settingsViewModel.setPhoneControllerEnabled(it) },
                onSetPhoneControllerAlwaysOn = { settingsViewModel.setPhoneControllerAlwaysOn(it) },
                onGeneratePairingCode = { role -> settingsViewModel.generatePairingCode(role) },
                onCancelPairingCode = { settingsViewModel.cancelPairingCode() },
                onRevokePairedDevice = { settingsViewModel.revokePairedDevice(it) },
                onSetLanPinLoginEnabled = { settingsViewModel.setLanPinLoginEnabled(it) },
                onSetPairedTokenExpiryDays = { settingsViewModel.setPairedTokenExpiryDays(it) },
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
                adminHintTrigger = adminHintTrigger,
                exitHintTrigger = exitHintTrigger,
                onSkipOverflowQuranVerse = { mosqueViewModel.skipCurrentQuranVerse() },
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
            onUpgradePinHash = { verifiedPin ->
                settingsViewModel.setAdminPin(verifiedPin)
            },
            onDismiss = onClosePinDialog
        )
    }

    // Brief on-TV confirmation banner whenever a new phone/browser pairs
    pairedDeviceNotice?.let { device ->
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(top = 32.dp),
            contentAlignment = androidx.compose.ui.Alignment.TopCenter
        ) {
            val roleLabel = if (device.role == com.salatsign.tv.security.RemoteRole.MANAGE) {
                androidx.compose.ui.res.stringResource(R.string.pair_role_manage)
            } else {
                androidx.compose.ui.res.stringResource(R.string.pair_role_notify)
            }
            androidx.tv.material3.Text(
                text = androidx.compose.ui.res.stringResource(R.string.pair_tv_notice, device.deviceName, roleLabel),
                style = com.salatsign.tv.ui.theme.TvTypography.CardTitle.copy(
                    color = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier
                    .background(
                        androidx.compose.ui.graphics.Color(0xE6065F46),
                        androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            )
        }
    }
}
