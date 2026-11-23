// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.service

import android.content.Context
import android.media.AudioAttributes
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
 * Uses USAGE_ALARM to match the exact audio stream used by [AdhanPlaybackService].
 */
class AdhanPreviewPlayer(private val context: Context) {

    companion object {
        private const val TAG = "AdhanPreviewPlayer"
    }

    private var mediaPlayer: MediaPlayer? = null
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
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
            }

            var sourceLoaded = false
            if (option.type == AdhanAudioType.CUSTOM_FILE && !option.customUri.isNullOrBlank()) {
                try {
                    val localAudio = File(File(context.filesDir, "content/audio"), option.customUri!!)
                    if (localAudio.exists() && localAudio.canRead()) {
                        player.setDataSource(localAudio.absolutePath)
                        sourceLoaded = true
                    } else {
                        player.setDataSource(context.applicationContext, Uri.parse(option.customUri))
                        sourceLoaded = true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load custom adhan for preview: ${option.customUri}", e)
                }
            }

            if (!sourceLoaded) {
                val rawResId = AdhanAudioResolver.resolveRawResource(
                    if (option.type == AdhanAudioType.CUSTOM_FILE) AdhanAudioType.DEFAULT_STANDARD else option.type
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
        loadedOption = null
        _state.value = AdhanPreviewState.STOPPED
    }
}
