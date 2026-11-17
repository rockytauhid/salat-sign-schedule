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

        // Schedule daily recurring midnight refresh via WorkManager
        DailyScheduleWorker.enqueueDailyWork(this)

        // Initial schedule check and legacy slide migration on startup
        CoroutineScope(Dispatchers.IO).launch {
            LegacySlideMigration.migrateIfNeeded(this@SalatSignApp, contentRepository)
            val settings = settingsRepository.appSettingsFlow.first()
            alarmScheduler.scheduleNext(settings)
        }
    }
}
