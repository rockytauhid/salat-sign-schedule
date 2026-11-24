// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.screensaver

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.salatschedule.tv.SalatScheduleApp
import com.salatsign.tv.data.repository.LanguageSyncCache
import com.salatsign.tv.ui.common.LocaleHelper

/**
 * Full-screen preview activity hosting the exact same composable content as [SalatScreensaverService].
 * Any key press (Back, OK, D-pad) closes the preview cleanly.
 */
class ScreensaverPreviewActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val language = LanguageSyncCache.getLanguage(newBase)
        super.attachBaseContext(LocaleHelper.wrap(newBase, language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val app = application as SalatScheduleApp
        setContent {
            ScreensaverHost(
                settingsRepository = app.settingsRepository,
                quranVerseRepository = app.quranVerseRepository
            )
        }
    }

    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            finish()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
