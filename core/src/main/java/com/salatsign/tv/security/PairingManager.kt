// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.security

import kotlinx.serialization.Serializable
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
enum class RemoteRole {
    NOTIFY,
    MANAGE,
    CRITICAL;

    fun hasPermission(required: RemoteRole): Boolean {
        val rank = when (this) {
            NOTIFY -> 1
            MANAGE -> 2
            CRITICAL -> 3
        }
        val reqRank = when (required) {
            NOTIFY -> 1
            MANAGE -> 2
            CRITICAL -> 3
        }
        return rank >= reqRank
    }
}

@Serializable
data class PairedDeviceRecord(
    val deviceId: String,
    val deviceName: String,
    val tokenHash: String,
    val role: RemoteRole = RemoteRole.MANAGE,
    val createdAtEpochMillis: Long,
    val lastSeenAtEpochMillis: Long,
    val revokedAtEpochMillis: Long? = null
) {
    val isRevoked: Boolean get() = revokedAtEpochMillis != null

    fun isExpired(nowEpochMillis: Long, expiryDays: Int): Boolean {
        if (expiryDays <= 0) return false
        val ttlMillis = expiryDays.toLong() * 24L * 60L * 60L * 1000L
        return (nowEpochMillis - lastSeenAtEpochMillis) > ttlMillis
    }
}

data class ActivePairingCode(
    val code: String,
    val role: RemoteRole,
    val createdAtEpochMillis: Long,
    val expiresAtEpochMillis: Long
)

sealed interface PairRedeemResult {
    data class Success(val record: PairedDeviceRecord, val rawToken: String) : PairRedeemResult
    data object NoActivePairing : PairRedeemResult
    data object Expired : PairRedeemResult
    data object InvalidCode : PairRedeemResult
    data class RateLimited(val remainingSeconds: Int) : PairRedeemResult
}

/**
 * Pure-Kotlin browser pairing manager for SalatSign Phone Controller (Phase 1).
 * - Single-use 6-digit pairing codes with short TTL (120s).
 * - Per-client IP rate limiting and progressive lockout (one client can never lock out another).
 * - Stores only SHA-256 hashes of 256-bit random bearer tokens; constant-time comparison.
 */
class PairingManager(
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val codeTtlMillis: Long = DEFAULT_CODE_TTL_MILLIS
) {
    companion object {
        const val DEFAULT_CODE_TTL_MILLIS: Long = 120_000L // 2 minutes
        const val MAX_WRONG_CODES_PER_IP: Int = 5
        const val MAX_GLOBAL_WRONG_CODES_PER_CODE: Int = 10
        private const val BASE_LOCKOUT_SECONDS: Int = 30
        private const val MAX_LOCKOUT_SECONDS: Int = 15 * 60
        private const val IP_ENTRY_STALE_MILLIS: Long = 30 * 60 * 1000L

        val ALLOWED_EXPIRY_DAYS: Set<Int> = setOf(0, 30, 90)

        fun normalizeExpiryDays(days: Int): Int = when {
            days <= 0 -> 0
            days <= 30 -> 30
            else -> 90
        }

        fun sha256Hex(raw: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(raw.toByteArray(StandardCharsets.UTF_8))
            return PinHasher.bytesToHex(digest)
        }

        fun findMatchingActiveDevice(
            rawToken: String,
            devices: List<PairedDeviceRecord>,
            nowEpochMillis: Long,
            expiryDays: Int
        ): PairedDeviceRecord? {
            if (rawToken.isBlank()) return null
            val candidateHashBytes = sha256Hex(rawToken).toByteArray(StandardCharsets.US_ASCII)
            for (device in devices) {
                if (device.isRevoked) continue
                if (device.isExpired(nowEpochMillis, expiryDays)) continue
                val storedHashBytes = device.tokenHash.toByteArray(StandardCharsets.US_ASCII)
                if (MessageDigest.isEqual(candidateHashBytes, storedHashBytes)) {
                    return device
                }
            }
            return null
        }
    }

    private data class IpFailState(
        val failedAttempts: Int = 0,
        val lockedUntilEpochMillis: Long = 0L,
        val lastAttemptEpochMillis: Long = 0L
    )

    private val secureRandom = SecureRandom()
    private val lock = Any()
    private var activeCode: ActivePairingCode? = null
    private var globalWrongCodesForActive: Int = 0
    private val failuresByIp = ConcurrentHashMap<String, IpFailState>()

    fun generatePairingCode(role: RemoteRole = RemoteRole.MANAGE): ActivePairingCode = synchronized(lock) {
        val now = nowMillisProvider()
        val numeric = secureRandom.nextInt(1_000_000)
        val code = "%06d".format(numeric)
        val created = ActivePairingCode(
            code = code,
            role = role,
            createdAtEpochMillis = now,
            expiresAtEpochMillis = now + codeTtlMillis
        )
        activeCode = created
        globalWrongCodesForActive = 0
        created
    }

    fun currentActiveCode(): ActivePairingCode? = synchronized(lock) {
        val current = activeCode ?: return null
        if (nowMillisProvider() > current.expiresAtEpochMillis) {
            activeCode = null
            globalWrongCodesForActive = 0
            return null
        }
        current
    }

    fun cancelPairingCode() = synchronized(lock) {
        activeCode = null
        globalWrongCodesForActive = 0
    }

    fun redeemCode(
        enteredCode: String,
        deviceName: String,
        remoteIp: String
    ): PairRedeemResult = synchronized(lock) {
        val now = nowMillisProvider()
        pruneStaleIpFailures(now)

        val ipKey = remoteIp.ifBlank { "unknown" }
        val ipState = failuresByIp[ipKey] ?: IpFailState()
        if (now < ipState.lockedUntilEpochMillis) {
            val remaining = ((ipState.lockedUntilEpochMillis - now + 999L) / 1000L).toInt().coerceAtLeast(1)
            return PairRedeemResult.RateLimited(remaining)
        }

        val current = activeCode ?: return PairRedeemResult.NoActivePairing
        if (now > current.expiresAtEpochMillis) {
            activeCode = null
            return PairRedeemResult.Expired
        }

        val cleanedCode = enteredCode.filter { it.isDigit() }
        val expectedBytes = current.code.toByteArray(StandardCharsets.US_ASCII)
        val enteredBytes = cleanedCode.toByteArray(StandardCharsets.US_ASCII)
        if (!MessageDigest.isEqual(enteredBytes, expectedBytes)) {
            val nextFails = ipState.failedAttempts + 1
            val lockedUntil = if (nextFails >= MAX_WRONG_CODES_PER_IP) {
                val exp = (nextFails - MAX_WRONG_CODES_PER_IP).coerceAtMost(5)
                val sec = (BASE_LOCKOUT_SECONDS * (1 shl exp)).coerceAtMost(MAX_LOCKOUT_SECONDS)
                now + sec * 1000L
            } else {
                0L
            }
            failuresByIp[ipKey] = IpFailState(
                failedAttempts = nextFails,
                lockedUntilEpochMillis = lockedUntil,
                lastAttemptEpochMillis = now
            )
            // Review 4 M1: Global cap across all source IPs invalidates the active code
            globalWrongCodesForActive++
            if (globalWrongCodesForActive >= MAX_GLOBAL_WRONG_CODES_PER_CODE) {
                activeCode = null
                globalWrongCodesForActive = 0
            }
            return PairRedeemResult.InvalidCode
        }

        // Valid single-use code: consume immediately and clear this IP's failure state
        activeCode = null
        globalWrongCodesForActive = 0
        failuresByIp.remove(ipKey)

        val tokenBytes = ByteArray(32)
        secureRandom.nextBytes(tokenBytes)
        val rawToken = PinHasher.bytesToHex(tokenBytes)
        val cleanName = deviceName.trim().take(48).ifEmpty { "Phone Browser" }
        val record = PairedDeviceRecord(
            deviceId = "dev_${UUID.randomUUID()}",
            deviceName = cleanName,
            tokenHash = sha256Hex(rawToken),
            role = current.role,
            createdAtEpochMillis = now,
            lastSeenAtEpochMillis = now,
            revokedAtEpochMillis = null
        )
        PairRedeemResult.Success(record = record, rawToken = rawToken)
    }

    private fun pruneStaleIpFailures(now: Long) {
        val it = failuresByIp.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next().value
            if (now >= entry.lockedUntilEpochMillis && (now - entry.lastAttemptEpochMillis) > IP_ENTRY_STALE_MILLIS) {
                it.remove()
            }
        }
    }
}
