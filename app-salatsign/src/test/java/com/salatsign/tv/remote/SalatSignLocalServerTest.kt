// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.remote

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
        val token = SimpleJson.parseObject(authResp.body.toString(StandardCharsets.UTF_8))["token"] as String
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
        val json = SimpleJson.parseObject(statusResp.body.toString(StandardCharsets.UTF_8))
        assertEquals("SalatSign", json["app"])
        assertEquals(1L, json["apiVersion"])
        assertEquals(true, json["pinConfigured"])
        assertNull(json["mosqueName"])
        assertNull(json["cityName"])
    }

    @Test
    fun auth_refusesRemoteLoginWhileDefaultPinIsActive() = runBlocking {
        val (server, _, _) = createServer(customPin = null)
        val resp = server.handleRequest(
            HttpRequest("POST", "/api/v1/auth", body = """{"pin":"0000"}""".toByteArray())
        )
        assertEquals(403, resp.statusCode)
        val json = SimpleJson.parseObject(resp.body.toString(StandardCharsets.UTF_8))
        assertEquals("default_pin_blocked", json["error"])
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
        val token = SimpleJson.parseObject(ok.body.toString(StandardCharsets.UTF_8))["token"] as? String
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
        val slideId = SimpleJson.parseObject(addResp.body.toString(StandardCharsets.UTF_8))["id"] as String
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
                firstSlideId = SimpleJson.parseObject(r.body.toString(StandardCharsets.UTF_8))["id"] as String
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
        val seventhId = SimpleJson.parseObject(seventhDisabled.body.toString(StandardCharsets.UTF_8))["id"] as String
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
            val json = SimpleJson.parseObject(body)
            assertEquals("SalatSign", json["app"])
            assertEquals(freePort.toLong(), json["port"])
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
}
