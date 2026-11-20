// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.salatschedule.tv.service.AdhanPlaybackService
import com.salatschedule.tv.service.PrayerAlarmScheduler
import com.salatschedule.tv.ui.home.HomeScreen
import com.salatschedule.tv.ui.home.HomeViewModel
import com.salatschedule.tv.ui.settings.HomeSettingsScreen
import com.salatschedule.tv.ui.settings.HomeSettingsViewModel
import com.salatschedule.tv.ui.setup.HomeSetupWizardScreen
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.data.repository.LanguageSyncCache
import com.salatsign.tv.ui.common.LocaleHelper
import com.salatsign.tv.ui.theme.SalatSignTheme
import java.time.LocalTime

/**
 * SalatSchedule MainActivity — Interactive Home Prayer Times TV experience.
 * Supports D-pad date navigation, 15-minute pre-prayer reminders, and exact-time Adhan audio.
 */
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val settingsViewModel: HomeSettingsViewModel by viewModels()

    private val isSettingsOpen = mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied; graceful fallback */ }

    override fun attachBaseContext(newBase: Context) {
        val language = LanguageSyncCache.getLanguage(newBase)
        super.attachBaseContext(LocaleHelper.wrap(newBase, language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+ for 15-min reminders & Adhan playback notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val appliedLanguage = LanguageSyncCache.getLanguage(this)

        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            val homeUiState by homeViewModel.uiState.collectAsState()

            var activeLanguage by remember { mutableStateOf(appliedLanguage) }
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
                        val now = LocalTime.now()
                        val prayerTimes = homeUiState.prayerTimesDay
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
                SalatScheduleMainContent(
                    homeViewModel = homeViewModel,
                    settingsViewModel = settingsViewModel,
                    isSettingsOpen = isSettingsOpen.value,
                    onOpenSettings = { isSettingsOpen.value = true },
                    onCloseSettings = { isSettingsOpen.value = false },
                    onStopAdhan = {
                        val stopIntent = Intent(this, AdhanPlaybackService::class.java).apply {
                            action = PrayerAlarmScheduler.ACTION_STOP_ADHAN
                        }
                        startService(stopIntent)
                    }
                )
            }
        }
    }
}

@Composable
private fun SalatScheduleMainContent(
    homeViewModel: HomeViewModel,
    settingsViewModel: HomeSettingsViewModel,
    isSettingsOpen: Boolean,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onStopAdhan: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val isAdhanPlaying by AdhanPlaybackService.isPlaying.collectAsState()

    if (isSettingsOpen) {
        BackHandler { onCloseSettings() }
    }

    when {
        !settings.isSetupCompleted -> {
            HomeSetupWizardScreen(
                settings = settings,
                onSelectCity = { settingsViewModel.selectCity(it) },
                onSetCalculationMethod = { settingsViewModel.setCalculationMethod(it) },
                onSetMadhab = { settingsViewModel.setMadhab(it) },
                onTogglePrePrayerReminder = { settingsViewModel.togglePrePrayerReminder(it) },
                onCompleteSetup = { settingsViewModel.completeSetup() }
            )
        }
        isSettingsOpen -> {
            HomeSettingsScreen(
                settings = settings,
                onSetThemeMode = { settingsViewModel.setThemeMode(it) },
                onSelectCity = { settingsViewModel.selectCity(it) },
                onSetCalculationMethod = { settingsViewModel.setCalculationMethod(it) },
                onSetMadhab = { settingsViewModel.setMadhab(it) },
                onSetHighLatitudeRule = { settingsViewModel.setHighLatitudeRule(it) },
                onSetPrayerOffset = { prayer, mins -> settingsViewModel.setPrayerOffset(prayer, mins) },
                onSetSunriseOffset = { mins -> settingsViewModel.setSunriseOffset(mins) },
                onSetHijriOffset = { settingsViewModel.setHijriOffset(it) },
                onSetHijriChangeAtMidnight = { settingsViewModel.setHijriChangeAtMidnight(it) },
                onTogglePrePrayerReminder = { settingsViewModel.togglePrePrayerReminder(it) },
                onSetAdhanAudio = { prayer, option -> settingsViewModel.setAdhanAudio(prayer, option) },
                onSetCustomAdhan = { prayer, file, name -> settingsViewModel.setCustomAdhan(prayer, file, name) },
                onSetAppLanguage = { settingsViewModel.setAppLanguage(it) },
                onSet24HourFormat = { settingsViewModel.set24HourFormat(it) },
                onSetShowArabicPrayerNames = { settingsViewModel.setShowArabicPrayerNames(it) },
                onRestartSetupWizard = {
                    onCloseSettings()
                    settingsViewModel.restartSetupWizard()
                },
                onBack = onCloseSettings
            )
        }
        else -> {
            HomeScreen(
                uiState = homeUiState,
                onPreviousDay = { homeViewModel.previousDay() },
                onNextDay = { homeViewModel.nextDay() },
                onTodayClick = { homeViewModel.resetToToday() },
                onOpenSettings = onOpenSettings,
                onDismissAdhanHint = { homeViewModel.dismissAdhanHint() }
            )
        }
    }
}
