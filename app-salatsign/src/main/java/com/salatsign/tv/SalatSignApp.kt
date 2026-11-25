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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
            }
        )
        nsdHelper = SalatSignNsdHelper(this)

        // Ensure mosque mode, stable TV device ID, and any legacy slide migration on startup
        CoroutineScope(Dispatchers.IO).launch {
            LegacySlideMigration.migrateIfNeeded(this@SalatSignApp, contentRepository)
            settingsRepository.setMosqueMode(true)
            settingsRepository.ensureDeviceId()
        }
    }

    fun setPhoneControllerEnabled(enabled: Boolean) {
        if (enabled) {
            if (localServer.start()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val deviceId = settingsRepository.ensureDeviceId()
                    val s = settingsRepository.appSettingsFlow.first()
                    nsdHelper.register(
                        port = localServer.port,
                        deviceId = deviceId,
                        deviceName = s.deviceName,
                        roomGroup = s.roomGroup
                    )
                }
                val ip = LocalNetworkUtils.getLocalIpv4Address()
                _phoneControllerUrl.value = ip?.let { "http://$it:${localServer.port}" }
            }
        } else {
            nsdHelper.unregister()
            localServer.stop()
            _phoneControllerUrl.value = null
        }
    }
}
