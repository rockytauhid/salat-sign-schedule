package com.salatsign.tv.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.salatsign.tv.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class DailyScheduleWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DailyScheduleWorker"
        const val WORK_NAME = "salatsign_daily_prayer_scheduling"

        /**
         * Enqueues a periodic WorkManager task that runs once every 24 hours,
         * with initial delay set so it fires just past midnight (~00:05).
         */
        fun enqueueDailyWork(context: Context) {
            val now = LocalDateTime.now()
            var nextRun = now.toLocalDate().atTime(0, 5) // 00:05 AM
            if (now.isAfter(nextRun)) {
                nextRun = nextRun.plusDays(1)
            }
            val initialDelayMinutes = Duration.between(now, nextRun).toMinutes().coerceAtLeast(1)

            val workRequest = PeriodicWorkRequestBuilder<DailyScheduleWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            Log.d(TAG, "Enqueued daily prayer scheduling worker with initial delay of $initialDelayMinutes mins.")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val repository = SettingsRepository(applicationContext)
            val scheduler = PrayerAlarmScheduler(applicationContext)
            val settings = repository.appSettingsFlow.first()

            scheduler.scheduleNext(settings)
            Log.d(TAG, "Next prayer alarm scheduled successfully by WorkManager safety net.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error running DailyScheduleWorker", e)
            Result.retry()
        }
    }
}
