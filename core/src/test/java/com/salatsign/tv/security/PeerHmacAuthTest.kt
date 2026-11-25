// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeerHmacAuthTest {

    @Test
    fun verifyRequest_acceptsValidSignatureAndRejectsTamperReplayAndSkew() {
        var now = 1_700_000_000_000L
        val auth = PeerHmacAuth(nowMillisProvider = { now })
        val secret = PeerHmacAuth.generateGroupSecretHex()
        val body = """{"hello":"world"}""".toByteArray()
        val nonce = PeerHmacAuth.generateNonce()
        val sig = PeerHmacAuth.computeSignature(secret, "POST", "/api/v1/peer/sync", now, nonce, body)

        assertTrue(auth.verifyRequest(secret, "POST", "/api/v1/peer/sync", now.toString(), nonce, sig, body))

        // Replay of the exact same nonce MUST be rejected
        assertFalse(auth.verifyRequest(secret, "POST", "/api/v1/peer/sync", now.toString(), nonce, sig, body))

        // Tampered body MUST be rejected
        val nonce2 = PeerHmacAuth.generateNonce()
        val sig2 = PeerHmacAuth.computeSignature(secret, "POST", "/api/v1/peer/sync", now, nonce2, body)
        assertFalse(auth.verifyRequest(secret, "POST", "/api/v1/peer/sync", now.toString(), nonce2, sig2, """{"hello":"evil"}""".toByteArray()))

        // Timestamp outside ±120s window MUST be rejected
        val staleTs = now - 200_000L
        val nonce3 = PeerHmacAuth.generateNonce()
        val sig3 = PeerHmacAuth.computeSignature(secret, "POST", "/api/v1/peer/sync", staleTs, nonce3, body)
        assertFalse(auth.verifyRequest(secret, "POST", "/api/v1/peer/sync", staleTs.toString(), nonce3, sig3, body))
    }

    @Test
    fun groupJoinManager_isSingleUseAndEnforcesExpiry() {
        var now = 1_700_000_000_000L
        val mgr = GroupJoinManager(nowMillisProvider = { now }, ttlMillis = 60_000L)
        val active = mgr.generateJoinCode()
        assertEquals(6, active.code.length)
        assertTrue(mgr.consumeIfValid(active.code))
        assertFalse("Second use must fail", mgr.consumeIfValid(active.code))

        val expiring = mgr.generateJoinCode()
        now += 61_000L
        assertFalse("Expired code must fail", mgr.consumeIfValid(expiring.code))
    }

    @Test
    fun conflictResolver_prefersHigherVersionAndDeviceIdTiebreaker() {
        assertTrue(GroupSyncConflictResolver.shouldAdoptPeerConfig(1L, "tv-a", 2L, "tv-b"))
        assertFalse(GroupSyncConflictResolver.shouldAdoptPeerConfig(3L, "tv-a", 2L, "tv-b"))
        assertTrue(GroupSyncConflictResolver.shouldAdoptPeerConfig(2L, "tv-a", 2L, "tv-b"))
        assertFalse(GroupSyncConflictResolver.shouldAdoptPeerConfig(2L, "tv-b", 2L, "tv-a"))
    }

    @Test
    fun mergePairedDevices_revocationTombstoneAlwaysWins() {
        val now = 1_700_000_000_000L
        val activeLocal = PairedDeviceRecord("dev_1", "Phone", "hash1", RemoteRole.MANAGE, now - 5000, now - 1000, null)
        val revokedPeer = activeLocal.copy(revokedAtEpochMillis = now - 500)

        val merged1 = GroupSyncConflictResolver.mergePairedDevices(listOf(activeLocal), listOf(revokedPeer))
        assertEquals(1, merged1.size)
        assertTrue(merged1.first().isRevoked)
        assertNotNull(merged1.first().revokedAtEpochMillis)

        // Reverse order: offline peer still thinks device is active, local has tombstone -> tombstone still wins!
        val merged2 = GroupSyncConflictResolver.mergePairedDevices(listOf(revokedPeer), listOf(activeLocal))
        assertTrue(merged2.first().isRevoked)
    }
}
