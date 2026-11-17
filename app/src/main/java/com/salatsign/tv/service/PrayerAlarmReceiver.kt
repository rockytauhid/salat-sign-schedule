package com.salatsign.tv.service

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
import com.salatsign.tv.MainActivity
import com.salatsign.tv.R
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.repository.SettingsRepository
import com.salatsign.tv.engine.PrayerCalculationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PrayerAlarmReceiver"
        const val REMINDER_CHANNEL_ID = "channel_prayer_reminder"
        const val REMINDER_CHANNEL_NAME = "Pengingat Shalat"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val prayerName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: ""
        val prayerDisplayName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_DISPLAY_NAME) ?: prayerName
        val adhanType = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_ADHAN_TYPE)
        val adhanUri = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_ADHAN_URI)
        val scheduledAt = intent.getLongExtra(PrayerAlarmScheduler.EXTRA_SCHEDULED_AT_EPOCH_MILLIS, 0L)

        Log.d(TAG, "onReceive action=$action prayer=$prayerDisplayName scheduledAt=$scheduledAt")

        val repository = SettingsRepository(context)
        val scheduler = PrayerAlarmScheduler(context)

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                withTimeout(8_000) {
                    val settings = repository.appSettingsFlow.first()
                    if (settings.isMosqueMode) {
                        Log.d(TAG, "Mosque Mode is active: Aborting alarm event execution.")
                        return@withTimeout
                    }

                    // Check if alarm fired more than 5 minutes late (device was off/asleep)
                    val isStale = PrayerCalculationEngine.isAlarmStale(scheduledAt)
                    if (isStale) {
                        Log.w(TAG, "Alarm for $prayerDisplayName fired >5 min late. Skipping playback/notification.")
                    } else {
                        when (action) {
                            PrayerAlarmScheduler.ACTION_PLAY_ADHAN -> {
                                // Only play adhan if prayer option is not SILENT
                                if (adhanType != AdhanAudioType.SILENT.name) {
                                    val serviceIntent = Intent(context, AdhanPlaybackService::class.java).apply {
                                        putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
                                        putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_DISPLAY_NAME, prayerDisplayName)
                                        putExtra(PrayerAlarmScheduler.EXTRA_ADHAN_TYPE, adhanType)
                                        putExtra(PrayerAlarmScheduler.EXTRA_ADHAN_URI, adhanUri)
                                    }
                                    ContextCompat.startForegroundService(context, serviceIntent)
                                } else {
                                    Log.d(TAG, "Adhan for $prayerDisplayName is SILENT. Skipping audio playback.")
                                }
                            }

                            PrayerAlarmScheduler.ACTION_PRE_PRAYER_REMINDER -> {
                                if (settings.prePrayerReminderEnabled) {
                                    showReminderNotification(context, prayerDisplayName)
                                }
                            }
                        }
                    }

                    // ALWAYS advance the alarm chain to the next prayer
                    scheduler.scheduleNext(settings)
                    Log.d(TAG, "Successfully advanced alarm chain after $action for $prayerDisplayName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in PrayerAlarmReceiver", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun showReminderNotification(context: Context, prayerDisplayName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi 15 menit menjelang waktu shalat"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

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
            .setContentTitle("15 Menit Menjelang $prayerDisplayName")
            .setContentText("Waktu shalat $prayerDisplayName akan segera tiba. Siapkan diri untuk beribadah.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .build()

        notificationManager.notify(prayerDisplayName.hashCode(), notification)
    }
}
