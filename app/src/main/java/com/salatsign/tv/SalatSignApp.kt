// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv

import android.app.Application
import com.salatsign.tv.data.migration.LegacySlideMigration
import com.salatsign.tv.data.repository.ContentRepository
import com.salatsign.tv.data.repository.SettingsRepository
import com.salatsign.tv.service.DailyScheduleWorker
import com.salatsign.tv.service.PrayerAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SalatSignApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var contentRepository: ContentRepository
        private set

    lateinit var alarmScheduler: PrayerAlarmScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        contentRepository = ContentRepository(this)
        alarmScheduler = PrayerAlarmScheduler(this)

        // Initial schedule check, daily WorkManager refresh (in configured zone), and legacy slide migration
        CoroutineScope(Dispatchers.IO).launch {
            LegacySlideMigration.migrateIfNeeded(this@SalatSignApp, contentRepository)
            val settings = settingsRepository.appSettingsFlow.first()
            DailyScheduleWorker.enqueueDailyWork(this@SalatSignApp, settings.zoneId)
            alarmScheduler.scheduleNext(settings)
        }
    }
}
