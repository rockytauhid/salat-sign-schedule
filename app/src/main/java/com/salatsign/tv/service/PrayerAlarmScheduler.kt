package com.salatsign.tv.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.salatsign.tv.MainActivity
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.engine.PrayerCalculationEngine
import java.time.ZonedDateTime

class PrayerAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "PrayerAlarmScheduler"

        const val ACTION_PLAY_ADHAN = "com.salatsign.tv.action.PLAY_ADHAN"
        const val ACTION_PRE_PRAYER_REMINDER = "com.salatsign.tv.action.PRE_PRAYER_REMINDER"
        const val ACTION_STOP_ADHAN = "com.salatsign.tv.action.STOP_ADHAN"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_DISPLAY_NAME = "extra_prayer_display_name"
        const val EXTRA_ADHAN_TYPE = "extra_adhan_type"
        const val EXTRA_ADHAN_URI = "extra_adhan_uri"
        const val EXTRA_SCHEDULED_AT_EPOCH_MILLIS = "extra_scheduled_at_epoch_millis"

        const val REQUEST_CODE_ADHAN = 100
        const val REQUEST_CODE_REMINDER = 200
    }

    /**
     * Schedules the next adhan and 15-minute reminder using two single-slot alarms.
     * Evaluates across midnight to guarantee uninterrupted chaining.
     */
    fun scheduleNext(settings: AppSettings, now: ZonedDateTime = ZonedDateTime.now()) {
        if (settings.isMosqueMode) {
            Log.d(TAG, "Mosque Mode is active: Canceling all pending alarms and aborting scheduling.")
            cancelAllAlarms()
            return
        }

        // 1. Next Adhan (Single slot with fixed REQUEST_CODE_ADHAN)
        val nextAdhan = PrayerCalculationEngine.nextPrayerEvent(now, settings)
        val adhanOption = settings.adhanFor(nextAdhan.prayer)
        val adhanTriggerMillis = nextAdhan.at.toInstant().toEpochMilli()

        val adhanIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PLAY_ADHAN
            putExtra(EXTRA_PRAYER_NAME, nextAdhan.prayer.name)
            putExtra(EXTRA_PRAYER_DISPLAY_NAME, nextAdhan.prayer.displayName)
            putExtra(EXTRA_ADHAN_TYPE, adhanOption.type.name)
            putExtra(EXTRA_ADHAN_URI, adhanOption.customUri)
            putExtra(EXTRA_SCHEDULED_AT_EPOCH_MILLIS, adhanTriggerMillis)
        }
        val adhanPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ADHAN,
            adhanIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(adhanTriggerMillis, adhanPendingIntent)
        Log.d(TAG, "Scheduled next Adhan for ${nextAdhan.prayer.displayName} at ${nextAdhan.at} (epoch: $adhanTriggerMillis, type: ${adhanOption.type})")

        // 2. Next 15-Minute Reminder (Single slot with fixed REQUEST_CODE_REMINDER)
        if (settings.prePrayerReminderEnabled) {
            val nextReminder = PrayerCalculationEngine.nextReminderEvent(now, settings)
            val reminderTriggerMillis = nextReminder.reminderAt.toInstant().toEpochMilli()

            val reminderIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = ACTION_PRE_PRAYER_REMINDER
                putExtra(EXTRA_PRAYER_NAME, nextReminder.prayer.name)
                putExtra(EXTRA_PRAYER_DISPLAY_NAME, nextReminder.prayer.displayName)
                putExtra(EXTRA_SCHEDULED_AT_EPOCH_MILLIS, reminderTriggerMillis)
            }
            val reminderPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_REMINDER,
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setExactAlarm(reminderTriggerMillis, reminderPendingIntent)
            Log.d(TAG, "Scheduled next 15-min reminder for ${nextReminder.prayer.displayName} at ${nextReminder.reminderAt} (epoch: $reminderTriggerMillis)")
        } else {
            cancelReminderAlarm()
        }
    }

    private fun setExactAlarm(triggerMillis: Long, pendingIntent: PendingIntent) {
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            0,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    fun cancelAllAlarms() {
        cancelAdhanAlarm()
        cancelReminderAlarm()
    }

    fun cancelAdhanAlarm() {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PLAY_ADHAN
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ADHAN,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelReminderAlarm() {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRE_PRAYER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REMINDER,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
