// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.security

import kotlinx.serialization.Serializable
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

@Serializable
data class PeerTvRecord(
    val deviceId: String,
    val deviceName: String = "",
    val roomGroup: String = "",
    val lastKnownHost: String,
    val port: Int = 8088,
    val lastSeenEpochMillis: Long = 0L
)

@Serializable
data class UrgentNoticeRecord(
    val id: String,
    val title: String = "Urgent Notice",
    val message: String,
    val createdAtEpochMillis: Long,
    val durationMinutes: Int,
    val receivedAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
    val originDeviceId: String = ""
) {
    fun isActive(nowEpochMillis: Long): Boolean {
        return message.isNotBlank() && nowEpochMillis < expiresAtEpochMillis
    }

    fun remainingMinutes(nowEpochMillis: Long): Long {
        if (!isActive(nowEpochMillis)) return 0L
        return ((expiresAtEpochMillis - nowEpochMillis) + 59_999L) / 60_000L
    }
}

/**
 * HMAC-SHA256 signer/verifier and replay guard for TV-to-TV group sync (Phase 3).
 * Browsers never hold the group secret and never call peer endpoints directly.
 */
class PeerHmacAuth(
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val maxSkewMillis: Long = MAX_TIMESTAMP_SKEW_MILLIS
) {
    companion object {
        const val HEADER_PEER_ID = "x-salatsign-peer-id"
        const val HEADER_TIMESTAMP = "x-salatsign-timestamp"
        const val HEADER_NONCE = "x-salatsign-nonce"
        const val HEADER_SIGNATURE = "x-salatsign-signature"
        const val MAX_TIMESTAMP_SKEW_MILLIS = 120_000L // ±2 minutes

        private val secureRandom = SecureRandom()

        fun generateGroupSecretHex(): String {
            val bytes = ByteArray(32)
            secureRandom.nextBytes(bytes)
            return PinHasher.bytesToHex(bytes)
        }

        fun generateNonce(): String {
            val bytes = ByteArray(16)
            secureRandom.nextBytes(bytes)
            return PinHasher.bytesToHex(bytes)
        }

        fun computeSignature(
            groupSecretHex: String,
            method: String,
            path: String,
            timestampMillis: Long,
            nonce: String,
            body: ByteArray
        ): String {
            val bodyHash = PinHasher.bytesToHex(MessageDigest.getInstance("SHA-256").digest(body))
            val canonical = "${method.uppercase()}\n$path\n$timestampMillis\n$nonce\n$bodyHash"
            val secretBytes = PinHasher.hexToBytes(groupSecretHex)
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secretBytes, "HmacSHA256"))
            return PinHasher.bytesToHex(mac.doFinal(canonical.toByteArray(StandardCharsets.UTF_8)))
        }
    }

    private val seenNonces = ConcurrentHashMap<String, Long>()

    fun verifyRequest(
        groupSecretHex: String,
        method: String,
        path: String,
        timestampHeader: String?,
        nonceHeader: String?,
        signatureHeader: String?,
        body: ByteArray
    ): Boolean {
        if (groupSecretHex.isBlank()) return false
        val ts = timestampHeader?.trim()?.toLongOrNull() ?: return false
        val nonce = nonceHeader?.trim()?.takeIf { it.length in 8..128 } ?: return false
        val sig = signatureHeader?.trim()?.takeIf { it.length == 64 } ?: return false

        val now = nowMillisProvider()
        if (abs(now - ts) > maxSkewMillis) return false

        pruneExpiredNonces(now)
        if (seenNonces.putIfAbsent(nonce, now) != null) {
            return false // replay detected
        }

        val expected = computeSignature(groupSecretHex, method, path, ts, nonce, body)
        return MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.US_ASCII),
            sig.lowercase().toByteArray(StandardCharsets.US_ASCII)
        )
    }

    private fun pruneExpiredNonces(now: Long) {
        val it = seenNonces.entries.iterator()
        while (it.hasNext()) {
            if (now - it.next().value > maxSkewMillis * 2L) {
                it.remove()
            }
        }
    }
}

/**
 * Pure conflict-resolution rules for multi-TV config & paired-device sync (Phase 3b).
 */
object GroupSyncConflictResolver {

    /**
     * Highest configVersion wins; on equal version, lexicographically greater deviceId wins
     * as a deterministic tiebreaker across all TVs in the group.
     */
    fun shouldAdoptPeerConfig(
        localVersion: Long,
        localDeviceId: String,
        peerVersion: Long,
        peerDeviceId: String
    ): Boolean {
        if (peerVersion > localVersion) return true
        if (peerVersion < localVersion) return false
        return peerDeviceId > localDeviceId
    }

    /**
     * Merges paired-device lists from local and peer TVs.
     * Revocation tombstones ALWAYS win so an offline TV returning to the LAN can never
     * resurrect a revoked phone.
     */
    fun mergePairedDevices(
        local: List<PairedDeviceRecord>,
        peer: List<PairedDeviceRecord>
    ): List<PairedDeviceRecord> {
        val merged = linkedMapOf<String, PairedDeviceRecord>()
        for (rec in local) {
            merged[rec.deviceId] = rec
        }
        for (incoming in peer) {
            val existing = merged[incoming.deviceId]
            if (existing == null) {
                merged[incoming.deviceId] = incoming
            } else {
                val revokedAt = listOfNotNull(existing.revokedAtEpochMillis, incoming.revokedAtEpochMillis).minOrNull()
                val latestSeen = maxOf(existing.lastSeenAtEpochMillis, incoming.lastSeenAtEpochMillis)
                merged[incoming.deviceId] = existing.copy(
                    deviceName = incoming.deviceName.ifBlank { existing.deviceName },
                    lastSeenAtEpochMillis = latestSeen,
                    revokedAtEpochMillis = revokedAt
                )
            }
        }
        return merged.values.toList()
    }
}

/**
 * Short-lived single-use 6-digit group join code generator/redeemer on the entry TV.
 */
class GroupJoinManager(
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val ttlMillis: Long = 180_000L
) {
    data class ActiveJoinCode(
        val code: String,
        val expiresAtEpochMillis: Long
    )

    private val secureRandom = SecureRandom()
    private val lock = Any()
    private var active: ActiveJoinCode? = null

    fun generateJoinCode(): ActiveJoinCode = synchronized(lock) {
        val now = nowMillisProvider()
        val code = "%06d".format(secureRandom.nextInt(1_000_000))
        val created = ActiveJoinCode(code = code, expiresAtEpochMillis = now + ttlMillis)
        active = created
        created
    }

    fun currentJoinCode(): ActiveJoinCode? = synchronized(lock) {
        val cur = active ?: return null
        if (nowMillisProvider() > cur.expiresAtEpochMillis) {
            active = null
            return null
        }
        cur
    }

    fun consumeIfValid(enteredCode: String): Boolean = synchronized(lock) {
        val cur = currentJoinCode() ?: return false
        val cleaned = enteredCode.filter { it.isDigit() }
        val ok = MessageDigest.isEqual(
            cleaned.toByteArray(StandardCharsets.US_ASCII),
            cur.code.toByteArray(StandardCharsets.US_ASCII)
        )
        if (ok) active = null
        ok
    }
}
