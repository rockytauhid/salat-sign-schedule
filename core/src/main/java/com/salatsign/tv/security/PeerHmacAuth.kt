// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.security

import kotlinx.serialization.Serializable
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
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
 * Pure filename & asset validation helpers for peer-synced images (Review 4 H3).
 */
object PeerAssetValidator {
    const val MAX_PEER_ASSET_BYTES: Int = 10 * 1024 * 1024 // 10 MB cap
    private val SAFE_NAME_REGEX = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")

    fun isSafeFileName(fileName: String?): Boolean {
        if (fileName.isNullOrBlank()) return false
        if (fileName.contains("..") || fileName.contains('/') || fileName.contains('\\')) return false
        return SAFE_NAME_REGEX.matches(fileName)
    }

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return PinHasher.bytesToHex(digest)
    }
}

/**
 * Ephemeral ECDH (P-256) + AES-256-GCM key exchange bound to the one-time group join code (Review 4 H1).
 * Protects `groupSecretHex` against both passive LAN sniffing and active MITMs who do not possess the join code.
 * Uses standard JCA APIs available on Android API 26+ and JVM 17+.
 */
object PeerJoinCrypto {
    private const val CURVE = "secp256r1"
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private val secureRandom = SecureRandom()

    data class EncryptedJoinEnvelope(
        val serverPublicKeyBase64: String,
        val ivBase64: String,
        val ciphertextBase64: String
    )

    fun generateEphemeralKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec(CURVE), secureRandom)
        return kpg.generateKeyPair()
    }

    fun encodePublicKey(keyPair: KeyPair): String {
        return Base64.getEncoder().encodeToString(keyPair.public.encoded)
    }

    fun encryptForPeer(
        clientPublicKeyBase64: String,
        normalizedJoinCode: String,
        plaintextUtf8: ByteArray
    ): EncryptedJoinEnvelope {
        val serverKeyPair = generateEphemeralKeyPair()
        val aesKey = deriveJoinKey(serverKeyPair, clientPublicKeyBase64, normalizedJoinCode)
        val iv = ByteArray(GCM_IV_BYTES)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(normalizedJoinCode.toByteArray(StandardCharsets.US_ASCII))
        val ciphertext = cipher.doFinal(plaintextUtf8)
        return EncryptedJoinEnvelope(
            serverPublicKeyBase64 = encodePublicKey(serverKeyPair),
            ivBase64 = Base64.getEncoder().encodeToString(iv),
            ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext)
        )
    }

    fun decryptFromServer(
        clientKeyPair: KeyPair,
        serverPublicKeyBase64: String,
        ivBase64: String,
        ciphertextBase64: String,
        normalizedJoinCode: String
    ): ByteArray {
        val aesKey = deriveJoinKey(clientKeyPair, serverPublicKeyBase64, normalizedJoinCode)
        val iv = Base64.getDecoder().decode(ivBase64)
        require(iv.size == GCM_IV_BYTES) { "Invalid GCM IV length" }
        val ciphertext = Base64.getDecoder().decode(ciphertextBase64)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(normalizedJoinCode.toByteArray(StandardCharsets.US_ASCII))
        return cipher.doFinal(ciphertext)
    }

    private fun deriveJoinKey(
        ownKeyPair: KeyPair,
        remotePublicKeyBase64: String,
        normalizedJoinCode: String
    ): ByteArray {
        val remotePubBytes = Base64.getDecoder().decode(remotePublicKeyBase64)
        val remotePub = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(remotePubBytes))
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(ownKeyPair.private)
        ka.doPhase(remotePub, true)
        val sharedSecret = ka.generateSecret()
        val md = MessageDigest.getInstance("SHA-256")
        md.update(sharedSecret)
        md.update(":salatsign-peer-join-v1:".toByteArray(StandardCharsets.US_ASCII))
        md.update(normalizedJoinCode.toByteArray(StandardCharsets.US_ASCII))
        return md.digest()
    }
}

/**
 * HMAC-SHA256 signer/verifier and replay guard for TV-to-TV group sync (Phase 3 + Review 4 H2/M2/L3).
 * - Binds sender peerId and target deviceId into the canonical string (L3).
 * - Verifies HMAC signature BEFORE recording nonces, bounds the nonce table, and rate-limits
 *   failed signatures per IP to prevent pre-auth nonce-store DoS (M2).
 * - Signs and verifies peer responses over response body + request nonce + responder deviceId (H2).
 */
class PeerHmacAuth(
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val maxSkewMillis: Long = MAX_TIMESTAMP_SKEW_MILLIS,
    private val maxSeenNonces: Int = MAX_SEEN_NONCES
) {
    companion object {
        const val HEADER_PEER_ID = "x-salatsign-peer-id"
        const val HEADER_TARGET_ID = "x-salatsign-target-id"
        const val HEADER_TIMESTAMP = "x-salatsign-timestamp"
        const val HEADER_NONCE = "x-salatsign-nonce"
        const val HEADER_SIGNATURE = "x-salatsign-signature"
        const val HEADER_RESP_SIGNATURE = "x-salatsign-resp-sig"
        const val HEADER_RESP_TIMESTAMP = "x-salatsign-resp-ts"
        const val MAX_TIMESTAMP_SKEW_MILLIS = 120_000L // ±2 minutes
        const val MAX_SEEN_NONCES = 4096
        const val MAX_FAILED_SIGS_PER_IP = 10
        private const val FAILED_SIG_LOCKOUT_MILLIS = 60_000L
        private const val FAILED_SIG_WINDOW_MILLIS = 120_000L

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
            peerId: String,
            targetDeviceId: String,
            timestampMillis: Long,
            nonce: String,
            body: ByteArray
        ): String {
            val bodyHash = PeerAssetValidator.sha256Hex(body)
            val canonical = "${method.uppercase()}\n$path\n$peerId\n$targetDeviceId\n$timestampMillis\n$nonce\n$bodyHash"
            return hmacSha256Hex(groupSecretHex, canonical)
        }

        fun computeResponseSignature(
            groupSecretHex: String,
            responderDeviceId: String,
            requestNonce: String,
            responseTimestampMillis: Long,
            responseBody: ByteArray
        ): String {
            val bodyHash = PeerAssetValidator.sha256Hex(responseBody)
            val canonical = "RESP\n$responderDeviceId\n$requestNonce\n$responseTimestampMillis\n$bodyHash"
            return hmacSha256Hex(groupSecretHex, canonical)
        }

        fun verifyResponse(
            groupSecretHex: String,
            expectedResponderDeviceId: String,
            requestNonce: String,
            responseTimestampHeader: String?,
            responseSignatureHeader: String?,
            responseBody: ByteArray,
            nowEpochMillis: Long = System.currentTimeMillis(),
            maxSkewMillis: Long = MAX_TIMESTAMP_SKEW_MILLIS
        ): Boolean {
            if (groupSecretHex.isBlank() || expectedResponderDeviceId.isBlank() || requestNonce.isBlank()) return false
            val ts = responseTimestampHeader?.trim()?.toLongOrNull() ?: return false
            val sig = responseSignatureHeader?.trim()?.takeIf { it.length == 64 } ?: return false
            if (abs(nowEpochMillis - ts) > maxSkewMillis) return false
            val expected = computeResponseSignature(
                groupSecretHex = groupSecretHex,
                responderDeviceId = expectedResponderDeviceId,
                requestNonce = requestNonce,
                responseTimestampMillis = ts,
                responseBody = responseBody
            )
            return MessageDigest.isEqual(
                expected.toByteArray(StandardCharsets.US_ASCII),
                sig.lowercase().toByteArray(StandardCharsets.US_ASCII)
            )
        }

        private fun hmacSha256Hex(groupSecretHex: String, canonical: String): String {
            val secretBytes = PinHasher.hexToBytes(groupSecretHex)
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secretBytes, "HmacSHA256"))
            return PinHasher.bytesToHex(mac.doFinal(canonical.toByteArray(StandardCharsets.UTF_8)))
        }
    }

    private data class SigFailState(
        val count: Int = 0,
        val lastFailMillis: Long = 0L,
        val lockedUntilMillis: Long = 0L
    )

    private val seenNonces = ConcurrentHashMap<String, Long>()
    private val failedSigsByIp = ConcurrentHashMap<String, SigFailState>()

    internal val seenNonceCount: Int get() = seenNonces.size

    fun verifyRequest(
        groupSecretHex: String,
        method: String,
        path: String,
        expectedTargetDeviceId: String,
        peerIdHeader: String?,
        targetIdHeader: String?,
        timestampHeader: String?,
        nonceHeader: String?,
        signatureHeader: String?,
        body: ByteArray,
        remoteIp: String = "unknown"
    ): Boolean {
        val now = nowMillisProvider()
        val ipKey = remoteIp.ifBlank { "unknown" }
        val failState = failedSigsByIp[ipKey]
        if (failState != null && now < failState.lockedUntilMillis) {
            return false
        }

        if (groupSecretHex.isBlank() || expectedTargetDeviceId.isBlank()) return false
        val peerId = peerIdHeader?.trim()?.takeIf { it.isNotEmpty() } ?: run { recordFailure(ipKey, now); return false }
        val targetId = targetIdHeader?.trim()?.takeIf { it == expectedTargetDeviceId } ?: run { recordFailure(ipKey, now); return false }
        val ts = timestampHeader?.trim()?.toLongOrNull() ?: run { recordFailure(ipKey, now); return false }
        val nonce = nonceHeader?.trim()?.takeIf { it.length in 8..128 } ?: run { recordFailure(ipKey, now); return false }
        val sig = signatureHeader?.trim()?.takeIf { it.length == 64 } ?: run { recordFailure(ipKey, now); return false }

        if (abs(now - ts) > maxSkewMillis) {
            recordFailure(ipKey, now)
            return false
        }

        // Review 4 M2: Verify HMAC signature BEFORE touching seenNonces so unsigned LAN traffic cannot bloat memory.
        val expected = computeSignature(groupSecretHex, method, path, peerId, targetId, ts, nonce, body)
        val sigOk = MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.US_ASCII),
            sig.lowercase().toByteArray(StandardCharsets.US_ASCII)
        )
        if (!sigOk) {
            recordFailure(ipKey, now)
            return false
        }

        failedSigsByIp.remove(ipKey)
        pruneExpiredNonces(now)
        if (seenNonces.size >= maxSeenNonces) {
            evictOldestNonces()
        }
        val nonceKey = "$peerId:$nonce"
        if (seenNonces.putIfAbsent(nonceKey, now) != null) {
            return false // replay detected
        }
        return true
    }

    private fun recordFailure(ipKey: String, now: Long) {
        failedSigsByIp.compute(ipKey) { _, prev ->
            val baseCount = if (prev != null && (now - prev.lastFailMillis) <= FAILED_SIG_WINDOW_MILLIS) prev.count else 0
            val next = baseCount + 1
            val lockedUntil = if (next >= MAX_FAILED_SIGS_PER_IP) now + FAILED_SIG_LOCKOUT_MILLIS else 0L
            SigFailState(count = next, lastFailMillis = now, lockedUntilMillis = lockedUntil)
        }
    }

    private fun pruneExpiredNonces(now: Long) {
        val it = seenNonces.entries.iterator()
        while (it.hasNext()) {
            if (now - it.next().value > maxSkewMillis * 2L) {
                it.remove()
            }
        }
    }

    private fun evictOldestNonces() {
        val toDrop = (seenNonces.size - maxSeenNonces + 64).coerceAtLeast(1)
        seenNonces.entries
            .sortedBy { it.value }
            .take(toDrop)
            .forEach { seenNonces.remove(it.key, it.value) }
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

sealed interface JoinCodeRedeemResult {
    data class Success(val normalizedCode: String) : JoinCodeRedeemResult
    data object NoActiveCode : JoinCodeRedeemResult
    data object Expired : JoinCodeRedeemResult
    data object InvalidCode : JoinCodeRedeemResult
    data class RateLimited(val remainingSeconds: Int) : JoinCodeRedeemResult
}

/**
 * Short-lived single-use 8-character alphanumeric group join code generator/redeemer (Review 4 H1).
 * - Per-IP progressive lockout on failed attempts.
 * - Global 5-failure invalidation: after 5 wrong guesses across all IPs, the active code is invalidated.
 */
class GroupJoinManager(
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val ttlMillis: Long = 180_000L
) {
    companion object {
        const val CODE_LENGTH: Int = 8
        const val MAX_GLOBAL_FAILURES_PER_CODE: Int = 5
        const val MAX_WRONG_CODES_PER_IP: Int = 5
        private const val BASE_LOCKOUT_SECONDS: Int = 30
        private const val MAX_LOCKOUT_SECONDS: Int = 15 * 60
        private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        fun normalizeCode(raw: String): String =
            raw.uppercase().filter { it in CODE_ALPHABET }
    }

    data class ActiveJoinCode(
        val code: String,
        val expiresAtEpochMillis: Long
    )

    private data class IpFailState(
        val failedAttempts: Int = 0,
        val lockedUntilEpochMillis: Long = 0L
    )

    private val secureRandom = SecureRandom()
    private val lock = Any()
    private var active: ActiveJoinCode? = null
    private var globalFailuresForActiveCode: Int = 0
    private val failuresByIp = ConcurrentHashMap<String, IpFailState>()

    fun generateJoinCode(): ActiveJoinCode = synchronized(lock) {
        val now = nowMillisProvider()
        val sb = StringBuilder(CODE_LENGTH)
        repeat(CODE_LENGTH) {
            sb.append(CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)])
        }
        val created = ActiveJoinCode(code = sb.toString(), expiresAtEpochMillis = now + ttlMillis)
        active = created
        globalFailuresForActiveCode = 0
        created
    }

    fun currentJoinCode(): ActiveJoinCode? = synchronized(lock) {
        val cur = active ?: return null
        if (nowMillisProvider() > cur.expiresAtEpochMillis) {
            active = null
            globalFailuresForActiveCode = 0
            return null
        }
        cur
    }

    fun redeemCode(enteredCode: String, remoteIp: String = "unknown"): JoinCodeRedeemResult = synchronized(lock) {
        val now = nowMillisProvider()
        val ipKey = remoteIp.ifBlank { "unknown" }
        val ipState = failuresByIp[ipKey] ?: IpFailState()
        if (now < ipState.lockedUntilEpochMillis) {
            val remaining = ((ipState.lockedUntilEpochMillis - now + 999L) / 1000L).toInt().coerceAtLeast(1)
            return JoinCodeRedeemResult.RateLimited(remaining)
        }

        val cur = active ?: return JoinCodeRedeemResult.NoActiveCode
        if (now > cur.expiresAtEpochMillis) {
            active = null
            globalFailuresForActiveCode = 0
            return JoinCodeRedeemResult.Expired
        }

        val cleaned = normalizeCode(enteredCode)
        val ok = MessageDigest.isEqual(
            cleaned.toByteArray(StandardCharsets.US_ASCII),
            cur.code.toByteArray(StandardCharsets.US_ASCII)
        )
        if (!ok) {
            val nextIpFails = ipState.failedAttempts + 1
            val lockedUntil = if (nextIpFails >= MAX_WRONG_CODES_PER_IP) {
                val exp = (nextIpFails - MAX_WRONG_CODES_PER_IP).coerceAtMost(5)
                val sec = (BASE_LOCKOUT_SECONDS * (1 shl exp)).coerceAtMost(MAX_LOCKOUT_SECONDS)
                now + sec * 1000L
            } else 0L
            failuresByIp[ipKey] = IpFailState(failedAttempts = nextIpFails, lockedUntilEpochMillis = lockedUntil)

            globalFailuresForActiveCode++
            if (globalFailuresForActiveCode >= MAX_GLOBAL_FAILURES_PER_CODE) {
                active = null
                globalFailuresForActiveCode = 0
            }
            return JoinCodeRedeemResult.InvalidCode
        }

        active = null
        globalFailuresForActiveCode = 0
        failuresByIp.remove(ipKey)
        JoinCodeRedeemResult.Success(cur.code)
    }
}
