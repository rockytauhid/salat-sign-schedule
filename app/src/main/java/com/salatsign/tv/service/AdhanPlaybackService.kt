package com.salatsign.tv.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.salatsign.tv.MainActivity
import com.salatsign.tv.R
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.Prayer

class AdhanPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "AdhanPlaybackService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "channel_adhan_playback"
        const val CHANNEL_NAME = "Pemutaran Adhan"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SalatSign:AdhanPlaybackWakeLock"
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
        val prayerDisplayName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_DISPLAY_NAME) ?: "Waktu Shalat"
        val adhanTypeStr = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_ADHAN_TYPE)
        val customUriStr = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_ADHAN_URI)

        val prayer = runCatching { Prayer.valueOf(prayerName) }.getOrDefault(Prayer.SUBUH)
        val adhanType = runCatching {
            AdhanAudioType.valueOf(adhanTypeStr ?: "")
        }.getOrDefault(
            if (prayer == Prayer.SUBUH) AdhanAudioType.DEFAULT_SUBUH else AdhanAudioType.DEFAULT_STANDARD
        )

        createNotificationChannel()
        val notification = buildForegroundNotification(prayerDisplayName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        playAdhan(prayer, adhanType, customUriStr)

        return START_NOT_STICKY
    }

    private fun playAdhan(prayer: Prayer, type: AdhanAudioType, customUriStr: String?) {
        releaseMediaPlayer()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
            }

            var prepared = false

            if (type == AdhanAudioType.CUSTOM_FILE && !customUriStr.isNullOrBlank()) {
                try {
                    player.setDataSource(applicationContext, Uri.parse(customUriStr))
                    player.prepare()
                    prepared = true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load custom audio URI: $customUriStr. Falling back to default asset.", e)
                }
            }

            if (!prepared) {
                val rawResId = if (type == AdhanAudioType.DEFAULT_SUBUH || prayer == Prayer.SUBUH) {
                    R.raw.adhan_subuh
                } else {
                    R.raw.adhan_default
                }

                val afd = resources.openRawResourceFd(rawResId)
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                player.prepare()
            }

            player.setOnCompletionListener {
                Log.d(TAG, "Adhan audio playback completed normally.")
                stopSelf()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                stopSelf()
                true
            }

            player.start()
            mediaPlayer = player
            Log.d(TAG, "Adhan audio playback started for ${prayer.displayName}.")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPlayer for Adhan playback", e)
            stopSelf()
        }
    }

    private fun buildForegroundNotification(prayerDisplayName: String): Notification {
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
            .setContentTitle("Waktu Shalat $prayerDisplayName Telah Tiba")
            .setContentText("Mengumandangkan Adhan $prayerDisplayName...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(tapPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Hentikan Adhan", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pemutaran suara Adhan waktu shalat"
                setSound(null, null) // Audio is handled explicitly via USAGE_ALARM MediaPlayer
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun releaseMediaPlayer() {
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
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
        Log.d(TAG, "AdhanPlaybackService destroyed.")
    }
}
