// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.remote

import android.util.Log
import com.salatsign.tv.data.api.ApiConstants
import com.salatsign.tv.data.importing.ImageImporter
import com.salatsign.tv.data.importing.ImageTargetType
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.SlideType
import com.salatsign.tv.data.repository.IContentRepository
import com.salatsign.tv.data.repository.ISettingsRepository
import com.salatsign.tv.security.PinHasher
import com.salatsign.tv.security.PinVerifyResult
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
import java.util.concurrent.ConcurrentHashMap

data class HttpRequest(
    val method: String,
    val path: String,
    val queryParams: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray = ByteArray(0)
)

data class HttpResponse(
    val statusCode: Int,
    val statusText: String,
    val contentType: String = "application/json; charset=utf-8",
    val body: ByteArray = ByteArray(0)
)

/**
 * Offline-first local LAN HTTP server for SalatSign remote administration.
 * Uses pure Kotlin + java.net.ServerSocket (zero third-party HTTP dependencies, API 26+).
 * Protected by the mosque's PBKDF2-hashed Admin PIN with persisted brute-force lockout.
 */
class SalatSignLocalServer(
    private val settingsRepository: ISettingsRepository,
    private val contentRepository: IContentRepository,
    private val slideImageImporter: ImageImporter,
    private val logoImageImporter: ImageImporter,
    val port: Int = ApiConstants.DEFAULT_HTTP_PORT,
    private val appVersionName: String = "1.0",
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() }
) {
    companion object {
        private const val TAG = "SalatSignLocalServer"
        private const val MAX_BODY_BYTES = 15 * 1024 * 1024 // 15 MB (aligned with ImageImporter)
        private const val SESSION_TTL_MILLIS = 30 * 60 * 1000L // 30 minutes
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val secureRandom = SecureRandom()
    private val sessions = ConcurrentHashMap<String, Long>()

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
                        launch { handleClient(client) }
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
        Log.d(TAG, "SalatSign Local Phone Controller stopped")
    }

    private suspend fun handleClient(client: Socket) {
        client.use { socket ->
            socket.soTimeout = 15_000
            val request = readHttpRequest(socket.getInputStream()) ?: return
            val response = try {
                handleRequest(request)
            } catch (e: Exception) {
                Log.w(TAG, "Unhandled error processing ${request.method} ${request.path}", e)
                jsonResponse(500, "Internal Server Error", mapOf("error" to "internal_error", "message" to (e.message ?: "Server error")))
            }
            writeHttpResponse(socket.getOutputStream(), response)
        }
    }

    internal suspend fun handleRequest(req: HttpRequest): HttpResponse {
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
            val slideCount = contentRepository.slidesFlow.value.slides.size
            return jsonResponse(
                200, "OK",
                mapOf(
                    "app" to "SalatSign",
                    "apiVersion" to ApiConstants.API_VERSION,
                    "version" to appVersionName,
                    "port" to port,
                    "mosqueName" to settings.mosqueName,
                    "cityName" to settings.cityName,
                    "slideCount" to slideCount
                )
            )
        }

        if (method == "POST" && path == "/api/v1/auth") {
            return handleAuth(req)
        }

        // All remaining /api/v1/* endpoints require valid bearer token
        if (!isAuthorized(req)) {
            return jsonResponse(401, "Unauthorized", mapOf("error" to "unauthorized", "message" to "Valid admin session required"))
        }

        return when {
            method == "GET" && path == "/api/v1/settings" -> handleGetSettings()
            method == "PATCH" && path == "/api/v1/settings" -> handlePatchSettings(req)
            method == "POST" && path == "/api/v1/silent/start" -> handleStartManualSilent(req)
            method == "POST" && path == "/api/v1/silent/stop" -> handleStopManualSilent()
            method == "GET" && path == "/api/v1/slides" -> handleGetSlides()
            method == "POST" && path == "/api/v1/slides/text" -> handleAddTextSlide(req)
            method == "POST" && path == "/api/v1/slides/image" -> handleAddImageSlide(req)
            method == "DELETE" && path.startsWith("/api/v1/slides/") -> {
                val id = URLDecoder.decode(path.removePrefix("/api/v1/slides/"), "UTF-8")
                handleDeleteSlide(id)
            }
            method == "POST" && path == "/api/v1/logo" -> handleUploadLogo(req)
            else -> jsonResponse(404, "Not Found", mapOf("error" to "not_found", "message" to "Unknown endpoint"))
        }
    }

    private suspend fun handleAuth(req: HttpRequest): HttpResponse {
        val bodyMap = SimpleJson.parseObject(req.body.toString(StandardCharsets.UTF_8))
        val pin = bodyMap["pin"]?.toString() ?: ""
        val settings = settingsRepository.appSettingsFlow.first()
        val now = nowMillisProvider()

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

    private fun isAuthorized(req: HttpRequest): Boolean {
        val auth = req.headers["authorization"] ?: return false
        if (!auth.startsWith("Bearer ", ignoreCase = true)) return false
        val token = auth.substring(7).trim()
        val expiry = sessions[token] ?: return false
        if (nowMillisProvider() > expiry) {
            sessions.remove(token)
            return false
        }
        return true
    }

    private suspend fun handleGetSettings(): HttpResponse {
        val s = settingsRepository.appSettingsFlow.first()
        val now = nowMillisProvider()
        val manualUntil = s.silentScreen.manualSilentUntilEpochMillis
        val manualActive = manualUntil > now
        return jsonResponse(
            200, "OK",
            mapOf(
                "mosqueName" to s.mosqueName,
                "mosqueSubtitle" to s.mosqueSubtitle,
                "mosqueLogoUri" to s.mosqueLogoUri,
                "cityName" to s.cityName,
                "zoneId" to s.zoneId,
                "calculationMethodName" to s.calculationMethodName,
                "madhab" to s.madhab,
                "scheduleDurationSeconds" to s.scheduleDurationSeconds,
                "hijriOffsetDays" to s.hijriOffsetDays,
                "is24HourFormat" to s.is24HourFormat,
                "showStrikeHighlight" to s.showStrikeHighlight,
                "jumuahMode" to s.jumuahMode.name,
                "jumuahTimes" to s.jumuahTimes,
                "iqamahSubuh" to s.iqamahSubuh,
                "iqamahZuhur" to s.iqamahZuhur,
                "iqamahAshr" to s.iqamahAshr,
                "iqamahMagrib" to s.iqamahMagrib,
                "iqamahIsya" to s.iqamahIsya,
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

        val newName = (patch["mosqueName"] as? String) ?: current.mosqueName
        val newSubtitle = (patch["mosqueSubtitle"] as? String) ?: current.mosqueSubtitle
        if (patch.containsKey("mosqueName") || patch.containsKey("mosqueSubtitle")) {
            settingsRepository.updateMosqueBranding(newName, newSubtitle, current.mosqueLogoUri)
        }

        (patch["scheduleDurationSeconds"] as? Number)?.toInt()?.let {
            settingsRepository.updateScheduleDuration(it.coerceIn(10, 600))
        }
        (patch["hijriOffsetDays"] as? Number)?.toInt()?.let {
            settingsRepository.updateHijriOffset(it.coerceIn(-2, 2))
        }
        (patch["is24HourFormat"] as? Boolean)?.let {
            settingsRepository.set24HourFormat(it)
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

        return handleGetSettings()
    }

    private fun handleGetSlides(): HttpResponse {
        val container = contentRepository.slidesFlow.value
        val slides = container.slides.map { slide ->
            mapOf(
                "id" to slide.id,
                "type" to slide.type.name,
                "title" to slide.title,
                "body" to slide.body,
                "imageFile" to slide.imageFile,
                "durationSeconds" to slide.durationSeconds
            )
        }
        return jsonResponse(200, "OK", mapOf("revision" to container.revision, "slides" to slides))
    }

    private suspend fun handleAddTextSlide(req: HttpRequest): HttpResponse {
        val body = SimpleJson.parseObject(req.body.toString(StandardCharsets.UTF_8))
        val title = (body["title"] as? String)?.trim().orEmpty()
        val text = (body["body"] as? String)?.trim().orEmpty()
        if (title.isEmpty() && text.isEmpty()) {
            return jsonResponse(400, "Bad Request", mapOf("error" to "empty_slide", "message" to "Slide title or body is required"))
        }
        val duration = ((body["durationSeconds"] as? Number)?.toInt() ?: 15).coerceIn(3, 120)
        val slide = AnnouncementSlide(
            id = "slide_${nowMillisProvider()}",
            type = SlideType.TEXT,
            title = title,
            body = text,
            durationSeconds = duration
        )
        val result = contentRepository.addSlide(slide)
        return if (result.isSuccess) {
            jsonResponse(201, "Created", mapOf("id" to slide.id, "slideCount" to result.getOrThrow().slides.size))
        } else {
            jsonResponse(400, "Bad Request", mapOf("error" to "add_failed", "message" to (result.exceptionOrNull()?.message ?: "Failed to add slide")))
        }
    }

    private suspend fun handleAddImageSlide(req: HttpRequest): HttpResponse {
        if (req.body.isEmpty()) {
            return jsonResponse(400, "Bad Request", mapOf("error" to "empty_image", "message" to "Image payload is empty"))
        }
        val title = req.queryParams["title"]?.takeIf { it.isNotBlank() } ?: "Announcement"
        val duration = (req.queryParams["duration"]?.toIntOrNull() ?: 15).coerceIn(3, 120)
        val imported = slideImageImporter.importImage(req.body, ImageTargetType.SLIDE)
        val processed = imported.getOrElse { err ->
            return jsonResponse(400, "Bad Request", mapOf("error" to "image_invalid", "message" to (err.message ?: "Invalid image")))
        }
        val slide = AnnouncementSlide(
            id = "slide_${nowMillisProvider()}",
            type = SlideType.IMAGE,
            title = title,
            imageFile = processed.relativeFileName,
            durationSeconds = duration
        )
        val added = contentRepository.addSlide(slide)
        return if (added.isSuccess) {
            jsonResponse(201, "Created", mapOf("id" to slide.id, "imageFile" to processed.relativeFileName))
        } else {
            jsonResponse(400, "Bad Request", mapOf("error" to "add_failed", "message" to (added.exceptionOrNull()?.message ?: "Failed to add slide")))
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

    private fun readHttpRequest(input: InputStream): HttpRequest? {
        val buffered = BufferedInputStream(input)
        val requestLine = readLine(buffered) ?: return null
        val parts = requestLine.trim().split(" ")
        if (parts.size < 2) return null

        val method = parts[0]
        val rawTarget = parts[1]
        val qIdx = rawTarget.indexOf('?')
        val path = if (qIdx >= 0) rawTarget.substring(0, qIdx) else rawTarget
        val queryParams = mutableMapOf<String, String>()
        if (qIdx >= 0 && qIdx + 1 < rawTarget.length) {
            rawTarget.substring(qIdx + 1).split("&").forEach { pair ->
                val eq = pair.indexOf('=')
                if (eq > 0) {
                    val k = URLDecoder.decode(pair.substring(0, eq), "UTF-8")
                    val v = URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
                    queryParams[k] = v
                }
            }
        }

        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = readLine(buffered) ?: break
            if (line.isEmpty()) break
            val colon = line.indexOf(':')
            if (colon > 0) {
                headers[line.substring(0, colon).trim().lowercase(Locale.ROOT)] =
                    line.substring(colon + 1).trim()
            }
        }

        val contentLength = headers["content-length"]?.toIntOrNull()?.coerceIn(0, MAX_BODY_BYTES) ?: 0
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

        return HttpRequest(method = method, path = path, queryParams = queryParams, headers = headers, body = body)
    }

    private fun readLine(input: InputStream): String? {
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
