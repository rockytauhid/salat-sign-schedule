// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.remote

import android.util.Log
import com.salatsign.tv.data.api.ApiConstants
import com.salatsign.tv.data.importing.ImageImporter
import com.salatsign.tv.data.importing.ImageTargetType
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerSilentConfig
import com.salatsign.tv.data.model.SilentAnchor
import com.salatsign.tv.data.model.SlideTextAlign
import com.salatsign.tv.data.model.SlideType
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.data.repository.IContentRepository
import com.salatsign.tv.data.repository.ISettingsRepository
import com.salatsign.tv.engine.SlideBudget
import com.salatsign.tv.security.PinHasher
import com.salatsign.tv.security.PinVerifyResult
import com.salatsign.tv.ui.mosque.BuiltInSlideRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class HttpRequest(
    val method: String,
    val path: String,
    val queryParams: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray = ByteArray(0),
    val remoteAddress: String = "127.0.0.1"
)

data class HttpResponse(
    val statusCode: Int,
    val statusText: String,
    val contentType: String = "application/json; charset=utf-8",
    val body: ByteArray = ByteArray(0)
)

private sealed interface ReadRequestOutcome {
    data class Success(val request: HttpRequest) : ReadRequestOutcome
    data class Error(val response: HttpResponse) : ReadRequestOutcome
    data object Closed : ReadRequestOutcome
}

/**
 * Offline-first local LAN HTTP server for SalatSign remote administration.
 * Uses pure Kotlin + java.net.ServerSocket (zero third-party HTTP dependencies, API 26+).
 *
 * Security hardening:
 * - Serialized auth path (Mutex) + per-IP rate limiting + persisted PBKDF2 lockout.
 * - Remote login refused while the TV is still on the default 0000 PIN.
 * - Bounded concurrent connections, request lines, header counts, and pre-auth body size (2 KB).
 * - Host header validation against local IP / localhost / .local (mitigates DNS rebinding).
 * - Strict CSP / X-Frame-Options / X-Content-Type-Options headers on all responses.
 */
class SalatSignLocalServer(
    private val settingsRepository: ISettingsRepository,
    private val contentRepository: IContentRepository,
    private val slideImageImporter: ImageImporter,
    private val logoImageImporter: ImageImporter,
    val port: Int = ApiConstants.DEFAULT_HTTP_PORT,
    private val appVersionName: String = "1.0",
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val allowedHostProvider: () -> Set<String> = {
        buildSet {
            add("127.0.0.1")
            add("localhost")
            add("[::1]")
            LocalNetworkUtils.getLocalIpv4Address()?.let { add(it) }
        }
    }
) {
    companion object {
        private const val TAG = "SalatSignLocalServer"
        private const val MAX_PRE_AUTH_BODY_BYTES = 2 * 1024           // 2 KB for /api/v1/auth
        private const val MAX_JSON_BODY_BYTES = 256 * 1024             // 256 KB for authenticated JSON
        private const val MAX_UPLOAD_BODY_BYTES = 15 * 1024 * 1024     // 15 MB for image/logo uploads
        private const val MAX_REQUEST_LINE_BYTES = 4096
        private const val MAX_HEADER_LINE_BYTES = 8192
        private const val MAX_HEADERS = 64
        private const val MAX_CONCURRENT_CONNECTIONS = 16
        private const val SESSION_TTL_MILLIS = 30 * 60 * 1000L         // 30 minutes
        private const val RATE_LIMIT_WINDOW_MILLIS = 60_000L           // 1 minute
        private const val RATE_LIMIT_MAX_AUTH_PER_IP = 15
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val secureRandom = SecureRandom()
    private val authMutex = Mutex()
    private val connectionSemaphore = Semaphore(MAX_CONCURRENT_CONNECTIONS)
    private val sessions = ConcurrentHashMap<String, Long>()
    private val authAttemptsByIp = ConcurrentHashMap<String, MutableList<Long>>()

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    @Synchronized
    fun start(): Boolean {
        if (_isRunning.value) return true
        return try {
            val socket = ServerSocket(port)
            serverSocket = socket
            _isRunning.value = true
            acceptJob = scope.launch {
                Log.d(TAG, "SalatSign Local Phone Controller started on port $port")
                while (isActive && !socket.isClosed) {
                    try {
                        val client = socket.accept()
                        if (!connectionSemaphore.tryAcquire()) {
                            runCatching {
                                writeHttpResponse(
                                    client.getOutputStream(),
                                    jsonResponse(503, "Service Unavailable", mapOf("error" to "too_many_connections", "message" to "Server busy"))
                                )
                            }
                            runCatching { client.close() }
                            continue
                        }
                        launch {
                            try {
                                handleClient(client)
                            } finally {
                                connectionSemaphore.release()
                            }
                        }
                    } catch (e: Exception) {
                        if (!socket.isClosed) {
                            Log.w(TAG, "Error accepting LAN client", e)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start local server on port $port", e)
            _isRunning.value = false
            false
        }
    }

    @Synchronized
    fun stop() {
        _isRunning.value = false
        acceptJob?.cancel()
        acceptJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        sessions.clear()
        authAttemptsByIp.clear()
        Log.d(TAG, "SalatSign Local Phone Controller stopped")
    }

    private suspend fun handleClient(client: Socket) {
        client.use { socket ->
            socket.soTimeout = 15_000
            val remoteIp = socket.inetAddress?.hostAddress ?: "unknown"
            val response = when (val outcome = readHttpRequest(socket.getInputStream(), remoteIp)) {
                is ReadRequestOutcome.Closed -> return
                is ReadRequestOutcome.Error -> outcome.response
                is ReadRequestOutcome.Success -> {
                    val req = outcome.request
                    val hostHeader = req.headers["host"]
                    if (hostHeader == null || !isValidHostHeader(hostHeader)) {
                        jsonResponse(403, "Forbidden", mapOf("error" to "invalid_host", "message" to "Host header not allowed"))
                    } else {
                        try {
                            handleRequest(req)
                        } catch (e: IllegalArgumentException) {
                            Log.w(TAG, "Bad request on ${req.method} ${req.path}", e)
                            jsonResponse(400, "Bad Request", mapOf("error" to "bad_request", "message" to "Malformed request payload"))
                        } catch (e: Exception) {
                            Log.w(TAG, "Unhandled error processing ${req.method} ${req.path}", e)
                            jsonResponse(500, "Internal Server Error", mapOf("error" to "internal_error", "message" to "Internal server error"))
                        }
                    }
                }
            }
            runCatching { writeHttpResponse(socket.getOutputStream(), response) }
        }
    }

    internal fun isValidHostHeader(rawHost: String): Boolean {
        val trimmed = rawHost.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) return false
        val hostOnly = if (trimmed.startsWith("[")) {
            val endBracket = trimmed.indexOf(']')
            if (endBracket < 0) return false
            trimmed.substring(0, endBracket + 1)
        } else {
            trimmed.substringBefore(':')
        }
        if (hostOnly.endsWith(".local")) return true
        val allowed = allowedHostProvider().map { it.lowercase(Locale.ROOT) }.toSet()
        return hostOnly in allowed
    }

    internal suspend fun handleRequest(req: HttpRequest): HttpResponse {
        // Validate Host header when present (also covers unit tests passing Host)
        req.headers["host"]?.let { host ->
            if (!isValidHostHeader(host)) {
                return jsonResponse(403, "Forbidden", mapOf("error" to "invalid_host", "message" to "Host header not allowed"))
            }
        }

        val method = req.method.uppercase(Locale.ROOT)
        val path = req.path

        if (method == "OPTIONS") {
            return HttpResponse(204, "No Content", "text/plain", ByteArray(0))
        }

        // Public endpoints
        if (method == "GET" && (path == "/" || path == "/index.html")) {
            return HttpResponse(
                statusCode = 200,
                statusText = "OK",
                contentType = "text/html; charset=utf-8",
                body = PhoneControllerHtml.renderHtml().toByteArray(StandardCharsets.UTF_8)
            )
        }

        if (method == "GET" && path == "/api/v1/status") {
            val settings = settingsRepository.appSettingsFlow.first()
            return jsonResponse(
                200, "OK",
                mapOf(
                    "app" to "SalatSign",
                    "apiVersion" to ApiConstants.API_VERSION,
                    "version" to appVersionName,
                    "port" to port,
                    "pinConfigured" to settings.hasCustomPin
                )
            )
        }

        if (method == "POST" && path == "/api/v1/auth") {
            if (req.body.size > MAX_PRE_AUTH_BODY_BYTES) {
                return jsonResponse(413, "Payload Too Large", mapOf("error" to "payload_too_large", "message" to "Request body exceeds limit"))
            }
            return try {
                handleAuth(req)
            } catch (_: IllegalArgumentException) {
                jsonResponse(400, "Bad Request", mapOf("error" to "invalid_json", "message" to "Malformed JSON payload"))
            }
        }

        // All remaining /api/v1/* endpoints require valid bearer token
        val bearerToken = extractValidBearerToken(req.headers)
            ?: return jsonResponse(401, "Unauthorized", mapOf("error" to "unauthorized", "message" to "Valid admin session required"))

        return try {
            when {
                method == "POST" && path == "/api/v1/logout" -> {
                    sessions.remove(bearerToken)
                    jsonResponse(200, "OK", mapOf("loggedOut" to true))
                }
                method == "GET" && path == "/api/v1/settings" -> handleGetSettings()
                method == "PATCH" && path == "/api/v1/settings" -> handlePatchSettings(req)
                method == "POST" && path == "/api/v1/silent/start" -> handleStartManualSilent(req)
                method == "POST" && path == "/api/v1/silent/stop" -> handleStopManualSilent()
                method == "GET" && path == "/api/v1/slides" -> handleGetSlides()
                method == "POST" && path == "/api/v1/slides/text" -> handleAddTextSlide(req)
                method == "POST" && path == "/api/v1/slides/image" -> handleAddImageSlide(req)
                method == "PATCH" && path.startsWith("/api/v1/slides/") -> {
                    val id = URLDecoder.decode(path.removePrefix("/api/v1/slides/"), "UTF-8")
                    handlePatchSlide(id, req)
                }
                method == "DELETE" && path.startsWith("/api/v1/slides/") -> {
                    val id = URLDecoder.decode(path.removePrefix("/api/v1/slides/"), "UTF-8")
                    handleDeleteSlide(id)
                }
                method == "POST" && path == "/api/v1/logo" -> handleUploadLogo(req)
                else -> jsonResponse(404, "Not Found", mapOf("error" to "not_found", "message" to "Unknown endpoint"))
            }
        } catch (_: IllegalArgumentException) {
            jsonResponse(400, "Bad Request", mapOf("error" to "invalid_json", "message" to "Malformed JSON payload"))
        }
    }

    private suspend fun handleAuth(req: HttpRequest): HttpResponse = authMutex.withLock {
        val now = nowMillisProvider()
        pruneExpiredSessions(now)

        // Per-IP throttling
        val attempts = authAttemptsByIp.computeIfAbsent(req.remoteAddress) { mutableListOf() }
        attempts.removeAll { now - it > RATE_LIMIT_WINDOW_MILLIS }
        if (attempts.size >= RATE_LIMIT_MAX_AUTH_PER_IP) {
            return jsonResponse(
                429, "Too Many Requests",
                mapOf("error" to "rate_limited", "message" to "Too many login attempts from this device. Try again in a minute.")
            )
        }
        attempts.add(now)

        val settings = settingsRepository.appSettingsFlow.first()
        // Review 2.1: Refuse remote LAN login while the TV still has the default 0000 PIN
        if (!settings.hasCustomPin) {
            return jsonResponse(
                403, "Forbidden",
                mapOf(
                    "error" to "default_pin_blocked",
                    "message" to "Remote login is disabled while using the default PIN. Set a custom Admin PIN on the TV first (Settings → Remote, PIN & About)."
                )
            )
        }

        val bodyMap = SimpleJson.parseObject(req.body.toString(StandardCharsets.UTF_8))
        val pin = bodyMap["pin"]?.toString() ?: ""

        val result = PinHasher.verifyPin(
            enteredPin = pin,
            hasCustomPin = settings.hasCustomPin,
            saltHex = settings.pinSaltHex,
            hashHex = settings.pinHashHex,
            nowEpochMillis = now,
            currentFailedAttempts = settings.pinFailedAttempts,
            lockedUntilEpochMillis = settings.pinLockedUntilEpochMillis
        )

        return when (result) {
            is PinVerifyResult.Success -> {
                settingsRepository.recordPinSuccess()
                val tokenBytes = ByteArray(24)
                secureRandom.nextBytes(tokenBytes)
                val token = PinHasher.bytesToHex(tokenBytes)
                sessions[token] = now + SESSION_TTL_MILLIS
                jsonResponse(200, "OK", mapOf("token" to token, "expiresInSeconds" to (SESSION_TTL_MILLIS / 1000L)))
            }
            is PinVerifyResult.Incorrect -> {
                val lockout = PinHasher.calculateLockout(settings.pinFailedAttempts, now)
                settingsRepository.recordPinFailure(lockout.newFailedAttempts, lockout.lockedUntilEpochMillis)
                jsonResponse(
                    401, "Unauthorized",
                    mapOf(
                        "error" to "invalid_pin",
                        "attemptsLeft" to result.attemptsLeftBeforeLockout,
                        "message" to "Incorrect PIN (${result.attemptsLeftBeforeLockout} attempts remaining)"
                    )
                )
            }
            is PinVerifyResult.LockedOut -> {
                jsonResponse(
                    429, "Too Many Requests",
                    mapOf(
                        "error" to "locked_out",
                        "remainingSeconds" to result.remainingSeconds,
                        "message" to "PIN locked. Try again in ${result.remainingSeconds} seconds."
                    )
                )
            }
            is PinVerifyResult.InvalidFormat -> {
                jsonResponse(400, "Bad Request", mapOf("error" to "invalid_format", "message" to "PIN must be 4–8 digits"))
            }
            is PinVerifyResult.ResetRequired -> {
                jsonResponse(423, "Locked", mapOf("error" to "reset_required", "message" to "PIN storage corrupted. Clear app data on TV."))
            }
        }
    }

    private fun pruneExpiredSessions(now: Long) {
        val iterator = sessions.entries.iterator()
        while (iterator.hasNext()) {
            if (now > iterator.next().value) {
                iterator.remove()
            }
        }
    }

    private fun extractValidBearerToken(headers: Map<String, String>): String? {
        val auth = headers["authorization"] ?: return null
        if (!auth.startsWith("Bearer ", ignoreCase = true)) return null
        val token = auth.substring(7).trim()
        val expiry = sessions[token] ?: return null
        if (nowMillisProvider() > expiry) {
            sessions.remove(token)
            return null
        }
        return token
    }

    private suspend fun handleGetSettings(): HttpResponse {
        val s = settingsRepository.appSettingsFlow.first()
        val now = nowMillisProvider()
        val manualUntil = s.silentScreen.manualSilentUntilEpochMillis
        val manualActive = manualUntil > now
        val dailySilentMap = listOf(Prayer.SUBUH, Prayer.ZUHUR, Prayer.ASHR, Prayer.MAGRIB, Prayer.ISYA).associate { p ->
            val cfg = s.silentScreen.dailyPrayers[p] ?: PrayerSilentConfig()
            p.name to mapOf(
                "enabled" to cfg.enabled,
                "anchor" to cfg.anchor.name,
                "leadMinutes" to cfg.leadMinutes,
                "durationMinutes" to cfg.durationMinutes
            )
        }
        return jsonResponse(
            200, "OK",
            mapOf(
                "mosqueName" to s.mosqueName,
                "mosqueSubtitle" to s.mosqueSubtitle,
                "mosqueLogoUri" to s.mosqueLogoUri,
                "mosqueIconPreset" to s.mosqueIconPreset.name,
                "mosqueFontPreset" to s.mosqueFontPreset.name,
                "appLanguage" to s.appLanguage.name,
                "cityName" to s.cityName,
                "zoneId" to s.zoneId,
                "calculationMethodName" to s.calculationMethodName,
                "madhab" to s.madhab,
                "themeMode" to s.themeMode.name,
                "scheduleDurationSeconds" to s.scheduleDurationSeconds,
                "hijriOffsetDays" to s.hijriOffsetDays,
                "hijriChangeAtMidnight" to s.hijriChangeAtMidnight,
                "is24HourFormat" to s.is24HourFormat,
                "showArabicPrayerNames" to s.showArabicPrayerNames,
                "showStrikeHighlight" to s.showStrikeHighlight,
                "jumuahMode" to s.jumuahMode.name,
                "jumuahTimes" to s.jumuahTimes,
                "iqamahSubuh" to s.iqamahSubuh,
                "iqamahZuhur" to s.iqamahZuhur,
                "iqamahAshr" to s.iqamahAshr,
                "iqamahMagrib" to s.iqamahMagrib,
                "iqamahIsya" to s.iqamahIsya,
                "islamicCalendarSlideEnabled" to s.islamicCalendarSlideEnabled,
                "islamicCalendarSlideDurationSeconds" to s.islamicCalendarSlideDurationSeconds,
                "quranVerseEnabled" to s.quranVerseEnabled,
                "quranShowTranslation" to s.quranShowTranslation,
                "quranVerseDurationSeconds" to s.quranVerseDurationSeconds,
                "silentJumuahEnabled" to s.silentScreen.jumuahEnabled,
                "silentJumuahLeadMinutes" to s.silentScreen.jumuahLeadMinutes,
                "silentJumuahDurationMinutes" to s.silentScreen.jumuahDurationMinutes,
                "silentDailyPrayers" to dailySilentMap,
                "manualSilentUntilEpochMillis" to if (manualActive) manualUntil else 0L,
                "manualSilentActive" to manualActive,
                "manualSilentRemainingMinutes" to if (manualActive) (((manualUntil - now) + 59_999L) / 60_000L) else 0L
            )
        )
    }

    private suspend fun handleStartManualSilent(req: HttpRequest): HttpResponse {
        val body = SimpleJson.parseObject(req.body.toString(StandardCharsets.UTF_8))
        val durationMinutes = ((body["durationMinutes"] as? Number)?.toInt() ?: 15).coerceIn(1, 120)
        val until = nowMillisProvider() + durationMinutes * 60_000L
        settingsRepository.setManualSilentUntil(until)
        return jsonResponse(
            200, "OK",
            mapOf(
                "manualSilentActive" to true,
                "manualSilentUntilEpochMillis" to until,
                "durationMinutes" to durationMinutes,
                "manualSilentRemainingMinutes" to durationMinutes
            )
        )
    }

    private suspend fun handleStopManualSilent(): HttpResponse {
        settingsRepository.setManualSilentUntil(0L)
        return jsonResponse(
            200, "OK",
            mapOf(
                "manualSilentActive" to false,
                "manualSilentUntilEpochMillis" to 0L,
                "manualSilentRemainingMinutes" to 0
            )
        )
    }

    private suspend fun handlePatchSettings(req: HttpRequest): HttpResponse {
        val patch = SimpleJson.parseObject(req.body.toString(StandardCharsets.UTF_8))
        val current = settingsRepository.appSettingsFlow.first()

        // Enforce 6-slide rotation budget if enabling built-in slides via PATCH
        val wantCalEnabled = patch["islamicCalendarSlideEnabled"] as? Boolean
        val wantQuranEnabled = patch["quranVerseEnabled"] as? Boolean
        if (wantCalEnabled == true || wantQuranEnabled == true) {
            val enabledAnnouncements = contentRepository.slidesFlow.value.slides.count { it.enabled }
            val nextCal = wantCalEnabled ?: current.islamicCalendarSlideEnabled
            val nextQuran = wantQuranEnabled ?: current.quranVerseEnabled
            val nextBuiltIns = (if (nextCal) 1 else 0) + (if (nextQuran) 1 else 0)
            val currentBuiltIns = (if (current.islamicCalendarSlideEnabled) 1 else 0) + (if (current.quranVerseEnabled) 1 else 0)
            if (nextBuiltIns > currentBuiltIns &&
                SlideBudget.usedSlots(enabledAnnouncements, nextBuiltIns) > SlideBudget.MAX_ROTATION_SLIDES
            ) {
                return jsonResponse(
                    409, "Conflict",
                    mapOf(
                        "error" to "slide_limit_reached",
                        "message" to "The slideshow is limited to 6 slides, including built-in slides. Disable or remove a slide first."
                    )
                )
            }
        }

        val newName = (patch["mosqueName"] as? String)?.trim() ?: current.mosqueName
        val newSubtitle = (patch["mosqueSubtitle"] as? String) ?: current.mosqueSubtitle
        if (patch.containsKey("mosqueName") && newName.isBlank()) {
            return jsonResponse(
                400, "Bad Request",
                mapOf("error" to "mosque_name_required", "message" to "Mosque name cannot be empty.")
            )
        }
        if (patch.containsKey("mosqueName") || patch.containsKey("mosqueSubtitle")) {
            settingsRepository.updateMosqueBranding(newName, newSubtitle, current.mosqueLogoUri)
        }
        (patch["appLanguage"] as? String)?.let { raw ->
            runCatching { com.salatsign.tv.data.model.AppLanguage.valueOf(raw.uppercase(Locale.ROOT)) }
                .getOrNull()
                ?.takeIf { it in com.salatsign.tv.data.model.AppLanguage.ShippedLanguages }
                ?.let { settingsRepository.setAppLanguage(it) }
        }
        (patch["mosqueIconPreset"] as? String)?.let { raw ->
            runCatching { MosqueIconPreset.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()?.let {
                settingsRepository.setMosqueIconPreset(it)
            }
        }
        (patch["mosqueFontPreset"] as? String)?.let { raw ->
            runCatching { MosqueFontPreset.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()?.let {
                settingsRepository.setMosqueFontPreset(it)
            }
        }
        (patch["themeMode"] as? String)?.let { raw ->
            runCatching { ThemeMode.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()?.let {
                settingsRepository.setThemeMode(it)
            }
        }
        (patch["scheduleDurationSeconds"] as? Number)?.toInt()?.let {
            settingsRepository.updateScheduleDuration(it.coerceIn(10, 600))
        }
        (patch["hijriOffsetDays"] as? Number)?.toInt()?.let {
            settingsRepository.updateHijriOffset(it.coerceIn(-2, 2))
        }
        (patch["hijriChangeAtMidnight"] as? Boolean)?.let {
            settingsRepository.setHijriChangeAtMidnight(it)
        }
        (patch["is24HourFormat"] as? Boolean)?.let {
            settingsRepository.set24HourFormat(it)
        }
        (patch["showArabicPrayerNames"] as? Boolean)?.let {
            settingsRepository.setShowArabicPrayerNames(it)
        }
        (patch["showStrikeHighlight"] as? Boolean)?.let {
            settingsRepository.setShowStrikeHighlight(it)
        }
        (patch["jumuahMode"] as? String)?.let { raw ->
            runCatching { JumuahMode.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()?.let {
                settingsRepository.setJumuahMode(it)
            }
        }
        (patch["jumuahTimes"] as? List<*>)?.let { rawList ->
            val cleaned = rawList.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }.take(3)
            if (cleaned.isNotEmpty()) {
                settingsRepository.setJumuahTimes(cleaned)
            }
        }
        (patch["iqamahSubuh"] as? Number)?.toInt()?.let { settingsRepository.setIqamahOffset(Prayer.SUBUH, it.coerceIn(0, 60)) }
        (patch["iqamahZuhur"] as? Number)?.toInt()?.let { settingsRepository.setIqamahOffset(Prayer.ZUHUR, it.coerceIn(0, 60)) }
        (patch["iqamahAshr"] as? Number)?.toInt()?.let { settingsRepository.setIqamahOffset(Prayer.ASHR, it.coerceIn(0, 60)) }
        (patch["iqamahMagrib"] as? Number)?.toInt()?.let { settingsRepository.setIqamahOffset(Prayer.MAGRIB, it.coerceIn(0, 60)) }
        (patch["iqamahIsya"] as? Number)?.toInt()?.let { settingsRepository.setIqamahOffset(Prayer.ISYA, it.coerceIn(0, 60)) }

        wantCalEnabled?.let { settingsRepository.setIslamicCalendarSlideEnabled(it) }
        (patch["islamicCalendarSlideDurationSeconds"] as? Number)?.toInt()?.let {
            settingsRepository.setIslamicCalendarSlideDurationSeconds(it.coerceIn(10, 60))
        }
        wantQuranEnabled?.let { settingsRepository.setQuranVerseEnabled(it) }
        (patch["quranShowTranslation"] as? Boolean)?.let {
            settingsRepository.setQuranShowTranslation(it)
        }
        (patch["quranVerseDurationSeconds"] as? Number)?.toInt()?.let {
            settingsRepository.setQuranVerseDurationSeconds(if (it <= 0) 0 else it.coerceIn(15, 60))
        }

        (patch["silentJumuahEnabled"] as? Boolean)?.let { settingsRepository.setSilentJumuahEnabled(it) }
        (patch["silentJumuahLeadMinutes"] as? Number)?.toInt()?.let {
            settingsRepository.setSilentJumuahLeadMinutes(it.coerceIn(0, 15))
        }
        (patch["silentJumuahDurationMinutes"] as? Number)?.toInt()?.let {
            settingsRepository.setSilentJumuahDurationMinutes(it.coerceIn(30, 90))
        }
        (patch["silentDailyPrayers"] as? Map<*, *>)?.let { rawMap ->
            for ((k, v) in rawMap) {
                val prayer = runCatching { Prayer.valueOf(k.toString().uppercase(Locale.ROOT)) }.getOrNull() ?: continue
                val cfgMap = v as? Map<*, *> ?: continue
                val existing = current.silentScreen.dailyPrayers[prayer] ?: PrayerSilentConfig()
                val anchor = (cfgMap["anchor"] as? String)?.let {
                    runCatching { SilentAnchor.valueOf(it.uppercase(Locale.ROOT)) }.getOrNull()
                } ?: existing.anchor
                val updated = PrayerSilentConfig(
                    enabled = (cfgMap["enabled"] as? Boolean) ?: existing.enabled,
                    anchor = anchor,
                    leadMinutes = ((cfgMap["leadMinutes"] as? Number)?.toInt() ?: existing.leadMinutes).coerceIn(0, 10),
                    durationMinutes = ((cfgMap["durationMinutes"] as? Number)?.toInt() ?: existing.durationMinutes).coerceIn(5, 45)
                )
                settingsRepository.setSilentDailyPrayerConfig(prayer, updated)
            }
        }

        return handleGetSettings()
    }

    private suspend fun handleGetSlides(): HttpResponse {
        val container = contentRepository.slidesFlow.value
        val settings = settingsRepository.appSettingsFlow.first()
        val enabledBuiltIns = BuiltInSlideRegistry.registrationsFor(settings).count { it.enabled }
        val enabledAnnouncements = container.slides.count { it.enabled }
        val slides = container.slides.map { slide ->
            mapOf(
                "id" to slide.id,
                "type" to slide.type.name,
                "title" to slide.title,
                "body" to slide.body,
                "imageFile" to slide.imageFile,
                "durationSeconds" to slide.durationSeconds,
                "enabled" to slide.enabled,
                "textAlign" to slide.textAlign.name,
                "sortOrder" to slide.sortOrder
            )
        }
        return jsonResponse(
            200, "OK",
            mapOf(
                "revision" to container.revision,
                "usedSlots" to SlideBudget.usedSlots(enabledAnnouncements, enabledBuiltIns),
                "maxRotationSlides" to SlideBudget.MAX_ROTATION_SLIDES,
                "slides" to slides
            )
        )
    }

    private suspend fun handleAddTextSlide(req: HttpRequest): HttpResponse {
        val body = SimpleJson.parseObject(req.body.toString(StandardCharsets.UTF_8))
        val title = (body["title"] as? String)?.trim().orEmpty()
        val text = (body["body"] as? String)?.trim().orEmpty()
        if (title.isEmpty() && text.isEmpty()) {
            return jsonResponse(400, "Bad Request", mapOf("error" to "empty_slide", "message" to "Slide title or body is required"))
        }
        val enabled = (body["enabled"] as? Boolean) ?: true
        val settings = settingsRepository.appSettingsFlow.first()
        val enabledBuiltIns = BuiltInSlideRegistry.registrationsFor(settings).count { it.enabled }
        val enabledAnnouncements = contentRepository.slidesFlow.value.slides.count { it.enabled }
        if (enabled && !SlideBudget.canEnableOrAddAnnouncement(enabledAnnouncements, enabledBuiltIns)) {
            return jsonResponse(
                409, "Conflict",
                mapOf(
                    "error" to "slide_limit_reached",
                    "message" to "The slideshow is limited to 6 slides, including built-in slides. Disable or remove a slide first."
                )
            )
        }
        val duration = ((body["durationSeconds"] as? Number)?.toInt() ?: 15).coerceIn(3, 120)
        val textAlign = (body["textAlign"] as? String)?.let { raw ->
            runCatching { SlideTextAlign.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()
        } ?: SlideTextAlign.CENTER
        val nextOrder = (contentRepository.slidesFlow.value.slides.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val slide = AnnouncementSlide(
            id = "slide_${UUID.randomUUID()}",
            type = SlideType.TEXT,
            title = title,
            body = text,
            durationSeconds = duration,
            enabled = enabled,
            textAlign = textAlign,
            sortOrder = nextOrder
        )
        val result = contentRepository.addSlide(slide)
        return if (result.isSuccess) {
            jsonResponse(201, "Created", mapOf("id" to slide.id, "enabled" to slide.enabled, "slideCount" to result.getOrThrow().slides.size))
        } else {
            jsonResponse(400, "Bad Request", mapOf("error" to "add_failed", "message" to (result.exceptionOrNull()?.message ?: "Failed to add slide")))
        }
    }

    private suspend fun handleAddImageSlide(req: HttpRequest): HttpResponse {
        if (req.body.isEmpty()) {
            return jsonResponse(400, "Bad Request", mapOf("error" to "empty_image", "message" to "Image payload is empty"))
        }
        val enabled = req.queryParams["enabled"]?.equals("false", ignoreCase = true)?.not() ?: true
        val settings = settingsRepository.appSettingsFlow.first()
        val enabledBuiltIns = BuiltInSlideRegistry.registrationsFor(settings).count { it.enabled }
        val enabledAnnouncements = contentRepository.slidesFlow.value.slides.count { it.enabled }
        if (enabled && !SlideBudget.canEnableOrAddAnnouncement(enabledAnnouncements, enabledBuiltIns)) {
            return jsonResponse(
                409, "Conflict",
                mapOf(
                    "error" to "slide_limit_reached",
                    "message" to "The slideshow is limited to 6 slides, including built-in slides. Disable or remove a slide first."
                )
            )
        }
        val title = req.queryParams["title"]?.takeIf { it.isNotBlank() } ?: "Poster"
        val duration = (req.queryParams["duration"]?.toIntOrNull() ?: 15).coerceIn(3, 120)
        val imported = slideImageImporter.importImage(req.body, ImageTargetType.SLIDE)
        val processed = imported.getOrElse { err ->
            return jsonResponse(400, "Bad Request", mapOf("error" to "image_invalid", "message" to (err.message ?: "Invalid image")))
        }
        val nextOrder = (contentRepository.slidesFlow.value.slides.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val slide = AnnouncementSlide(
            id = "slide_${UUID.randomUUID()}",
            type = SlideType.IMAGE,
            title = title,
            imageFile = processed.relativeFileName,
            durationSeconds = duration,
            enabled = enabled,
            sortOrder = nextOrder
        )
        val added = contentRepository.addSlide(slide)
        return if (added.isSuccess) {
            jsonResponse(201, "Created", mapOf("id" to slide.id, "enabled" to slide.enabled, "imageFile" to processed.relativeFileName))
        } else {
            jsonResponse(400, "Bad Request", mapOf("error" to "add_failed", "message" to (added.exceptionOrNull()?.message ?: "Failed to add slide")))
        }
    }

    private suspend fun handlePatchSlide(slideId: String, req: HttpRequest): HttpResponse {
        val current = contentRepository.slidesFlow.value.slides.firstOrNull { it.id == slideId }
            ?: return jsonResponse(404, "Not Found", mapOf("error" to "not_found", "message" to "Slide not found"))
        val patch = SimpleJson.parseObject(req.body.toString(StandardCharsets.UTF_8))
        val targetEnabled = (patch["enabled"] as? Boolean) ?: current.enabled
        if (targetEnabled && !current.enabled) {
            val settings = settingsRepository.appSettingsFlow.first()
            val enabledBuiltIns = BuiltInSlideRegistry.registrationsFor(settings).count { it.enabled }
            val enabledAnnouncements = contentRepository.slidesFlow.value.slides.count { it.enabled }
            if (!SlideBudget.canEnableOrAddAnnouncement(enabledAnnouncements, enabledBuiltIns)) {
                return jsonResponse(
                    409, "Conflict",
                    mapOf(
                        "error" to "slide_limit_reached",
                        "message" to "The slideshow is limited to 6 slides, including built-in slides. Disable or remove a slide first."
                    )
                )
            }
        }
        val targetTitle = (patch["title"] as? String)?.trim() ?: current.title
        val targetBody = (patch["body"] as? String)?.trim() ?: current.body
        val targetDuration = ((patch["durationSeconds"] as? Number)?.toInt() ?: current.durationSeconds).coerceIn(3, 120)
        val targetAlign = (patch["textAlign"] as? String)?.let { raw ->
            runCatching { SlideTextAlign.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()
        } ?: current.textAlign
        val updated = current.copy(
            title = targetTitle,
            body = if (current.type == SlideType.TEXT) targetBody else current.body,
            durationSeconds = targetDuration,
            enabled = targetEnabled,
            textAlign = targetAlign
        )
        val res = contentRepository.updateSlide(updated)
        return if (res.isSuccess) {
            jsonResponse(
                200, "OK",
                mapOf(
                    "id" to updated.id,
                    "title" to updated.title,
                    "body" to updated.body,
                    "durationSeconds" to updated.durationSeconds,
                    "enabled" to updated.enabled
                )
            )
        } else {
            jsonResponse(400, "Bad Request", mapOf("error" to "update_failed", "message" to (res.exceptionOrNull()?.message ?: "Failed to update slide")))
        }
    }

    private suspend fun handleDeleteSlide(slideId: String): HttpResponse {
        if (slideId.isBlank()) {
            return jsonResponse(400, "Bad Request", mapOf("error" to "missing_id", "message" to "Slide ID is required"))
        }
        val result = contentRepository.deleteSlide(slideId)
        return if (result.isSuccess) {
            jsonResponse(200, "OK", mapOf("deleted" to slideId, "slideCount" to result.getOrThrow().slides.size))
        } else {
            jsonResponse(400, "Bad Request", mapOf("error" to "delete_failed", "message" to (result.exceptionOrNull()?.message ?: "Failed to delete slide")))
        }
    }

    private suspend fun handleUploadLogo(req: HttpRequest): HttpResponse {
        if (req.body.isEmpty()) {
            return jsonResponse(400, "Bad Request", mapOf("error" to "empty_logo", "message" to "Logo image payload is empty"))
        }
        val processed = logoImageImporter.importImage(req.body, ImageTargetType.LOGO).getOrElse { err ->
            return jsonResponse(400, "Bad Request", mapOf("error" to "logo_invalid", "message" to (err.message ?: "Invalid logo image")))
        }
        val current = settingsRepository.appSettingsFlow.first()
        settingsRepository.setMosqueIconPreset(MosqueIconPreset.CUSTOM)
        settingsRepository.updateMosqueBranding(current.mosqueName, current.mosqueSubtitle, processed.relativeFileName)
        return jsonResponse(200, "OK", mapOf("logoFile" to processed.relativeFileName))
    }

    private fun jsonResponse(code: Int, text: String, payload: Map<String, Any?>): HttpResponse {
        return HttpResponse(
            statusCode = code,
            statusText = text,
            contentType = "application/json; charset=utf-8",
            body = SimpleJson.toJson(payload).toByteArray(StandardCharsets.UTF_8)
        )
    }

    private fun readHttpRequest(input: InputStream, remoteIp: String): ReadRequestOutcome {
        val buffered = BufferedInputStream(input)
        val requestLine = readBoundedLine(buffered, MAX_REQUEST_LINE_BYTES)
            ?: return ReadRequestOutcome.Closed
        if (requestLine == LINE_TOO_LONG) {
            return ReadRequestOutcome.Error(
                jsonResponse(414, "URI Too Long", mapOf("error" to "uri_too_long", "message" to "Request line too long"))
            )
        }

        val parts = requestLine.trim().split(" ")
        if (parts.size < 2) {
            return ReadRequestOutcome.Error(
                jsonResponse(400, "Bad Request", mapOf("error" to "bad_request", "message" to "Invalid HTTP request line"))
            )
        }

        val method = parts[0]
        val rawTarget = parts[1]
        val qIdx = rawTarget.indexOf('?')
        val path = if (qIdx >= 0) rawTarget.substring(0, qIdx) else rawTarget
        val queryParams = mutableMapOf<String, String>()
        if (qIdx >= 0 && qIdx + 1 < rawTarget.length) {
            rawTarget.substring(qIdx + 1).split("&").forEach { pair ->
                val eq = pair.indexOf('=')
                if (eq > 0) {
                    runCatching {
                        val k = URLDecoder.decode(pair.substring(0, eq), "UTF-8")
                        val v = URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
                        queryParams[k] = v
                    }
                }
            }
        }

        val headers = mutableMapOf<String, String>()
        var headerCount = 0
        while (true) {
            val line = readBoundedLine(buffered, MAX_HEADER_LINE_BYTES) ?: break
            if (line == LINE_TOO_LONG) {
                return ReadRequestOutcome.Error(
                    jsonResponse(431, "Request Header Fields Too Large", mapOf("error" to "header_too_large", "message" to "Header line too large"))
                )
            }
            if (line.isEmpty()) break
            headerCount++
            if (headerCount > MAX_HEADERS) {
                return ReadRequestOutcome.Error(
                    jsonResponse(431, "Request Header Fields Too Large", mapOf("error" to "too_many_headers", "message" to "Too many headers"))
                )
            }
            val colon = line.indexOf(':')
            if (colon > 0) {
                headers[line.substring(0, colon).trim().lowercase(Locale.ROOT)] =
                    line.substring(colon + 1).trim()
            }
        }

        val contentLengthRaw = headers["content-length"]
        val contentLength = if (contentLengthRaw != null) {
            contentLengthRaw.toIntOrNull() ?: return ReadRequestOutcome.Error(
                jsonResponse(400, "Bad Request", mapOf("error" to "invalid_content_length", "message" to "Invalid Content-Length"))
            )
        } else 0

        if (contentLength < 0) {
            return ReadRequestOutcome.Error(
                jsonResponse(400, "Bad Request", mapOf("error" to "invalid_content_length", "message" to "Negative Content-Length"))
            )
        }

        val isAuthed = extractValidBearerToken(headers) != null
        val isUploadRoute = path == "/api/v1/logo" || path == "/api/v1/slides/image"
        val maxAllowedBytes = when {
            !isAuthed -> MAX_PRE_AUTH_BODY_BYTES
            isUploadRoute -> MAX_UPLOAD_BODY_BYTES
            else -> MAX_JSON_BODY_BYTES
        }
        if (contentLength > maxAllowedBytes) {
            return ReadRequestOutcome.Error(
                jsonResponse(413, "Payload Too Large", mapOf("error" to "payload_too_large", "message" to "Request body exceeds limit"))
            )
        }

        val body = if (contentLength > 0) {
            val buf = ByteArray(contentLength)
            var offset = 0
            while (offset < contentLength) {
                val read = buffered.read(buf, offset, contentLength - offset)
                if (read <= 0) break
                offset += read
            }
            if (offset == contentLength) buf else buf.copyOf(offset)
        } else {
            ByteArray(0)
        }

        return ReadRequestOutcome.Success(
            HttpRequest(
                method = method,
                path = path,
                queryParams = queryParams,
                headers = headers,
                body = body,
                remoteAddress = remoteIp
            )
        )
    }

    private val LINE_TOO_LONG = "\u0000__LINE_TOO_LONG__"

    private fun readBoundedLine(input: InputStream, maxBytes: Int): String? {
        val out = ByteArrayOutputStream()
        var prev = -1
        while (true) {
            val b = input.read()
            if (b == -1) {
                return if (out.size() == 0) null else out.toString(StandardCharsets.UTF_8.name())
            }
            if (b == '\n'.code) {
                val bytes = out.toByteArray()
                val len = if (prev == '\r'.code && bytes.isNotEmpty()) bytes.size - 1 else bytes.size
                return String(bytes, 0, len, StandardCharsets.UTF_8)
            }
            if (out.size() >= maxBytes) {
                return LINE_TOO_LONG
            }
            out.write(b)
            prev = b
        }
    }

    private fun writeHttpResponse(output: OutputStream, response: HttpResponse) {
        val header = buildString {
            append("HTTP/1.1 ${response.statusCode} ${response.statusText}\r\n")
            append("Content-Type: ${response.contentType}\r\n")
            append("Content-Length: ${response.body.size}\r\n")
            append("Cache-Control: no-store\r\n")
            append("X-Content-Type-Options: nosniff\r\n")
            append("X-Frame-Options: DENY\r\n")
            append("Referrer-Policy: no-referrer\r\n")
            append("Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; frame-ancestors 'none'; base-uri 'none'; form-action 'self'\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }
        output.write(header.toByteArray(StandardCharsets.US_ASCII))
        if (response.body.isNotEmpty()) {
            output.write(response.body)
        }
        output.flush()
    }
}
