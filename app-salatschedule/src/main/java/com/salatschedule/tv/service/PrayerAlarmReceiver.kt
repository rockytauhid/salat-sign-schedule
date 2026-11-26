// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.salatschedule.tv.MainActivity
import com.salatsign.core.tv.R as CoreR
import com.salatschedule.tv.R
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.repository.SettingsRepository
import com.salatsign.tv.engine.PrayerCalculationEngine
import com.salatsign.tv.ui.common.LocaleHelper
import com.salatsign.tv.ui.common.labelRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PrayerAlarmReceiver"
        const val REMINDER_CHANNEL_ID = "channel_prayer_reminder"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val prayerName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: ""
        val adhanType = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_ADHAN_TYPE)
        val adhanUri = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_ADHAN_URI)
        val scheduledAt = intent.getLongExtra(PrayerAlarmScheduler.EXTRA_SCHEDULED_AT_EPOCH_MILLIS, 0L)

        Log.d(TAG, "onReceive action=$action prayer=$prayerName scheduledAt=$scheduledAt")

        val repository = SettingsRepository(context)
        val scheduler = PrayerAlarmScheduler(context)

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                withTimeout(8_000) {
                    val settings = repository.appSettingsFlow.first()
                    val isReminderAllowed = settings.prePrayerReminderEnabled

                    // Check if alarm fired more than 5 minutes late (device was off/asleep)
                    val isStale = PrayerCalculationEngine.isAlarmStale(scheduledAt)
                    if (isStale) {
                        Log.w(TAG, "Alarm for $prayerName fired >5 min late. Skipping playback/notification.")
                    } else {
                        when (action) {
                            PrayerAlarmScheduler.ACTION_PLAY_ADHAN -> {
                                if (adhanType != AdhanAudioType.SILENT.name) {
                                    val serviceIntent = Intent(context, AdhanPlaybackService::class.java).apply {
                                        putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
                                        putExtra(PrayerAlarmScheduler.EXTRA_ADHAN_TYPE, adhanType)
                                        putExtra(PrayerAlarmScheduler.EXTRA_ADHAN_URI, adhanUri)
                                        putExtra(PrayerAlarmScheduler.EXTRA_LANGUAGE, settings.appLanguage.name)
                                        putExtra(PrayerAlarmScheduler.EXTRA_ZONE_ID, settings.zoneId)
                                    }
                                    ContextCompat.startForegroundService(context, serviceIntent)
                                } else {
                                    Log.d(TAG, "Adhan for $prayerName is SILENT. Skipping audio playback.")
                                }
                            }

                            PrayerAlarmScheduler.ACTION_PRE_PRAYER_REMINDER -> {
                                if (isReminderAllowed) {
                                    val prayer = runCatching { Prayer.valueOf(prayerName) }.getOrNull()
                                    if (prayer != null) {
                                        val localizedContext = LocaleHelper.wrap(context, settings.appLanguage)
                                        showReminderNotification(localizedContext, prayer, settings)
                                    }
                                }
                            }
                        }
                    }

                    // ALWAYS advance the alarm chain to the next prayer
                    scheduler.scheduleNext(settings)
                    Log.d(TAG, "Successfully advanced alarm chain after $action for $prayerName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in PrayerAlarmReceiver", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun showReminderNotification(context: Context, prayer: Prayer, settings: AppSettings) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                context.getString(CoreR.string.notif_channel_reminder_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(CoreR.string.notif_channel_reminder_desc)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // SalatSchedule displays Friday as plain Zuhr (D16/D18)
        val prayerLabel = context.getString(prayer.labelRes(isFriday = false))

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(CoreR.string.notif_reminder_title, prayerLabel))
            .setContentText(context.getString(CoreR.string.notif_reminder_text, prayerLabel))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .build()

        notificationManager.notify(prayer.hashCode(), notification)
    }
}
