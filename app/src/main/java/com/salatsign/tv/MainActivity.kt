package com.salatsign.tv

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
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
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.data.repository.SettingsRepository
import com.salatsign.tv.service.AdhanPlaybackService
import com.salatsign.tv.service.PrayerAlarmScheduler
import com.salatsign.tv.ui.common.LocaleHelper
import com.salatsign.tv.ui.common.PinDialog
import com.salatsign.tv.ui.common.PinDialogMode
import com.salatsign.tv.ui.home.HomeScreen
import com.salatsign.tv.ui.home.HomeViewModel
import com.salatsign.tv.ui.mosque.MosqueSignageScreen
import com.salatsign.tv.ui.mosque.MosqueSlideshowViewModel
import com.salatsign.tv.ui.settings.SettingsScreen
import com.salatsign.tv.ui.settings.SettingsViewModel
import com.salatsign.tv.ui.setup.SetupWizardScreen
import com.salatsign.tv.ui.theme.SalatSignTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalTime

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val mosqueViewModel: MosqueSlideshowViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private var dpadCenterHoldJob: Job? = null
    private val isSettingsOpen = mutableStateOf(false)
    private val isPinDialogOpen = mutableStateOf(false)

    override fun attachBaseContext(newBase: Context) {
        val language = runCatching {
            runBlocking {
                SettingsRepository(newBase).appSettingsFlow.first().appLanguage
            }
        }.getOrDefault(AppLanguage.SYSTEM)
        super.attachBaseContext(LocaleHelper.wrap(newBase, language))
    }

    private fun requestOpenSettings() {
        val currentSettings = settingsViewModel.settings.value
        val requiresPin = currentSettings.isMosqueMode || currentSettings.homeModePinRequired
        if (requiresPin) {
            isPinDialogOpen.value = true
        } else {
            isSettingsOpen.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            val homeUiState by homeViewModel.uiState.collectAsState()

            var activeLanguage by remember { mutableStateOf(settings.appLanguage) }
            LaunchedEffect(settings.appLanguage) {
                if (settings.appLanguage != activeLanguage) {
                    activeLanguage = settings.appLanguage
                    recreate()
                }
            }

            // Resolve Dark / Light Theme
            val isDark = remember(settings.themeMode, homeUiState.prayerTimesDay) {
                when (settings.themeMode) {
                    ThemeMode.ALWAYS_DARK -> true
                    ThemeMode.ALWAYS_LIGHT -> false
                    ThemeMode.AUTO_DAY_NIGHT -> {
                        // Daytime: between Sunrise and Magrib (Sunset)
                        val now = LocalTime.now()
                        val prayerTimes = homeUiState.prayerTimesDay
                        if (prayerTimes != null) {
                            val sunrise = prayerTimes.sunrise
                            val magrib = prayerTimes.prayers.firstOrNull { it.prayer == Prayer.MAGRIB }?.time
                                ?: LocalTime.of(18, 0)
                            // If currently daytime, use Light Theme; otherwise Dark Theme
                            !(now.isAfter(sunrise) && now.isBefore(magrib))
                        } else {
                            // Default to dark
                            now.hour < 6 || now.hour >= 18
                        }
                    }
                }
            }

            SalatSignTheme(isDark = isDark) {
                MainContent(
                    homeViewModel = homeViewModel,
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

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Task P1.4: Stop adhan playback if user presses Back, OK / DPAD_CENTER / ENTER while adhan is playing
        if (AdhanPlaybackService.isPlaying.value && event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK ||
                event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                event.keyCode == KeyEvent.KEYCODE_ENTER ||
                event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
            ) {
                stopAdhanPlayback()
                return true
            }
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            // Task P3.2: Menu key directly toggles settings (via PIN check if required)
            if (event.keyCode == KeyEvent.KEYCODE_MENU) {
                if (isSettingsOpen.value) {
                    isSettingsOpen.value = false
                } else if (isPinDialogOpen.value) {
                    isPinDialogOpen.value = false
                } else {
                    requestOpenSettings()
                }
                return true
            }

            // Task P3.2: DPAD_CENTER 3-second hold detection using coroutine timer (does not rely on remote repeat events)
            if (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event.repeatCount == 0) {
                val currentSettings = settingsViewModel.settings.value
                if (currentSettings.isSetupCompleted && !isSettingsOpen.value && !isPinDialogOpen.value) {
                    dpadCenterHoldJob?.cancel()
                    dpadCenterHoldJob = lifecycleScope.launch {
                        delay(3000L)
                        requestOpenSettings()
                    }
                }
            }

            // Task P3.2: In Mosque Mode kiosk, intercept Back key to prevent accidental app exit
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                val currentSettings = settingsViewModel.settings.value
                if (currentSettings.isMosqueMode && currentSettings.isSetupCompleted) {
                    if (isPinDialogOpen.value) {
                        isPinDialogOpen.value = false
                        return true
                    }
                    if (isSettingsOpen.value) {
                        isSettingsOpen.value = false
                        return true
                    }
                    // Prevent leaving the app in Mosque digital signage mode
                    return true
                }
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            if (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                dpadCenterHoldJob?.cancel()
                dpadCenterHoldJob = null
            }
        }

        return super.dispatchKeyEvent(event)
    }

    private fun stopAdhanPlayback() {
        val stopIntent = Intent(this, AdhanPlaybackService::class.java).apply {
            action = PrayerAlarmScheduler.ACTION_STOP_ADHAN
        }
        startService(stopIntent)
    }
}

@Composable
fun MainContent(
    homeViewModel: HomeViewModel,
    mosqueViewModel: MosqueSlideshowViewModel,
    settingsViewModel: SettingsViewModel,
    isSettingsOpen: Boolean,
    isPinDialogOpen: Boolean,
    onRequestOpenSettings: () -> Unit,
    onOpenSettingsDirect: () -> Unit,
    onCloseSettings: () -> Unit,
    onClosePinDialog: () -> Unit
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    val mosqueUiState by mosqueViewModel.uiState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val slideContainer by settingsViewModel.slideContainer.collectAsState()

    val currentView = LocalView.current
    val isAdhanPlaying by AdhanPlaybackService.isPlaying.collectAsState()
    val context = currentView.context

    // Rule: Screen Wake Lock must be active in Mosque Mode (FLAG_KEEP_SCREEN_ON)
    DisposableEffect(settings.isMosqueMode) {
        val window = (currentView.context as? ComponentActivity)?.window
        if (settings.isMosqueMode) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Back button handling
    BackHandler(enabled = isAdhanPlaying || isPinDialogOpen || isSettingsOpen || !homeUiState.isViewingToday || settings.isMosqueMode) {
        if (isAdhanPlaying) {
            val stopIntent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = PrayerAlarmScheduler.ACTION_STOP_ADHAN
            }
            context.startService(stopIntent)
        } else if (isPinDialogOpen) {
            onClosePinDialog()
        } else if (isSettingsOpen) {
            onCloseSettings()
        } else if (!homeUiState.isViewingToday) {
            homeViewModel.resetToToday()
        } else if (settings.isMosqueMode) {
            // Intercept Back in Mosque Mode kiosk
        }
    }

    // 1. Initial Setup Wizard (Shown on first run or when restarted from Settings)
    if (!settings.isSetupCompleted) {
        SetupWizardScreen(
            settings = settings,
            onSelectLanguage = { settingsViewModel.setAppLanguage(it) },
            onSelectMode = { settingsViewModel.toggleMosqueMode(it) },
            onSelectCity = { settingsViewModel.selectCity(it) },
            onSelectMethod = { settingsViewModel.setCalculationMethod(it) },
            onSelectMadhab = { settingsViewModel.setMadhab(it) },
            onSelectMosqueName = { settingsViewModel.setMosqueName(it) },
            onSelectMosqueIcon = { settingsViewModel.setMosqueIconPreset(it) },
            onSetAdminPin = { settingsViewModel.setCustomPin(it) },
            onCompleteSetup = { settingsViewModel.completeSetup() }
        )
    } else if (isSettingsOpen) {
        // 2. Settings Screen (Strict separation between Home Mode and Mosque Mode)
        SettingsScreen(
            settings = settings,
            slides = slideContainer.slides,
            onToggleMosqueMode = { settingsViewModel.toggleMosqueMode(it) },
            onSetThemeMode = { settingsViewModel.setThemeMode(it) },
            onSelectCity = { settingsViewModel.selectCity(it) },
            onSetHijriOffset = { settingsViewModel.setHijriOffset(it) },
            onToggleReminder = { settingsViewModel.togglePrePrayerReminder(it) },
            onSetAdhanAudio = { prayer, option -> settingsViewModel.setAdhanAudio(prayer, option) },
            onSetMosqueName = { settingsViewModel.setMosqueName(it) },
            onSetMosqueSubtitle = { settingsViewModel.setMosqueSubtitle(it) },
            onSetMosqueIconPreset = { settingsViewModel.setMosqueIconPreset(it) },
            onSetMosqueFontPreset = { settingsViewModel.setMosqueFontPreset(it) },
            onUpdateScheduleDuration = { settingsViewModel.updateScheduleDuration(it) },
            onAddSampleSlide = { settingsViewModel.addSampleAnnouncementSlide() },
            onClearSlides = { settingsViewModel.clearSlides() },
            onSetCustomLogo = { settingsViewModel.setCustomLogo(it) },
            onSetCustomAdhan = { prayer, file, name -> settingsViewModel.setCustomAdhan(prayer, file, name) },
            onAddImageSlide = { title, file, dur -> settingsViewModel.addImageSlide(title, file, dur) },
            onAddTextSlide = { title, body, dur -> settingsViewModel.addTextSlide(title, body, dur) },
            onDeleteSlide = { settingsViewModel.deleteSlide(it) },
            onSetCustomPin = { settingsViewModel.setCustomPin(it) },
            onResetPin = { settingsViewModel.resetToDefaultPin() },
            onToggleHomePin = { settingsViewModel.toggleHomeModePinRequired(it) },
            onDismissDefaultPinPrompt = { settingsViewModel.dismissDefaultPinPrompt() },
            onSetCalculationMethod = { settingsViewModel.setCalculationMethod(it) },
            onSetMadhab = { settingsViewModel.setMadhab(it) },
            onSetHighLatitudeRule = { settingsViewModel.setHighLatitudeRule(it) },
            onSetPrayerOffset = { prayer, mins -> settingsViewModel.setPrayerOffset(prayer, mins) },
            onSetSunriseOffset = { settingsViewModel.setSunriseOffset(it) },
            onSetIqamahOffset = { prayer, mins -> settingsViewModel.setIqamahOffset(prayer, mins) },
            onSetHijriChangeAtMidnight = { settingsViewModel.setHijriChangeAtMidnight(it) },
            onSetMosqueModeAdhan = { settingsViewModel.setMosqueModeAdhan(it) },
            onSetMosqueModeReminders = { settingsViewModel.setMosqueModeReminders(it) },
            onSetShowStrikeHighlight = { settingsViewModel.setShowStrikeHighlight(it) },
            onSetAppLanguage = { settingsViewModel.setAppLanguage(it) },
            onSet24HourFormat = { settingsViewModel.set24HourFormat(it) },
            onSetShowArabicPrayerNames = { settingsViewModel.setShowArabicPrayerNames(it) },
            onRestartSetupWizard = {
                onCloseSettings()
                settingsViewModel.restartSetupWizard()
            },
            onClose = onCloseSettings
        )
    } else if (settings.isMosqueMode) {
        // 3. Mosque White-Label Signage Screen
        MosqueSignageScreen(
            uiState = mosqueUiState,
            onOpenSettings = onRequestOpenSettings
        )
    } else {
        // 4. Home Mode Screen
        HomeScreen(
            uiState = homeUiState,
            onPreviousDay = { homeViewModel.previousDay() },
            onNextDay = { homeViewModel.nextDay() },
            onTodayClick = { homeViewModel.resetToToday() },
            onOpenSettings = onRequestOpenSettings
        )
    }

    // Overlay: Admin PIN verification dialog when opening Settings
    if (isPinDialogOpen) {
        PinDialog(
            mode = PinDialogMode.VERIFY,
            settings = settings,
            onPinVerified = {
                onClosePinDialog()
                onOpenSettingsDirect()
            },
            onPinFailure = { newFailedAttempts, lockedUntil ->
                settingsViewModel.recordPinFailure(newFailedAttempts, lockedUntil)
            },
            onPinSuccess = {
                settingsViewModel.recordPinSuccess()
            },
            onDismiss = onClosePinDialog
        )
    }
}
