package com.salatsign.tv

import android.app.Application
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

    lateinit var alarmScheduler: PrayerAlarmScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        alarmScheduler = PrayerAlarmScheduler(this)

        // Schedule daily recurring midnight refresh via WorkManager
        DailyScheduleWorker.enqueueDailyWork(this)

        // Initial schedule check on startup
        CoroutineScope(Dispatchers.IO).launch {
            val settings = settingsRepository.appSettingsFlow.first()
            alarmScheduler.scheduleNext(settings)
        }
    }
}
