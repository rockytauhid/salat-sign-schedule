// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.security

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class PinErrorCode {
    INVALID_FORMAT,
    RESET_REQUIRED
}

sealed interface PinVerifyResult {
    data object Success : PinVerifyResult
    data class Incorrect(val attemptsLeftBeforeLockout: Int, val totalFailedAttempts: Int) : PinVerifyResult
    data class LockedOut(val remainingSeconds: Int) : PinVerifyResult
    data class InvalidFormat(val errorCode: PinErrorCode = PinErrorCode.INVALID_FORMAT) : PinVerifyResult
    data object ResetRequired : PinVerifyResult
}

data class LockoutState(
    val newFailedAttempts: Int,
    val lockedUntilEpochMillis: Long
)

data class HashedPin(
    val saltHex: String,
    val hashHex: String
)

object PinHasher {

    const val DEFAULT_PIN = "0000"
    const val MIN_PIN_LENGTH = 4
    const val MAX_PIN_LENGTH = 8
    const val MAX_FAILED_ATTEMPTS_BEFORE_LOCK = 5
    const val BASE_LOCKOUT_SECONDS = 30
    const val MAX_LOCKOUT_SECONDS = 15 * 60 // 15 minutes

    const val LEGACY_ITERATIONS = 10_000
    const val CURRENT_ITERATIONS = 120_000
    private const val HASH_V2_PREFIX = "v2:"
    private const val MIN_ALLOWED_ITERATIONS = 10_000
    private const val MAX_ALLOWED_ITERATIONS = 1_000_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    private val secureRandom = SecureRandom()

    private data class ParsedStoredHash(
        val iterations: Int,
        val hashBytes: ByteArray
    )

    private fun parseStoredHash(storedHash: String): ParsedStoredHash? {
        if (storedHash.startsWith(HASH_V2_PREFIX)) {
            val parts = storedHash.split(":")
            if (parts.size != 3) return null
            val iterations = parts[1].toIntOrNull() ?: return null
            if (iterations !in MIN_ALLOWED_ITERATIONS..MAX_ALLOWED_ITERATIONS) return null
            val bytes = hexToBytes(parts[2])
            if (bytes.size != KEY_LENGTH_BITS / 8) return null
            return ParsedStoredHash(iterations = iterations, hashBytes = bytes)
        }
        val legacyBytes = hexToBytes(storedHash)
        if (legacyBytes.isEmpty()) return null
        return ParsedStoredHash(iterations = LEGACY_ITERATIONS, hashBytes = legacyBytes)
    }

    fun validatePinFormat(pin: String): Boolean {
        return pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it.isDigit() }
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun hashPin(pin: String, salt: ByteArray, iterations: Int = CURRENT_ITERATIONS): ByteArray {
        val spec: KeySpec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        val factory = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        } catch (_: NoSuchAlgorithmException) {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        }
        return factory.generateSecret(spec).encoded
    }

    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hexToBytes(hex: String): ByteArray {
        if (hex.isBlank() || hex.length % 2 != 0) return ByteArray(0)
        return runCatching {
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }.getOrDefault(ByteArray(0))
    }

    /**
     * Returns true if a custom PIN is supposedly configured, but its stored salt or hash
     * is missing or corrupted (non-hex / empty / invalid v2 format). Such state requires
     * an app-data reset (F12).
     */
    fun isStoredPinCorrupted(hasCustomPin: Boolean, saltHex: String, hashHex: String): Boolean {
        if (!hasCustomPin) return false
        val salt = hexToBytes(saltHex)
        val parsed = parseStoredHash(hashHex)
        return salt.isEmpty() || parsed == null
    }

    fun createHashedPin(pin: String, iterations: Int = CURRENT_ITERATIONS): HashedPin {
        require(validatePinFormat(pin)) { "Invalid PIN format (must be $MIN_PIN_LENGTH-$MAX_PIN_LENGTH digits)" }
        require(iterations in MIN_ALLOWED_ITERATIONS..MAX_ALLOWED_ITERATIONS) { "Unsupported PBKDF2 iteration count: $iterations" }
        val salt = generateSalt()
        val hash = hashPin(pin, salt, iterations)
        return HashedPin(
            saltHex = bytesToHex(salt),
            hashHex = "$HASH_V2_PREFIX$iterations:${bytesToHex(hash)}"
        )
    }

    /**
     * Calculates new failed attempts count and lockout timestamp after an incorrect attempt.
     * Attempt 5: 30s
     * Attempt 6: 60s
     * Attempt 7: 120s
     * Doubling each subsequent failure up to 900s (15 min).
     */
    fun calculateLockout(currentFailedAttempts: Int, nowEpochMillis: Long): LockoutState {
        val newCount = currentFailedAttempts + 1
        if (newCount < MAX_FAILED_ATTEMPTS_BEFORE_LOCK) {
            return LockoutState(newFailedAttempts = newCount, lockedUntilEpochMillis = 0L)
        }

        val exponent = (newCount - MAX_FAILED_ATTEMPTS_BEFORE_LOCK).coerceAtMost(5) // 30, 60, 120, 240, 480, 960 -> capped at 900
        val lockoutSeconds = (BASE_LOCKOUT_SECONDS * (1 shl exponent)).coerceAtMost(MAX_LOCKOUT_SECONDS)
        val lockedUntil = nowEpochMillis + (lockoutSeconds * 1000L)

        return LockoutState(newFailedAttempts = newCount, lockedUntilEpochMillis = lockedUntil)
    }

    /**
     * Verifies the entered PIN against the stored hash (or default "0000" if no custom PIN).
     */
    fun verifyPin(
        enteredPin: String,
        hasCustomPin: Boolean,
        saltHex: String,
        hashHex: String,
        nowEpochMillis: Long,
        currentFailedAttempts: Int,
        lockedUntilEpochMillis: Long
    ): PinVerifyResult {
        // 1. Check if stored custom PIN is corrupted (F12: never silently fall back to "0000")
        if (isStoredPinCorrupted(hasCustomPin, saltHex, hashHex)) {
            return PinVerifyResult.ResetRequired
        }

        // 2. Check if currently locked out
        if (nowEpochMillis < lockedUntilEpochMillis) {
            val remainingSec = ((lockedUntilEpochMillis - nowEpochMillis + 999) / 1000).toInt()
            return PinVerifyResult.LockedOut(remainingSec)
        }

        // 3. Validate input format
        if (!validatePinFormat(enteredPin)) {
            return PinVerifyResult.InvalidFormat(PinErrorCode.INVALID_FORMAT)
        }

        // 4. Match against effective PIN (constant-time for both default and custom PINs)
        val matches = if (!hasCustomPin) {
            MessageDigest.isEqual(
                enteredPin.toByteArray(Charsets.UTF_8),
                DEFAULT_PIN.toByteArray(Charsets.UTF_8)
            )
        } else {
            val salt = hexToBytes(saltHex)
            val parsed = parseStoredHash(hashHex) ?: return PinVerifyResult.ResetRequired
            val computedHash = hashPin(enteredPin, salt, parsed.iterations)
            MessageDigest.isEqual(computedHash, parsed.hashBytes)
        }

        return if (matches) {
            PinVerifyResult.Success
        } else {
            val attemptsAfterFailure = currentFailedAttempts + 1
            val attemptsLeft = (MAX_FAILED_ATTEMPTS_BEFORE_LOCK - attemptsAfterFailure).coerceAtLeast(0)
            PinVerifyResult.Incorrect(
                attemptsLeftBeforeLockout = attemptsLeft,
                totalFailedAttempts = attemptsAfterFailure
            )
        }
    }
}
