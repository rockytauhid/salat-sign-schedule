package com.salatsign.tv.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.salatsign.tv.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootAndTimeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootAndTimeReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive action=$action - recalculating and rescheduling prayer alarms")

        val repository = SettingsRepository(context)
        val scheduler = PrayerAlarmScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = repository.appSettingsFlow.first()
                scheduler.scheduleNext(settings)
                Log.d(TAG, "Successfully recalculated and rescheduled alarms after $action")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule alarms after $action", e)
            }
        }
    }
}
