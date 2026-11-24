// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun defaultPin_succeedsWhenNoCustomPin() {
        val result = PinHasher.verifyPin(
            enteredPin = "0000",
            hasCustomPin = false,
            saltHex = "",
            hashHex = "",
            nowEpochMillis = 1_000_000L,
            currentFailedAttempts = 0,
            lockedUntilEpochMillis = 0L
        )

        assertTrue("Default PIN 0000 should succeed when no custom PIN is set", result is PinVerifyResult.Success)
    }

    @Test
    fun defaultPin_failsWhenWrongPinEntered() {
        val result = PinHasher.verifyPin(
            enteredPin = "1234",
            hasCustomPin = false,
            saltHex = "",
            hashHex = "",
            nowEpochMillis = 1_000_000L,
            currentFailedAttempts = 0,
            lockedUntilEpochMillis = 0L
        )

        assertTrue(result is PinVerifyResult.Incorrect)
        val incorrect = result as PinVerifyResult.Incorrect
        assertEquals(4, incorrect.attemptsLeftBeforeLockout)
        assertEquals(1, incorrect.totalFailedAttempts)
    }

    @Test
    fun validatePinFormat_acceptsFourToEightDigits() {
        assertTrue(PinHasher.validatePinFormat("0000"))
        assertTrue(PinHasher.validatePinFormat("12345"))
        assertTrue(PinHasher.validatePinFormat("123456"))
        assertTrue(PinHasher.validatePinFormat("1234567"))
        assertTrue(PinHasher.validatePinFormat("12345678"))

        // Rejections
        assertFalse(PinHasher.validatePinFormat(""))
        assertFalse(PinHasher.validatePinFormat("123"))
        assertFalse(PinHasher.validatePinFormat("123456789")) // 9 digits
        assertFalse(PinHasher.validatePinFormat("123a"))
        assertFalse(PinHasher.validatePinFormat("abcd"))
        assertFalse(PinHasher.validatePinFormat("12 34"))
    }

    @Test
    fun customPin_hashingAndVerification() {
        val pin = "849201"
        val hashed = PinHasher.createHashedPin(pin)

        assertFalse(hashed.saltHex.isBlank())
        assertFalse(hashed.hashHex.isBlank())
        assertEquals(32, hashed.saltHex.length) // 16 bytes = 32 hex chars
        assertTrue(hashed.hashHex.startsWith("v2:${PinHasher.CURRENT_ITERATIONS}:"))
        val digestHex = hashed.hashHex.substringAfterLast(':')
        assertEquals(64, digestHex.length) // 32 bytes (256 bits) = 64 hex chars

        // Correct PIN
        val successResult = PinHasher.verifyPin(
            enteredPin = pin,
            hasCustomPin = true,
            saltHex = hashed.saltHex,
            hashHex = hashed.hashHex,
            nowEpochMillis = 1_000_000L,
            currentFailedAttempts = 0,
            lockedUntilEpochMillis = 0L
        )
        assertTrue(successResult is PinVerifyResult.Success)

        // Incorrect PIN
        val failureResult = PinHasher.verifyPin(
            enteredPin = "849200",
            hasCustomPin = true,
            saltHex = hashed.saltHex,
            hashHex = hashed.hashHex,
            nowEpochMillis = 1_000_000L,
            currentFailedAttempts = 0,
            lockedUntilEpochMillis = 0L
        )
        assertTrue(failureResult is PinVerifyResult.Incorrect)
    }

    @Test
    fun salt_isUniquePerInvocation() {
        val salt1 = PinHasher.generateSalt()
        val salt2 = PinHasher.generateSalt()
        assertFalse(salt1.contentEquals(salt2))

        val hashed1 = PinHasher.createHashedPin("1234")
        val hashed2 = PinHasher.createHashedPin("1234")
        assertNotEquals(hashed1.saltHex, hashed2.saltHex)
        assertNotEquals(hashed1.hashHex, hashed2.hashHex)
    }

    @Test
    fun calculateLockout_progressionAndDoubling() {
        val now = 100_000_000L

        // Attempt 1 to 4: no lock
        for (attempt in 0..3) {
            val state = PinHasher.calculateLockout(currentFailedAttempts = attempt, nowEpochMillis = now)
            assertEquals(attempt + 1, state.newFailedAttempts)
            assertEquals(0L, state.lockedUntilEpochMillis)
        }

        // Attempt 5: locks for 30 seconds
        val state5 = PinHasher.calculateLockout(currentFailedAttempts = 4, nowEpochMillis = now)
        assertEquals(5, state5.newFailedAttempts)
        assertEquals(now + 30_000L, state5.lockedUntilEpochMillis)

        // Attempt 6: locks for 60 seconds (30 * 2)
        val state6 = PinHasher.calculateLockout(currentFailedAttempts = 5, nowEpochMillis = now)
        assertEquals(6, state6.newFailedAttempts)
        assertEquals(now + 60_000L, state6.lockedUntilEpochMillis)

        // Attempt 7: locks for 120 seconds (30 * 4)
        val state7 = PinHasher.calculateLockout(currentFailedAttempts = 6, nowEpochMillis = now)
        assertEquals(7, state7.newFailedAttempts)
        assertEquals(now + 120_000L, state7.lockedUntilEpochMillis)

        // Attempt 8: locks for 240 seconds (30 * 8)
        val state8 = PinHasher.calculateLockout(currentFailedAttempts = 7, nowEpochMillis = now)
        assertEquals(8, state8.newFailedAttempts)
        assertEquals(now + 240_000L, state8.lockedUntilEpochMillis)

        // Attempt 9: locks for 480 seconds (30 * 16)
        val state9 = PinHasher.calculateLockout(currentFailedAttempts = 8, nowEpochMillis = now)
        assertEquals(9, state9.newFailedAttempts)
        assertEquals(now + 480_000L, state9.lockedUntilEpochMillis)

        // Attempt 10: capped at 900 seconds (15 minutes)
        val state10 = PinHasher.calculateLockout(currentFailedAttempts = 9, nowEpochMillis = now)
        assertEquals(10, state10.newFailedAttempts)
        assertEquals(now + 900_000L, state10.lockedUntilEpochMillis)

        // Attempt 20: still capped at 900 seconds
        val state20 = PinHasher.calculateLockout(currentFailedAttempts = 19, nowEpochMillis = now)
        assertEquals(20, state20.newFailedAttempts)
        assertEquals(now + 900_000L, state20.lockedUntilEpochMillis)
    }

    @Test
    fun verifyPin_lockedOutStateEnforced() {
        val now = 1_000_000L
        val lockedUntil = now + 25_000L // 25 seconds remaining

        val result = PinHasher.verifyPin(
            enteredPin = "0000",
            hasCustomPin = false,
            saltHex = "",
            hashHex = "",
            nowEpochMillis = now,
            currentFailedAttempts = 5,
            lockedUntilEpochMillis = lockedUntil
        )

        assertTrue(result is PinVerifyResult.LockedOut)
        val lockedOut = result as PinVerifyResult.LockedOut
        assertEquals(25, lockedOut.remainingSeconds)
    }

    @Test
    fun verifyPin_allowsAttemptAfterLockoutExpires() {
        val now = 1_000_000L
        val lockedUntil = now - 1L // Lockout expired 1 ms ago

        val result = PinHasher.verifyPin(
            enteredPin = "0000",
            hasCustomPin = false,
            saltHex = "",
            hashHex = "",
            nowEpochMillis = now,
            currentFailedAttempts = 5,
            lockedUntilEpochMillis = lockedUntil
        )

        assertTrue("After lockout expires, entering correct PIN succeeds", result is PinVerifyResult.Success)
    }

    @Test
    fun verifyPin_corruptedOrEmptyCustomPin_returnsResetRequiredAndNeverAcceptsDefault() {
        val emptyHashResult = PinHasher.verifyPin(
            enteredPin = "0000",
            hasCustomPin = true,
            saltHex = "00112233445566778899aabbccddeeff",
            hashHex = "",
            nowEpochMillis = 1_000_000L,
            currentFailedAttempts = 0,
            lockedUntilEpochMillis = 0L
        )
        assertTrue("Empty hash must return ResetRequired", emptyHashResult is PinVerifyResult.ResetRequired)

        val corruptHexResult = PinHasher.verifyPin(
            enteredPin = "0000",
            hasCustomPin = true,
            saltHex = "zzzz_corrupted_hex",
            hashHex = "also_not_hex",
            nowEpochMillis = 1_000_000L,
            currentFailedAttempts = 0,
            lockedUntilEpochMillis = 0L
        )
        assertTrue("Corrupt non-hex salt/hash must return ResetRequired", corruptHexResult is PinVerifyResult.ResetRequired)

        assertTrue(PinHasher.isStoredPinCorrupted(true, "", ""))
        assertTrue(PinHasher.isStoredPinCorrupted(true, "not_hex!", "not_hex!"))
        assertFalse(PinHasher.isStoredPinCorrupted(false, "", ""))
    }

    @Test
    fun verifyPin_invalidFormat_returnsErrorCode() {
        val result = PinHasher.verifyPin(
            enteredPin = "12",
            hasCustomPin = false,
            saltHex = "",
            hashHex = "",
            nowEpochMillis = 1_000_000L,
            currentFailedAttempts = 0,
            lockedUntilEpochMillis = 0L
        )
        assertTrue(result is PinVerifyResult.InvalidFormat)
        assertEquals(PinErrorCode.INVALID_FORMAT, (result as PinVerifyResult.InvalidFormat).errorCode)
    }

    @Test
    fun legacyPinHash_10kIterations_stillVerifiesCleanly() {
        val pin = "5522"
        val salt = PinHasher.generateSalt()
        val legacyHashHex = PinHasher.bytesToHex(PinHasher.hashPin(pin, salt, PinHasher.LEGACY_ITERATIONS))
        val saltHex = PinHasher.bytesToHex(salt)

        assertFalse(legacyHashHex.startsWith("v2:"))
        assertFalse(PinHasher.isStoredPinCorrupted(true, saltHex, legacyHashHex))

        val ok = PinHasher.verifyPin(
            enteredPin = pin,
            hasCustomPin = true,
            saltHex = saltHex,
            hashHex = legacyHashHex,
            nowEpochMillis = 1_000_000L,
            currentFailedAttempts = 0,
            lockedUntilEpochMillis = 0L
        )
        assertTrue("Legacy 10k-iteration PIN hash must still verify", ok is PinVerifyResult.Success)

        val wrong = PinHasher.verifyPin(
            enteredPin = "5523",
            hasCustomPin = true,
            saltHex = saltHex,
            hashHex = legacyHashHex,
            nowEpochMillis = 1_000_000L,
            currentFailedAttempts = 0,
            lockedUntilEpochMillis = 0L
        )
        assertTrue(wrong is PinVerifyResult.Incorrect)
    }

    @Test
    fun malformedV2Hash_returnsResetRequired() {
        val saltHex = "00112233445566778899aabbccddeeff"
        val badHashes = listOf(
            "v2:100:" + "a".repeat(64),        // below minimum iterations
            "v2:notanint:" + "a".repeat(64),   // non-numeric iterations
            "v2:120000:abcd",                  // wrong digest length
            "v2:120000"                        // missing digest
        )
        for (bad in badHashes) {
            assertTrue("Expected corrupted for $bad", PinHasher.isStoredPinCorrupted(true, saltHex, bad))
            val res = PinHasher.verifyPin("1234", true, saltHex, bad, 1_000_000L, 0, 0L)
            assertTrue("Expected ResetRequired for $bad", res is PinVerifyResult.ResetRequired)
        }
    }
}
