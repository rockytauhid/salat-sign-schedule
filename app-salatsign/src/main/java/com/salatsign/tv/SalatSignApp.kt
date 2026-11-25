// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv

import android.app.Application
import com.salatsign.tv.data.api.ApiConstants
import com.salatsign.tv.data.importing.ImageImporter
import com.salatsign.tv.data.migration.LegacySlideMigration
import com.salatsign.tv.data.repository.ContentRepository
import com.salatsign.tv.data.repository.SettingsRepository
import com.salatsign.tv.engine.QuranVerseRepository
import com.salatsign.tv.quran.AssetQuranVerseRepository
import com.salatsign.tv.remote.LocalNetworkUtils
import com.salatsign.tv.remote.SalatSignLocalServer
import com.salatsign.tv.remote.SalatSignNsdHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * SalatSign (Mosque Digital Signage) Application.
 * Strictly silent: manages mosque settings, announcement slides, and optional LAN phone controller
 * without any audio or alarm services.
 */
class SalatSignApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var contentRepository: ContentRepository
        private set

    lateinit var quranVerseRepository: QuranVerseRepository
        private set

    lateinit var localServer: SalatSignLocalServer
        private set

    private lateinit var nsdHelper: SalatSignNsdHelper

    private val _phoneControllerUrl = MutableStateFlow<String?>(null)
    val phoneControllerUrl: StateFlow<String?> = _phoneControllerUrl.asStateFlow()

    private val _lastPairedDeviceNotice = MutableStateFlow<com.salatsign.tv.security.PairedDeviceRecord?>(null)
    val lastPairedDeviceNotice: StateFlow<com.salatsign.tv.security.PairedDeviceRecord?> = _lastPairedDeviceNotice.asStateFlow()

    private var ipRefreshJob: Job? = null

    fun clearPairedDeviceNotice() {
        _lastPairedDeviceNotice.value = null
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        contentRepository = ContentRepository(this)
        quranVerseRepository = AssetQuranVerseRepository(this)

        val slideImagesDir = File(filesDir, "content/images")
        val logoImagesDir = File(filesDir, "content/logo")
        localServer = SalatSignLocalServer(
            settingsRepository = settingsRepository,
            contentRepository = contentRepository,
            slideImageImporter = ImageImporter(slideImagesDir),
            logoImageImporter = ImageImporter(logoImagesDir),
            port = ApiConstants.DEFAULT_HTTP_PORT,
            appVersionName = BuildConfig.VERSION_NAME,
            webAssetProvider = {
                assets.open("phone_controller/index.html").use { it.readBytes() }
            },
            onDevicePaired = { record ->
                _lastPairedDeviceNotice.value = record
            }
        )
        nsdHelper = SalatSignNsdHelper(this)

        // Ensure mosque mode, stable TV device ID, any legacy slide migration, and Always-On auto-start
        CoroutineScope(Dispatchers.IO).launch {
            LegacySlideMigration.migrateIfNeeded(this@SalatSignApp, contentRepository)
            settingsRepository.setMosqueMode(true)
            settingsRepository.ensureDeviceId()
            val settings = settingsRepository.appSettingsFlow.first()
            if (settings.phoneControllerAlwaysOn) {
                setPhoneControllerEnabled(true)
            }
        }
    }

    fun setPhoneControllerEnabled(enabled: Boolean) {
        ipRefreshJob?.cancel()
        ipRefreshJob = null
        if (enabled) {
            if (localServer.start()) {
                val initialIp = LocalNetworkUtils.getLocalIpv4Address()
                _phoneControllerUrl.value = initialIp?.let { "http://$it:${localServer.port}" }
                ipRefreshJob = CoroutineScope(Dispatchers.IO).launch {
                    val deviceId = settingsRepository.ensureDeviceId()
                    val s = settingsRepository.appSettingsFlow.first()
                    nsdHelper.register(
                        port = localServer.port,
                        deviceId = deviceId,
                        deviceName = s.deviceName,
                        roomGroup = s.roomGroup
                    )
                    // Right after a TV cold boot, Wi-Fi/Ethernet may take several seconds to obtain an IP
                    var attempt = 0
                    while (isActive && _phoneControllerUrl.value == null && attempt < 15) {
                        delay(2_000L)
                        attempt++
                        val ip = LocalNetworkUtils.getLocalIpv4Address()
                        if (ip != null) {
                            _phoneControllerUrl.value = "http://$ip:${localServer.port}"
                            break
                        }
                    }
                }
            }
        } else {
            nsdHelper.unregister()
            localServer.stop()
            _phoneControllerUrl.value = null
        }
    }
}
