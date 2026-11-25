// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.remote

import android.util.Log
import com.salatsign.tv.data.api.ApiConstants
import com.salatsign.tv.data.api.ApiErrorResponse
import com.salatsign.tv.data.api.AuthMeResponse
import com.salatsign.tv.data.api.AuthRequest
import com.salatsign.tv.data.api.AuthResponse
import com.salatsign.tv.data.api.BrowserPeerJoinRequest
import com.salatsign.tv.data.api.CreateSlideResponse
import com.salatsign.tv.data.api.CreateTextSlideRequest
import com.salatsign.tv.data.api.DailySilentPrayerDto
import com.salatsign.tv.data.api.DeleteSlideResponse
import com.salatsign.tv.data.api.FullConfigResponse
import com.salatsign.tv.data.api.FullConfigWriteRequest
import com.salatsign.tv.data.api.GroupJoinCodeResponse
import com.salatsign.tv.data.api.LogoUploadResponse
import com.salatsign.tv.data.api.LogoutResponse
import com.salatsign.tv.data.api.ManualSilentResponse
import com.salatsign.tv.data.api.ManualSilentStartRequest
import com.salatsign.tv.data.api.PairExchangeRequest
import com.salatsign.tv.data.api.PairExchangeResponse
import com.salatsign.tv.data.api.PairedDeviceDto
import com.salatsign.tv.data.api.PairedDevicesResponse
import com.salatsign.tv.data.api.PatchSlideRequest
import com.salatsign.tv.data.api.PatchSlideResponse
import com.salatsign.tv.data.api.PeerDeliveryStatusDto
import com.salatsign.tv.data.api.PeerJoinRequest
import com.salatsign.tv.data.api.PeerJoinResponse
import com.salatsign.tv.data.api.PeerListResponse
import com.salatsign.tv.data.api.PeerNoticeMessage
import com.salatsign.tv.data.api.PeerSyncPayload
import com.salatsign.tv.data.api.PeerSyncPushResultResponse
import com.salatsign.tv.data.api.PhoneControllerJson
import com.salatsign.tv.data.api.SettingsPatchRequest
import com.salatsign.tv.data.api.SettingsResponse
import com.salatsign.tv.data.api.SlideDto
import com.salatsign.tv.data.api.SlideLineDto
import com.salatsign.tv.data.api.SlidesResponse
import com.salatsign.tv.data.api.StatusResponse
import com.salatsign.tv.data.api.UrgentNoticeRequest
import com.salatsign.tv.data.api.UrgentNoticeResponse
import com.salatsign.tv.data.importing.ImageImporter
import com.salatsign.tv.data.importing.ImageTargetType
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerSilentConfig
import com.salatsign.tv.data.model.SilentAnchor
import com.salatsign.tv.data.model.SlideFontSize
import com.salatsign.tv.data.model.SlideTextAlign
import com.salatsign.tv.data.model.SlideTextLine
import com.salatsign.tv.data.model.SlideType
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.data.repository.IContentRepository
import com.salatsign.tv.data.repository.ISettingsRepository
import com.salatsign.tv.engine.SlideBudget
import com.salatsign.tv.security.GroupJoinManager
import com.salatsign.tv.security.GroupSyncConflictResolver
import com.salatsign.tv.security.PairRedeemResult
import com.salatsign.tv.security.PairedDeviceRecord
import com.salatsign.tv.security.PairingManager
import com.salatsign.tv.security.PeerHmacAuth
import com.salatsign.tv.security.PeerTvRecord
import com.salatsign.tv.security.PinHasher
import com.salatsign.tv.security.PinVerifyResult
import com.salatsign.tv.security.RemoteRole
import com.salatsign.tv.security.UrgentNoticeRecord
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
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
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
    val body: ByteArray = ByteArray(0),
    val etag: String? = null
)

private sealed interface ReadRequestOutcome {
    data class Success(val request: HttpRequest) : ReadRequestOutcome
    data class Error(val response: HttpResponse) : ReadRequestOutcome
    data object Closed : ReadRequestOutcome
}

/**
 * Offline-first local LAN HTTP server for SalatSign remote administration.
 * Uses pure Kotlin + java.net.ServerSocket (zero third-party HTTP dependencies, API 26+)
 * and typed kotlinx.serialization DTOs from :core.
 *
 * Security hardening:
 * - Serialized auth path (Mutex) + per-IP rate limiting + isolated remote PBKDF2 lockout.
 * - Remote login refused while the TV is still on the default 0000 PIN.
 * - Automatic transparent upgrade of legacy 10k PBKDF2 PIN hashes on successful login.
 * - Bounded concurrent connections (global + per-IP), request lines, headers, deadlines, and body sizes.
 * - Host header validation against all active local IPv4s / localhost / .local (mitigates DNS rebinding).
 * - Strict CSP / X-Frame-Options / X-Content-Type-Options / Referrer-Policy headers on all responses.
 * - Optimistic concurrency via ETag / If-Match (412 Precondition Failed) and idempotent slide UUID writes.
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
            addAll(LocalNetworkUtils.getAllLocalIpv4Addresses())
        }
    },
    private val webAssetProvider: () -> ByteArray = defaultJvmWebAssetProvider,
    private val maxConcurrentConnections: Int = MAX_CONCURRENT_CONNECTIONS,
    private val maxConnectionsPerIp: Int = MAX_CONNECTIONS_PER_IP,
    private val headerDeadlineMillis: Long = HEADER_DEADLINE_MILLIS,
    private val jsonBodyDeadlineMillis: Long = JSON_BODY_DEADLINE_MILLIS,
    private val uploadBodyDeadlineMillis: Long = UPLOAD_BODY_DEADLINE_MILLIS,
    val pairingManager: PairingManager = PairingManager(nowMillisProvider),
    val groupJoinManager: GroupJoinManager = GroupJoinManager(nowMillisProvider),
    private val peerHmacAuth: PeerHmacAuth = PeerHmacAuth(nowMillisProvider),
    private val peerSyncClient: PeerSyncClient = PeerSyncClient(nowMillisProvider),
    private val slideImagesDir: File? = null,
    private val logoImagesDir: File? = null,
    private val onDevicePaired: (PairedDeviceRecord) -> Unit = {}
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
        private const val MAX_CONNECTIONS_PER_IP = 4
        private const val HEADER_DEADLINE_MILLIS = 10_000L
        private const val JSON_BODY_DEADLINE_MILLIS = 30_000L
        private const val UPLOAD_BODY_DEADLINE_MILLIS = 120_000L
        private const val SESSION_TTL_MILLIS = 30 * 60 * 1000L
        private const val RATE_LIMIT_WINDOW_MILLIS = 60_000L
        private const val RATE_LIMIT_MAX_AUTH_PER_IP = 15

        private val defaultJvmWebAssetProvider: () -> ByteArray = {
            val candidates = listOf(
                File("src/main/assets/phone_controller/index.html"),
                File("app-salatsign/src/main/assets/phone_controller/index.html")
            )
            val found = candidates.firstOrNull { it.exists() && it.isFile }
            found?.readBytes()
                ?: "<!DOCTYPE html><html><body>SalatSign Remote</body></html>".toByteArray(StandardCharsets.UTF_8)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val secureRandom = SecureRandom()
    private val authMutex = Mutex()
    private val connectionSemaphore = Semaphore(maxConcurrentConnections)
    private val activeConnectionsByIp = ConcurrentHashMap<String, Int>()
    private val sessions = ConcurrentHashMap<String, Long>()
    private val authAttemptsByIp = ConcurrentHashMap<String, MutableList<Long>>()
    private val cachedActivePairedHashes = ConcurrentHashMap.newKeySet<String>()
    private val lastTouchByDeviceId = ConcurrentHashMap<String, Long>()

    private sealed interface AuthContext {
        val role: RemoteRole
        data class Paired(val device: PairedDeviceRecord) : AuthContext {
            override val role: RemoteRole get() = device.role
        }
        data class PinSession(val token: String) : AuthContext {
            override val role: RemoteRole get() = RemoteRole.MANAGE
        }
    }

    // Isolated remote lockout state so a LAN client guessing PINs can never lock out the TV's own Settings PIN dialog.
    private var remoteFailedAttempts: Int = 0
    private var remoteLockedUntilEpochMillis: Long = 0L

    internal val currentRemoteFailedAttempts: Int get() = remoteFailedAttempts

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
                        val remoteIp = client.inetAddress?.hostAddress ?: "unknown"
                        if (!connectionSemaphore.tryAcquire()) {
                            rejectBusyOffAcceptLoop(client, "too_many_connections", "Server busy")
                            continue
                        }
                        if (!tryAcquireIpSlot(remoteIp)) {
                            connectionSemaphore.release()
                            rejectBusyOffAcceptLoop(client, "too_many_connections_per_ip", "Too many concurrent connections from this device")
                            continue
                        }
                        launch {
                            try {
                                handleClient(client, remoteIp)
                            } finally {
                                releaseIpSlot(remoteIp)
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

    private fun tryAcquireIpSlot(ip: String): Boolean {
        var acquired = false
        activeConnectionsByIp.compute(ip) { _, existing ->
            val current = existing ?: 0
            if (current >= maxConnectionsPerIp) {
                current
            } else {
                acquired = true
                current + 1
            }
        }
        return acquired
    }

    private fun releaseIpSlot(ip: String) {
        activeConnectionsByIp.compute(ip) { _, existing ->
            val next = (existing ?: 0) - 1
            if (next <= 0) null else next
        }
    }

    private fun rejectBusyOffAcceptLoop(client: Socket, errorCode: String, message: String) {
        scope.launch {
            runCatching {
                client.use { s ->
                    s.soTimeout = 2_000
                    writeHttpResponse(
                        s.getOutputStream(),
                        errorResponse(503, "Service Unavailable", errorCode, message)
                    )
                }
            }
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
        activeConnectionsByIp.clear()
        Log.d(TAG, "SalatSign Local Phone Controller stopped")
    }

    private suspend fun handleClient(client: Socket, remoteIp: String) {
        client.use { socket ->
            socket.soTimeout = headerDeadlineMillis.coerceIn(500L, 15_000L).toInt()
            val response = when (val outcome = readHttpRequest(socket, socket.getInputStream(), remoteIp)) {
                is ReadRequestOutcome.Closed -> return
                is ReadRequestOutcome.Error -> outcome.response
                is ReadRequestOutcome.Success -> {
                    val req = outcome.request
                    val hostHeader = req.headers["host"]
                    if (hostHeader == null || !isValidHostHeader(hostHeader)) {
                        errorResponse(403, "Forbidden", "invalid_host", "Host header not allowed")
                    } else {
                        try {
                            handleRequest(req)
                        } catch (e: IllegalArgumentException) {
                            Log.w(TAG, "Bad request on ${req.method} ${req.path}", e)
                            errorResponse(400, "Bad Request", "bad_request", "Malformed request payload")
                        } catch (e: Exception) {
                            Log.w(TAG, "Unhandled error processing ${req.method} ${req.path}", e)
                            errorResponse(500, "Internal Server Error", "internal_error", "Internal server error")
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
        req.headers["host"]?.let { host ->
            if (!isValidHostHeader(host)) {
                return errorResponse(403, "Forbidden", "invalid_host", "Host header not allowed")
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
                body = webAssetProvider()
            )
        }

        if (method == "GET" && path == "/api/v1/status") {
            val deviceId = settingsRepository.ensureDeviceId()
            val settings = settingsRepository.appSettingsFlow.first()
            syncPairedHashCache(settings)
            val activePairedCount = settings.pairedDevices.count { !it.isRevoked }
            return typedJsonResponse(
                200, "OK",
                StatusResponse(
                    app = "SalatSign",
                    apiVersion = ApiConstants.API_VERSION,
                    version = appVersionName,
                    port = port,
                    pinConfigured = settings.hasCustomPin,
                    deviceId = deviceId,
                    deviceName = settings.deviceName,
                    roomGroup = settings.roomGroup,
                    configVersion = settings.configVersion,
                    pairedDeviceCount = activePairedCount,
                    lanPinLoginEnabled = settings.lanPinLoginEnabled,
                    pairingActive = pairingManager.currentActiveCode() != null
                ),
                etag = "\"${settings.configVersion}\""
            )
        }

        if (method == "POST" && path == "/api/v1/pair") {
            if (req.body.size > MAX_PRE_AUTH_BODY_BYTES) {
                return errorResponse(413, "Payload Too Large", "payload_too_large", "Request body exceeds limit")
            }
            return try {
                handlePairExchange(req)
            } catch (_: IllegalArgumentException) {
                errorResponse(400, "Bad Request", "invalid_json", "Malformed JSON payload")
            }
        }

        // Phase 3a/3b/3c: TV-to-TV peer endpoints (join code or HMAC-SHA256 signed — browser never calls these)
        if (method == "POST" && path == "/api/v1/peer/join") {
            if (req.body.size > MAX_PRE_AUTH_BODY_BYTES) {
                return errorResponse(413, "Payload Too Large", "payload_too_large", "Request body exceeds limit")
            }
            return try {
                handlePeerJoin(req)
            } catch (_: IllegalArgumentException) {
                errorResponse(400, "Bad Request", "invalid_json", "Malformed JSON payload")
            }
        }
        if (path.startsWith("/api/v1/peer/")) {
            return handleSignedPeerRequest(method, path, req)
        }

        if (method == "POST" && path == "/api/v1/auth") {
            if (req.body.size > MAX_PRE_AUTH_BODY_BYTES) {
                return errorResponse(413, "Payload Too Large", "payload_too_large", "Request body exceeds limit")
            }
            return try {
                handleAuth(req)
            } catch (_: IllegalArgumentException) {
                errorResponse(400, "Bad Request", "invalid_json", "Malformed JSON payload")
            }
        }

        // All remaining /api/v1/* endpoints require a valid paired device token or active PIN session
        val currentSettings = settingsRepository.appSettingsFlow.first()
        syncPairedHashCache(currentSettings)
        val authCtx = authenticateRequest(req.headers, currentSettings)
            ?: return errorResponse(401, "Unauthorized", "unauthorized", "Valid paired device or admin session required")

        return try {
            when {
                method == "GET" && path == "/api/v1/auth/me" -> {
                    when (authCtx) {
                        is AuthContext.Paired -> typedJsonResponse(
                            200, "OK",
                            AuthMeResponse(
                                authType = "PAIRED",
                                deviceId = authCtx.device.deviceId,
                                deviceName = authCtx.device.deviceName,
                                role = authCtx.role.name
                            )
                        )
                        is AuthContext.PinSession -> typedJsonResponse(
                            200, "OK",
                            AuthMeResponse(authType = "PIN_SESSION", role = authCtx.role.name)
                        )
                    }
                }
                method == "POST" && path == "/api/v1/logout" -> {
                    if (authCtx is AuthContext.PinSession) {
                        sessions.remove(authCtx.token)
                    }
                    typedJsonResponse(200, "OK", LogoutResponse(loggedOut = true))
                }
                method == "GET" && path == "/api/v1/peers" -> handleGetPeers(currentSettings)
                method == "GET" && path == "/api/v1/notices" -> handleGetNotice(currentSettings)
                method == "POST" && path == "/api/v1/notices" -> handleSendNotice(req, currentSettings)
                method == "DELETE" && path == "/api/v1/notices" -> handleClearNotice(currentSettings)
                !authCtx.role.hasPermission(RemoteRole.MANAGE) -> {
                    errorResponse(403, "Forbidden", "insufficient_role", "This device only has Notify permission")
                }
                method == "POST" && path == "/api/v1/peers/code" -> handleCreateGroupJoinCode()
                method == "POST" && path == "/api/v1/peers/join" -> handleBrowserJoinGroup(req, currentSettings)
                method == "POST" && path == "/api/v1/peers/push" -> handlePushConfigToPeers(currentSettings)
                method == "GET" && path == "/api/v1/devices" -> handleGetDevices(currentSettings)
                method == "DELETE" && path.startsWith("/api/v1/devices/") -> {
                    val devId = URLDecoder.decode(path.removePrefix("/api/v1/devices/"), "UTF-8")
                    handleRevokeDevice(devId)
                }
                method == "GET" && path == "/api/v1/config" -> handleGetFullConfig()
                method == "PUT" && path == "/api/v1/config" -> handlePutFullConfig(req)
                method == "GET" && path == "/api/v1/settings" -> handleGetSettings()
                method == "PATCH" && path == "/api/v1/settings" -> handlePatchSettings(req)
                method == "POST" && path == "/api/v1/silent/start" -> handleStartManualSilent(req)
                method == "POST" && path == "/api/v1/silent/stop" -> handleStopManualSilent()
                method == "GET" && path == "/api/v1/slides" -> handleGetSlides()
                method == "POST" && path == "/api/v1/slides/text" -> handleAddTextSlide(req)
                method == "POST" && path == "/api/v1/slides/image" -> handleAddImageSlide(req)
                method == "PUT" && path.startsWith("/api/v1/slides/") -> {
                    val id = URLDecoder.decode(path.removePrefix("/api/v1/slides/"), "UTF-8")
                    handlePutSlide(id, req)
                }
                method == "PATCH" && path.startsWith("/api/v1/slides/") -> {
                    val id = URLDecoder.decode(path.removePrefix("/api/v1/slides/"), "UTF-8")
                    handlePatchSlide(id, req)
                }
                method == "DELETE" && path.startsWith("/api/v1/slides/") -> {
                    val id = URLDecoder.decode(path.removePrefix("/api/v1/slides/"), "UTF-8")
                    handleDeleteSlide(id)
                }
                method == "POST" && path == "/api/v1/logo" -> handleUploadLogo(req)
                else -> errorResponse(404, "Not Found", "not_found", "Unknown endpoint")
            }
        } catch (_: IllegalArgumentException) {
            errorResponse(400, "Bad Request", "invalid_json", "Malformed JSON payload")
        }
    }

    private fun checkIfMatch(req: HttpRequest, currentConfigVersion: Long): HttpResponse? {
        val raw = req.headers["if-match"]?.trim() ?: return null
        if (raw == "*") return null
        val cleaned = raw.removePrefix("W/").removePrefix("w/").trim().trim('"')
        val expected = cleaned.toLongOrNull()
        if (expected == null || expected != currentConfigVersion) {
            return typedJsonResponse(
                412, "Precondition Failed",
                ApiErrorResponse(
                    error = "precondition_failed",
                    message = "Config version mismatch (expected $currentConfigVersion)",
                    currentConfigVersion = currentConfigVersion
                ),
                etag = "\"$currentConfigVersion\""
            )
        }
        return null
    }

    private suspend fun handlePairExchange(req: HttpRequest): HttpResponse {
        val pairReq = PhoneControllerJson.decode<PairExchangeRequest>(req.body.toString(StandardCharsets.UTF_8))
        return when (val res = pairingManager.redeemCode(pairReq.code, pairReq.deviceName, req.remoteAddress)) {
            is PairRedeemResult.Success -> {
                settingsRepository.addPairedDevice(res.record)
                cachedActivePairedHashes.add(res.record.tokenHash)
                runCatching { onDevicePaired(res.record) }
                typedJsonResponse(
                    200, "OK",
                    PairExchangeResponse(
                        deviceId = res.record.deviceId,
                        deviceName = res.record.deviceName,
                        role = res.record.role.name,
                        token = res.rawToken
                    )
                )
            }
            is PairRedeemResult.NoActivePairing -> {
                errorResponse(400, "Bad Request", "pairing_inactive", "Open Settings → PIN & Remote → Pair Phone on the TV first.")
            }
            is PairRedeemResult.Expired -> {
                errorResponse(400, "Bad Request", "pairing_expired", "Pairing code expired. Generate a new code on the TV.")
            }
            is PairRedeemResult.InvalidCode -> {
                errorResponse(401, "Unauthorized", "invalid_pairing_code", "Incorrect pairing code.")
            }
            is PairRedeemResult.RateLimited -> {
                typedJsonResponse(
                    429, "Too Many Requests",
                    ApiErrorResponse(
                        error = "rate_limited",
                        message = "Too many wrong pairing codes from this device. Try again in ${res.remainingSeconds} seconds.",
                        remainingSeconds = res.remainingSeconds
                    )
                )
            }
        }
    }

    private fun handleGetDevices(settings: AppSettings): HttpResponse {
        val list = settings.pairedDevices.filterNot { it.isRevoked }.map { d ->
            PairedDeviceDto(
                deviceId = d.deviceId,
                deviceName = d.deviceName,
                role = d.role.name,
                createdAtEpochMillis = d.createdAtEpochMillis,
                lastSeenAtEpochMillis = d.lastSeenAtEpochMillis,
                revoked = d.isRevoked
            )
        }
        return typedJsonResponse(
            200, "OK",
            PairedDevicesResponse(
                devices = list,
                lanPinLoginEnabled = settings.lanPinLoginEnabled,
                tokenExpiryDays = settings.pairedTokenExpiryDays
            )
        )
    }

    private suspend fun handleRevokeDevice(deviceId: String): HttpResponse {
        if (deviceId.isBlank()) {
            return errorResponse(400, "Bad Request", "missing_id", "Device ID is required")
        }
        val now = nowMillisProvider()
        settingsRepository.revokePairedDevice(deviceId, now)
        val refreshed = settingsRepository.appSettingsFlow.first()
        syncPairedHashCache(refreshed)
        return handleGetDevices(refreshed)
    }

    // --- Phase 3: Multi-TV Group Sync & Urgent Notice Handlers ---

    private suspend fun selfPeerRecord(settings: AppSettings): PeerTvRecord {
        val id = settingsRepository.ensureDeviceId()
        val host = LocalNetworkUtils.getLocalIpv4Address() ?: "127.0.0.1"
        return PeerTvRecord(
            deviceId = id,
            deviceName = settings.deviceName.ifBlank { "SalatSign TV" },
            roomGroup = settings.roomGroup,
            lastKnownHost = host,
            port = port,
            lastSeenEpochMillis = nowMillisProvider()
        )
    }

    private suspend fun buildPeerSyncPayload(settings: AppSettings): PeerSyncPayload {
        val self = selfPeerRecord(settings)
        val dailyMap = listOf(Prayer.SUBUH, Prayer.ZUHUR, Prayer.ASHR, Prayer.MAGRIB, Prayer.ISYA).associate { p ->
            val cfg = settings.silentScreen.dailyPrayers[p] ?: PrayerSilentConfig()
            p.name to DailySilentPrayerDto(cfg.enabled, cfg.anchor.name, cfg.leadMinutes, cfg.durationMinutes)
        }
        // Only group-syncable fields (D24): never include deviceName, roomGroup, themeMode, or phoneControllerAlwaysOn
        val groupPatch = SettingsPatchRequest(
            mosqueName = settings.mosqueName.takeIf { it.isNotBlank() },
            mosqueSubtitle = settings.mosqueSubtitle,
            mosqueIconPreset = settings.mosqueIconPreset.name,
            mosqueFontPreset = settings.mosqueFontPreset.name,
            appLanguage = settings.appLanguage.name,
            scheduleDurationSeconds = settings.scheduleDurationSeconds,
            hijriOffsetDays = settings.hijriOffsetDays,
            hijriChangeAtMidnight = settings.hijriChangeAtMidnight,
            is24HourFormat = settings.is24HourFormat,
            showArabicPrayerNames = settings.showArabicPrayerNames,
            showStrikeHighlight = settings.showStrikeHighlight,
            jumuahMode = settings.jumuahMode.name,
            jumuahTimes = settings.jumuahTimes,
            iqamahSubuh = settings.iqamahSubuh,
            iqamahZuhur = settings.iqamahZuhur,
            iqamahAshr = settings.iqamahAshr,
            iqamahMagrib = settings.iqamahMagrib,
            iqamahIsya = settings.iqamahIsya,
            islamicCalendarSlideEnabled = settings.islamicCalendarSlideEnabled,
            islamicCalendarSlideDurationSeconds = settings.islamicCalendarSlideDurationSeconds,
            quranVerseEnabled = settings.quranVerseEnabled,
            quranShowTranslation = settings.quranShowTranslation,
            quranVerseDurationSeconds = settings.quranVerseDurationSeconds,
            silentJumuahEnabled = settings.silentScreen.jumuahEnabled,
            silentJumuahLeadMinutes = settings.silentScreen.jumuahLeadMinutes,
            silentJumuahDurationMinutes = settings.silentScreen.jumuahDurationMinutes,
            silentDailyPrayers = dailyMap
        )
        val allPeers = (settings.peerTvs.filterNot { it.deviceId == self.deviceId } + self)
        return PeerSyncPayload(
            originDeviceId = self.deviceId,
            originHost = self.lastKnownHost,
            originPort = self.port,
            configVersion = settings.configVersion,
            settings = groupPatch,
            slides = contentRepository.slidesFlow.value.slides.map(::slideToDto),
            pairedDevices = settings.pairedDevices,
            peers = allPeers
        )
    }

    private suspend fun handleCreateGroupJoinCode(): HttpResponse {
        val settings = settingsRepository.appSettingsFlow.first()
        if (settings.groupSecretHex.isBlank()) {
            settingsRepository.setGroupSecretHex(PeerHmacAuth.generateGroupSecretHex())
        }
        val active = groupJoinManager.generateJoinCode()
        return typedJsonResponse(200, "OK", GroupJoinCodeResponse(active.code, active.expiresAtEpochMillis))
    }

    private suspend fun handlePeerJoin(req: HttpRequest): HttpResponse {
        val joinReq = PhoneControllerJson.decode<PeerJoinRequest>(req.body.toString(StandardCharsets.UTF_8))
        if (!groupJoinManager.consumeIfValid(joinReq.code)) {
            return errorResponse(401, "Unauthorized", "invalid_join_code", "Invalid or expired TV group join code")
        }
        if (joinReq.deviceId.isBlank()) {
            return errorResponse(400, "Bad Request", "missing_device_id", "Joining TV must supply deviceId")
        }
        var settings = settingsRepository.appSettingsFlow.first()
        var secret = settings.groupSecretHex
        if (secret.isBlank()) {
            secret = PeerHmacAuth.generateGroupSecretHex()
            settingsRepository.setGroupSecretHex(secret)
            settings = settingsRepository.appSettingsFlow.first()
        }
        val peerHost = joinReq.host.trim().ifBlank { req.remoteAddress }
        val joinedPeer = PeerTvRecord(
            deviceId = joinReq.deviceId.trim(),
            deviceName = joinReq.deviceName.trim().take(48),
            roomGroup = joinReq.roomGroup.trim().take(48),
            lastKnownHost = peerHost,
            port = joinReq.port.coerceIn(1024, 65535),
            lastSeenEpochMillis = nowMillisProvider()
        )
        settingsRepository.upsertPeerTv(joinedPeer)
        val updatedSettings = settingsRepository.appSettingsFlow.first()
        val self = selfPeerRecord(updatedSettings)
        val peersForJoiner = (updatedSettings.peerTvs.filterNot { it.deviceId == self.deviceId } + self)
        return typedJsonResponse(200, "OK", PeerJoinResponse(groupSecretHex = secret, peers = peersForJoiner))
    }

    private suspend fun handleBrowserJoinGroup(req: HttpRequest, settings: AppSettings): HttpResponse {
        val body = PhoneControllerJson.decode<BrowserPeerJoinRequest>(req.body.toString(StandardCharsets.UTF_8))
        val host = body.host.trim()
        if (host.isBlank() || body.code.filter { it.isDigit() }.length != 6) {
            return errorResponse(400, "Bad Request", "invalid_join_request", "Host IP and 6-digit join code are required")
        }
        val self = selfPeerRecord(settings)
        val result = peerSyncClient.joinGroup(host, body.port.coerceIn(1024, 65535), body.code, self)
        val joined = result.getOrElse { err ->
            return errorResponse(502, "Bad Gateway", "peer_join_failed", err.message ?: "Could not reach peer TV")
        }
        settingsRepository.setGroupSecretHex(joined.groupSecretHex)
        val filteredPeers = joined.peers.filterNot { it.deviceId == self.deviceId }
        settingsRepository.setPeerTvs(filteredPeers)
        // Immediately catch up config from peers if they are ahead
        pullNewerConfigFromPeers()
        val refreshed = settingsRepository.appSettingsFlow.first()
        return handleGetPeers(refreshed)
    }

    private suspend fun handleGetPeers(settings: AppSettings): HttpResponse {
        val self = selfPeerRecord(settings)
        val peers = settings.peerTvs.filterNot { it.deviceId == self.deviceId }
        val groups = (listOf(self.roomGroup) + peers.map { it.roomGroup })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        return typedJsonResponse(
            200, "OK",
            PeerListResponse(
                self = self,
                peers = peers,
                roomGroups = groups,
                groupLinked = settings.groupSecretHex.isNotBlank()
            )
        )
    }

    private suspend fun handlePushConfigToPeers(settings: AppSettings): HttpResponse {
        val selfId = settingsRepository.ensureDeviceId()
        val peers = settings.peerTvs.filterNot { it.deviceId == selfId }
        val payload = buildPeerSyncPayload(settings)
        val deliveries = peerSyncClient.pushConfigToPeers(peers, payload, settings.groupSecretHex, selfId)
        return typedJsonResponse(
            200, "OK",
            PeerSyncPushResultResponse(configVersion = settings.configVersion, deliveries = deliveries)
        )
    }

    private suspend fun handleSignedPeerRequest(method: String, path: String, req: HttpRequest): HttpResponse {
        val settings = settingsRepository.appSettingsFlow.first()
        val verified = peerHmacAuth.verifyRequest(
            groupSecretHex = settings.groupSecretHex,
            method = method,
            path = path,
            timestampHeader = req.headers[PeerHmacAuth.HEADER_TIMESTAMP],
            nonceHeader = req.headers[PeerHmacAuth.HEADER_NONCE],
            signatureHeader = req.headers[PeerHmacAuth.HEADER_SIGNATURE],
            body = req.body
        )
        if (!verified) {
            return errorResponse(401, "Unauthorized", "invalid_peer_signature", "Invalid or replayed peer HMAC signature")
        }
        return try {
            when {
                method == "GET" && path == "/api/v1/peer/sync" -> {
                    typedJsonResponse(200, "OK", buildPeerSyncPayload(settings))
                }
                method == "POST" && path == "/api/v1/peer/sync" -> {
                    val incoming = PhoneControllerJson.decode<PeerSyncPayload>(req.body.toString(StandardCharsets.UTF_8))
                    applyIncomingPeerSync(incoming, settings)
                    val refreshed = settingsRepository.appSettingsFlow.first()
                    typedJsonResponse(200, "OK", mapOf("configVersion" to refreshed.configVersion))
                }
                method == "POST" && path == "/api/v1/peer/notice" -> {
                    val msg = PhoneControllerJson.decode<PeerNoticeMessage>(req.body.toString(StandardCharsets.UTF_8))
                    applyIncomingPeerNotice(msg)
                    typedJsonResponse(200, "OK", LogoutResponse(loggedOut = false))
                }
                method == "GET" && path.startsWith("/api/v1/peer/asset/") -> {
                    handleServePeerAsset(path.removePrefix("/api/v1/peer/asset/"))
                }
                else -> errorResponse(404, "Not Found", "not_found", "Unknown peer endpoint")
            }
        } catch (_: IllegalArgumentException) {
            errorResponse(400, "Bad Request", "invalid_json", "Malformed peer JSON payload")
        }
    }

    private suspend fun applyIncomingPeerSync(incoming: PeerSyncPayload, current: AppSettings) {
        val selfId = settingsRepository.ensureDeviceId()

        // 1. Always merge paired devices with revocation-wins tombstones (D24)
        val mergedDevices = GroupSyncConflictResolver.mergePairedDevices(current.pairedDevices, incoming.pairedDevices)
        if (mergedDevices != current.pairedDevices) {
            settingsRepository.setPairedDevices(mergedDevices)
            syncPairedHashCache(settingsRepository.appSettingsFlow.first())
        }

        // 2. Merge peer list (excluding self)
        if (incoming.peers.isNotEmpty()) {
            val peerMap = current.peerTvs.associateBy { it.deviceId }.toMutableMap()
            for (p in incoming.peers) {
                if (p.deviceId.isNotBlank() && p.deviceId != selfId) {
                    peerMap[p.deviceId] = p
                }
            }
            settingsRepository.setPeerTvs(peerMap.values.toList())
        }

        // 3. Adopt group config & slides only if incoming wins conflict resolution
        if (!GroupSyncConflictResolver.shouldAdoptPeerConfig(
                localVersion = current.configVersion,
                localDeviceId = selfId,
                peerVersion = incoming.configVersion,
                peerDeviceId = incoming.originDeviceId
            )
        ) {
            return
        }

        // Strip local-only fields (D24) so peer sync never overwrites TV identity or theme
        val safeGroupPatch = incoming.settings.copy(
            deviceName = null,
            roomGroup = null,
            themeMode = null
        )
        applySettingsPatch(safeGroupPatch, current, bumpVersion = false)

        // Replace slides and fetch any missing slide images from origin TV
        val mappedSlides = incoming.slides.mapIndexed { idx, dto ->
            val slideType = runCatching { SlideType.valueOf(dto.type.uppercase(Locale.ROOT)) }.getOrDefault(SlideType.TEXT)
            val parsedLines = parseSlideLineDtos(dto.lines)
            val textBody = if (parsedLines.isNotEmpty()) parsedLines.joinToString("\n") { it.text } else dto.body.trim()
            val align = runCatching { SlideTextAlign.valueOf(dto.textAlign.uppercase(Locale.ROOT)) }.getOrNull()
                ?: parsedLines.firstOrNull()?.align
                ?: SlideTextAlign.CENTER
            val targetSlideDir = slideImagesDir
            val imageFileName = dto.imageFile
            if (slideType == SlideType.IMAGE && !imageFileName.isNullOrBlank() && targetSlideDir != null && incoming.originHost.isNotBlank()) {
                val localFile = File(targetSlideDir, imageFileName)
                if (!localFile.exists()) {
                    val bytes = peerSyncClient.downloadPeerAsset(
                        incoming.originHost, incoming.originPort, "slide", imageFileName, current.groupSecretHex, selfId
                    )
                    if (bytes != null && bytes.isNotEmpty()) {
                        runCatching {
                            targetSlideDir.mkdirs()
                            localFile.writeBytes(bytes)
                        }
                    }
                }
            }
            AnnouncementSlide(
                id = dto.id.trim().ifEmpty { "slide_${UUID.randomUUID()}" },
                type = slideType,
                title = dto.title.trim(),
                body = if (slideType == SlideType.TEXT) textBody else dto.body,
                lines = if (slideType == SlideType.TEXT) parsedLines else emptyList(),
                imageFile = dto.imageFile,
                durationSeconds = dto.durationSeconds.coerceIn(3, 120),
                enabled = dto.enabled,
                textAlign = align,
                sortOrder = idx
            )
        }
        val rev = contentRepository.slidesFlow.value.revision
        contentRepository.replaceAll(rev, mappedSlides)
        settingsRepository.setConfigVersion(incoming.configVersion)
    }

    suspend fun pullNewerConfigFromPeers() {
        val settings = settingsRepository.appSettingsFlow.first()
        if (settings.groupSecretHex.isBlank() || settings.peerTvs.isEmpty()) return
        val selfId = settingsRepository.ensureDeviceId()
        for (peer in settings.peerTvs) {
            if (peer.deviceId == selfId) continue
            val pulled = peerSyncClient.pullConfigFromPeer(peer, settings.groupSecretHex, selfId).getOrNull() ?: continue
            val latestLocal = settingsRepository.appSettingsFlow.first()
            applyIncomingPeerSync(pulled, latestLocal)
        }
    }

    private fun handleServePeerAsset(relative: String): HttpResponse {
        val parts = relative.split("/", limit = 2)
        if (parts.size != 2) return errorResponse(404, "Not Found", "not_found", "Asset not found")
        val kind = parts[0]
        val fileName = parts[1]
        if (fileName.isBlank() || fileName.contains('/') || fileName.contains('\\') || fileName.contains("..")) {
            return errorResponse(400, "Bad Request", "invalid_file", "Invalid asset filename")
        }
        val dir = when (kind) {
            "slide" -> slideImagesDir
            "logo" -> logoImagesDir
            else -> null
        } ?: return errorResponse(404, "Not Found", "not_found", "Asset directory unavailable")
        val target = File(dir, fileName)
        val canonicalDir = dir.canonicalPath + File.separator
        if (!target.canonicalPath.startsWith(canonicalDir) || !target.exists() || !target.isFile) {
            return errorResponse(404, "Not Found", "not_found", "Asset not found")
        }
        return HttpResponse(200, "OK", "application/octet-stream", target.readBytes())
    }

    private suspend fun applyIncomingPeerNotice(msg: PeerNoticeMessage) {
        if (msg.clear) {
            settingsRepository.setActiveUrgentNotice(null)
            return
        }
        val now = nowMillisProvider()
        val duration = msg.durationMinutes.coerceIn(1, 60)
        val record = UrgentNoticeRecord(
            id = msg.id.ifBlank { "notice_${UUID.randomUUID()}" },
            title = msg.title.trim().take(60).ifBlank { "Urgent Notice" },
            message = msg.message.trim().take(240),
            createdAtEpochMillis = if (msg.createdAtEpochMillis > 0L) msg.createdAtEpochMillis else now,
            durationMinutes = duration,
            // Enforce expiry from LOCAL receive time so clock drift between TVs cannot extend it
            receivedAtEpochMillis = now,
            expiresAtEpochMillis = now + duration * 60_000L,
            originDeviceId = msg.originDeviceId
        )
        if (record.message.isNotBlank()) {
            settingsRepository.setActiveUrgentNotice(record)
        }
    }

    private fun handleGetNotice(settings: AppSettings): HttpResponse {
        val now = nowMillisProvider()
        val notice = settings.activeUrgentNotice?.takeIf { it.isActive(now) }
        return typedJsonResponse(
            200, "OK",
            UrgentNoticeResponse(
                active = notice != null,
                notice = notice,
                remainingMinutes = notice?.remainingMinutes(now) ?: 0L
            )
        )
    }

    private suspend fun handleSendNotice(req: HttpRequest, settings: AppSettings): HttpResponse {
        val body = PhoneControllerJson.decode<UrgentNoticeRequest>(req.body.toString(StandardCharsets.UTF_8))
        val cleanMsg = body.message.trim().take(240)
        if (cleanMsg.isBlank()) {
            return errorResponse(400, "Bad Request", "empty_notice", "Urgent notice message is required")
        }
        val now = nowMillisProvider()
        val selfId = settingsRepository.ensureDeviceId()
        val duration = body.durationMinutes.coerceIn(1, 60)
        val noticeId = body.id?.trim()?.takeIf { it.isNotEmpty() } ?: "notice_${UUID.randomUUID()}"
        val cleanTitle = body.title.trim().take(60).ifBlank { "Urgent Notice" }

        val scope = body.targetScope.uppercase(Locale.ROOT)
        val targetRoom = body.targetRoomGroup?.trim().orEmpty()
        val targetIds = body.targetDeviceIds?.toSet().orEmpty()

        val includeSelf = when (scope) {
            "ROOM" -> targetRoom.isEmpty() || settings.roomGroup.equals(targetRoom, ignoreCase = true)
            "SELECTED" -> targetIds.isEmpty() || selfId in targetIds
            else -> true
        }
        val targetPeers = settings.peerTvs.filter { peer ->
            peer.deviceId != selfId && when (scope) {
                "ROOM" -> targetRoom.isEmpty() || peer.roomGroup.equals(targetRoom, ignoreCase = true)
                "SELECTED" -> peer.deviceId in targetIds
                else -> true
            }
        }

        val deliveries = mutableListOf<PeerDeliveryStatusDto>()
        var localRecord: UrgentNoticeRecord? = null
        if (includeSelf) {
            localRecord = UrgentNoticeRecord(
                id = noticeId,
                title = cleanTitle,
                message = cleanMsg,
                createdAtEpochMillis = now,
                durationMinutes = duration,
                receivedAtEpochMillis = now,
                expiresAtEpochMillis = now + duration * 60_000L,
                originDeviceId = selfId
            )
            settingsRepository.setActiveUrgentNotice(localRecord)
            deliveries.add(
                PeerDeliveryStatusDto(
                    deviceId = selfId,
                    deviceName = settings.deviceName.ifBlank { "This TV" },
                    status = "delivered"
                )
            )
        }

        if (targetPeers.isNotEmpty() && settings.groupSecretHex.isNotBlank()) {
            val peerMsg = PeerNoticeMessage(
                clear = false,
                id = noticeId,
                title = cleanTitle,
                message = cleanMsg,
                createdAtEpochMillis = now,
                durationMinutes = duration,
                originDeviceId = selfId
            )
            deliveries.addAll(peerSyncClient.pushNoticeToPeers(targetPeers, peerMsg, settings.groupSecretHex, selfId))
        }

        return typedJsonResponse(
            200, "OK",
            UrgentNoticeResponse(
                active = localRecord != null,
                notice = localRecord,
                remainingMinutes = localRecord?.remainingMinutes(now) ?: 0L,
                deliveries = deliveries
            )
        )
    }

    private suspend fun handleClearNotice(settings: AppSettings): HttpResponse {
        val selfId = settingsRepository.ensureDeviceId()
        settingsRepository.setActiveUrgentNotice(null)
        val deliveries = mutableListOf(
            PeerDeliveryStatusDto(
                deviceId = selfId,
                deviceName = settings.deviceName.ifBlank { "This TV" },
                status = "delivered"
            )
        )
        val peers = settings.peerTvs.filterNot { it.deviceId == selfId }
        if (peers.isNotEmpty() && settings.groupSecretHex.isNotBlank()) {
            deliveries.addAll(
                peerSyncClient.pushNoticeToPeers(peers, PeerNoticeMessage(clear = true, originDeviceId = selfId), settings.groupSecretHex, selfId)
            )
        }
        return typedJsonResponse(
            200, "OK",
            UrgentNoticeResponse(active = false, notice = null, remainingMinutes = 0L, deliveries = deliveries)
        )
    }

    private suspend fun handleAuth(req: HttpRequest): HttpResponse = authMutex.withLock {
        val now = nowMillisProvider()
        pruneExpiredSessions(now)

        // Per-IP throttling
        val attempts = authAttemptsByIp.computeIfAbsent(req.remoteAddress) { mutableListOf() }
        attempts.removeAll { now - it > RATE_LIMIT_WINDOW_MILLIS }
        if (attempts.size >= RATE_LIMIT_MAX_AUTH_PER_IP) {
            return errorResponse(
                429, "Too Many Requests",
                "rate_limited",
                "Too many login attempts from this device. Try again in a minute."
            )
        }
        attempts.add(now)

        val settings = settingsRepository.appSettingsFlow.first()
        if (!settings.lanPinLoginEnabled) {
            return errorResponse(
                403, "Forbidden",
                "pin_login_disabled",
                "PIN login over LAN is disabled. Pair your phone from TV Settings → PIN & Remote → Pair Phone."
            )
        }
        if (!settings.hasCustomPin) {
            return errorResponse(
                403, "Forbidden",
                "default_pin_blocked",
                "Remote login is disabled while using the default PIN. Set a custom Admin PIN on the TV first (Settings → PIN & Remote)."
            )
        }

        val authReq = PhoneControllerJson.decode<AuthRequest>(req.body.toString(StandardCharsets.UTF_8))
        val pin = authReq.pin

        val result = PinHasher.verifyPin(
            enteredPin = pin,
            hasCustomPin = settings.hasCustomPin,
            saltHex = settings.pinSaltHex,
            hashHex = settings.pinHashHex,
            nowEpochMillis = now,
            currentFailedAttempts = remoteFailedAttempts,
            lockedUntilEpochMillis = remoteLockedUntilEpochMillis
        )

        return when (result) {
            is PinVerifyResult.Success -> {
                remoteFailedAttempts = 0
                remoteLockedUntilEpochMillis = 0L
                if (PinHasher.needsRehash(settings.hasCustomPin, settings.pinHashHex)) {
                    settingsRepository.setCustomPin(pin)
                }
                val tokenBytes = ByteArray(24)
                secureRandom.nextBytes(tokenBytes)
                val token = PinHasher.bytesToHex(tokenBytes)
                sessions[token] = now + SESSION_TTL_MILLIS
                typedJsonResponse(200, "OK", AuthResponse(token = token, expiresInSeconds = SESSION_TTL_MILLIS / 1000L))
            }
            is PinVerifyResult.Incorrect -> {
                val lockout = PinHasher.calculateLockout(remoteFailedAttempts, now)
                remoteFailedAttempts = lockout.newFailedAttempts
                remoteLockedUntilEpochMillis = lockout.lockedUntilEpochMillis
                typedJsonResponse(
                    401, "Unauthorized",
                    ApiErrorResponse(
                        error = "invalid_pin",
                        message = "Incorrect PIN (${result.attemptsLeftBeforeLockout} attempts remaining)",
                        attemptsLeft = result.attemptsLeftBeforeLockout
                    )
                )
            }
            is PinVerifyResult.LockedOut -> {
                typedJsonResponse(
                    429, "Too Many Requests",
                    ApiErrorResponse(
                        error = "locked_out",
                        message = "Remote login locked. Try again in ${result.remainingSeconds} seconds.",
                        remainingSeconds = result.remainingSeconds
                    )
                )
            }
            is PinVerifyResult.InvalidFormat -> {
                errorResponse(400, "Bad Request", "invalid_format", "PIN must be 4–8 digits")
            }
            is PinVerifyResult.ResetRequired -> {
                errorResponse(423, "Locked", "reset_required", "PIN storage corrupted. Clear app data on TV.")
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
        val ipIterator = authAttemptsByIp.entries.iterator()
        while (ipIterator.hasNext()) {
            val entry = ipIterator.next()
            entry.value.removeAll { now - it > RATE_LIMIT_WINDOW_MILLIS }
            if (entry.value.isEmpty()) {
                ipIterator.remove()
            }
        }
    }

    private fun syncPairedHashCache(settings: AppSettings) {
        val now = nowMillisProvider()
        val activeHashes = settings.pairedDevices
            .filter { !it.isRevoked && !it.isExpired(now, settings.pairedTokenExpiryDays) }
            .map { it.tokenHash }
            .toSet()
        cachedActivePairedHashes.retainAll(activeHashes)
        cachedActivePairedHashes.addAll(activeHashes)
    }

    private fun extractRawBearerToken(headers: Map<String, String>): String? {
        val auth = headers["authorization"] ?: return null
        if (!auth.startsWith("Bearer ", ignoreCase = true)) return null
        return auth.substring(7).trim().takeIf { it.isNotEmpty() }
    }

    private fun isTokenLikelyAuthenticatedForBodyLimit(headers: Map<String, String>): Boolean {
        if (!headers[PeerHmacAuth.HEADER_SIGNATURE].isNullOrBlank()) return true
        val token = extractRawBearerToken(headers) ?: return false
        val sessionExpiry = sessions[token]
        if (sessionExpiry != null && nowMillisProvider() <= sessionExpiry) return true
        val hash = PairingManager.sha256Hex(token)
        return hash in cachedActivePairedHashes
    }

    private suspend fun authenticateRequest(headers: Map<String, String>, settings: AppSettings): AuthContext? {
        val token = extractRawBearerToken(headers) ?: return null
        val now = nowMillisProvider()

        val paired = PairingManager.findMatchingActiveDevice(
            rawToken = token,
            devices = settings.pairedDevices,
            nowEpochMillis = now,
            expiryDays = settings.pairedTokenExpiryDays
        )
        if (paired != null) {
            val lastTouch = lastTouchByDeviceId[paired.deviceId] ?: 0L
            if (now - lastTouch >= 60_000L) {
                lastTouchByDeviceId[paired.deviceId] = now
                settingsRepository.touchPairedDeviceLastSeen(paired.deviceId, now)
            }
            return AuthContext.Paired(paired)
        }

        if (settings.lanPinLoginEnabled) {
            val expiry = sessions[token]
            if (expiry != null) {
                if (now > expiry) {
                    sessions.remove(token)
                    return null
                }
                return AuthContext.PinSession(token)
            }
        }
        return null
    }

    private fun buildSettingsDto(s: AppSettings, deviceId: String): SettingsResponse {
        val now = nowMillisProvider()
        val manualUntil = s.silentScreen.manualSilentUntilEpochMillis
        val manualActive = manualUntil > now
        val dailySilentMap = listOf(Prayer.SUBUH, Prayer.ZUHUR, Prayer.ASHR, Prayer.MAGRIB, Prayer.ISYA).associate { p ->
            val cfg = s.silentScreen.dailyPrayers[p] ?: PrayerSilentConfig()
            p.name to DailySilentPrayerDto(
                enabled = cfg.enabled,
                anchor = cfg.anchor.name,
                leadMinutes = cfg.leadMinutes,
                durationMinutes = cfg.durationMinutes
            )
        }
        return SettingsResponse(
            deviceId = deviceId,
            deviceName = s.deviceName,
            roomGroup = s.roomGroup,
            configVersion = s.configVersion,
            mosqueName = s.mosqueName,
            mosqueSubtitle = s.mosqueSubtitle,
            mosqueLogoUri = s.mosqueLogoUri,
            mosqueIconPreset = s.mosqueIconPreset.name,
            mosqueFontPreset = s.mosqueFontPreset.name,
            appLanguage = s.appLanguage.name,
            cityName = s.cityName,
            zoneId = s.zoneId,
            calculationMethodName = s.calculationMethodName,
            madhab = s.madhab,
            themeMode = s.themeMode.name,
            scheduleDurationSeconds = s.scheduleDurationSeconds,
            hijriOffsetDays = s.hijriOffsetDays,
            hijriChangeAtMidnight = s.hijriChangeAtMidnight,
            is24HourFormat = s.is24HourFormat,
            showArabicPrayerNames = s.showArabicPrayerNames,
            showStrikeHighlight = s.showStrikeHighlight,
            jumuahMode = s.jumuahMode.name,
            jumuahTimes = s.jumuahTimes,
            iqamahSubuh = s.iqamahSubuh,
            iqamahZuhur = s.iqamahZuhur,
            iqamahAshr = s.iqamahAshr,
            iqamahMagrib = s.iqamahMagrib,
            iqamahIsya = s.iqamahIsya,
            islamicCalendarSlideEnabled = s.islamicCalendarSlideEnabled,
            islamicCalendarSlideDurationSeconds = s.islamicCalendarSlideDurationSeconds,
            quranVerseEnabled = s.quranVerseEnabled,
            quranShowTranslation = s.quranShowTranslation,
            quranVerseDurationSeconds = s.quranVerseDurationSeconds,
            silentJumuahEnabled = s.silentScreen.jumuahEnabled,
            silentJumuahLeadMinutes = s.silentScreen.jumuahLeadMinutes,
            silentJumuahDurationMinutes = s.silentScreen.jumuahDurationMinutes,
            silentDailyPrayers = dailySilentMap,
            manualSilentUntilEpochMillis = if (manualActive) manualUntil else 0L,
            manualSilentActive = manualActive,
            manualSilentRemainingMinutes = if (manualActive) (((manualUntil - now) + 59_999L) / 60_000L) else 0L
        )
    }

    private fun slideToDto(slide: AnnouncementSlide): SlideDto = SlideDto(
        id = slide.id,
        type = slide.type.name,
        title = slide.title,
        body = slide.body,
        lines = slide.effectiveLines().map { line ->
            SlideLineDto(text = line.text, align = line.align.name, fontSize = line.fontSize.name)
        },
        imageFile = slide.imageFile,
        durationSeconds = slide.durationSeconds,
        enabled = slide.enabled,
        textAlign = slide.textAlign.name,
        sortOrder = slide.sortOrder
    )

    private suspend fun handleGetFullConfig(): HttpResponse {
        val deviceId = settingsRepository.ensureDeviceId()
        val s = settingsRepository.appSettingsFlow.first()
        val slides = contentRepository.slidesFlow.value.slides.map(::slideToDto)
        return typedJsonResponse(
            200, "OK",
            FullConfigResponse(
                apiVersion = ApiConstants.API_VERSION,
                configVersion = s.configVersion,
                deviceId = deviceId,
                deviceName = s.deviceName,
                roomGroup = s.roomGroup,
                settings = buildSettingsDto(s, deviceId),
                slides = slides
            ),
            etag = "\"${s.configVersion}\""
        )
    }

    private suspend fun handlePutFullConfig(req: HttpRequest): HttpResponse {
        val current = settingsRepository.appSettingsFlow.first()
        checkIfMatch(req, current.configVersion)?.let { return it }

        val writeReq = PhoneControllerJson.decode<FullConfigWriteRequest>(req.body.toString(StandardCharsets.UTF_8))

        writeReq.settings?.let { patch ->
            applySettingsPatch(patch, current, bumpVersion = false)?.let { return it }
        }

        writeReq.slides?.let { incomingSlides ->
            val currentSettings = settingsRepository.appSettingsFlow.first()
            val enabledBuiltIns = BuiltInSlideRegistry.registrationsFor(currentSettings).count { it.enabled }
            val enabledAnnouncements = incomingSlides.count { it.enabled }
            if (SlideBudget.usedSlots(enabledAnnouncements, enabledBuiltIns) > SlideBudget.MAX_ROTATION_SLIDES) {
                return errorResponse(
                    409, "Conflict",
                    "slide_limit_reached",
                    "The slideshow is limited to 6 slides, including built-in slides. Disable or remove a slide first."
                )
            }
            val mapped = incomingSlides.mapIndexed { idx, dto ->
                val slideType = runCatching { SlideType.valueOf(dto.type.uppercase(Locale.ROOT)) }.getOrDefault(SlideType.TEXT)
                val parsedLines = parseSlideLineDtos(dto.lines)
                val textBody = if (parsedLines.isNotEmpty()) parsedLines.joinToString("\n") { it.text } else dto.body.trim()
                val align = runCatching { SlideTextAlign.valueOf(dto.textAlign.uppercase(Locale.ROOT)) }.getOrNull()
                    ?: parsedLines.firstOrNull()?.align
                    ?: SlideTextAlign.CENTER
                AnnouncementSlide(
                    id = dto.id.trim().ifEmpty { "slide_${UUID.randomUUID()}" },
                    type = slideType,
                    title = dto.title.trim(),
                    body = if (slideType == SlideType.TEXT) textBody else dto.body,
                    lines = if (slideType == SlideType.TEXT) parsedLines else emptyList(),
                    imageFile = dto.imageFile,
                    durationSeconds = dto.durationSeconds.coerceIn(3, 120),
                    enabled = dto.enabled,
                    textAlign = align,
                    sortOrder = idx
                )
            }
            val rev = contentRepository.slidesFlow.value.revision
            val res = contentRepository.replaceAll(rev, mapped)
            if (res.isFailure) {
                return errorResponse(400, "Bad Request", "replace_failed", res.exceptionOrNull()?.message ?: "Failed to replace slides")
            }
        }

        settingsRepository.bumpConfigVersion()
        return handleGetFullConfig()
    }

    private suspend fun handleGetSettings(): HttpResponse {
        val deviceId = settingsRepository.ensureDeviceId()
        val s = settingsRepository.appSettingsFlow.first()
        return typedJsonResponse(200, "OK", buildSettingsDto(s, deviceId), etag = "\"${s.configVersion}\"")
    }

    private suspend fun handleStartManualSilent(req: HttpRequest): HttpResponse {
        val body = PhoneControllerJson.decode<ManualSilentStartRequest>(req.body.toString(StandardCharsets.UTF_8))
        val durationMinutes = body.durationMinutes.coerceIn(1, 120)
        val until = nowMillisProvider() + durationMinutes * 60_000L
        settingsRepository.setManualSilentUntil(until)
        return typedJsonResponse(
            200, "OK",
            ManualSilentResponse(
                manualSilentActive = true,
                manualSilentUntilEpochMillis = until,
                manualSilentRemainingMinutes = durationMinutes.toLong(),
                durationMinutes = durationMinutes
            )
        )
    }

    private suspend fun handleStopManualSilent(): HttpResponse {
        settingsRepository.setManualSilentUntil(0L)
        return typedJsonResponse(
            200, "OK",
            ManualSilentResponse(
                manualSilentActive = false,
                manualSilentUntilEpochMillis = 0L,
                manualSilentRemainingMinutes = 0L
            )
        )
    }

    private suspend fun handlePatchSettings(req: HttpRequest): HttpResponse {
        val current = settingsRepository.appSettingsFlow.first()
        checkIfMatch(req, current.configVersion)?.let { return it }

        val patch = PhoneControllerJson.decode<SettingsPatchRequest>(req.body.toString(StandardCharsets.UTF_8))
        applySettingsPatch(patch, current, bumpVersion = true)?.let { return it }
        return handleGetSettings()
    }

    private suspend fun applySettingsPatch(
        patch: SettingsPatchRequest,
        current: AppSettings,
        bumpVersion: Boolean
    ): HttpResponse? {
        // Enforce 6-slide rotation budget if enabling built-in slides via PATCH
        val wantCalEnabled = patch.islamicCalendarSlideEnabled
        val wantQuranEnabled = patch.quranVerseEnabled
        if (wantCalEnabled == true || wantQuranEnabled == true) {
            val enabledAnnouncements = contentRepository.slidesFlow.value.slides.count { it.enabled }
            val nextCal = wantCalEnabled ?: current.islamicCalendarSlideEnabled
            val nextQuran = wantQuranEnabled ?: current.quranVerseEnabled
            val nextBuiltIns = (if (nextCal) 1 else 0) + (if (nextQuran) 1 else 0)
            val currentBuiltIns = (if (current.islamicCalendarSlideEnabled) 1 else 0) + (if (current.quranVerseEnabled) 1 else 0)
            if (nextBuiltIns > currentBuiltIns &&
                SlideBudget.usedSlots(enabledAnnouncements, nextBuiltIns) > SlideBudget.MAX_ROTATION_SLIDES
            ) {
                return errorResponse(
                    409, "Conflict",
                    "slide_limit_reached",
                    "The slideshow is limited to 6 slides, including built-in slides. Disable or remove a slide first."
                )
            }
        }

        if (patch.deviceName != null || patch.roomGroup != null) {
            settingsRepository.setDeviceIdentity(
                name = patch.deviceName ?: current.deviceName,
                roomGroup = patch.roomGroup ?: current.roomGroup
            )
        }

        val patchedMosqueName = patch.mosqueName
        if (patchedMosqueName != null && patchedMosqueName.trim().isBlank()) {
            return errorResponse(400, "Bad Request", "mosque_name_required", "Mosque name cannot be empty.")
        }
        if (patchedMosqueName != null || patch.mosqueSubtitle != null) {
            val newName = patchedMosqueName?.trim() ?: current.mosqueName
            val newSubtitle = patch.mosqueSubtitle ?: current.mosqueSubtitle
            settingsRepository.updateMosqueBranding(newName, newSubtitle, current.mosqueLogoUri)
        }
        patch.appLanguage?.let { raw ->
            runCatching { AppLanguage.valueOf(raw.uppercase(Locale.ROOT)) }
                .getOrNull()
                ?.takeIf { it in AppLanguage.ShippedLanguages }
                ?.let { settingsRepository.setAppLanguage(it) }
        }
        patch.mosqueIconPreset?.let { raw ->
            runCatching { MosqueIconPreset.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()?.let {
                settingsRepository.setMosqueIconPreset(it)
            }
        }
        patch.mosqueFontPreset?.let { raw ->
            runCatching { MosqueFontPreset.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()?.let {
                settingsRepository.setMosqueFontPreset(it)
            }
        }
        patch.themeMode?.let { raw ->
            runCatching { ThemeMode.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()?.let {
                settingsRepository.setThemeMode(it)
            }
        }
        patch.scheduleDurationSeconds?.let { settingsRepository.updateScheduleDuration(it.coerceIn(10, 600)) }
        patch.hijriOffsetDays?.let { settingsRepository.updateHijriOffset(it.coerceIn(-2, 2)) }
        patch.hijriChangeAtMidnight?.let { settingsRepository.setHijriChangeAtMidnight(it) }
        patch.is24HourFormat?.let { settingsRepository.set24HourFormat(it) }
        patch.showArabicPrayerNames?.let { settingsRepository.setShowArabicPrayerNames(it) }
        patch.showStrikeHighlight?.let { settingsRepository.setShowStrikeHighlight(it) }
        patch.jumuahMode?.let { raw ->
            runCatching { JumuahMode.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()?.let {
                settingsRepository.setJumuahMode(it)
            }
        }
        patch.jumuahTimes?.let { rawList ->
            val cleaned = rawList.map { it.trim() }.filter(String::isNotEmpty).take(3)
            if (cleaned.isNotEmpty()) {
                settingsRepository.setJumuahTimes(cleaned)
            }
        }
        patch.iqamahSubuh?.let { settingsRepository.setIqamahOffset(Prayer.SUBUH, it.coerceIn(0, 60)) }
        patch.iqamahZuhur?.let { settingsRepository.setIqamahOffset(Prayer.ZUHUR, it.coerceIn(0, 60)) }
        patch.iqamahAshr?.let { settingsRepository.setIqamahOffset(Prayer.ASHR, it.coerceIn(0, 60)) }
        patch.iqamahMagrib?.let { settingsRepository.setIqamahOffset(Prayer.MAGRIB, it.coerceIn(0, 60)) }
        patch.iqamahIsya?.let { settingsRepository.setIqamahOffset(Prayer.ISYA, it.coerceIn(0, 60)) }

        wantCalEnabled?.let { settingsRepository.setIslamicCalendarSlideEnabled(it) }
        patch.islamicCalendarSlideDurationSeconds?.let {
            settingsRepository.setIslamicCalendarSlideDurationSeconds(it.coerceIn(10, 60))
        }
        wantQuranEnabled?.let { settingsRepository.setQuranVerseEnabled(it) }
        patch.quranShowTranslation?.let { settingsRepository.setQuranShowTranslation(it) }
        patch.quranVerseDurationSeconds?.let {
            settingsRepository.setQuranVerseDurationSeconds(if (it <= 0) 0 else it.coerceIn(15, 60))
        }

        patch.silentJumuahEnabled?.let { settingsRepository.setSilentJumuahEnabled(it) }
        patch.silentJumuahLeadMinutes?.let { settingsRepository.setSilentJumuahLeadMinutes(it.coerceIn(0, 15)) }
        patch.silentJumuahDurationMinutes?.let { settingsRepository.setSilentJumuahDurationMinutes(it.coerceIn(30, 90)) }
        patch.silentDailyPrayers?.let { rawMap ->
            for ((k, dto) in rawMap) {
                val prayer = runCatching { Prayer.valueOf(k.uppercase(Locale.ROOT)) }.getOrNull() ?: continue
                val existing = current.silentScreen.dailyPrayers[prayer] ?: PrayerSilentConfig()
                val anchor = dto.anchor?.let {
                    runCatching { SilentAnchor.valueOf(it.uppercase(Locale.ROOT)) }.getOrNull()
                } ?: existing.anchor
                val updated = PrayerSilentConfig(
                    enabled = dto.enabled ?: existing.enabled,
                    anchor = anchor,
                    leadMinutes = (dto.leadMinutes ?: existing.leadMinutes).coerceIn(0, 10),
                    durationMinutes = (dto.durationMinutes ?: existing.durationMinutes).coerceIn(5, 45)
                )
                settingsRepository.setSilentDailyPrayerConfig(prayer, updated)
            }
        }

        if (bumpVersion) {
            settingsRepository.bumpConfigVersion()
        }
        return null
    }

    private suspend fun handleGetSlides(): HttpResponse {
        val container = contentRepository.slidesFlow.value
        val settings = settingsRepository.appSettingsFlow.first()
        val enabledBuiltIns = BuiltInSlideRegistry.registrationsFor(settings).count { it.enabled }
        val enabledAnnouncements = container.slides.count { it.enabled }
        return typedJsonResponse(
            200, "OK",
            SlidesResponse(
                revision = container.revision,
                configVersion = settings.configVersion,
                usedSlots = SlideBudget.usedSlots(enabledAnnouncements, enabledBuiltIns),
                maxRotationSlides = SlideBudget.MAX_ROTATION_SLIDES,
                slides = container.slides.map(::slideToDto)
            ),
            etag = "\"${settings.configVersion}\""
        )
    }

    private fun parseSlideLineDtos(raw: List<SlideLineDto>?): List<SlideTextLine> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.take(10).mapNotNull { entry ->
            val text = entry.text.replace("\n", " ").trimEnd().take(100)
            if (text.isBlank()) return@mapNotNull null
            val align = runCatching { SlideTextAlign.valueOf(entry.align.uppercase(Locale.ROOT)) }.getOrDefault(SlideTextAlign.CENTER)
            val fontSize = runCatching { SlideFontSize.valueOf(entry.fontSize.uppercase(Locale.ROOT)) }.getOrDefault(SlideFontSize.MEDIUM)
            SlideTextLine(text = text, align = align, fontSize = fontSize)
        }
    }

    private suspend fun handleAddTextSlide(req: HttpRequest): HttpResponse {
        val settings = settingsRepository.appSettingsFlow.first()
        checkIfMatch(req, settings.configVersion)?.let { return it }

        val body = PhoneControllerJson.decode<CreateTextSlideRequest>(req.body.toString(StandardCharsets.UTF_8))
        val requestedId = body.id?.trim()?.takeIf { it.isNotEmpty() && it.length <= 80 }
        return upsertTextSlide(
            slideId = requestedId ?: "slide_${UUID.randomUUID()}",
            titleRaw = body.title,
            bodyRaw = body.body,
            linesRaw = body.lines,
            durationRaw = body.durationSeconds,
            enabledRaw = body.enabled,
            textAlignRaw = body.textAlign,
            settings = settings,
            isExplicitPut = false
        )
    }

    private suspend fun handlePutSlide(slideId: String, req: HttpRequest): HttpResponse {
        if (slideId.isBlank() || slideId.length > 80) {
            return errorResponse(400, "Bad Request", "invalid_id", "Slide ID must be 1–80 characters")
        }
        val settings = settingsRepository.appSettingsFlow.first()
        checkIfMatch(req, settings.configVersion)?.let { return it }

        val body = PhoneControllerJson.decode<CreateTextSlideRequest>(req.body.toString(StandardCharsets.UTF_8))
        return upsertTextSlide(
            slideId = slideId,
            titleRaw = body.title,
            bodyRaw = body.body,
            linesRaw = body.lines,
            durationRaw = body.durationSeconds,
            enabledRaw = body.enabled,
            textAlignRaw = body.textAlign,
            settings = settings,
            isExplicitPut = true
        )
    }

    private suspend fun upsertTextSlide(
        slideId: String,
        titleRaw: String,
        bodyRaw: String,
        linesRaw: List<SlideLineDto>?,
        durationRaw: Int?,
        enabledRaw: Boolean?,
        textAlignRaw: String?,
        settings: AppSettings,
        isExplicitPut: Boolean
    ): HttpResponse {
        val title = titleRaw.trim()
        val parsedLines = parseSlideLineDtos(linesRaw)
        val rawText = bodyRaw.trim()
        val text = if (parsedLines.isNotEmpty()) parsedLines.joinToString("\n") { it.text } else rawText
        if (title.isEmpty() && text.isEmpty()) {
            return errorResponse(400, "Bad Request", "empty_slide", "Slide title or body is required")
        }

        val existingSlides = contentRepository.slidesFlow.value.slides
        val existing = existingSlides.firstOrNull { it.id == slideId }
        val enabled = enabledRaw ?: existing?.enabled ?: true
        val enabledBuiltIns = BuiltInSlideRegistry.registrationsFor(settings).count { it.enabled }
        val enabledAnnouncements = existingSlides.count { it.enabled }

        if (existing != null) {
            if (enabled && !existing.enabled && !SlideBudget.canEnableOrAddAnnouncement(enabledAnnouncements, enabledBuiltIns)) {
                return errorResponse(
                    409, "Conflict",
                    "slide_limit_reached",
                    "The slideshow is limited to 6 slides, including built-in slides. Disable or remove a slide first."
                )
            }
            val duration = (durationRaw ?: existing.durationSeconds).coerceIn(3, 120)
            val textAlign = textAlignRaw?.let { raw ->
                runCatching { SlideTextAlign.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()
            } ?: parsedLines.firstOrNull()?.align ?: existing.textAlign
            val updated = existing.copy(
                title = title,
                body = text,
                lines = parsedLines,
                durationSeconds = duration,
                enabled = enabled,
                textAlign = textAlign
            )
            // Idempotent no-op if identical on retry (avoids unnecessary configVersion bump)
            if (!isExplicitPut && updated == existing) {
                return typedJsonResponse(
                    200, "OK",
                    CreateSlideResponse(
                        id = existing.id,
                        enabled = existing.enabled,
                        slideCount = existingSlides.size,
                        configVersion = settings.configVersion
                    ),
                    etag = "\"${settings.configVersion}\""
                )
            }
            val res = contentRepository.updateSlide(updated)
            return if (res.isSuccess) {
                val nextVersion = settingsRepository.bumpConfigVersion()
                typedJsonResponse(
                    200, "OK",
                    CreateSlideResponse(
                        id = updated.id,
                        enabled = updated.enabled,
                        slideCount = res.getOrThrow().slides.size,
                        configVersion = nextVersion
                    ),
                    etag = "\"$nextVersion\""
                )
            } else {
                errorResponse(400, "Bad Request", "update_failed", res.exceptionOrNull()?.message ?: "Failed to update slide")
            }
        }

        if (enabled && !SlideBudget.canEnableOrAddAnnouncement(enabledAnnouncements, enabledBuiltIns)) {
            return errorResponse(
                409, "Conflict",
                "slide_limit_reached",
                "The slideshow is limited to 6 slides, including built-in slides. Disable or remove a slide first."
            )
        }
        val duration = (durationRaw ?: 15).coerceIn(3, 120)
        val textAlign = textAlignRaw?.let { raw ->
            runCatching { SlideTextAlign.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()
        } ?: parsedLines.firstOrNull()?.align ?: SlideTextAlign.CENTER
        val nextOrder = (existingSlides.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val slide = AnnouncementSlide(
            id = slideId,
            type = SlideType.TEXT,
            title = title,
            body = text,
            lines = parsedLines,
            durationSeconds = duration,
            enabled = enabled,
            textAlign = textAlign,
            sortOrder = nextOrder
        )
        val result = contentRepository.addSlide(slide)
        return if (result.isSuccess) {
            val nextVersion = settingsRepository.bumpConfigVersion()
            typedJsonResponse(
                201, "Created",
                CreateSlideResponse(
                    id = slide.id,
                    enabled = slide.enabled,
                    slideCount = result.getOrThrow().slides.size,
                    configVersion = nextVersion
                ),
                etag = "\"$nextVersion\""
            )
        } else {
            errorResponse(400, "Bad Request", "add_failed", result.exceptionOrNull()?.message ?: "Failed to add slide")
        }
    }

    private suspend fun handleAddImageSlide(req: HttpRequest): HttpResponse {
        val settings = settingsRepository.appSettingsFlow.first()
        checkIfMatch(req, settings.configVersion)?.let { return it }

        if (req.body.isEmpty()) {
            return errorResponse(400, "Bad Request", "empty_image", "Image payload is empty")
        }
        val enabled = req.queryParams["enabled"]?.equals("false", ignoreCase = true)?.not() ?: true
        val enabledBuiltIns = BuiltInSlideRegistry.registrationsFor(settings).count { it.enabled }
        val enabledAnnouncements = contentRepository.slidesFlow.value.slides.count { it.enabled }
        if (enabled && !SlideBudget.canEnableOrAddAnnouncement(enabledAnnouncements, enabledBuiltIns)) {
            return errorResponse(
                409, "Conflict",
                "slide_limit_reached",
                "The slideshow is limited to 6 slides, including built-in slides. Disable or remove a slide first."
            )
        }
        val requestedId = req.queryParams["id"]?.trim()?.takeIf { it.isNotEmpty() && it.length <= 80 }
        if (requestedId != null) {
            val existing = contentRepository.slidesFlow.value.slides.firstOrNull { it.id == requestedId }
            if (existing != null) {
                return typedJsonResponse(
                    200, "OK",
                    CreateSlideResponse(
                        id = existing.id,
                        enabled = existing.enabled,
                        imageFile = existing.imageFile,
                        configVersion = settings.configVersion
                    ),
                    etag = "\"${settings.configVersion}\""
                )
            }
        }
        val title = req.queryParams["title"]?.takeIf { it.isNotBlank() } ?: "Poster"
        val duration = (req.queryParams["duration"]?.toIntOrNull() ?: 15).coerceIn(3, 120)
        val imported = slideImageImporter.importImage(req.body, ImageTargetType.SLIDE)
        val processed = imported.getOrElse { err ->
            return errorResponse(400, "Bad Request", "image_invalid", err.message ?: "Invalid image")
        }
        val nextOrder = (contentRepository.slidesFlow.value.slides.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val slide = AnnouncementSlide(
            id = requestedId ?: "slide_${UUID.randomUUID()}",
            type = SlideType.IMAGE,
            title = title,
            imageFile = processed.relativeFileName,
            durationSeconds = duration,
            enabled = enabled,
            sortOrder = nextOrder
        )
        val added = contentRepository.addSlide(slide)
        return if (added.isSuccess) {
            val nextVersion = settingsRepository.bumpConfigVersion()
            typedJsonResponse(
                201, "Created",
                CreateSlideResponse(
                    id = slide.id,
                    enabled = slide.enabled,
                    imageFile = processed.relativeFileName,
                    configVersion = nextVersion
                ),
                etag = "\"$nextVersion\""
            )
        } else {
            errorResponse(400, "Bad Request", "add_failed", added.exceptionOrNull()?.message ?: "Failed to add slide")
        }
    }

    private suspend fun handlePatchSlide(slideId: String, req: HttpRequest): HttpResponse {
        val settings = settingsRepository.appSettingsFlow.first()
        checkIfMatch(req, settings.configVersion)?.let { return it }

        val current = contentRepository.slidesFlow.value.slides.firstOrNull { it.id == slideId }
            ?: return errorResponse(404, "Not Found", "not_found", "Slide not found")
        val patch = PhoneControllerJson.decode<PatchSlideRequest>(req.body.toString(StandardCharsets.UTF_8))
        val targetEnabled = patch.enabled ?: current.enabled
        if (targetEnabled && !current.enabled) {
            val enabledBuiltIns = BuiltInSlideRegistry.registrationsFor(settings).count { it.enabled }
            val enabledAnnouncements = contentRepository.slidesFlow.value.slides.count { it.enabled }
            if (!SlideBudget.canEnableOrAddAnnouncement(enabledAnnouncements, enabledBuiltIns)) {
                return errorResponse(
                    409, "Conflict",
                    "slide_limit_reached",
                    "The slideshow is limited to 6 slides, including built-in slides. Disable or remove a slide first."
                )
            }
        }
        val targetTitle = patch.title?.trim() ?: current.title
        val hasLinesPatch = patch.lines != null
        val targetLines = if (hasLinesPatch) parseSlideLineDtos(patch.lines) else current.lines
        val targetBody = if (hasLinesPatch) {
            targetLines.joinToString("\n") { it.text }
        } else {
            patch.body?.trim() ?: current.body
        }
        val targetDuration = (patch.durationSeconds ?: current.durationSeconds).coerceIn(3, 120)
        val targetAlign = patch.textAlign?.let { raw ->
            runCatching { SlideTextAlign.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()
        } ?: targetLines.firstOrNull()?.align ?: current.textAlign
        val updated = current.copy(
            title = targetTitle,
            body = if (current.type == SlideType.TEXT) targetBody else current.body,
            lines = if (current.type == SlideType.TEXT) targetLines else current.lines,
            durationSeconds = targetDuration,
            enabled = targetEnabled,
            textAlign = targetAlign
        )
        val res = contentRepository.updateSlide(updated)
        return if (res.isSuccess) {
            val nextVersion = settingsRepository.bumpConfigVersion()
            typedJsonResponse(
                200, "OK",
                PatchSlideResponse(
                    id = updated.id,
                    title = updated.title,
                    body = updated.body,
                    durationSeconds = updated.durationSeconds,
                    enabled = updated.enabled,
                    configVersion = nextVersion
                ),
                etag = "\"$nextVersion\""
            )
        } else {
            errorResponse(400, "Bad Request", "update_failed", res.exceptionOrNull()?.message ?: "Failed to update slide")
        }
    }

    private suspend fun handleDeleteSlide(slideId: String): HttpResponse {
        if (slideId.isBlank()) {
            return errorResponse(400, "Bad Request", "missing_id", "Slide ID is required")
        }
        val result = contentRepository.deleteSlide(slideId)
        return if (result.isSuccess) {
            val nextVersion = settingsRepository.bumpConfigVersion()
            typedJsonResponse(
                200, "OK",
                DeleteSlideResponse(
                    deleted = slideId,
                    slideCount = result.getOrThrow().slides.size,
                    configVersion = nextVersion
                ),
                etag = "\"$nextVersion\""
            )
        } else {
            errorResponse(400, "Bad Request", "delete_failed", result.exceptionOrNull()?.message ?: "Failed to delete slide")
        }
    }

    private suspend fun handleUploadLogo(req: HttpRequest): HttpResponse {
        val current = settingsRepository.appSettingsFlow.first()
        checkIfMatch(req, current.configVersion)?.let { return it }

        if (req.body.isEmpty()) {
            return errorResponse(400, "Bad Request", "empty_logo", "Logo image payload is empty")
        }
        val processed = logoImageImporter.importImage(req.body, ImageTargetType.LOGO).getOrElse { err ->
            return errorResponse(400, "Bad Request", "logo_invalid", err.message ?: "Invalid logo image")
        }
        settingsRepository.setMosqueIconPreset(MosqueIconPreset.CUSTOM)
        settingsRepository.updateMosqueBranding(current.mosqueName, current.mosqueSubtitle, processed.relativeFileName)
        val nextVersion = settingsRepository.bumpConfigVersion()
        return typedJsonResponse(
            200, "OK",
            LogoUploadResponse(logoFile = processed.relativeFileName, configVersion = nextVersion),
            etag = "\"$nextVersion\""
        )
    }

    private inline fun <reified T> typedJsonResponse(
        code: Int,
        text: String,
        payload: T,
        etag: String? = null
    ): HttpResponse {
        return HttpResponse(
            statusCode = code,
            statusText = text,
            contentType = "application/json; charset=utf-8",
            body = PhoneControllerJson.encode(payload).toByteArray(StandardCharsets.UTF_8),
            etag = etag
        )
    }

    private fun errorResponse(code: Int, text: String, error: String, message: String): HttpResponse {
        return typedJsonResponse(code, text, ApiErrorResponse(error = error, message = message))
    }

    private fun readHttpRequest(socket: Socket, input: InputStream, remoteIp: String): ReadRequestOutcome {
        val headerDeadline = System.currentTimeMillis() + headerDeadlineMillis
        val buffered = BufferedInputStream(input)

        fun applyRemainingTimeout(deadlineMillis: Long): Boolean {
            val remaining = deadlineMillis - System.currentTimeMillis()
            if (remaining <= 0L) return false
            socket.soTimeout = remaining.coerceIn(1L, 15_000L).toInt()
            return true
        }

        return try {
            if (!applyRemainingTimeout(headerDeadline)) {
                return ReadRequestOutcome.Error(errorResponse(408, "Request Timeout", "request_timeout", "Request headers timed out"))
            }
            val requestLine = readBoundedLine(buffered, MAX_REQUEST_LINE_BYTES, headerDeadline) {
                applyRemainingTimeout(headerDeadline)
            } ?: return ReadRequestOutcome.Closed
            if (requestLine == LINE_TIMED_OUT) {
                return ReadRequestOutcome.Error(errorResponse(408, "Request Timeout", "request_timeout", "Request headers timed out"))
            }
            if (requestLine == LINE_TOO_LONG) {
                return ReadRequestOutcome.Error(errorResponse(414, "URI Too Long", "uri_too_long", "Request line too long"))
            }

            val parts = requestLine.trim().split(" ")
            if (parts.size < 2) {
                return ReadRequestOutcome.Error(errorResponse(400, "Bad Request", "bad_request", "Invalid HTTP request line"))
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
                val line = readBoundedLine(buffered, MAX_HEADER_LINE_BYTES, headerDeadline) {
                    applyRemainingTimeout(headerDeadline)
                } ?: break
                if (line == LINE_TIMED_OUT) {
                    return ReadRequestOutcome.Error(errorResponse(408, "Request Timeout", "request_timeout", "Request headers timed out"))
                }
                if (line == LINE_TOO_LONG) {
                    return ReadRequestOutcome.Error(errorResponse(431, "Request Header Fields Too Large", "header_too_large", "Header line too large"))
                }
                if (line.isEmpty()) break
                headerCount++
                if (headerCount > MAX_HEADERS) {
                    return ReadRequestOutcome.Error(errorResponse(431, "Request Header Fields Too Large", "too_many_headers", "Too many headers"))
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
                    errorResponse(400, "Bad Request", "invalid_content_length", "Invalid Content-Length")
                )
            } else 0

            if (contentLength < 0) {
                return ReadRequestOutcome.Error(errorResponse(400, "Bad Request", "invalid_content_length", "Negative Content-Length"))
            }

            val isAuthed = isTokenLikelyAuthenticatedForBodyLimit(headers)
            val isUploadRoute = path == "/api/v1/logo" || path == "/api/v1/slides/image"
            val maxAllowedBytes = when {
                !isAuthed -> MAX_PRE_AUTH_BODY_BYTES
                isUploadRoute -> MAX_UPLOAD_BODY_BYTES
                else -> MAX_JSON_BODY_BYTES
            }
            if (contentLength > maxAllowedBytes) {
                return ReadRequestOutcome.Error(errorResponse(413, "Payload Too Large", "payload_too_large", "Request body exceeds limit"))
            }

            val body = if (contentLength > 0) {
                val bodyDeadline = System.currentTimeMillis() +
                    if (isUploadRoute) uploadBodyDeadlineMillis else jsonBodyDeadlineMillis
                val buf = ByteArray(contentLength)
                var offset = 0
                while (offset < contentLength) {
                    if (!applyRemainingTimeout(bodyDeadline)) {
                        return ReadRequestOutcome.Error(errorResponse(408, "Request Timeout", "request_timeout", "Request body timed out"))
                    }
                    val read = buffered.read(buf, offset, contentLength - offset)
                    if (read <= 0) break
                    offset += read
                }
                if (offset < contentLength) {
                    return ReadRequestOutcome.Error(errorResponse(400, "Bad Request", "truncated_body", "Request body shorter than Content-Length"))
                }
                buf
            } else {
                ByteArray(0)
            }

            ReadRequestOutcome.Success(
                HttpRequest(
                    method = method,
                    path = path,
                    queryParams = queryParams,
                    headers = headers,
                    body = body,
                    remoteAddress = remoteIp
                )
            )
        } catch (_: SocketTimeoutException) {
            ReadRequestOutcome.Error(errorResponse(408, "Request Timeout", "request_timeout", "Request timed out"))
        }
    }

    private val LINE_TOO_LONG = "\u0000__LINE_TOO_LONG__"
    private val LINE_TIMED_OUT = "\u0000__LINE_TIMED_OUT__"

    private fun readBoundedLine(
        input: InputStream,
        maxBytes: Int,
        deadlineMillis: Long,
        refreshTimeout: () -> Boolean
    ): String? {
        if (!refreshTimeout()) {
            return LINE_TIMED_OUT
        }
        val out = ByteArrayOutputStream()
        var prev = -1
        while (true) {
            if (System.currentTimeMillis() >= deadlineMillis) {
                return LINE_TIMED_OUT
            }
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
            response.etag?.let { append("ETag: $it\r\n") }
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
