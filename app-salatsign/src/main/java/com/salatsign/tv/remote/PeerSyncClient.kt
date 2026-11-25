// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.remote

import android.util.Log
import com.salatsign.tv.data.api.PeerDeliveryStatusDto
import com.salatsign.tv.data.api.PeerJoinRequest
import com.salatsign.tv.data.api.PeerJoinResponse
import com.salatsign.tv.data.api.PeerJoinSecretPayload
import com.salatsign.tv.data.api.PeerNoticeMessage
import com.salatsign.tv.data.api.PeerSecretRotatePayload
import com.salatsign.tv.data.api.PeerSecretRotateRequest
import com.salatsign.tv.data.api.PeerSyncAckResponse
import com.salatsign.tv.data.api.PeerSyncPayload
import com.salatsign.tv.data.api.PhoneControllerJson
import com.salatsign.tv.security.GroupJoinManager
import com.salatsign.tv.security.PeerAssetValidator
import com.salatsign.tv.security.PeerHmacAuth
import com.salatsign.tv.security.PeerJoinCrypto
import com.salatsign.tv.security.PeerTvRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Pure-JVM HTTP client for HMAC-signed TV-to-TV group sync, ECDH-encrypted join,
 * authenticated asset pull, secret rotation, and Urgent Notice fan-out (Phase 3 + Review 4 H1/H2/H3/M3/L3).
 * Uses java.net.HttpURLConnection (zero extra dependencies, API 26+).
 */
class PeerSyncClient(
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val connectTimeoutMs: Int = 3_500,
    private val readTimeoutMs: Int = 8_000
) {
    companion object {
        private const val TAG = "PeerSyncClient"
        private const val MAX_JSON_RESPONSE_BYTES = 512 * 1024
    }

    /**
     * Review 4 H1: Joins a TV group using an ephemeral ECDH (P-256) key exchange bound to the
     * one-time join code. The group secret is AES-256-GCM encrypted in transit.
     */
    suspend fun joinGroup(
        targetHost: String,
        targetPort: Int,
        joinCode: String,
        selfPeer: PeerTvRecord
    ): Result<PeerJoinSecretPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedCode = GroupJoinManager.normalizeCode(joinCode)
            require(normalizedCode.length == GroupJoinManager.CODE_LENGTH) {
                "Join code must be ${GroupJoinManager.CODE_LENGTH} characters"
            }
            val clientKeyPair = PeerJoinCrypto.generateEphemeralKeyPair()
            val clientPubB64 = PeerJoinCrypto.encodePublicKey(clientKeyPair)
            // Review 5 R5-2: Prove knowledge of join code via PBKDF2-HMAC proof without sending code in cleartext
            val codeProof = PeerJoinCrypto.computeJoinCodeProof(
                normalizedJoinCode = normalizedCode,
                deviceId = selfPeer.deviceId,
                clientPublicKeyBase64 = clientPubB64
            )
            val req = PeerJoinRequest(
                codeProofHex = codeProof,
                deviceId = selfPeer.deviceId,
                deviceName = selfPeer.deviceName,
                roomGroup = selfPeer.roomGroup,
                host = selfPeer.lastKnownHost,
                port = selfPeer.port,
                clientPublicKeyBase64 = clientPubB64,
                identityPublicKeyBase64 = selfPeer.publicKeyBase64
            )
            val body = PhoneControllerJson.encode(req).toByteArray(StandardCharsets.UTF_8)
            val conn = (URL("http://$targetHost:$targetPort/api/v1/peer/join").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            try {
                conn.outputStream.use { it.write(body) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val respBytes = stream?.use { readBoundedBytes(it, MAX_JSON_RESPONSE_BYTES) } ?: ByteArray(0)
                val text = respBytes.toString(StandardCharsets.UTF_8)
                if (code !in 200..299) {
                    error("Peer rejected join ($code): $text")
                }
                val envelope = PhoneControllerJson.decode<PeerJoinResponse>(text)
                val plaintext = PeerJoinCrypto.decryptFromServer(
                    clientKeyPair = clientKeyPair,
                    serverPublicKeyBase64 = envelope.serverPublicKeyBase64,
                    ivBase64 = envelope.ivBase64,
                    ciphertextBase64 = envelope.ciphertextBase64,
                    normalizedJoinCode = normalizedCode
                )
                PhoneControllerJson.decode<PeerJoinSecretPayload>(plaintext.toString(StandardCharsets.UTF_8))
            } finally {
                conn.disconnect()
            }
        }
    }

    /**
     * Review 4 H2: Verifies that a candidate mDNS host:port actually holds the group secret
     * and matches [expectedPeerId] via a signed challenge/response before updating peer routing.
     */
    suspend fun verifyPeerAddress(
        expectedPeerId: String,
        candidateHost: String,
        candidatePort: Int,
        groupSecretHex: String,
        selfDeviceId: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (groupSecretHex.isBlank() || expectedPeerId.isBlank() || candidateHost.isBlank()) return@withContext false
        runCatching {
            val path = "/api/v1/peer/ping"
            val ts = nowMillisProvider()
            val nonce = PeerHmacAuth.generateNonce()
            val sig = PeerHmacAuth.computeSignature(
                groupSecretHex = groupSecretHex,
                method = "GET",
                path = path,
                peerId = selfDeviceId,
                targetDeviceId = expectedPeerId,
                timestampMillis = ts,
                nonce = nonce,
                body = ByteArray(0)
            )
            val conn = (URL("http://$candidateHost:$candidatePort$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty(PeerHmacAuth.HEADER_PEER_ID, selfDeviceId)
                setRequestProperty(PeerHmacAuth.HEADER_TARGET_ID, expectedPeerId)
                setRequestProperty(PeerHmacAuth.HEADER_TIMESTAMP, ts.toString())
                setRequestProperty(PeerHmacAuth.HEADER_NONCE, nonce)
                setRequestProperty(PeerHmacAuth.HEADER_SIGNATURE, sig)
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) return@runCatching false
                val respBytes = conn.inputStream.use { readBoundedBytes(it, MAX_JSON_RESPONSE_BYTES) } ?: return@runCatching false
                verifySignedResponse(conn, groupSecretHex, expectedPeerId, nonce, respBytes)
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(false)
    }

    suspend fun pushConfigToPeers(
        peers: List<PeerTvRecord>,
        payload: PeerSyncPayload,
        groupSecretHex: String,
        selfDeviceId: String
    ): List<PeerDeliveryStatusDto> = coroutineScope {
        if (groupSecretHex.isBlank() || peers.isEmpty()) return@coroutineScope emptyList()
        val body = PhoneControllerJson.encode(payload).toByteArray(StandardCharsets.UTF_8)
        peers.map { peer ->
            async(Dispatchers.IO) {
                sendSignedPost(peer, "/api/v1/peer/sync", body, groupSecretHex, selfDeviceId, parseSyncAck = true)
            }
        }.awaitAll()
    }

    suspend fun pushNoticeToPeers(
        peers: List<PeerTvRecord>,
        notice: PeerNoticeMessage,
        groupSecretHex: String,
        selfDeviceId: String
    ): List<PeerDeliveryStatusDto> = coroutineScope {
        if (groupSecretHex.isBlank() || peers.isEmpty()) return@coroutineScope emptyList()
        val body = PhoneControllerJson.encode(notice).toByteArray(StandardCharsets.UTF_8)
        peers.map { peer ->
            async(Dispatchers.IO) {
                sendSignedPost(peer, "/api/v1/peer/notice", body, groupSecretHex, selfDeviceId)
            }
        }.awaitAll()
    }

    /**
     * Review 5 R5-1: ECIES-encrypts the rotated group secret individually to each remaining peer's
     * long-term EC public key so neither LAN sniffers nor a removed TV can decrypt the new secret.
     */
    suspend fun rotateSecretOnPeers(
        peers: List<PeerTvRecord>,
        payload: PeerSecretRotatePayload,
        oldGroupSecretHex: String,
        selfDeviceId: String
    ): List<PeerDeliveryStatusDto> = coroutineScope {
        if (oldGroupSecretHex.isBlank() || peers.isEmpty()) return@coroutineScope emptyList()
        val plaintext = PhoneControllerJson.encode(payload).toByteArray(StandardCharsets.UTF_8)
        peers.map { peer ->
            async(Dispatchers.IO) {
                if (peer.publicKeyBase64.isBlank()) {
                    PeerDeliveryStatusDto(
                        deviceId = peer.deviceId,
                        deviceName = peer.deviceName.ifBlank { peer.lastKnownHost },
                        status = "failed",
                        message = "Peer missing EC key — re-join required"
                    )
                } else {
                    val env = runCatching {
                        PeerJoinCrypto.encryptForRecipientPublicKey(
                            recipientPublicKeyBase64 = peer.publicKeyBase64,
                            recipientDeviceId = peer.deviceId,
                            plaintextUtf8 = plaintext
                        )
                    }.getOrElse { err ->
                        return@async PeerDeliveryStatusDto(
                            deviceId = peer.deviceId,
                            deviceName = peer.deviceName.ifBlank { peer.lastKnownHost },
                            status = "failed",
                            message = "ECIES encrypt failed: ${err.message}"
                        )
                    }
                    val req = PeerSecretRotateRequest(
                        ephemeralPublicKeyBase64 = env.serverPublicKeyBase64,
                        ivBase64 = env.ivBase64,
                        ciphertextBase64 = env.ciphertextBase64
                    )
                    val body = PhoneControllerJson.encode(req).toByteArray(StandardCharsets.UTF_8)
                    sendSignedPost(peer, "/api/v1/peer/rotate", body, oldGroupSecretHex, selfDeviceId)
                }
            }
        }.awaitAll()
    }

    /**
     * Review 5 R5-5: Best-effort signed notification to peers before a TV leaves the group locally.
     */
    suspend fun notifyLeaveToPeers(
        peers: List<PeerTvRecord>,
        groupSecretHex: String,
        selfDeviceId: String
    ): List<PeerDeliveryStatusDto> = coroutineScope {
        if (groupSecretHex.isBlank() || peers.isEmpty()) return@coroutineScope emptyList()
        val body = "{}".toByteArray(StandardCharsets.UTF_8)
        peers.map { peer ->
            async(Dispatchers.IO) {
                sendSignedPost(peer, "/api/v1/peer/leave", body, groupSecretHex, selfDeviceId)
            }
        }.awaitAll()
    }

    suspend fun pullConfigFromPeer(
        peer: PeerTvRecord,
        groupSecretHex: String,
        selfDeviceId: String
    ): Result<PeerSyncPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val path = "/api/v1/peer/sync"
            val ts = nowMillisProvider()
            val nonce = PeerHmacAuth.generateNonce()
            val sig = PeerHmacAuth.computeSignature(
                groupSecretHex = groupSecretHex,
                method = "GET",
                path = path,
                peerId = selfDeviceId,
                targetDeviceId = peer.deviceId,
                timestampMillis = ts,
                nonce = nonce,
                body = ByteArray(0)
            )
            val conn = (URL("http://${peer.lastKnownHost}:${peer.port}$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty(PeerHmacAuth.HEADER_PEER_ID, selfDeviceId)
                setRequestProperty(PeerHmacAuth.HEADER_TARGET_ID, peer.deviceId)
                setRequestProperty(PeerHmacAuth.HEADER_TIMESTAMP, ts.toString())
                setRequestProperty(PeerHmacAuth.HEADER_NONCE, nonce)
                setRequestProperty(PeerHmacAuth.HEADER_SIGNATURE, sig)
            }
            try {
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val respBytes = stream?.use { readBoundedBytes(it, MAX_JSON_RESPONSE_BYTES) }
                    ?: error("Response exceeded size limit")
                if (code !in 200..299) error("HTTP $code")
                // Review 4 H2: Authenticate peer response before adopting config
                if (!verifySignedResponse(conn, groupSecretHex, peer.deviceId, nonce, respBytes)) {
                    error("Unauthenticated or tampered peer response from ${peer.deviceId}")
                }
                PhoneControllerJson.decode<PeerSyncPayload>(respBytes.toString(StandardCharsets.UTF_8))
            } finally {
                conn.disconnect()
            }
        }
    }

    /**
     * Review 4 H2 & H3: Downloads an asset only from an authenticated known [peer] with strict filename
     * validation, hard 10 MB size cap, response HMAC verification, and optional SHA-256 verification.
     */
    suspend fun downloadPeerAsset(
        peer: PeerTvRecord,
        kind: String, // "slide" or "logo"
        fileName: String,
        groupSecretHex: String,
        selfDeviceId: String,
        expectedSha256: String? = null
    ): ByteArray? = withContext(Dispatchers.IO) {
        if (!PeerAssetValidator.isSafeFileName(fileName)) return@withContext null
        if (kind != "slide" && kind != "logo") return@withContext null
        runCatching {
            val path = "/api/v1/peer/asset/$kind/$fileName"
            val ts = nowMillisProvider()
            val nonce = PeerHmacAuth.generateNonce()
            val sig = PeerHmacAuth.computeSignature(
                groupSecretHex = groupSecretHex,
                method = "GET",
                path = path,
                peerId = selfDeviceId,
                targetDeviceId = peer.deviceId,
                timestampMillis = ts,
                nonce = nonce,
                body = ByteArray(0)
            )
            val conn = (URL("http://${peer.lastKnownHost}:${peer.port}$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = 15_000
                setRequestProperty(PeerHmacAuth.HEADER_PEER_ID, selfDeviceId)
                setRequestProperty(PeerHmacAuth.HEADER_TARGET_ID, peer.deviceId)
                setRequestProperty(PeerHmacAuth.HEADER_TIMESTAMP, ts.toString())
                setRequestProperty(PeerHmacAuth.HEADER_NONCE, nonce)
                setRequestProperty(PeerHmacAuth.HEADER_SIGNATURE, sig)
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) return@runCatching null
                val bytes = conn.inputStream.use { readBoundedBytes(it, PeerAssetValidator.MAX_PEER_ASSET_BYTES) }
                    ?: return@runCatching null
                if (!verifySignedResponse(conn, groupSecretHex, peer.deviceId, nonce, bytes)) {
                    Log.w(TAG, "Rejected unauthenticated asset response for $fileName from ${peer.deviceId}")
                    return@runCatching null
                }
                if (!expectedSha256.isNullOrBlank()) {
                    val actualSha = PeerAssetValidator.sha256Hex(bytes)
                    if (!actualSha.equals(expectedSha256.trim(), ignoreCase = true)) {
                        Log.w(TAG, "Rejected asset $fileName: SHA-256 mismatch")
                        return@runCatching null
                    }
                }
                bytes
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun sendSignedPost(
        peer: PeerTvRecord,
        path: String,
        body: ByteArray,
        groupSecretHex: String,
        selfDeviceId: String,
        parseSyncAck: Boolean = false
    ): PeerDeliveryStatusDto {
        return try {
            val ts = nowMillisProvider()
            val nonce = PeerHmacAuth.generateNonce()
            val sig = PeerHmacAuth.computeSignature(
                groupSecretHex = groupSecretHex,
                method = "POST",
                path = path,
                peerId = selfDeviceId,
                targetDeviceId = peer.deviceId,
                timestampMillis = ts,
                nonce = nonce,
                body = body
            )
            val conn = (URL("http://${peer.lastKnownHost}:${peer.port}$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty(PeerHmacAuth.HEADER_PEER_ID, selfDeviceId)
                setRequestProperty(PeerHmacAuth.HEADER_TARGET_ID, peer.deviceId)
                setRequestProperty(PeerHmacAuth.HEADER_TIMESTAMP, ts.toString())
                setRequestProperty(PeerHmacAuth.HEADER_NONCE, nonce)
                setRequestProperty(PeerHmacAuth.HEADER_SIGNATURE, sig)
            }
            try {
                conn.outputStream.use { it.write(body) }
                val code = conn.responseCode
                val respBytes = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.use { readBoundedBytes(it, MAX_JSON_RESPONSE_BYTES) } ?: ByteArray(0)
                if (code in 200..299) {
                    if (!verifySignedResponse(conn, groupSecretHex, peer.deviceId, nonce, respBytes)) {
                        PeerDeliveryStatusDto(
                            deviceId = peer.deviceId,
                            deviceName = peer.deviceName.ifBlank { peer.lastKnownHost },
                            status = "failed",
                            message = "Unauthenticated peer response"
                        )
                    } else if (parseSyncAck) {
                        // Review 5 R5-3: Report actual post-apply adoption and missing assets from peer
                        val ack = runCatching {
                            PhoneControllerJson.decode<PeerSyncAckResponse>(respBytes.toString(StandardCharsets.UTF_8))
                        }.getOrNull()
                        if (ack == null) {
                            PeerDeliveryStatusDto(
                                deviceId = peer.deviceId,
                                deviceName = peer.deviceName.ifBlank { peer.lastKnownHost },
                                status = "delivered"
                            )
                        } else {
                            val st = when {
                                ack.missingAssets.isNotEmpty() -> "missing_assets"
                                !ack.adopted -> "not_adopted"
                                else -> "delivered"
                            }
                            val msg = when {
                                ack.missingAssets.isNotEmpty() -> "Missing ${ack.missingAssets.size} image(s)"
                                !ack.adopted -> "Peer at v${ack.configVersion}"
                                else -> null
                            }
                            PeerDeliveryStatusDto(
                                deviceId = peer.deviceId,
                                deviceName = peer.deviceName.ifBlank { peer.lastKnownHost },
                                status = st,
                                message = msg,
                                configVersion = ack.configVersion,
                                adopted = ack.adopted,
                                missingAssets = ack.missingAssets
                            )
                        }
                    } else {
                        PeerDeliveryStatusDto(
                            deviceId = peer.deviceId,
                            deviceName = peer.deviceName.ifBlank { peer.lastKnownHost },
                            status = "delivered"
                        )
                    }
                } else {
                    PeerDeliveryStatusDto(
                        deviceId = peer.deviceId,
                        deviceName = peer.deviceName.ifBlank { peer.lastKnownHost },
                        status = "failed",
                        message = "HTTP $code"
                    )
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Peer push to ${peer.lastKnownHost}:${peer.port}$path failed: ${e.message}")
            PeerDeliveryStatusDto(
                deviceId = peer.deviceId,
                deviceName = peer.deviceName.ifBlank { peer.lastKnownHost },
                status = "failed",
                message = e.message ?: "Unreachable"
            )
        }
    }

    private fun verifySignedResponse(
        conn: HttpURLConnection,
        groupSecretHex: String,
        expectedResponderDeviceId: String,
        requestNonce: String,
        responseBytes: ByteArray
    ): Boolean {
        val respTs = conn.getHeaderField(PeerHmacAuth.HEADER_RESP_TIMESTAMP)
        val respSig = conn.getHeaderField(PeerHmacAuth.HEADER_RESP_SIGNATURE)
        return PeerHmacAuth.verifyResponse(
            groupSecretHex = groupSecretHex,
            expectedResponderDeviceId = expectedResponderDeviceId,
            requestNonce = requestNonce,
            responseTimestampHeader = respTs,
            responseSignatureHeader = respSig,
            responseBody = responseBytes,
            nowEpochMillis = nowMillisProvider()
        )
    }

    private fun readBoundedBytes(input: InputStream, maxBytes: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val read = input.read(buf)
            if (read <= 0) break
            total += read
            if (total > maxBytes) return null
            out.write(buf, 0, read)
        }
        return out.toByteArray()
    }
}
