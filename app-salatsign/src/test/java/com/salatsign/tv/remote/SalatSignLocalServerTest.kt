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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.ServerSocket
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
        override suspend fun setShowArabicPrayerNames(enabled: Boolean) {}
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
        settingsRepo: FakeSettingsRepository = FakeSettingsRepository(),
        contentRepo: FakeContentRepository? = null,
        port: Int = 0,
        nowProvider: () -> Long = { 1_700_000_000_000L }
    ): Triple<SalatSignLocalServer, FakeSettingsRepository, FakeContentRepository> {
        val dir = createTempDirectory("salatsign-remote-test").toFile()
        val slidesRepo = contentRepo ?: FakeContentRepository(dir)
        val server = SalatSignLocalServer(
            settingsRepository = settingsRepo,
            contentRepository = slidesRepo,
            slideImageImporter = ImageImporter(File(dir, "images")),
            logoImageImporter = ImageImporter(File(dir, "logo")),
            port = port,
            appVersionName = "1.0-test",
            nowMillisProvider = nowProvider
        )
        return Triple(server, settingsRepo, slidesRepo)
    }

    @Test
    fun publicStatusAndWebControllerHtml_areAccessibleWithoutAuth() = runBlocking {
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
    }

    @Test
    fun protectedEndpoints_rejectUnauthenticatedRequests() = runBlocking {
        val (server, _, _) = createServer()
        val resp = server.handleRequest(HttpRequest("GET", "/api/v1/settings"))
        assertEquals(401, resp.statusCode)
    }

    @Test
    fun auth_verifiesPinAndEnforcesBruteForceLockout() = runBlocking {
        var now = 1_700_000_000_000L
        val (server, settingsRepo, _) = createServer(nowProvider = { now })

        // 5 wrong attempts trigger 30s lockout
        repeat(5) {
            val bad = server.handleRequest(
                HttpRequest("POST", "/api/v1/auth", body = """{"pin":"9999"}""".toByteArray())
            )
            assertEquals(401, bad.statusCode)
        }
        assertEquals(5, settingsRepo.current.pinFailedAttempts)

        // 6th attempt while locked out returns 429 even with correct default PIN
        val locked = server.handleRequest(
            HttpRequest("POST", "/api/v1/auth", body = """{"pin":"0000"}""".toByteArray())
        )
        assertEquals(429, locked.statusCode)

        // After 31 seconds, correct PIN succeeds and resets failed attempts
        now += 31_000L
        val ok = server.handleRequest(
            HttpRequest("POST", "/api/v1/auth", body = """{"pin":"0000"}""".toByteArray())
        )
        assertEquals(200, ok.statusCode)
        assertEquals(0, settingsRepo.current.pinFailedAttempts)
        val token = SimpleJson.parseObject(ok.body.toString(StandardCharsets.UTF_8))["token"] as? String
        assertNotNull(token)
    }

    @Test
    fun authenticatedSettingsPatchAndSlideCrud_persistChanges() = runBlocking {
        val (server, settingsRepo, contentRepo) = createServer()
        val authResp = server.handleRequest(
            HttpRequest("POST", "/api/v1/auth", body = """{"pin":"0000"}""".toByteArray())
        )
        val token = SimpleJson.parseObject(authResp.body.toString(StandardCharsets.UTF_8))["token"] as String
        val authHeaders = mapOf("authorization" to "Bearer $token")

        val patchResp = server.handleRequest(
            HttpRequest(
                method = "PATCH",
                path = "/api/v1/settings",
                headers = authHeaders,
                body = """{"mosqueName":"Masjid Al-Noor","mosqueSubtitle":"Canberra","scheduleDurationSeconds":45,"hijriOffsetDays":1,"is24HourFormat":true,"jumuahMode":"FIXED","jumuahTimes":["13:00","13:45"],"iqamahSubuh":15}""".toByteArray()
            )
        )
        assertEquals(200, patchResp.statusCode)
        assertEquals("Masjid Al-Noor", settingsRepo.current.mosqueName)
        assertEquals("Canberra", settingsRepo.current.mosqueSubtitle)
        assertEquals(45, settingsRepo.current.scheduleDurationSeconds)
        assertEquals(1, settingsRepo.current.hijriOffsetDays)
        assertTrue(settingsRepo.current.is24HourFormat)
        assertEquals(listOf("13:00", "13:45"), settingsRepo.current.jumuahTimes)
        assertEquals(15, settingsRepo.current.iqamahSubuh)

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

        // Unauthenticated start/stop must be rejected
        assertEquals(401, server.handleRequest(HttpRequest("POST", "/api/v1/silent/start")).statusCode)
        assertEquals(401, server.handleRequest(HttpRequest("POST", "/api/v1/silent/stop")).statusCode)

        val authResp = server.handleRequest(
            HttpRequest("POST", "/api/v1/auth", body = """{"pin":"0000"}""".toByteArray())
        )
        val token = SimpleJson.parseObject(authResp.body.toString(StandardCharsets.UTF_8))["token"] as String
        val authHeaders = mapOf("authorization" to "Bearer $token")

        // Start manual silent screen for 30 minutes
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
        val startJson = SimpleJson.parseObject(startResp.body.toString(StandardCharsets.UTF_8))
        assertEquals(true, startJson["manualSilentActive"])
        assertEquals(expectedUntil, startJson["manualSilentUntilEpochMillis"])

        // GET /api/v1/settings reports manual silent screen active
        val getResp = server.handleRequest(HttpRequest("GET", "/api/v1/settings", headers = authHeaders))
        val getJson = SimpleJson.parseObject(getResp.body.toString(StandardCharsets.UTF_8))
        assertEquals(true, getJson["manualSilentActive"])
        assertEquals(30L, getJson["manualSilentRemainingMinutes"])

        // Stop manual silent screen
        val stopResp = server.handleRequest(HttpRequest("POST", "/api/v1/silent/stop", headers = authHeaders))
        assertEquals(200, stopResp.statusCode)
        assertEquals(0L, settingsRepo.current.silentScreen.manualSilentUntilEpochMillis)
        val stopJson = SimpleJson.parseObject(stopResp.body.toString(StandardCharsets.UTF_8))
        assertEquals(false, stopJson["manualSilentActive"])
    }

    @Test
    fun slideBudget_refusesSeventhEnabledSlide_allowsDisabledAndReEnableAfterDisable() = runBlocking {
        var now = 1_700_000_000_000L
        val (server, _, contentRepo) = createServer(nowProvider = { now++ })
        val authResp = server.handleRequest(HttpRequest("POST", "/api/v1/auth", body = """{"pin":"0000"}""".toByteArray()))
        val token = SimpleJson.parseObject(authResp.body.toString(StandardCharsets.UTF_8))["token"] as String
        val authHeaders = mapOf("authorization" to "Bearer $token")

        // Add 6 enabled slides -> all succeed
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
        val seventhEnabled = server.handleRequest(
            HttpRequest("POST", "/api/v1/slides/text", headers = authHeaders, body = """{"title":"S7","body":"B7","enabled":true}""".toByteArray())
        )
        assertEquals(409, seventhEnabled.statusCode)

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
    fun tcpLoopbackRoundTrip_servesStatusOverRealSocket() {
        val freePort = ServerSocket(0).use { it.localPort }
        val (server, _, _) = createServer(port = freePort)
        assertTrue(server.start())
        try {
            val conn = URL("http://127.0.0.1:$freePort/api/v1/status").openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            assertEquals(200, conn.responseCode)
            val body = conn.inputStream.bufferedReader().readText()
            val json = SimpleJson.parseObject(body)
            assertEquals("SalatSign", json["app"])
            assertEquals(freePort.toLong(), json["port"])
        } finally {
            server.stop()
        }
    }
}
