// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.remote

import com.salatsign.tv.data.api.ApiErrorResponse
import com.salatsign.tv.data.api.AuthMeResponse
import com.salatsign.tv.data.api.AuthResponse
import com.salatsign.tv.data.api.CreateSlideResponse
import com.salatsign.tv.data.api.FullConfigResponse
import com.salatsign.tv.data.api.GroupJoinCodeResponse
import com.salatsign.tv.data.api.PairExchangeResponse
import com.salatsign.tv.data.api.PairedDevicesResponse
import com.salatsign.tv.data.api.PeerJoinResponse
import com.salatsign.tv.data.api.PeerJoinSecretPayload
import com.salatsign.tv.data.api.PeerListResponse
import com.salatsign.tv.data.api.PeerSecretRotatePayload
import com.salatsign.tv.data.api.PeerSecretRotateRequest
import com.salatsign.tv.data.api.PeerSyncAckResponse
import com.salatsign.tv.data.api.PeerSyncPayload
import com.salatsign.tv.data.api.PhoneControllerJson
import com.salatsign.tv.data.api.SettingsPatchRequest
import com.salatsign.tv.data.api.SlideDto
import com.salatsign.tv.data.api.StatusResponse
import com.salatsign.tv.data.api.UrgentNoticeResponse
import com.salatsign.tv.data.importing.ImageImporter
import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.FridayAdhanTiming
import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerSilentConfig
import com.salatsign.tv.data.model.SlideContainer
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.data.repository.IContentRepository
import com.salatsign.tv.data.repository.ISettingsRepository
import com.salatsign.tv.security.PinHasher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import kotlin.io.path.createTempDirectory

class SalatSignLocalServerTest {

    private class FakeSettingsRepository(initial: AppSettings = AppSettings(isMosqueMode = true)) : ISettingsRepository {
        private val state = MutableStateFlow(initial)
        override val appSettingsFlow: Flow<AppSettings> = state
        val current: AppSettings get() = state.value

        override suspend fun setSetupCompleted(completed: Boolean) { state.value = state.value.copy(isSetupCompleted = completed) }
        override suspend fun setMosqueMode(enabled: Boolean) { state.value = state.value.copy(isMosqueMode = enabled) }
        override suspend fun setThemeMode(mode: ThemeMode) { state.value = state.value.copy(themeMode = mode) }
        override suspend fun selectCity(city: CityPreset) { state.value = state.value.copy(cityName = city.name) }
        override suspend fun updateLocation(latitude: Double, longitude: Double, cityName: String, method: String?, zoneId: String?) {}
        override suspend fun setZoneId(zoneId: String) { state.value = state.value.copy(zoneId = zoneId) }
        override suspend fun setCalculationMethod(method: String) { state.value = state.value.copy(calculationMethodName = method) }
        override suspend fun setMadhab(madhab: String) { state.value = state.value.copy(madhab = madhab) }
        override suspend fun setHighLatitudeRule(rule: String?) { state.value = state.value.copy(highLatitudeRule = rule) }
        override suspend fun updateHijriOffset(offsetDays: Int) { state.value = state.value.copy(hijriOffsetDays = offsetDays) }
        override suspend fun setHijriChangeAtMidnight(atMidnight: Boolean) { state.value = state.value.copy(hijriChangeAtMidnight = atMidnight) }
        override suspend fun setPrePrayerReminder(enabled: Boolean) {}
        override suspend fun updatePrayerOffset(prayer: Prayer, offsetMinutes: Int) {}
        override suspend fun setPrayerOffset(prayer: Prayer, offsetMinutes: Int) {}
        override suspend fun updateSunriseOffset(offsetMinutes: Int) {}
        override suspend fun setSunriseOffset(offsetMinutes: Int) {}
        override suspend fun updateIqamahOffset(prayer: Prayer, minutesAfter: Int) { setIqamahOffset(prayer, minutesAfter) }
        override suspend fun setIqamahOffset(prayer: Prayer, minutesAfter: Int) {
            state.value = when (prayer) {
                Prayer.SUBUH -> state.value.copy(iqamahSubuh = minutesAfter)
                Prayer.ZUHUR -> state.value.copy(iqamahZuhur = minutesAfter)
                Prayer.ASHR -> state.value.copy(iqamahAshr = minutesAfter)
                Prayer.MAGRIB -> state.value.copy(iqamahMagrib = minutesAfter)
                Prayer.ISYA -> state.value.copy(iqamahIsya = minutesAfter)
            }
        }
        override suspend fun setMosqueModeAdhan(enabled: Boolean) {}
        override suspend fun setMosqueModeReminders(enabled: Boolean) {}
        override suspend fun setMosqueModeAdhanEnabled(enabled: Boolean) {}
        override suspend fun setMosqueModeRemindersEnabled(enabled: Boolean) {}
        override suspend fun setShowStrikeHighlight(show: Boolean) { state.value = state.value.copy(showStrikeHighlight = show) }
        override suspend fun updateAdhanOption(prayer: Prayer, option: AdhanAudioOption) {}
        override suspend fun updateMosqueBranding(name: String, subtitle: String, logoUri: String?) {
            state.value = state.value.copy(mosqueName = name, mosqueSubtitle = subtitle, mosqueLogoUri = logoUri)
        }
        override suspend fun setMosqueIconPreset(preset: MosqueIconPreset) { state.value = state.value.copy(mosqueIconPreset = preset) }
        override suspend fun setMosqueFontPreset(preset: MosqueFontPreset) { state.value = state.value.copy(mosqueFontPreset = preset) }
        override suspend fun updateScheduleDuration(seconds: Int) { state.value = state.value.copy(scheduleDurationSeconds = seconds) }
        override suspend fun setCustomPin(pin: String) {
            val hashed = PinHasher.createHashedPin(pin)
            state.value = state.value.copy(hasCustomPin = true, pinSaltHex = hashed.saltHex, pinHashHex = hashed.hashHex)
        }
        override suspend fun resetToDefaultPin() { state.value = state.value.copy(hasCustomPin = false, pinSaltHex = "", pinHashHex = "") }
        override suspend fun setHomeModePinRequired(required: Boolean) {}
        override suspend fun recordPinSuccess() { state.value = state.value.copy(pinFailedAttempts = 0, pinLockedUntilEpochMillis = 0L) }
        override suspend fun recordPinFailure(newFailedAttempts: Int, lockedUntilEpochMillis: Long) {
            state.value = state.value.copy(pinFailedAttempts = newFailedAttempts, pinLockedUntilEpochMillis = lockedUntilEpochMillis)
        }
        override suspend fun dismissDefaultPinPrompt() {}
        override suspend fun dismissAdhanHint() {}
        override suspend fun setAppLanguage(language: AppLanguage) {}
        override suspend fun set24HourFormat(enabled: Boolean) { state.value = state.value.copy(is24HourFormat = enabled) }
        override suspend fun setShowArabicPrayerNames(enabled: Boolean) { state.value = state.value.copy(showArabicPrayerNames = enabled) }
        override suspend fun setJumuahMode(mode: JumuahMode) { state.value = state.value.copy(jumuahMode = mode) }
        override suspend fun setJumuahTimes(times: List<String>) { state.value = state.value.copy(jumuahTimes = times) }
        override suspend fun setFridayAdhanTiming(timing: FridayAdhanTiming) {}
        override suspend fun setSilentJumuahEnabled(enabled: Boolean) {
            state.value = state.value.copy(silentScreen = state.value.silentScreen.copy(jumuahEnabled = enabled))
        }
        override suspend fun setSilentJumuahLeadMinutes(minutes: Int) {
            state.value = state.value.copy(silentScreen = state.value.silentScreen.copy(jumuahLeadMinutes = minutes))
        }
        override suspend fun setSilentJumuahDurationMinutes(minutes: Int) {
            state.value = state.value.copy(silentScreen = state.value.silentScreen.copy(jumuahDurationMinutes = minutes))
        }
        override suspend fun setSilentDailyPrayerConfig(prayer: Prayer, config: PrayerSilentConfig) {
            val updated = state.value.silentScreen.dailyPrayers.toMutableMap().apply { put(prayer, config) }
            state.value = state.value.copy(silentScreen = state.value.silentScreen.copy(dailyPrayers = updated))
        }
        override suspend fun setSilentDailyPrayerConfigs(configs: Map<Prayer, PrayerSilentConfig>) {
            state.value = state.value.copy(silentScreen = state.value.silentScreen.copy(dailyPrayers = configs))
        }
        override suspend fun setManualSilentUntil(epochMillis: Long) {
            state.value = state.value.copy(silentScreen = state.value.silentScreen.copy(manualSilentUntilEpochMillis = epochMillis))
        }
        override suspend fun setEnabledIslamicEventIds(ids: Set<String>) {
            state.value = state.value.copy(enabledIslamicEventIds = ids)
        }
        override suspend fun setShowNextEventOnHome(show: Boolean) {
            state.value = state.value.copy(showNextEventOnHome = show)
        }
        override suspend fun setIslamicCalendarSlideEnabled(enabled: Boolean) {
            state.value = state.value.copy(islamicCalendarSlideEnabled = enabled)
        }
        override suspend fun setIslamicCalendarSlideDurationSeconds(seconds: Int) {
            state.value = state.value.copy(islamicCalendarSlideDurationSeconds = seconds)
        }
        override suspend fun setQuranVerseEnabled(enabled: Boolean) {
            state.value = state.value.copy(quranVerseEnabled = enabled)
        }
        override suspend fun setQuranShowTranslation(show: Boolean) {
            state.value = state.value.copy(quranShowTranslation = show)
        }
        override suspend fun setQuranVerseDurationSeconds(seconds: Int) {
            state.value = state.value.copy(quranVerseDurationSeconds = seconds)
        }
        override suspend fun setQuranShuffleBagState(serializedState: String) {
            state.value = state.value.copy(quranShuffleBagState = serializedState)
        }
        override suspend fun setScreensaverScheduleEnabled(enabled: Boolean) {
            state.value = state.value.copy(screensaverScheduleEnabled = enabled)
        }
        override suspend fun setScreensaverCalendarEnabled(enabled: Boolean) {
            state.value = state.value.copy(screensaverCalendarEnabled = enabled)
        }
        override suspend fun setScreensaverQuranEnabled(enabled: Boolean) {
            state.value = state.value.copy(screensaverQuranEnabled = enabled)
        }
        override suspend fun setScreensaverScheduleDurationSeconds(seconds: Int) {
            state.value = state.value.copy(screensaverScheduleDurationSeconds = seconds)
        }
        override suspend fun setScreensaverCalendarDurationSeconds(seconds: Int) {
            state.value = state.value.copy(screensaverCalendarDurationSeconds = seconds)
        }
        override suspend fun ensureDeviceId(): String {
            val existing = state.value.deviceId
            if (existing.isNotBlank()) return existing
            val created = "tv-test-uuid-0001"
            state.value = state.value.copy(deviceId = created)
            return created
        }
        override suspend fun ensureDeviceEcKeyPair(): Pair<String, String> {
            val cur = state.value
            if (cur.deviceEcPublicKeyBase64.isNotBlank() && cur.deviceEcPrivateKeyBase64.isNotBlank()) {
                return cur.deviceEcPublicKeyBase64 to cur.deviceEcPrivateKeyBase64
            }
            val kp = com.salatsign.tv.security.PeerJoinCrypto.generateEphemeralKeyPair()
            val pub = com.salatsign.tv.security.PeerJoinCrypto.encodePublicKey(kp)
            val priv = com.salatsign.tv.security.PeerJoinCrypto.encodePrivateKey(kp)
            state.value = cur.copy(deviceEcPublicKeyBase64 = pub, deviceEcPrivateKeyBase64 = priv)
            return pub to priv
        }
        override suspend fun setDeviceIdentity(name: String, roomGroup: String) {
            state.value = state.value.copy(deviceName = name.trim(), roomGroup = roomGroup.trim())
        }
        override suspend fun bumpConfigVersion(): Long {
            val next = state.value.configVersion + 1L
            state.value = state.value.copy(configVersion = next)
            return next
        }
        override suspend fun setConfigVersion(version: Long) {
            state.value = state.value.copy(configVersion = version.coerceAtLeast(1L))
        }
        override suspend fun addPairedDevice(record: com.salatsign.tv.security.PairedDeviceRecord) {
            val hadActive = state.value.pairedDevices.any { !it.isRevoked }
            val updated = state.value.pairedDevices.filterNot { it.deviceId == record.deviceId } + record
            state.value = state.value.copy(
                pairedDevices = updated,
                lanPinLoginEnabled = if (!hadActive) false else state.value.lanPinLoginEnabled,
                configVersion = state.value.configVersion + 1L
            )
        }
        override suspend fun revokePairedDevice(deviceId: String, revokedAtEpochMillis: Long) {
            val updated = state.value.pairedDevices.map {
                if (it.deviceId == deviceId && !it.isRevoked) it.copy(revokedAtEpochMillis = revokedAtEpochMillis) else it
            }
            state.value = state.value.copy(
                pairedDevices = updated,
                configVersion = state.value.configVersion + 1L
            )
        }
        override suspend fun renamePairedDevice(deviceId: String, newName: String) {
            val clean = newName.trim().take(48)
            if (clean.isEmpty()) return
            val updated = state.value.pairedDevices.map {
                if (it.deviceId == deviceId) it.copy(deviceName = clean) else it
            }
            state.value = state.value.copy(
                pairedDevices = updated,
                configVersion = state.value.configVersion + 1L
            )
        }
        override suspend fun touchPairedDeviceLastSeen(deviceId: String, lastSeenAtEpochMillis: Long) {
            val updated = state.value.pairedDevices.map {
                if (it.deviceId == deviceId) it.copy(lastSeenAtEpochMillis = lastSeenAtEpochMillis) else it
            }
            state.value = state.value.copy(pairedDevices = updated)
        }
        override suspend fun setLanPinLoginEnabled(enabled: Boolean) {
            state.value = state.value.copy(lanPinLoginEnabled = enabled)
        }
        override suspend fun setPairedTokenExpiryDays(days: Int) {
            state.value = state.value.copy(pairedTokenExpiryDays = days)
        }
        override suspend fun setPhoneControllerAlwaysOn(enabled: Boolean) {
            state.value = state.value.copy(phoneControllerAlwaysOn = enabled)
        }
        override suspend fun setGroupSecretHex(secretHex: String) {
            state.value = state.value.copy(groupSecretHex = secretHex)
        }
        override suspend fun upsertPeerTv(peer: com.salatsign.tv.security.PeerTvRecord) {
            val updated = state.value.peerTvs.filterNot { it.deviceId == peer.deviceId } + peer
            state.value = state.value.copy(peerTvs = updated)
        }
        override suspend fun setPeerTvs(peers: List<com.salatsign.tv.security.PeerTvRecord>) {
            state.value = state.value.copy(peerTvs = peers)
        }
        override suspend fun setPairedDevices(devices: List<com.salatsign.tv.security.PairedDeviceRecord>) {
            state.value = state.value.copy(pairedDevices = devices)
        }
        override suspend fun setActiveUrgentNotice(notice: com.salatsign.tv.security.UrgentNoticeRecord?) {
            state.value = state.value.copy(activeUrgentNotice = notice)
        }
    }

    private class FakeContentRepository(private val tempDir: File) : IContentRepository {
        private val _slides = MutableStateFlow(SlideContainer())
        override val slidesFlow: StateFlow<SlideContainer> = _slides.asStateFlow()

        override suspend fun replaceAll(expectedRevision: Int, slides: List<AnnouncementSlide>): Result<SlideContainer> {
            val next = SlideContainer(revision = expectedRevision + 1, slides = slides)
            _slides.value = next
            return Result.success(next)
        }

        override suspend fun addSlide(slide: AnnouncementSlide): Result<SlideContainer> {
            val next = SlideContainer(revision = _slides.value.revision + 1, slides = _slides.value.slides + slide)
            _slides.value = next
            return Result.success(next)
        }

        override suspend fun updateSlide(slide: AnnouncementSlide): Result<SlideContainer> {
            val next = SlideContainer(
                revision = _slides.value.revision + 1,
                slides = _slides.value.slides.map { if (it.id == slide.id) slide else it }
            )
            _slides.value = next
            return Result.success(next)
        }

        override suspend fun deleteSlide(slideId: String): Result<SlideContainer> {
            val next = SlideContainer(
                revision = _slides.value.revision + 1,
                slides = _slides.value.slides.filterNot { it.id == slideId }
            )
            _slides.value = next
            return Result.success(next)
        }

        override fun getActiveSlides(slides: List<AnnouncementSlide>, today: LocalDate): List<AnnouncementSlide> = slides
        override fun getImageFile(fileName: String): File = File(tempDir, fileName)
    }

    private fun createServer(
        customPin: String? = "2468",
        port: Int = 0,
        nowProvider: () -> Long = { 1_700_000_000_000L },
        maxConnectionsPerIp: Int = 4,
        headerDeadlineMillis: Long = 10_000L
    ): Triple<SalatSignLocalServer, FakeSettingsRepository, FakeContentRepository> {
        val initial = if (customPin != null) {
            val hashed = PinHasher.createHashedPin(customPin)
            AppSettings(isMosqueMode = true, hasCustomPin = true, pinSaltHex = hashed.saltHex, pinHashHex = hashed.hashHex)
        } else {
            AppSettings(isMosqueMode = true, hasCustomPin = false)
        }
        val settingsRepo = FakeSettingsRepository(initial)
        val dir = createTempDirectory("salatsign-remote-test").toFile()
        val slidesRepo = FakeContentRepository(dir)
        val server = SalatSignLocalServer(
            settingsRepository = settingsRepo,
            contentRepository = slidesRepo,
            slideImageImporter = ImageImporter(File(dir, "images")),
            logoImageImporter = ImageImporter(File(dir, "logo")),
            port = port,
            appVersionName = "1.0-test",
            nowMillisProvider = nowProvider,
            allowedHostProvider = { setOf("127.0.0.1", "localhost", "[::1]", "192.168.4.34") },
            maxConnectionsPerIp = maxConnectionsPerIp,
            headerDeadlineMillis = headerDeadlineMillis
        )
        return Triple(server, settingsRepo, slidesRepo)
    }

    private suspend fun authenticate(server: SalatSignLocalServer, pin: String = "2468"): Map<String, String> {
        val authResp = server.handleRequest(
            HttpRequest("POST", "/api/v1/auth", body = """{"pin":"$pin"}""".toByteArray())
        )
        assertEquals(200, authResp.statusCode)
        val token = PhoneControllerJson.decode<AuthResponse>(authResp.body.toString(StandardCharsets.UTF_8)).token
        return mapOf("authorization" to "Bearer $token")
    }

    @Test
    fun publicStatusAndWebControllerHtml_areAccessibleWithoutAuth_andDoNotLeakMosqueDetails() = runBlocking {
        val (server, _, _) = createServer()

        val htmlResp = server.handleRequest(HttpRequest("GET", "/"))
        assertEquals(200, htmlResp.statusCode)
        assertTrue(htmlResp.contentType.startsWith("text/html"))
        assertTrue(htmlResp.body.toString(StandardCharsets.UTF_8).contains("Salat<span>Sign</span> Remote"))

        val statusResp = server.handleRequest(HttpRequest("GET", "/api/v1/status"))
        assertEquals(200, statusResp.statusCode)
        val rawBody = statusResp.body.toString(StandardCharsets.UTF_8)
        val status = PhoneControllerJson.decode<StatusResponse>(rawBody)
        assertEquals("SalatSign", status.app)
        assertEquals(1, status.apiVersion)
        assertEquals(true, status.pinConfigured)
        assertTrue(status.deviceId.isNotBlank())
        assertEquals(1L, status.configVersion)
        assertFalse(rawBody.contains("mosqueName"))
        assertFalse(rawBody.contains("cityName"))
    }

    @Test
    fun auth_refusesRemoteLoginWhileDefaultPinIsActive() = runBlocking {
        val (server, _, _) = createServer(customPin = null)
        val resp = server.handleRequest(
            HttpRequest("POST", "/api/v1/auth", body = """{"pin":"0000"}""".toByteArray())
        )
        assertEquals(403, resp.statusCode)
        val err = PhoneControllerJson.decode<ApiErrorResponse>(resp.body.toString(StandardCharsets.UTF_8))
        assertEquals("default_pin_blocked", err.error)
    }

    @Test
    fun protectedEndpoints_rejectUnauthenticatedRequests() = runBlocking {
        val (server, _, _) = createServer()
        val resp = server.handleRequest(HttpRequest("GET", "/api/v1/settings"))
        assertEquals(401, resp.statusCode)
    }

    @Test
    fun auth_verifiesPinAndEnforcesBruteForceLockout_serializedUnderConcurrency() = runBlocking {
        var now = 1_700_000_000_000L
        val (server, settingsRepo, _) = createServer(customPin = "2468", nowProvider = { now })

        // 5 concurrent wrong guesses MUST all count atomically (no lockout race)
        val results = (1..5).map {
            async {
                server.handleRequest(
                    HttpRequest("POST", "/api/v1/auth", body = """{"pin":"9999"}""".toByteArray())
                )
            }
        }.awaitAll()
        assertTrue(results.all { it.statusCode == 401 })
        assertEquals(5, server.currentRemoteFailedAttempts)
        // Review 3.1: Remote brute-force guesses must NEVER lock out the TV's own Settings PIN dialog
        assertEquals(0, settingsRepo.current.pinFailedAttempts)
        assertEquals(0L, settingsRepo.current.pinLockedUntilEpochMillis)

        // 6th attempt while locked out returns 429 even with correct custom PIN
        val locked = server.handleRequest(
            HttpRequest("POST", "/api/v1/auth", body = """{"pin":"2468"}""".toByteArray())
        )
        assertEquals(429, locked.statusCode)

        // After 31 seconds, correct PIN succeeds and resets remote failed attempts
        now += 31_000L
        val ok = server.handleRequest(
            HttpRequest("POST", "/api/v1/auth", body = """{"pin":"2468"}""".toByteArray())
        )
        assertEquals(200, ok.statusCode)
        assertEquals(0, server.currentRemoteFailedAttempts)
        val token = PhoneControllerJson.decode<AuthResponse>(ok.body.toString(StandardCharsets.UTF_8)).token
        assertNotNull(token)

        // Logout immediately revokes token
        val logout = server.handleRequest(
            HttpRequest("POST", "/api/v1/logout", headers = mapOf("authorization" to "Bearer $token"))
        )
        assertEquals(200, logout.statusCode)
        val afterLogout = server.handleRequest(
            HttpRequest("GET", "/api/v1/settings", headers = mapOf("authorization" to "Bearer $token"))
        )
        assertEquals(401, afterLogout.statusCode)
    }

    @Test
    fun securityHardening_rejectsDeepJsonOversizedPreAuthAndInvalidHost() = runBlocking {
        val (server, _, _) = createServer()

        // Deeply nested JSON does not StackOverflow — returns 400 cleanly
        val deepJson = "[".repeat(80) + "]".repeat(80)
        val deepResp = server.handleRequest(
            HttpRequest("POST", "/api/v1/auth", body = deepJson.toByteArray())
        )
        assertEquals(400, deepResp.statusCode)

        // Oversized pre-auth body returns 413
        val hugePreAuth = ByteArray(4096) { 'a'.code.toByte() }
        val hugeResp = server.handleRequest(HttpRequest("POST", "/api/v1/auth", body = hugePreAuth))
        assertEquals(413, hugeResp.statusCode)

        // DNS rebinding Host header is rejected with 403
        val badHost = server.handleRequest(
            HttpRequest("GET", "/api/v1/status", headers = mapOf("host" to "evil.attacker.com:8787"))
        )
        assertEquals(403, badHost.statusCode)

        // Local IP / localhost / .local are accepted
        assertTrue(server.isValidHostHeader("192.168.4.34:8787"))
        assertTrue(server.isValidHostHeader("localhost:8787"))
        assertTrue(server.isValidHostHeader("salatsign.local:8787"))
        assertFalse(server.isValidHostHeader("evil.com:8787"))
    }

    @Test
    fun authenticatedSettingsPatchAndSlideEditCrud_persistChanges() = runBlocking {
        val (server, settingsRepo, contentRepo) = createServer()
        val authHeaders = authenticate(server)

        val patchResp = server.handleRequest(
            HttpRequest(
                method = "PATCH",
                path = "/api/v1/settings",
                headers = authHeaders,
                body = """{"mosqueName":"Masjid Al-Noor","mosqueSubtitle":"Canberra","themeMode":"ALWAYS_LIGHT","mosqueIconPreset":"MINARET","mosqueFontPreset":"TRADITIONAL_SERIF","scheduleDurationSeconds":45,"hijriOffsetDays":1,"hijriChangeAtMidnight":true,"is24HourFormat":true,"showArabicPrayerNames":false,"jumuahMode":"FIXED","jumuahTimes":["13:00","13:45"],"iqamahSubuh":15,"islamicCalendarSlideEnabled":true,"islamicCalendarSlideDurationSeconds":30,"quranVerseEnabled":true,"quranShowTranslation":false,"quranVerseDurationSeconds":25,"silentJumuahEnabled":true,"silentJumuahLeadMinutes":10,"silentJumuahDurationMinutes":60}""".toByteArray()
            )
        )
        assertEquals(200, patchResp.statusCode)
        assertEquals("Masjid Al-Noor", settingsRepo.current.mosqueName)
        assertEquals(ThemeMode.ALWAYS_LIGHT, settingsRepo.current.themeMode)
        assertEquals(MosqueIconPreset.MINARET, settingsRepo.current.mosqueIconPreset)
        assertEquals(MosqueFontPreset.TRADITIONAL_SERIF, settingsRepo.current.mosqueFontPreset)
        assertEquals(45, settingsRepo.current.scheduleDurationSeconds)
        assertEquals(1, settingsRepo.current.hijriOffsetDays)
        assertTrue(settingsRepo.current.hijriChangeAtMidnight)
        assertTrue(settingsRepo.current.is24HourFormat)
        assertFalse(settingsRepo.current.showArabicPrayerNames)
        assertEquals(listOf("13:00", "13:45"), settingsRepo.current.jumuahTimes)
        assertEquals(15, settingsRepo.current.iqamahSubuh)
        assertTrue(settingsRepo.current.islamicCalendarSlideEnabled)
        assertEquals(30, settingsRepo.current.islamicCalendarSlideDurationSeconds)
        assertTrue(settingsRepo.current.quranVerseEnabled)
        assertFalse(settingsRepo.current.quranShowTranslation)
        assertEquals(25, settingsRepo.current.quranVerseDurationSeconds)
        assertEquals(10, settingsRepo.current.silentScreen.jumuahLeadMinutes)
        assertEquals(60, settingsRepo.current.silentScreen.jumuahDurationMinutes)

        // Add text slide
        val addResp = server.handleRequest(
            HttpRequest(
                method = "POST",
                path = "/api/v1/slides/text",
                headers = authHeaders,
                body = """{"title":"Community Dinner","body":"Saturday 7pm","durationSeconds":20}""".toByteArray()
            )
        )
        assertEquals(201, addResp.statusCode)
        val slideId = PhoneControllerJson.decode<CreateSlideResponse>(addResp.body.toString(StandardCharsets.UTF_8)).id
        assertEquals(1, contentRepo.slidesFlow.value.slides.size)

        // Edit slide title, body, and duration
        val editResp = server.handleRequest(
            HttpRequest(
                method = "PATCH",
                path = "/api/v1/slides/$slideId",
                headers = authHeaders,
                body = """{"title":"Updated Dinner","body":"Sunday 6pm","durationSeconds":30}""".toByteArray()
            )
        )
        assertEquals(200, editResp.statusCode)
        assertEquals("Updated Dinner", contentRepo.slidesFlow.value.slides.first().title)
        assertEquals("Sunday 6pm", contentRepo.slidesFlow.value.slides.first().body)
        assertEquals(30, contentRepo.slidesFlow.value.slides.first().durationSeconds)

        // Delete slide
        val delResp = server.handleRequest(
            HttpRequest(method = "DELETE", path = "/api/v1/slides/$slideId", headers = authHeaders)
        )
        assertEquals(200, delResp.statusCode)
        assertEquals(0, contentRepo.slidesFlow.value.slides.size)
    }

    @Test
    fun manualSilentScreen_requiresAuthAndStartsStopsCleanly() = runBlocking {
        val now = 1_700_000_000_000L
        val (server, settingsRepo, _) = createServer(nowProvider = { now })

        assertEquals(401, server.handleRequest(HttpRequest("POST", "/api/v1/silent/start")).statusCode)
        assertEquals(401, server.handleRequest(HttpRequest("POST", "/api/v1/silent/stop")).statusCode)

        val authHeaders = authenticate(server)

        val startResp = server.handleRequest(
            HttpRequest(
                method = "POST",
                path = "/api/v1/silent/start",
                headers = authHeaders,
                body = """{"durationMinutes":30}""".toByteArray()
            )
        )
        assertEquals(200, startResp.statusCode)
        val expectedUntil = now + 30 * 60_000L
        assertEquals(expectedUntil, settingsRepo.current.silentScreen.manualSilentUntilEpochMillis)

        val stopResp = server.handleRequest(HttpRequest("POST", "/api/v1/silent/stop", headers = authHeaders))
        assertEquals(200, stopResp.statusCode)
        assertEquals(0L, settingsRepo.current.silentScreen.manualSilentUntilEpochMillis)
    }

    @Test
    fun slideBudget_refusesSeventhEnabledSlide_allowsDisabledAndReEnableAfterDisable() = runBlocking {
        val (server, _, contentRepo) = createServer()
        val authHeaders = authenticate(server)

        var firstSlideId = ""
        repeat(6) { idx ->
            val r = server.handleRequest(
                HttpRequest("POST", "/api/v1/slides/text", headers = authHeaders, body = """{"title":"S$idx","body":"B$idx"}""".toByteArray())
            )
            assertEquals(201, r.statusCode)
            if (idx == 0) {
                firstSlideId = PhoneControllerJson.decode<CreateSlideResponse>(r.body.toString(StandardCharsets.UTF_8)).id
            }
        }

        // 7th enabled slide is refused with 409 Conflict
        assertEquals(
            409,
            server.handleRequest(
                HttpRequest("POST", "/api/v1/slides/text", headers = authHeaders, body = """{"title":"S7","body":"B7","enabled":true}""".toByteArray())
            ).statusCode
        )

        // 7th slide saved as disabled is allowed
        val seventhDisabled = server.handleRequest(
            HttpRequest("POST", "/api/v1/slides/text", headers = authHeaders, body = """{"title":"S7","body":"B7","enabled":false}""".toByteArray())
        )
        assertEquals(201, seventhDisabled.statusCode)
        val seventhId = PhoneControllerJson.decode<CreateSlideResponse>(seventhDisabled.body.toString(StandardCharsets.UTF_8)).id
        assertEquals(7, contentRepo.slidesFlow.value.slides.size)

        // Trying to enable 7th while 6 are enabled -> refused (409)
        assertEquals(
            409,
            server.handleRequest(HttpRequest("PATCH", "/api/v1/slides/$seventhId", headers = authHeaders, body = """{"enabled":true}""".toByteArray())).statusCode
        )

        // Disable 1st slide, then enable 7th -> succeeds (200)
        assertEquals(
            200,
            server.handleRequest(HttpRequest("PATCH", "/api/v1/slides/$firstSlideId", headers = authHeaders, body = """{"enabled":false}""".toByteArray())).statusCode
        )
        assertEquals(
            200,
            server.handleRequest(HttpRequest("PATCH", "/api/v1/slides/$seventhId", headers = authHeaders, body = """{"enabled":true}""".toByteArray())).statusCode
        )
    }

    @Test
    fun tcpLoopbackRoundTrip_servesStatusAndSecurityHeadersOverRealSocket() {
        val freePort = ServerSocket(0).use { it.localPort }
        val (server, _, _) = createServer(port = freePort)
        assertTrue(server.start())
        try {
            val conn = URL("http://127.0.0.1:$freePort/api/v1/status").openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            assertEquals(200, conn.responseCode)
            assertEquals("nosniff", conn.getHeaderField("X-Content-Type-Options"))
            assertEquals("DENY", conn.getHeaderField("X-Frame-Options"))
            assertNotNull(conn.getHeaderField("Content-Security-Policy"))
            val body = conn.inputStream.bufferedReader().readText()
            val status = PhoneControllerJson.decode<StatusResponse>(body)
            assertEquals("SalatSign", status.app)
            assertEquals(freePort, status.port)
        } finally {
            server.stop()
        }
    }

    private fun sendRawHttp(port: Int, rawRequest: String, shutdownOutput: Boolean = false): String {
        Socket("127.0.0.1", port).use { sock ->
            sock.soTimeout = 3000
            val out = sock.getOutputStream()
            out.write(rawRequest.toByteArray(StandardCharsets.US_ASCII))
            out.flush()
            if (shutdownOutput) {
                sock.shutdownOutput()
            }
            return sock.getInputStream().bufferedReader(StandardCharsets.UTF_8).readText()
        }
    }

    @Test
    fun requestReaderLimits_enforce414_431_truncated400_timeout408_andPerIpCap503() {
        val freePort = ServerSocket(0).use { it.localPort }
        val (server, _, _) = createServer(
            port = freePort,
            maxConnectionsPerIp = 2,
            headerDeadlineMillis = 600L
        )
        assertTrue(server.start())
        try {
            // 1. 414 URI Too Long (request line > 4096 bytes)
            val longUri = "GET /" + "a".repeat(4200) + " HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n"
            val resp414 = sendRawHttp(freePort, longUri)
            assertTrue("Expected 414, got: $resp414", resp414.startsWith("HTTP/1.1 414"))

            // 2. 431 Request Header Fields Too Large (> 64 headers)
            val manyHeaders = buildString {
                append("GET /api/v1/status HTTP/1.1\r\nHost: 127.0.0.1\r\n")
                repeat(70) { idx -> append("X-Test-$idx: val\r\n") }
                append("\r\n")
            }
            val resp431 = sendRawHttp(freePort, manyHeaders)
            assertTrue("Expected 431, got: $resp431", resp431.startsWith("HTTP/1.1 431"))

            // 3. 400 Bad Request on truncated body (EOF before Content-Length bytes)
            val truncated = "POST /api/v1/auth HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Length: 50\r\n\r\n{\"pin\":\"2468\"}"
            val resp400 = sendRawHttp(freePort, truncated, shutdownOutput = true)
            assertTrue("Expected 400 truncated_body, got: $resp400", resp400.startsWith("HTTP/1.1 400") && resp400.contains("truncated_body"))

            // 4. 408 Request Timeout when client trickles headers past deadline
            Socket("127.0.0.1", freePort).use { slow ->
                slow.soTimeout = 3000
                slow.getOutputStream().write("GET /api/v1/status HTTP/1.1\r\n".toByteArray(StandardCharsets.US_ASCII))
                slow.getOutputStream().flush()
                val resp408 = slow.getInputStream().bufferedReader(StandardCharsets.UTF_8).readText()
                assertTrue("Expected 408 timeout, got: $resp408", resp408.startsWith("HTTP/1.1 408"))
            }

            // 5. 503 Service Unavailable when per-IP connection cap (2) is exceeded
            val held1 = Socket("127.0.0.1", freePort)
            val held2 = Socket("127.0.0.1", freePort)
            try {
                // Give the accept loop a moment to register the 2 held sockets
                Thread.sleep(100)
                val resp503 = sendRawHttp(freePort, "GET /api/v1/status HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n")
                assertTrue("Expected 503 per-IP cap, got: $resp503", resp503.startsWith("HTTP/1.1 503"))
            } finally {
                runCatching { held1.close() }
                runCatching { held2.close() }
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun auth_upgradesLegacy10kPinHashToCurrentV2OnSuccessfulLogin() = runBlocking {
        val pin = "4321"
        val legacySalt = PinHasher.generateSalt()
        val legacyHashHex = PinHasher.bytesToHex(PinHasher.hashPin(pin, legacySalt, PinHasher.LEGACY_ITERATIONS))
        val legacySaltHex = PinHasher.bytesToHex(legacySalt)

        val settingsRepo = FakeSettingsRepository(
            AppSettings(
                isMosqueMode = true,
                hasCustomPin = true,
                pinSaltHex = legacySaltHex,
                pinHashHex = legacyHashHex
            )
        )
        val dir = createTempDirectory("salatsign-rehash-test").toFile()
        val slidesRepo = FakeContentRepository(dir)
        val server = SalatSignLocalServer(
            settingsRepository = settingsRepo,
            contentRepository = slidesRepo,
            slideImageImporter = ImageImporter(File(dir, "images")),
            logoImageImporter = ImageImporter(File(dir, "logo")),
            port = 0,
            appVersionName = "1.0-test",
            allowedHostProvider = { setOf("127.0.0.1", "localhost", "[::1]") }
        )

        assertTrue(PinHasher.needsRehash(true, settingsRepo.current.pinHashHex))
        val resp = server.handleRequest(HttpRequest("POST", "/api/v1/auth", body = """{"pin":"$pin"}""".toByteArray()))
        assertEquals(200, resp.statusCode)
        assertTrue(
            "Expected upgraded v2:${PinHasher.CURRENT_ITERATIONS} hash, got ${settingsRepo.current.pinHashHex}",
            settingsRepo.current.pinHashHex.startsWith("v2:${PinHasher.CURRENT_ITERATIONS}:")
        )
        assertFalse(PinHasher.needsRehash(true, settingsRepo.current.pinHashHex))
    }

    @Test
    fun phase0_fullConfigIfMatchAndIdempotentClientUuidSlides() = runBlocking {
        val (server, settingsRepo, contentRepo) = createServer()
        val authHeaders = authenticate(server)

        // 1. GET /api/v1/config returns device identity, configVersion, and ETag
        val initialCfgResp = server.handleRequest(HttpRequest("GET", "/api/v1/config", headers = authHeaders))
        assertEquals(200, initialCfgResp.statusCode)
        assertEquals("\"1\"", initialCfgResp.etag)
        val initialCfg = PhoneControllerJson.decode<FullConfigResponse>(initialCfgResp.body.toString(StandardCharsets.UTF_8))
        assertEquals(1L, initialCfg.configVersion)
        assertTrue(initialCfg.deviceId.isNotBlank())

        // 2. Stale If-Match returns 412 Precondition Failed
        val stalePatch = server.handleRequest(
            HttpRequest(
                method = "PATCH",
                path = "/api/v1/settings",
                headers = authHeaders + ("if-match" to "\"999\""),
                body = """{"deviceName":"Upstairs Left","roomGroup":"Upstairs"}""".toByteArray()
            )
        )
        assertEquals(412, stalePatch.statusCode)
        assertEquals("", settingsRepo.current.deviceName)

        // 3. Matching If-Match succeeds and bumps configVersion to 2
        val goodPatch = server.handleRequest(
            HttpRequest(
                method = "PATCH",
                path = "/api/v1/settings",
                headers = authHeaders + ("if-match" to "\"1\""),
                body = """{"deviceName":"Upstairs Left","roomGroup":"Upstairs","mosqueName":"Masjid Al-Huda"}""".toByteArray()
            )
        )
        assertEquals(200, goodPatch.statusCode)
        assertEquals("\"2\"", goodPatch.etag)
        assertEquals("Upstairs Left", settingsRepo.current.deviceName)
        assertEquals("Upstairs", settingsRepo.current.roomGroup)
        assertEquals(2L, settingsRepo.current.configVersion)

        // 4. Client-supplied UUID on POST /api/v1/slides/text is idempotent on retry (no duplicate created)
        val clientUuid = "client-slide-uuid-1234"
        val slidePayload = """{"id":"$clientUuid","title":"Friday Khutbah","body":"1:00 PM sharp","durationSeconds":20}""".toByteArray()
        val firstPost = server.handleRequest(HttpRequest("POST", "/api/v1/slides/text", headers = authHeaders, body = slidePayload))
        assertEquals(201, firstPost.statusCode)
        assertEquals(1, contentRepo.slidesFlow.value.slides.size)
        assertEquals(3L, settingsRepo.current.configVersion)

        val retryPost = server.handleRequest(HttpRequest("POST", "/api/v1/slides/text", headers = authHeaders, body = slidePayload))
        assertEquals(200, retryPost.statusCode)
        assertEquals(1, contentRepo.slidesFlow.value.slides.size)
        assertEquals(3L, settingsRepo.current.configVersion) // no-op retry does not spuriously bump version

        // 5. PUT /api/v1/slides/{id} upserts cleanly
        val putResp = server.handleRequest(
            HttpRequest(
                method = "PUT",
                path = "/api/v1/slides/$clientUuid",
                headers = authHeaders + ("if-match" to "\"3\""),
                body = """{"title":"Friday Khutbah Updated","body":"1:15 PM","durationSeconds":25}""".toByteArray()
            )
        )
        assertEquals(200, putResp.statusCode)
        assertEquals("Friday Khutbah Updated", contentRepo.slidesFlow.value.slides.first().title)
        assertEquals(4L, settingsRepo.current.configVersion)

        // 6. PUT /api/v1/config replaces settings & slides atomically with If-Match
        val putConfig = server.handleRequest(
            HttpRequest(
                method = "PUT",
                path = "/api/v1/config",
                headers = authHeaders + ("if-match" to "\"4\""),
                body = """{"settings":{"mosqueName":"Masjid Al-Noor"},"slides":[{"id":"s-1","type":"TEXT","title":"Welcome","body":"Assalamu alaikum","durationSeconds":15,"enabled":true}]}""".toByteArray()
            )
        )
        assertEquals(200, putConfig.statusCode)
        val updatedCfg = PhoneControllerJson.decode<FullConfigResponse>(putConfig.body.toString(StandardCharsets.UTF_8))
        assertEquals(5L, updatedCfg.configVersion)
        assertEquals("Masjid Al-Noor", updatedCfg.settings.mosqueName)
        assertEquals(1, updatedCfg.slides.size)
        assertEquals("s-1", updatedCfg.slides.first().id)
    }

    @Test
    fun phase1_browserPairingRoleEnforcementRevocationAndPinDisable() = runBlocking {
        val (server, settingsRepo, _) = createServer()

        // 1. Generate a MANAGE pairing code on the TV and pair a browser
        val manageCode = server.pairingManager.generatePairingCode(com.salatsign.tv.security.RemoteRole.MANAGE)
        val pairResp = server.handleRequest(
            HttpRequest(
                method = "POST",
                path = "/api/v1/pair",
                body = """{"code":"${manageCode.code}","deviceName":"Imam iPhone"}""".toByteArray(),
                remoteAddress = "192.168.4.50"
            )
        )
        assertEquals(200, pairResp.statusCode)
        val paired = PhoneControllerJson.decode<PairExchangeResponse>(pairResp.body.toString(StandardCharsets.UTF_8))
        assertEquals("Imam iPhone", paired.deviceName)
        assertEquals("MANAGE", paired.role)
        assertTrue(paired.token.isNotBlank())

        // 2. First paired device automatically disables LAN PIN login
        assertFalse(settingsRepo.current.lanPinLoginEnabled)
        val pinBlocked = server.handleRequest(
            HttpRequest("POST", "/api/v1/auth", body = """{"pin":"2468"}""".toByteArray())
        )
        assertEquals(403, pinBlocked.statusCode)
        assertEquals(
            "pin_login_disabled",
            PhoneControllerJson.decode<ApiErrorResponse>(pinBlocked.body.toString(StandardCharsets.UTF_8)).error
        )

        // 3. Paired MANAGE device can access settings and list devices
        val manageHeaders = mapOf("authorization" to "Bearer ${paired.token}")
        assertEquals(200, server.handleRequest(HttpRequest("GET", "/api/v1/settings", headers = manageHeaders)).statusCode)
        val devicesResp = server.handleRequest(HttpRequest("GET", "/api/v1/devices", headers = manageHeaders))
        assertEquals(200, devicesResp.statusCode)
        val devicesList = PhoneControllerJson.decode<PairedDevicesResponse>(devicesResp.body.toString(StandardCharsets.UTF_8))
        assertEquals(1, devicesList.devices.size)

        // 4. Pair a NOTIFY-only device -> allowed on /auth/me, blocked (403) on management endpoints
        val notifyCode = server.pairingManager.generatePairingCode(com.salatsign.tv.security.RemoteRole.NOTIFY)
        val notifyPair = server.handleRequest(
            HttpRequest("POST", "/api/v1/pair", body = """{"code":"${notifyCode.code}","deviceName":"Volunteer Phone"}""".toByteArray())
        )
        assertEquals(200, notifyPair.statusCode)
        val notifyToken = PhoneControllerJson.decode<PairExchangeResponse>(notifyPair.body.toString(StandardCharsets.UTF_8)).token
        val notifyHeaders = mapOf("authorization" to "Bearer $notifyToken")

        val meResp = server.handleRequest(HttpRequest("GET", "/api/v1/auth/me", headers = notifyHeaders))
        assertEquals(200, meResp.statusCode)
        assertEquals("NOTIFY", PhoneControllerJson.decode<AuthMeResponse>(meResp.body.toString(StandardCharsets.UTF_8)).role)
        assertEquals(
            403,
            server.handleRequest(HttpRequest("PATCH", "/api/v1/settings", headers = notifyHeaders, body = """{"mosqueName":"X"}""".toByteArray())).statusCode
        )

        // 5. Revoking a paired device makes its token fail immediately (401)
        val revokeResp = server.handleRequest(
            HttpRequest("DELETE", "/api/v1/devices/${paired.deviceId}", headers = manageHeaders)
        )
        assertEquals(200, revokeResp.statusCode)
        assertEquals(401, server.handleRequest(HttpRequest("GET", "/api/v1/settings", headers = manageHeaders)).statusCode)
    }

    @Test
    fun phase3_urgentNoticeForNotifyRoleAndHmacSignedPeerSyncPreservesLocalIdentity() = runBlocking {
        val now = 1_700_000_000_000L
        val (server, settingsRepo, _) = createServer(nowProvider = { now })
        settingsRepo.setDeviceIdentity("Upstairs Left", "Upstairs")
        settingsRepo.setThemeMode(ThemeMode.ALWAYS_LIGHT)

        // 1. Pair a NOTIFY-only phone -> CAN send and clear Urgent Notices!
        val notifyCode = server.pairingManager.generatePairingCode(com.salatsign.tv.security.RemoteRole.NOTIFY)
        val notifyPair = server.handleRequest(
            HttpRequest("POST", "/api/v1/pair", body = """{"code":"${notifyCode.code}","deviceName":"Volunteer"}""".toByteArray())
        )
        val notifyToken = PhoneControllerJson.decode<PairExchangeResponse>(notifyPair.body.toString(StandardCharsets.UTF_8)).token
        val notifyHeaders = mapOf("authorization" to "Bearer $notifyToken")

        val sendResp = server.handleRequest(
            HttpRequest(
                method = "POST",
                path = "/api/v1/notices",
                headers = notifyHeaders,
                body = """{"title":"Parking","message":"Move silver car ABC-123","durationMinutes":10}""".toByteArray()
            )
        )
        assertEquals(200, sendResp.statusCode)
        val noticeBody = PhoneControllerJson.decode<UrgentNoticeResponse>(sendResp.body.toString(StandardCharsets.UTF_8))
        assertTrue(noticeBody.active)
        assertEquals("Move silver car ABC-123", noticeBody.notice?.message)
        assertEquals(now + 10 * 60_000L, settingsRepo.current.activeUrgentNotice?.expiresAtEpochMillis)

        // Clear notice
        assertEquals(200, server.handleRequest(HttpRequest("DELETE", "/api/v1/notices", headers = notifyHeaders)).statusCode)
        assertNull(settingsRepo.current.activeUrgentNotice)

        // Paired device (even NOTIFY role) can rename itself via PATCH /api/v1/auth/me
        val renameResp = server.handleRequest(
            HttpRequest(
                method = "PATCH",
                path = "/api/v1/auth/me",
                headers = notifyHeaders,
                body = """{"deviceName":"Rocky Pixel 9"}""".toByteArray()
            )
        )
        assertEquals(200, renameResp.statusCode)
        val renamedMe = PhoneControllerJson.decode<AuthMeResponse>(renameResp.body.toString(StandardCharsets.UTF_8))
        assertEquals("Rocky Pixel 9", renamedMe.deviceName)
        assertTrue(settingsRepo.current.pairedDevices.any { it.deviceName == "Rocky Pixel 9" })

        // 2. Pair a MANAGE phone -> create group join code & join peer TV B
        val manageCode = server.pairingManager.generatePairingCode(com.salatsign.tv.security.RemoteRole.MANAGE)
        val managePair = server.handleRequest(
            HttpRequest("POST", "/api/v1/pair", body = """{"code":"${manageCode.code}","deviceName":"Admin"}""".toByteArray())
        )
        val manageHeaders = mapOf(
            "authorization" to "Bearer ${PhoneControllerJson.decode<PairExchangeResponse>(managePair.body.toString(StandardCharsets.UTF_8)).token}"
        )

        val joinCodeResp = server.handleRequest(HttpRequest("POST", "/api/v1/peers/code", headers = manageHeaders))
        assertEquals(200, joinCodeResp.statusCode)
        val joinCode = PhoneControllerJson.decode<GroupJoinCodeResponse>(joinCodeResp.body.toString(StandardCharsets.UTF_8)).code
        assertEquals(8, joinCode.length)

        // Review 4 H1 + Review 5 R5-2: Peer join uses PBKDF2-HMAC code proof (zero cleartext code!)
        // and ephemeral ECDH + AES-256-GCM so groupSecretHex is never sent in cleartext.
        val clientKp = com.salatsign.tv.security.PeerJoinCrypto.generateEphemeralKeyPair()
        val clientPubB64 = com.salatsign.tv.security.PeerJoinCrypto.encodePublicKey(clientKp)
        val identityKp = com.salatsign.tv.security.PeerJoinCrypto.generateEphemeralKeyPair()
        val identityPubB64 = com.salatsign.tv.security.PeerJoinCrypto.encodePublicKey(identityKp)
        val codeProofHex = com.salatsign.tv.security.PeerJoinCrypto.computeJoinCodeProof(joinCode, "tv-downstairs-1", clientPubB64)
        val peerJoinResp = server.handleRequest(
            HttpRequest(
                method = "POST",
                path = "/api/v1/peer/join",
                body = """{"codeProofHex":"$codeProofHex","deviceId":"tv-downstairs-1","deviceName":"Downstairs Left","roomGroup":"Downstairs","host":"192.168.4.40","port":8088,"clientPublicKeyBase64":"$clientPubB64","identityPublicKeyBase64":"$identityPubB64"}""".toByteArray()
            )
        )
        assertEquals(200, peerJoinResp.statusCode)
        val rawJoinJson = peerJoinResp.body.toString(StandardCharsets.UTF_8)
        assertFalse("Group secret must NOT appear in cleartext", rawJoinJson.contains(settingsRepo.current.groupSecretHex))
        val envelope = PhoneControllerJson.decode<PeerJoinResponse>(rawJoinJson)
        val decryptedBytes = com.salatsign.tv.security.PeerJoinCrypto.decryptFromServer(
            clientKp, envelope.serverPublicKeyBase64, envelope.ivBase64, envelope.ciphertextBase64, joinCode
        )
        val joined = PhoneControllerJson.decode<PeerJoinSecretPayload>(decryptedBytes.toString(StandardCharsets.UTF_8))
        assertTrue(joined.groupSecretHex.length == 64)

        val peersResp = server.handleRequest(HttpRequest("GET", "/api/v1/peers", headers = manageHeaders))
        assertEquals(200, peersResp.statusCode)
        val peerList = PhoneControllerJson.decode<PeerListResponse>(peersResp.body.toString(StandardCharsets.UTF_8))
        assertTrue(peerList.groupLinked)
        assertEquals(1, peerList.peers.size)

        // 3. Unsigned peer sync is rejected (401)
        val unsignedSync = server.handleRequest(HttpRequest("GET", "/api/v1/peer/sync"))
        assertEquals(401, unsignedSync.statusCode)

        // 4. Valid HMAC-signed peer sync (bound to target deviceId) adopts newer config, strips unsafe imageFile (H3),
        // preserves local TV identity + ThemeMode (D24), and returns an authenticated response signature (H2)!
        val selfDeviceId = settingsRepo.ensureDeviceId()
        val syncPayload = PeerSyncPayload(
            originDeviceId = "tv-downstairs-1",
            configVersion = 99L,
            settings = SettingsPatchRequest(
                deviceName = "SHOULD_NOT_OVERWRITE",
                roomGroup = "SHOULD_NOT_OVERWRITE",
                themeMode = "ALWAYS_DARK",
                mosqueName = "Masjid Al-Falah"
            ),
            slides = listOf(
                SlideDto(
                    id = "slide-evil",
                    type = "IMAGE",
                    title = "Traversal",
                    body = "",
                    imageFile = "../../evil.pb",
                    durationSeconds = 15,
                    enabled = true
                )
            )
        )
        val syncBytes = PhoneControllerJson.encode(syncPayload).toByteArray(StandardCharsets.UTF_8)
        val nonce = com.salatsign.tv.security.PeerHmacAuth.generateNonce()
        val sig = com.salatsign.tv.security.PeerHmacAuth.computeSignature(
            joined.groupSecretHex, "POST", "/api/v1/peer/sync", "tv-downstairs-1", selfDeviceId, now, nonce, syncBytes
        )
        val signedSyncResp = server.handleRequest(
            HttpRequest(
                method = "POST",
                path = "/api/v1/peer/sync",
                headers = mapOf(
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_PEER_ID to "tv-downstairs-1",
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_TARGET_ID to selfDeviceId,
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_TIMESTAMP to now.toString(),
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_NONCE to nonce,
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_SIGNATURE to sig
                ),
                body = syncBytes
            )
        )
        assertEquals(200, signedSyncResp.statusCode)
        val syncAck = PhoneControllerJson.decode<PeerSyncAckResponse>(signedSyncResp.body.toString(StandardCharsets.UTF_8))
        assertTrue(syncAck.adopted)
        assertEquals(99L, syncAck.configVersion)
        assertTrue(syncAck.missingAssets.isEmpty())

        // Review 5 R5-3: Older incoming version returns adopted=false without downgrading configVersion
        val stalePayload = syncPayload.copy(configVersion = 50L)
        val staleBytes = PhoneControllerJson.encode(stalePayload).toByteArray(StandardCharsets.UTF_8)
        val staleNonce = com.salatsign.tv.security.PeerHmacAuth.generateNonce()
        val staleSig = com.salatsign.tv.security.PeerHmacAuth.computeSignature(
            joined.groupSecretHex, "POST", "/api/v1/peer/sync", "tv-downstairs-1", selfDeviceId, now, staleNonce, staleBytes
        )
        val staleResp = server.handleRequest(
            HttpRequest(
                method = "POST",
                path = "/api/v1/peer/sync",
                headers = mapOf(
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_PEER_ID to "tv-downstairs-1",
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_TARGET_ID to selfDeviceId,
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_TIMESTAMP to now.toString(),
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_NONCE to staleNonce,
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_SIGNATURE to staleSig
                ),
                body = staleBytes
            )
        )
        assertEquals(200, staleResp.statusCode)
        val staleAck = PhoneControllerJson.decode<PeerSyncAckResponse>(staleResp.body.toString(StandardCharsets.UTF_8))
        assertFalse(staleAck.adopted)
        assertEquals(99L, staleAck.configVersion)

        // Review 5 R5-6: Signed /peer/ping returns signed response
        val pingNonce = com.salatsign.tv.security.PeerHmacAuth.generateNonce()
        val pingSig = com.salatsign.tv.security.PeerHmacAuth.computeSignature(
            joined.groupSecretHex, "GET", "/api/v1/peer/ping", "tv-downstairs-1", selfDeviceId, now, pingNonce, ByteArray(0)
        )
        val pingResp = server.handleRequest(
            HttpRequest(
                method = "GET",
                path = "/api/v1/peer/ping",
                headers = mapOf(
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_PEER_ID to "tv-downstairs-1",
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_TARGET_ID to selfDeviceId,
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_TIMESTAMP to now.toString(),
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_NONCE to pingNonce,
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_SIGNATURE to pingSig
                )
            )
        )
        assertEquals(200, pingResp.statusCode)
        assertTrue(
            com.salatsign.tv.security.PeerHmacAuth.verifyResponse(
                joined.groupSecretHex, selfDeviceId, pingNonce,
                pingResp.extraHeaders[com.salatsign.tv.security.PeerHmacAuth.HEADER_RESP_TIMESTAMP],
                pingResp.extraHeaders[com.salatsign.tv.security.PeerHmacAuth.HEADER_RESP_SIGNATURE],
                pingResp.body, now
            )
        )

        // Review 5 R5-1: ECIES-encrypted /peer/rotate updates local groupSecretHex cleanly
        val (selfEcPub, _) = settingsRepo.ensureDeviceEcKeyPair()
        val rotatedSecret = com.salatsign.tv.security.PeerHmacAuth.generateGroupSecretHex()
        val rotPayloadBytes = PhoneControllerJson.encode(
            PeerSecretRotatePayload(newGroupSecretHex = rotatedSecret, removedDeviceId = "tv-retired")
        ).toByteArray(StandardCharsets.UTF_8)
        val rotEnv = com.salatsign.tv.security.PeerJoinCrypto.encryptForRecipientPublicKey(selfEcPub, selfDeviceId, rotPayloadBytes)
        val rotReqBytes = PhoneControllerJson.encode(
            PeerSecretRotateRequest(rotEnv.serverPublicKeyBase64, rotEnv.ivBase64, rotEnv.ciphertextBase64)
        ).toByteArray(StandardCharsets.UTF_8)
        assertFalse("Rotated secret must NOT appear in cleartext", rotReqBytes.toString(StandardCharsets.UTF_8).contains(rotatedSecret))
        val rotNonce = com.salatsign.tv.security.PeerHmacAuth.generateNonce()
        val rotSig = com.salatsign.tv.security.PeerHmacAuth.computeSignature(
            joined.groupSecretHex, "POST", "/api/v1/peer/rotate", "tv-downstairs-1", selfDeviceId, now, rotNonce, rotReqBytes
        )
        val rotResp = server.handleRequest(
            HttpRequest(
                method = "POST",
                path = "/api/v1/peer/rotate",
                headers = mapOf(
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_PEER_ID to "tv-downstairs-1",
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_TARGET_ID to selfDeviceId,
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_TIMESTAMP to now.toString(),
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_NONCE to rotNonce,
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_SIGNATURE to rotSig
                ),
                body = rotReqBytes
            )
        )
        assertEquals(200, rotResp.statusCode)
        assertEquals(rotatedSecret, settingsRepo.current.groupSecretHex)

        // Review 5 R5-5: Signed /peer/leave removes leaving peer from peerTvs
        val leaveNonce = com.salatsign.tv.security.PeerHmacAuth.generateNonce()
        val leaveSig = com.salatsign.tv.security.PeerHmacAuth.computeSignature(
            rotatedSecret, "POST", "/api/v1/peer/leave", "tv-downstairs-1", selfDeviceId, now, leaveNonce, "{}".toByteArray()
        )
        val peerLeaveResp = server.handleRequest(
            HttpRequest(
                method = "POST",
                path = "/api/v1/peer/leave",
                headers = mapOf(
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_PEER_ID to "tv-downstairs-1",
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_TARGET_ID to selfDeviceId,
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_TIMESTAMP to now.toString(),
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_NONCE to leaveNonce,
                    com.salatsign.tv.security.PeerHmacAuth.HEADER_SIGNATURE to leaveSig
                ),
                body = "{}".toByteArray()
            )
        )
        assertEquals(200, peerLeaveResp.statusCode)
        assertTrue(settingsRepo.current.peerTvs.isEmpty())

        // Review 4 H2: Response must carry a valid HMAC signature bound to request nonce + responder deviceId
        assertTrue(
            com.salatsign.tv.security.PeerHmacAuth.verifyResponse(
                groupSecretHex = joined.groupSecretHex,
                expectedResponderDeviceId = selfDeviceId,
                requestNonce = nonce,
                responseTimestampHeader = signedSyncResp.extraHeaders[com.salatsign.tv.security.PeerHmacAuth.HEADER_RESP_TIMESTAMP],
                responseSignatureHeader = signedSyncResp.extraHeaders[com.salatsign.tv.security.PeerHmacAuth.HEADER_RESP_SIGNATURE],
                responseBody = signedSyncResp.body,
                nowEpochMillis = now
            )
        )
        assertEquals(99L, settingsRepo.current.configVersion)
        assertEquals("Masjid Al-Falah", settingsRepo.current.mosqueName)
        // Local-only fields MUST remain untouched (D24)
        assertEquals("Upstairs Left", settingsRepo.current.deviceName)
        assertEquals("Upstairs", settingsRepo.current.roomGroup)
        assertEquals(ThemeMode.ALWAYS_LIGHT, settingsRepo.current.themeMode)

        // 5. Review 4 M3: Remove TV & Rotate Secret + Leave Group
        val removeResp = server.handleRequest(
            HttpRequest(
                method = "POST",
                path = "/api/v1/peers/remove",
                headers = manageHeaders,
                body = """{"deviceId":"tv-downstairs-1"}""".toByteArray()
            )
        )
        assertEquals(200, removeResp.statusCode)
        assertTrue(settingsRepo.current.peerTvs.isEmpty())
        assertEquals("", settingsRepo.current.groupSecretHex)
    }

    @Test
    fun review4_staticAssetsServedAndPutConfigRejectsTraversalImageFile() = runBlocking {
        val (server, _, _) = createServer()
        val authHeaders = authenticate(server)

        // Review 4 L1: /app.js and /app.css served cleanly for strict CSP
        val jsResp = server.handleRequest(HttpRequest("GET", "/app.js"))
        assertEquals(200, jsResp.statusCode)
        assertTrue(jsResp.contentType.contains("javascript"))
        val cssResp = server.handleRequest(HttpRequest("GET", "/app.css"))
        assertEquals(200, cssResp.statusCode)
        assertTrue(cssResp.contentType.contains("text/css"))

        // Review 4 H3: PUT /api/v1/config rejects unsafe imageFile path traversal
        val badConfigResp = server.handleRequest(
            HttpRequest(
                method = "PUT",
                path = "/api/v1/config",
                headers = authHeaders,
                body = """{"slides":[{"id":"s1","type":"IMAGE","title":"Bad","body":"","imageFile":"../pin.dat","durationSeconds":15,"enabled":true}]}""".toByteArray()
            )
        )
        assertEquals(400, badConfigResp.statusCode)
    }
}
