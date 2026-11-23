// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.salatschedule.tv.MainActivity
import com.salatsign.core.tv.R as CoreR
import com.salatschedule.tv.R
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.repository.LanguageSyncCache
import com.salatsign.tv.data.repository.SettingsRepository
import com.salatsign.tv.ui.common.LocaleHelper
import com.salatsign.tv.ui.common.labelRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class AdhanPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioManager: AudioManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    companion object {
        private const val TAG = "AdhanPlaybackService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "channel_adhan_playback"

        private val _isPlaying = MutableStateFlow(false)
        val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SalatSchedule:AdhanPlaybackWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L) // 10 minutes timeout
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent.action == PrayerAlarmScheduler.ACTION_STOP_ADHAN) {
            Log.d(TAG, "Stop adhan action received. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        val prayerName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: "SUBUH"
        val adhanTypeStr = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_ADHAN_TYPE)
        val customUriStr = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_ADHAN_URI)

        val prayer = runCatching { Prayer.valueOf(prayerName) }.getOrDefault(Prayer.SUBUH)
        val adhanType = runCatching {
            AdhanAudioType.valueOf(adhanTypeStr ?: "")
        }.getOrDefault(
            if (prayer == Prayer.SUBUH) AdhanAudioType.DEFAULT_SUBUH else AdhanAudioType.DEFAULT_STANDARD
        )

        val passedLangStr = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_LANGUAGE)
        val language = if (!passedLangStr.isNullOrBlank()) {
            LanguageSyncCache.parseLanguage(passedLangStr)
        } else {
            LanguageSyncCache.getLanguage(this)
        }

        val passedZoneId = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_ZONE_ID)
        val zone = runCatching {
            if (!passedZoneId.isNullOrBlank()) ZoneId.of(passedZoneId) else ZoneId.systemDefault()
        }.getOrDefault(ZoneId.systemDefault())

        val localizedContext = LocaleHelper.wrap(this, language)
        // SalatSchedule is Home Mode: Friday displays plain Zuhr (D16/D18)
        val prayerDisplayName = localizedContext.getString(
            prayer.labelRes(isFriday = false)
        )

        createNotificationChannel(localizedContext)
        val notification = buildForegroundNotification(localizedContext, prayerDisplayName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // If language or settings were not passed via extras, update notification in background without blocking
        if (passedLangStr == null) {
            serviceScope.launch {
                val freshSettings = runCatching {
                    SettingsRepository(this@AdhanPlaybackService).appSettingsFlow.first()
                }.getOrNull() ?: return@launch

                val updatedContext = LocaleHelper.wrap(this@AdhanPlaybackService, freshSettings.appLanguage)
                val updatedName = updatedContext.getString(
                    prayer.labelRes(isFriday = false)
                )
                val updatedNotification = buildForegroundNotification(updatedContext, updatedName)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, updatedNotification)
            }
        }

        playAdhan(prayer, adhanType, customUriStr)

        return START_NOT_STICKY
    }

    private fun playAdhan(prayer: Prayer, type: AdhanAudioType, customUriStr: String?) {
        releaseAudioResources()

        if (type == AdhanAudioType.SILENT) {
            Log.d(TAG, "Adhan type is SILENT. Stopping service.")
            stopSelf()
            return
        }

        // Note: USAGE_ALARM may not follow the TV's volume control on some devices; this needs testing on real hardware (human task H3).
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        Log.d(TAG, "Audio focus lost ($focusChange). Stopping Adhan playback.")
                        stopSelf()
                    }
                }
            }
            .build()
        audioFocusRequest = focusRequest

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
            }

            var customLoaded = false
            if (type == AdhanAudioType.CUSTOM_FILE && !customUriStr.isNullOrBlank()) {
                try {
                    val audioDir = File(filesDir, "content/audio")
                    val isSimpleFileName = !customUriStr.contains('/') && !customUriStr.contains('\\') && !customUriStr.contains("..")
                    val localAudio = if (isSimpleFileName) File(audioDir, customUriStr) else null
                    val isInsideAudioDir = localAudio != null &&
                        localAudio.canonicalPath.startsWith(audioDir.canonicalPath + File.separator)
                    if (localAudio != null && isInsideAudioDir && localAudio.exists() && localAudio.canRead()) {
                        player.setDataSource(localAudio.absolutePath)
                        customLoaded = true
                    } else if (customUriStr.startsWith("content://")) {
                        player.setDataSource(applicationContext, Uri.parse(customUriStr))
                        customLoaded = true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load custom audio URI: $customUriStr. Falling back to default asset.", e)
                }
            }

            if (!customLoaded) {
                setRawDataSource(player, type)
            }

            player.setOnPreparedListener { mp ->
                val focusResult = audioManager?.requestAudioFocus(focusRequest)
                Log.d(TAG, "Audio focus request result: $focusResult")

                mp.start()
                _isPlaying.value = true
                Log.d(TAG, "Adhan audio playback started asynchronously for ${prayer.name}.")
            }

            player.setOnCompletionListener {
                Log.d(TAG, "Adhan audio playback completed normally.")
                _isPlaying.value = false
                stopSelf()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                _isPlaying.value = false
                stopSelf()
                true
            }

            mediaPlayer = player
            player.prepareAsync()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPlayer for Adhan playback", e)
            _isPlaying.value = false
            stopSelf()
        }
    }

    private fun setRawDataSource(player: MediaPlayer, type: AdhanAudioType) {
        val rawResId = AdhanAudioResolver.resolveRawResource(type)
        if (rawResId == null) {
            Log.w(TAG, "No raw audio resource resolved for type: $type")
            return
        }

        val afd = resources.openRawResourceFd(rawResId)
        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        afd.close()
    }

    private fun buildForegroundNotification(localizedContext: Context, prayerDisplayName: String): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AdhanPlaybackService::class.java).apply {
            action = PrayerAlarmScheduler.ACTION_STOP_ADHAN
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(localizedContext.getString(CoreR.string.notif_adhan_title, prayerDisplayName))
            .setContentText(localizedContext.getString(CoreR.string.notif_adhan_text, prayerDisplayName))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(tapPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                localizedContext.getString(CoreR.string.notif_adhan_stop_action),
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel(localizedContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                localizedContext.getString(CoreR.string.notif_channel_adhan_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = localizedContext.getString(CoreR.string.notif_channel_adhan_desc)
                setSound(null, null) // Audio is handled explicitly via USAGE_ALARM MediaPlayer
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun releaseAudioResources() {
        _isPlaying.value = false
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing MediaPlayer", e)
            }
        }
        mediaPlayer = null

        audioFocusRequest?.let { focusReq ->
            try {
                audioManager?.abandonAudioFocusRequest(focusReq)
            } catch (e: Exception) {
                Log.w(TAG, "Error abandoning AudioFocusRequest", e)
            }
        }
        audioFocusRequest = null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        releaseAudioResources()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
        Log.d(TAG, "AdhanPlaybackService destroyed.")
    }
}
