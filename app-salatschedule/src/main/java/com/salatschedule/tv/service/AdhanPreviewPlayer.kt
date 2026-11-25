// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AdhanAudioType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class AdhanPreviewState {
    STOPPED,
    PLAYING,
    PAUSED
}

/**
 * In-Settings Adhan audio preview player for SalatSchedule.
 * Uses USAGE_MEDIA + AUDIOFOCUS_GAIN_TRANSIENT to match [AdhanPlaybackService] and ensure
 * consistent TV speaker / HDMI ARC routing on both Android TV and Fire TV.
 */
class AdhanPreviewPlayer(private val context: Context) {

    companion object {
        private const val TAG = "AdhanPreviewPlayer"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var loadedOption: AdhanAudioOption? = null

    private val _state = MutableStateFlow(AdhanPreviewState.STOPPED)
    val state: StateFlow<AdhanPreviewState> = _state.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun togglePlayPause(option: AdhanAudioOption) {
        _errorMessage.value = null
        if (option.type == AdhanAudioType.SILENT) {
            stop()
            return
        }

        // If the option changed while paused/playing, restart cleanly with the new source
        if (loadedOption != null && loadedOption != option) {
            stop()
        }

        when (_state.value) {
            AdhanPreviewState.PLAYING -> {
                runCatching {
                    mediaPlayer?.pause()
                    _state.value = AdhanPreviewState.PAUSED
                }.onFailure {
                    Log.w(TAG, "Failed to pause preview", it)
                    stop()
                }
            }
            AdhanPreviewState.PAUSED -> {
                val player = mediaPlayer
                if (player != null) {
                    runCatching {
                        player.start()
                        _state.value = AdhanPreviewState.PLAYING
                    }.onFailure {
                        Log.w(TAG, "Failed to resume preview", it)
                        stop()
                    }
                } else {
                    startNew(option)
                }
            }
            AdhanPreviewState.STOPPED -> startNew(option)
        }
    }

    private fun startNew(option: AdhanAudioOption) {
        stop()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener { focusChange ->
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                ) {
                    stop()
                }
            }
            .build()
        audioFocusRequest = focusRequest

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
            }

            var sourceLoaded = false
            if (option.type == AdhanAudioType.CUSTOM_FILE && !option.customUri.isNullOrBlank()) {
                try {
                    val customUri = option.customUri!!
                    val audioDir = File(context.filesDir, "content/audio")
                    val isSimpleFileName = !customUri.contains('/') && !customUri.contains('\\') && !customUri.contains("..")
                    val localAudio = if (isSimpleFileName) File(audioDir, customUri) else null
                    val isInsideAudioDir = localAudio != null &&
                        localAudio.canonicalPath.startsWith(audioDir.canonicalPath + File.separator)
                    if (localAudio != null && isInsideAudioDir && localAudio.exists() && localAudio.canRead()) {
                        player.setDataSource(localAudio.absolutePath)
                        sourceLoaded = true
                    } else if (customUri.startsWith("content://")) {
                        player.setDataSource(context.applicationContext, Uri.parse(customUri))
                        sourceLoaded = true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load custom adhan for preview: ${option.customUri}", e)
                }
            }

            if (!sourceLoaded) {
                val rawResId = AdhanAudioResolver.resolveRawResource(
                    if (option.type == AdhanAudioType.CUSTOM_FILE) AdhanAudioType.BEEP else option.type
                )
                if (rawResId != null) {
                    val afd = context.resources.openRawResourceFd(rawResId)
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    sourceLoaded = true
                }
            }

            if (!sourceLoaded) {
                player.release()
                _errorMessage.value = "Unable to load audio file"
                _state.value = AdhanPreviewState.STOPPED
                return
            }

            player.setOnPreparedListener { mp ->
                val focusResult = audioManager?.requestAudioFocus(focusRequest)
                Log.d(TAG, "Preview audio focus request result: $focusResult")
                mp.start()
                _state.value = AdhanPreviewState.PLAYING
            }
            player.setOnCompletionListener {
                stop()
            }
            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "Preview MediaPlayer error: what=$what extra=$extra")
                _errorMessage.value = "Playback error ($what)"
                stop()
                true
            }

            mediaPlayer = player
            loadedOption = option
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Adhan preview", e)
            _errorMessage.value = e.message ?: "Playback failed"
            stop()
        }
    }

    fun stop() {
        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) {
                    player.stop()
                }
            }
            runCatching { player.release() }
        }
        mediaPlayer = null
        audioFocusRequest?.let { req ->
            runCatching { audioManager?.abandonAudioFocusRequest(req) }
        }
        audioFocusRequest = null
        loadedOption = null
        _state.value = AdhanPreviewState.STOPPED
    }
}
