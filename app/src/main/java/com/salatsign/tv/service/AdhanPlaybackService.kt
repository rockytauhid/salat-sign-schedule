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
import android.media.AudioFocusRequest
import android.media.AudioManager
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdhanPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioManager: AudioManager? = null

    companion object {
        private const val TAG = "AdhanPlaybackService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "channel_adhan_playback"
        const val CHANNEL_NAME = "Pemutaran Adhan"

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
        releaseAudioResources()

        // Code comment per remediation plan:
        // USAGE_ALARM may not follow the TV's volume control on some devices; this needs testing on real hardware (human task H3).
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
                    player.setDataSource(applicationContext, Uri.parse(customUriStr))
                    customLoaded = true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load custom audio URI: $customUriStr. Falling back to default asset.", e)
                }
            }

            if (!customLoaded) {
                setRawDataSource(player, prayer, type)
            }

            player.setOnPreparedListener { mp ->
                val focusResult = audioManager?.requestAudioFocus(focusRequest)
                Log.d(TAG, "Audio focus request result: $focusResult")

                mp.start()
                _isPlaying.value = true
                Log.d(TAG, "Adhan audio playback started asynchronously for ${prayer.displayName}.")
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

    private fun setRawDataSource(player: MediaPlayer, prayer: Prayer, type: AdhanAudioType) {
        val rawResId = if (type == AdhanAudioType.DEFAULT_SUBUH || prayer == Prayer.SUBUH) {
            R.raw.adhan_subuh
        } else {
            R.raw.adhan_default
        }

        val afd = resources.openRawResourceFd(rawResId)
        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        afd.close()
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
