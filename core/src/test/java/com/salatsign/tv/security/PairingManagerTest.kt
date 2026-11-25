// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingManagerTest {

    @Test
    fun redeemCode_isSingleUseAndProducesHashedToken() {
        var now = 1_700_000_000_000L
        val mgr = PairingManager(nowMillisProvider = { now })

        val active = mgr.generatePairingCode(RemoteRole.MANAGE)
        assertEquals(6, active.code.length)
        assertTrue(active.code.all { it.isDigit() })

        val first = mgr.redeemCode(active.code, "Imam Phone", "192.168.4.50")
        assertTrue(first is PairRedeemResult.Success)
        val success = first as PairRedeemResult.Success
        assertEquals("Imam Phone", success.record.deviceName)
        assertEquals(RemoteRole.MANAGE, success.record.role)
        assertEquals(64, success.rawToken.length) // 32 bytes hex
        assertNotEquals(success.rawToken, success.record.tokenHash)
        assertEquals(PairingManager.sha256Hex(success.rawToken), success.record.tokenHash)

        // Second redeem of the same code must fail (single-use)
        val second = mgr.redeemCode(active.code, "Second Phone", "192.168.4.51")
        assertTrue(second is PairRedeemResult.NoActivePairing)
    }

    @Test
    fun redeemCode_enforcesExpiryAndPerIpRateLimitingWithoutGlobalLockout() {
        var now = 1_700_000_000_000L
        val mgr = PairingManager(nowMillisProvider = { now }, codeTtlMillis = 120_000L)
        val active = mgr.generatePairingCode(RemoteRole.MANAGE)
        val wrongCode = if (active.code == "000000") "999999" else "000000"

        // 5 wrong attempts from attacker IP lock out ONLY attacker IP
        repeat(PairingManager.MAX_WRONG_CODES_PER_IP) {
            assertTrue(mgr.redeemCode(wrongCode, "Bad", "192.168.4.99") is PairRedeemResult.InvalidCode)
        }
        val locked = mgr.redeemCode(active.code, "Bad", "192.168.4.99")
        assertTrue("Attacker IP must be rate-limited", locked is PairRedeemResult.RateLimited)

        // Legitimate admin IP is NOT locked out and can still pair
        val legit = mgr.redeemCode(active.code, "Admin Phone", "192.168.4.20")
        assertTrue("Legitimate IP must not be blocked by attacker IP", legit is PairRedeemResult.Success)

        // Expiry check
        val expiring = mgr.generatePairingCode(RemoteRole.NOTIFY)
        now += 121_000L
        assertTrue(mgr.redeemCode(expiring.code, "Late Phone", "192.168.4.20") is PairRedeemResult.Expired)
    }

    @Test
    fun findMatchingActiveDevice_respectsRevocationAndInactivityExpiry() {
        val now = 1_700_000_000_000L
        val rawToken = "a".repeat(64)
        val active = PairedDeviceRecord(
            deviceId = "dev_1",
            deviceName = "Phone 1",
            tokenHash = PairingManager.sha256Hex(rawToken),
            role = RemoteRole.MANAGE,
            createdAtEpochMillis = now - 10_000L,
            lastSeenAtEpochMillis = now - (35L * 24 * 60 * 60 * 1000) // 35 days ago
        )

        // Never expires when expiryDays = 0
        assertNotNull(PairingManager.findMatchingActiveDevice(rawToken, listOf(active), now, expiryDays = 0))
        // Still valid under 90-day expiry
        assertNotNull(PairingManager.findMatchingActiveDevice(rawToken, listOf(active), now, expiryDays = 90))
        // Expired under 30-day expiry
        assertNull(PairingManager.findMatchingActiveDevice(rawToken, listOf(active), now, expiryDays = 30))

        // Revoked device is rejected immediately regardless of expiry
        val revoked = active.copy(revokedAtEpochMillis = now)
        assertNull(PairingManager.findMatchingActiveDevice(rawToken, listOf(revoked), now, expiryDays = 0))
    }

    @Test
    fun rolePermissions_enforceHierarchy() {
        assertTrue(RemoteRole.CRITICAL.hasPermission(RemoteRole.MANAGE))
        assertTrue(RemoteRole.MANAGE.hasPermission(RemoteRole.MANAGE))
        assertTrue(RemoteRole.MANAGE.hasPermission(RemoteRole.NOTIFY))
        assertTrue(RemoteRole.NOTIFY.hasPermission(RemoteRole.NOTIFY))
        assertFalse(RemoteRole.NOTIFY.hasPermission(RemoteRole.MANAGE))
        assertFalse(RemoteRole.MANAGE.hasPermission(RemoteRole.CRITICAL))
    }
}
