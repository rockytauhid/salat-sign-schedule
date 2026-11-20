// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.service

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
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class DailyScheduleWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DailyScheduleWorker"
        const val WORK_NAME = "salatsign_daily_prayer_scheduling"

        /**
         * Computes the initial delay (in minutes) until 00:05 in the configured [zoneId] (F20).
         */
        fun computeInitialDelayMinutes(
            zoneId: ZoneId = ZoneId.systemDefault(),
            now: ZonedDateTime = ZonedDateTime.now(zoneId)
        ): Long {
            val zonedNow = now.withZoneSameInstant(zoneId)
            var nextRun = zonedNow.toLocalDate().atTime(0, 5).atZone(zoneId)
            if (!zonedNow.isBefore(nextRun)) {
                nextRun = zonedNow.toLocalDate().plusDays(1).atTime(0, 5).atZone(zoneId)
            }
            return Duration.between(zonedNow, nextRun).toMinutes().coerceAtLeast(1)
        }

        /**
         * Enqueues a periodic WorkManager task that runs once every 24 hours,
         * with initial delay set so it fires just past midnight (~00:05) in the configured timezone.
         */
        fun enqueueDailyWork(context: Context, zoneIdString: String? = null) {
            val zone = runCatching {
                if (!zoneIdString.isNullOrBlank()) ZoneId.of(zoneIdString) else ZoneId.systemDefault()
            }.getOrDefault(ZoneId.systemDefault())
            val initialDelayMinutes = computeInitialDelayMinutes(zone)

            val workRequest = PeriodicWorkRequestBuilder<DailyScheduleWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            Log.d(TAG, "Enqueued daily prayer scheduling worker for zone ${zone.id} with initial delay of $initialDelayMinutes mins.")
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
