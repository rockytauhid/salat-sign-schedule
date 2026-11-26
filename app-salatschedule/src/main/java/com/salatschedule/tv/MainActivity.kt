// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.salatsign.core.tv.R as CoreR
import com.salatschedule.tv.service.AdhanPlaybackService
import com.salatschedule.tv.service.PrayerAlarmScheduler
import com.salatschedule.tv.ui.calendar.IslamicCalendarScreen
import com.salatschedule.tv.ui.home.HomeScreen
import com.salatschedule.tv.ui.home.HomeViewModel
import com.salatschedule.tv.ui.settings.HomeSettingsScreen
import com.salatschedule.tv.ui.settings.HomeSettingsViewModel
import com.salatschedule.tv.ui.setup.HomeSetupWizardScreen
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.data.repository.LanguageSyncCache
import com.salatsign.tv.ui.common.LocaleHelper
import com.salatsign.tv.ui.common.tvFocusable
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.SalatSignTheme
import com.salatsign.tv.ui.theme.TvTypography
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * SalatSchedule MainActivity — Interactive Home Prayer Times TV experience.
 * Supports D-pad date navigation, 15-minute pre-prayer reminders, and exact-time Adhan audio.
 */
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val settingsViewModel: HomeSettingsViewModel by viewModels()

    private val isSettingsOpen = mutableStateOf(false)
    private val isCalendarOpen = mutableStateOf(false)

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
            val isDark = remember(settings.themeMode, homeUiState.prayerTimesDay, settings.zoneId, themeTick) {
                when (settings.themeMode) {
                    ThemeMode.ALWAYS_DARK -> true
                    ThemeMode.ALWAYS_LIGHT -> false
                    ThemeMode.AUTO_DAY_NIGHT -> {
                        val zone = runCatching { ZoneId.of(settings.zoneId) }.getOrDefault(ZoneId.systemDefault())
                        val now = ZonedDateTime.now(zone).toLocalTime()
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
                    isCalendarOpen = isCalendarOpen.value,
                    onOpenSettings = {
                        isCalendarOpen.value = false
                        isSettingsOpen.value = true
                    },
                    onCloseSettings = { isSettingsOpen.value = false },
                    onOpenCalendar = {
                        isSettingsOpen.value = false
                        isCalendarOpen.value = true
                    },
                    onCloseCalendar = { isCalendarOpen.value = false },
                    onStopAdhan = { stopAdhan() }
                )
            }
        }
    }

    private fun stopAdhan() {
        val stopIntent = Intent(this, AdhanPlaybackService::class.java).apply {
            action = PrayerAlarmScheduler.ACTION_STOP_ADHAN
        }
        startService(stopIntent)
    }

    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Immediate remote stop control while Adhan is playing (MT-1.4 Procedure B)
        // Back button is handled via OnBackPressedDispatcher (BackHandler) for Android 16+ predictive back.
        if (AdhanPlaybackService.isPlaying.value && event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    stopAdhan()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}

@Composable
private fun SalatScheduleMainContent(
    homeViewModel: HomeViewModel,
    settingsViewModel: HomeSettingsViewModel,
    isSettingsOpen: Boolean,
    isCalendarOpen: Boolean,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onOpenCalendar: () -> Unit,
    onCloseCalendar: () -> Unit,
    onStopAdhan: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val isSettingsLoaded by settingsViewModel.isSettingsLoaded.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val isAdhanPlaying by AdhanPlaybackService.isPlaying.collectAsState()

    when {
        isAdhanPlaying -> BackHandler { onStopAdhan() }
        isSettingsOpen -> BackHandler { onCloseSettings() }
        isCalendarOpen -> BackHandler { onCloseCalendar() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !isSettingsLoaded -> {
                // Hold calm dark background briefly while DataStore loads saved setup state
                Box(modifier = Modifier.fillMaxSize().background(com.salatsign.tv.ui.theme.DarkBackground))
            }
            !settings.isSetupCompleted -> {
                HomeSetupWizardScreen(
                    settings = settings,
                    onSetAppLanguage = { settingsViewModel.setAppLanguage(it) },
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
                    onSetShowStrikeHighlight = { settingsViewModel.setShowStrikeHighlight(it) },
                    onToggleIslamicEvent = { settingsViewModel.toggleIslamicEvent(it) },
                    onSetShowNextEventOnHome = { settingsViewModel.setShowNextEventOnHome(it) },
                    onSetScreensaverScheduleEnabled = { settingsViewModel.setScreensaverScheduleEnabled(it) },
                    onSetScreensaverCalendarEnabled = { settingsViewModel.setScreensaverCalendarEnabled(it) },
                    onSetScreensaverQuranEnabled = { settingsViewModel.setScreensaverQuranEnabled(it) },
                    onSetScreensaverScheduleDuration = { settingsViewModel.setScreensaverScheduleDurationSeconds(it) },
                    onSetScreensaverCalendarDuration = { settingsViewModel.setScreensaverCalendarDurationSeconds(it) },
                    onRestartSetupWizard = {
                        onCloseSettings()
                        settingsViewModel.restartSetupWizard()
                    },
                    onBack = onCloseSettings
                )
            }
            isCalendarOpen -> {
                IslamicCalendarScreen(
                    settings = settings,
                    onBack = onCloseCalendar
                )
            }
            else -> {
                HomeScreen(
                    uiState = homeUiState,
                    onPreviousDay = { homeViewModel.previousDay() },
                    onNextDay = { homeViewModel.nextDay() },
                    onTodayClick = { homeViewModel.resetToToday() },
                    onOpenCalendar = onOpenCalendar,
                    onOpenSettings = onOpenSettings
                )
            }
        }

        if (isAdhanPlaying) {
            val colorScheme = MaterialTheme.colorScheme
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp)
                    .background(colorScheme.surface.copy(alpha = 0.96f), RoundedCornerShape(16.dp))
                    .border(2.dp, AccentGold, RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = stringResource(CoreR.string.notif_channel_adhan_name),
                    style = TvTypography.CardTitle.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                )
                Button(
                    onClick = onStopAdhan,
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFFDC2626),
                        focusedContainerColor = AccentGold,
                        contentColor = Color.White,
                        focusedContentColor = Color.Black
                    ),
                    modifier = Modifier.tvFocusable()
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "  " + stringResource(CoreR.string.notif_adhan_stop_action),
                        style = TvTypography.ButtonText.copy(fontSize = 16.sp)
                    )
                }
            }
        }
    }
}
