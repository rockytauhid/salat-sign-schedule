// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv

import android.app.Application
import com.salatschedule.tv.service.DailyScheduleWorker
import com.salatschedule.tv.service.PrayerAlarmScheduler
import com.salatsign.tv.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * SalatSchedule (Home Prayer Times) Application.
 * Schedules offline prayer alarms, 15-minute reminders, and WorkManager daily refresh.
 */
class SalatScheduleApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var alarmScheduler: PrayerAlarmScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        alarmScheduler = PrayerAlarmScheduler(this)

        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.setMosqueMode(false)
            val settings = settingsRepository.appSettingsFlow.first()
            DailyScheduleWorker.enqueueDailyWork(this@SalatScheduleApp, settings.zoneId)
            alarmScheduler.scheduleNext(settings)
        }
    }
}
