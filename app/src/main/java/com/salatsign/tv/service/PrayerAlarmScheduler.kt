package com.salatsign.tv.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.salatsign.tv.MainActivity
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.engine.PrayerCalculationEngine
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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

        private val PRAYERS = listOf(
            Prayer.SUBUH,
            Prayer.ZUHUR,
            Prayer.ASHR,
            Prayer.MAGRIB,
            Prayer.ISYA
        )
    }

    /**
     * Schedules alarms for today's prayers according to the current settings.
     * Rule: If isMosqueMode is true, cancels all pending alarms and aborts immediately.
     */
    fun scheduleAlarmsForToday(settings: AppSettings) {
        if (settings.isMosqueMode) {
            Log.d(TAG, "Mosque Mode is active: Canceling all pending alarms and aborting scheduling.")
            cancelAllAlarms()
            return
        }

        val today = LocalDate.now()
        val now = LocalTime.now()
        val zoneId = ZoneId.systemDefault()

        val prayerTimesDay = PrayerCalculationEngine.calculatePrayerTimesDay(
            date = today,
            latitude = settings.latitude,
            longitude = settings.longitude,
            calculationMethodName = settings.calculationMethodName,
            hijriOffsetDays = settings.hijriOffsetDays,
            referenceTime = now,
            referenceDate = today,
            zoneId = zoneId
        )

        for (item in prayerTimesDay.prayers) {
            val prayer = item.prayer
            val prayerTime = item.time
            val prayerDateTime = ZonedDateTime.of(today, prayerTime, zoneId)
            val adhanOption = settings.adhanFor(prayer)

            // 1. Pre-Prayer 15-Minute Reminder
            if (settings.prePrayerReminderEnabled) {
                val reminderDateTime = prayerDateTime.minusMinutes(15)
                val reminderMillis = reminderDateTime.toInstant().toEpochMilli()
                val currentMillis = System.currentTimeMillis()

                if (reminderMillis > currentMillis) {
                    val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                        action = ACTION_PRE_PRAYER_REMINDER
                        putExtra(EXTRA_PRAYER_NAME, prayer.name)
                        putExtra(EXTRA_PRAYER_DISPLAY_NAME, prayer.displayName)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        prayer.defaultRequestCodeReminder,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    setExactAlarm(reminderMillis, pendingIntent)
                    Log.d(TAG, "Scheduled 15-min reminder for ${prayer.displayName} at $reminderDateTime")
                } else {
                    cancelReminderAlarm(prayer)
                }
            } else {
                cancelReminderAlarm(prayer)
            }

            // 2. Exact Prayer Adhan Playback
            if (adhanOption.type != AdhanAudioType.SILENT) {
                val triggerMillis = prayerDateTime.toInstant().toEpochMilli()
                val currentMillis = System.currentTimeMillis()

                if (triggerMillis > currentMillis) {
                    val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                        action = ACTION_PLAY_ADHAN
                        putExtra(EXTRA_PRAYER_NAME, prayer.name)
                        putExtra(EXTRA_PRAYER_DISPLAY_NAME, prayer.displayName)
                        putExtra(EXTRA_ADHAN_TYPE, adhanOption.type.name)
                        putExtra(EXTRA_ADHAN_URI, adhanOption.customUri)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        prayer.defaultRequestCodeAdhan,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    setExactAlarm(triggerMillis, pendingIntent)
                    Log.d(TAG, "Scheduled Adhan for ${prayer.displayName} at $prayerDateTime")
                } else {
                    cancelAdhanAlarm(prayer)
                }
            } else {
                cancelAdhanAlarm(prayer)
            }
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
        for (prayer in PRAYERS) {
            cancelAdhanAlarm(prayer)
            cancelReminderAlarm(prayer)
        }
    }

    private fun cancelAdhanAlarm(prayer: Prayer) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PLAY_ADHAN
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayer.defaultRequestCodeAdhan,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun cancelReminderAlarm(prayer: Prayer) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRE_PRAYER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayer.defaultRequestCodeReminder,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
