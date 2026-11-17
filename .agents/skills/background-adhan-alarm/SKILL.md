---
name: background-adhan-alarm
description: Implements exact AlarmManager scheduling, ForegroundService Adhan playback over other apps, and boot recovery for Home Mode.
---

# Skill: Background Adhan & Reminder Scheduling Engine

## Architecture Flow
`PrayerAlarmScheduler` -> `AlarmManager.setAlarmClock()` -> `PrayerAlarmReceiver` -> `AdhanPlaybackService` (ForegroundService)

## Implementation Rules
1. **Mode Gatekeeper:** Before scheduling or executing any alarm, check `isMosqueMode`. If `true`, cancel all pending `PendingIntent` alarms and abort immediately.
2. **Exact Scheduling (`PrayerAlarmScheduler`):**
   - For each of the 5 prayers (`SUBUH`, `ZUHUR`, `ASHR`, `MAGRIB`, `ISYA`) that have not yet passed today:
     - If `prePrayerReminderEnabled == true`, schedule an alarm at `prayerTime.minusMinutes(15)` with action `ACTION_PRE_PRAYER_REMINDER`.
     - If the prayer's assigned Adhan is not `SILENT`, schedule an exact alarm at `prayerTime` using `alarmManager.setAlarmClock(AlarmClockInfo(triggerMillis, showIntent), pendingIntent)` with action `ACTION_PLAY_ADHAN`.
   - Use deterministic unique request codes per prayer and trigger type (e.g., `101` for Subuh Adhan, `201` for Subuh 15-min reminder) so updates cleanly overwrite existing alarms.
3. **Foreground Audio Service (`AdhanPlaybackService`):**
   - Start via `ContextCompat.startForegroundService(context, intent)` from `PrayerAlarmReceiver`.
   - Call `startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)` within 5 seconds.
   - Configure `MediaPlayer` with `AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()` so Adhan plays clearly even if another app is active or the TV is on the Home screen.
   - Listen for `KEYCODE_BACK` or a "Stop Adhan" notification action to allow the user to dismiss the Adhan early. Automatically stop the service (`stopSelf()`) upon `OnCompletionListener`.
4. **Resilience (`BootAndTimeReceiver`, Alarm Chaining & `DailyScheduleWorker`):**
   - Register a receiver for `ACTION_BOOT_COMPLETED`, `ACTION_TIME_CHANGED`, and `ACTION_TIMEZONE_CHANGED` that recalculates today's prayer times and re-registers alarms.
   - When scheduling, always chain alarms across midnight: if all prayers today have elapsed, schedule tomorrow's Subuh. After each alarm fires, schedule the next prayer event.
   - Run a daily `WorkManager` periodic task just after midnight (`00:05`) as an additional safety net to refresh the day's alarms.