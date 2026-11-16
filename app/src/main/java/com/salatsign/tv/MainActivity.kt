package com.salatsign.tv

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.salatsign.tv.ui.home.HomeScreen
import com.salatsign.tv.ui.home.HomeViewModel
import com.salatsign.tv.ui.mosque.MosqueSignageScreen
import com.salatsign.tv.ui.mosque.MosqueSlideshowViewModel
import com.salatsign.tv.ui.settings.SettingsScreen
import com.salatsign.tv.ui.settings.SettingsViewModel
import com.salatsign.tv.ui.theme.SalatSignTheme

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val mosqueViewModel: MosqueSlideshowViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private var centerKeyDownTime = 0L
    private val isSettingsOpen = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SalatSignTheme {
                MainContent(
                    homeViewModel = homeViewModel,
                    mosqueViewModel = mosqueViewModel,
                    settingsViewModel = settingsViewModel,
                    isSettingsOpen = isSettingsOpen.value,
                    onOpenSettings = { isSettingsOpen.value = true },
                    onCloseSettings = { isSettingsOpen.value = false }
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            isSettingsOpen.value = !isSettingsOpen.value
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            if (centerKeyDownTime == 0L) {
                centerKeyDownTime = System.currentTimeMillis()
            } else if (System.currentTimeMillis() - centerKeyDownTime >= 3000L) {
                centerKeyDownTime = 0L
                isSettingsOpen.value = true
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            centerKeyDownTime = 0L
        }
        return super.onKeyUp(keyCode, event)
    }
}

@Composable
fun MainContent(
    homeViewModel: HomeViewModel,
    mosqueViewModel: MosqueSlideshowViewModel,
    settingsViewModel: SettingsViewModel,
    isSettingsOpen: Boolean,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    val mosqueUiState by mosqueViewModel.uiState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    val currentView = LocalView.current

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
    BackHandler(enabled = isSettingsOpen || !homeUiState.isViewingToday) {
        if (isSettingsOpen) {
            onCloseSettings()
        } else if (!homeUiState.isViewingToday) {
            homeViewModel.resetToToday()
        }
    }

    if (isSettingsOpen) {
        SettingsScreen(
            settings = settings,
            onToggleMosqueMode = { settingsViewModel.toggleMosqueMode(it) },
            onSetHijriOffset = { settingsViewModel.setHijriOffset(it) },
            onToggleReminder = { settingsViewModel.togglePrePrayerReminder(it) },
            onSetAdhanAudio = { prayer, option -> settingsViewModel.setAdhanAudio(prayer, option) },
            onAddSampleSlide = { settingsViewModel.addSampleAnnouncementSlide() },
            onClearSlides = { settingsViewModel.clearSlides() },
            onClose = onCloseSettings
        )
    } else if (settings.isMosqueMode) {
        MosqueSignageScreen(
            uiState = mosqueUiState,
            onOpenSettings = onOpenSettings
        )
    } else {
        HomeScreen(
            uiState = homeUiState,
            onPreviousDay = { homeViewModel.previousDay() },
            onNextDay = { homeViewModel.nextDay() },
            onTodayClick = { homeViewModel.resetToToday() },
            onOpenSettings = onOpenSettings
        )
    }
}
