// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.salatsign.tv.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class BootAndTimeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootAndTimeReceiver"
        internal val ALLOWED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in ALLOWED_ACTIONS) {
            Log.w(TAG, "Ignoring unexpected broadcast action=$action")
            return
        }
        Log.d(TAG, "onReceive action=$action - recalculating and rescheduling prayer alarms")

        val repository = SettingsRepository(context)
        val scheduler = PrayerAlarmScheduler(context)

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                withTimeout(8_000) {
                    val settings = repository.appSettingsFlow.first()
                    scheduler.scheduleNext(settings)
                    Log.d(TAG, "Successfully recalculated and rescheduled alarms after $action")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule alarms after $action", e)
            } finally {
                pending.finish()
            }
        }
    }
}
