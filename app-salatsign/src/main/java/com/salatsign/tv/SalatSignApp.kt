// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv

import android.app.Application
import com.salatsign.tv.data.migration.LegacySlideMigration
import com.salatsign.tv.data.repository.ContentRepository
import com.salatsign.tv.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SalatSign (Mosque Digital Signage) Application.
 * Strictly silent: manages mosque settings and announcement slides without any audio or alarm services.
 */
class SalatSignApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var contentRepository: ContentRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        contentRepository = ContentRepository(this)

        // Ensure mosque mode is always active and migrate any legacy slides on startup
        CoroutineScope(Dispatchers.IO).launch {
            LegacySlideMigration.migrateIfNeeded(this@SalatSignApp, contentRepository)
            settingsRepository.setMosqueMode(true)
        }
    }
}
