// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.remote

import android.util.Log
import com.salatsign.tv.data.api.PeerDeliveryStatusDto
import com.salatsign.tv.data.api.PeerJoinRequest
import com.salatsign.tv.data.api.PeerJoinResponse
import com.salatsign.tv.data.api.PeerNoticeMessage
import com.salatsign.tv.data.api.PeerSyncPayload
import com.salatsign.tv.data.api.PhoneControllerJson
import com.salatsign.tv.security.PeerHmacAuth
import com.salatsign.tv.security.PeerTvRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Pure-JVM HTTP client for HMAC-signed TV-to-TV group sync, join, asset pull, and Urgent Notice fan-out.
 * Uses java.net.HttpURLConnection (zero extra dependencies, API 26+).
 */
class PeerSyncClient(
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val connectTimeoutMs: Int = 3_500,
    private val readTimeoutMs: Int = 8_000
) {
    companion object {
        private const val TAG = "PeerSyncClient"
    }

    suspend fun joinGroup(
        targetHost: String,
        targetPort: Int,
        joinCode: String,
        selfPeer: PeerTvRecord
    ): Result<PeerJoinResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val req = PeerJoinRequest(
                code = joinCode.filter { it.isDigit() },
                deviceId = selfPeer.deviceId,
                deviceName = selfPeer.deviceName,
                roomGroup = selfPeer.roomGroup,
                host = selfPeer.lastKnownHost,
                port = selfPeer.port
            )
            val body = PhoneControllerJson.encode(req).toByteArray(StandardCharsets.UTF_8)
            val conn = (URL("http://$targetHost:$targetPort/api/v1/peer/join").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            conn.disconnect()
            if (code !in 200..299) {
                error("Peer rejected join ($code): $text")
            }
            PhoneControllerJson.decode<PeerJoinResponse>(text)
        }
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
                sendSignedPost(peer, "/api/v1/peer/sync", body, groupSecretHex, selfDeviceId)
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

    suspend fun pullConfigFromPeer(
        peer: PeerTvRecord,
        groupSecretHex: String,
        selfDeviceId: String
    ): Result<PeerSyncPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val path = "/api/v1/peer/sync"
            val ts = nowMillisProvider()
            val nonce = PeerHmacAuth.generateNonce()
            val sig = PeerHmacAuth.computeSignature(groupSecretHex, "GET", path, ts, nonce, ByteArray(0))
            val conn = (URL("http://${peer.lastKnownHost}:${peer.port}$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty(PeerHmacAuth.HEADER_PEER_ID, selfDeviceId)
                setRequestProperty(PeerHmacAuth.HEADER_TIMESTAMP, ts.toString())
                setRequestProperty(PeerHmacAuth.HEADER_NONCE, nonce)
                setRequestProperty(PeerHmacAuth.HEADER_SIGNATURE, sig)
            }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            conn.disconnect()
            if (code !in 200..299) error("HTTP $code")
            PhoneControllerJson.decode<PeerSyncPayload>(text)
        }
    }

    suspend fun downloadPeerAsset(
        originHost: String,
        originPort: Int,
        kind: String, // "slide" or "logo"
        fileName: String,
        groupSecretHex: String,
        selfDeviceId: String
    ): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val path = "/api/v1/peer/asset/$kind/$fileName"
            val ts = nowMillisProvider()
            val nonce = PeerHmacAuth.generateNonce()
            val sig = PeerHmacAuth.computeSignature(groupSecretHex, "GET", path, ts, nonce, ByteArray(0))
            val conn = (URL("http://$originHost:$originPort$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = 15_000
                setRequestProperty(PeerHmacAuth.HEADER_PEER_ID, selfDeviceId)
                setRequestProperty(PeerHmacAuth.HEADER_TIMESTAMP, ts.toString())
                setRequestProperty(PeerHmacAuth.HEADER_NONCE, nonce)
                setRequestProperty(PeerHmacAuth.HEADER_SIGNATURE, sig)
            }
            val code = conn.responseCode
            val bytes = if (code in 200..299) conn.inputStream.use { it.readBytes() } else null
            conn.disconnect()
            bytes
        }.getOrNull()
    }

    private fun sendSignedPost(
        peer: PeerTvRecord,
        path: String,
        body: ByteArray,
        groupSecretHex: String,
        selfDeviceId: String
    ): PeerDeliveryStatusDto {
        return try {
            val ts = nowMillisProvider()
            val nonce = PeerHmacAuth.generateNonce()
            val sig = PeerHmacAuth.computeSignature(groupSecretHex, "POST", path, ts, nonce, body)
            val conn = (URL("http://${peer.lastKnownHost}:${peer.port}$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty(PeerHmacAuth.HEADER_PEER_ID, selfDeviceId)
                setRequestProperty(PeerHmacAuth.HEADER_TIMESTAMP, ts.toString())
                setRequestProperty(PeerHmacAuth.HEADER_NONCE, nonce)
                setRequestProperty(PeerHmacAuth.HEADER_SIGNATURE, sig)
            }
            conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) {
                PeerDeliveryStatusDto(
                    deviceId = peer.deviceId,
                    deviceName = peer.deviceName.ifBlank { peer.lastKnownHost },
                    status = "delivered"
                )
            } else {
                PeerDeliveryStatusDto(
                    deviceId = peer.deviceId,
                    deviceName = peer.deviceName.ifBlank { peer.lastKnownHost },
                    status = "failed",
                    message = "HTTP $code"
                )
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
}
