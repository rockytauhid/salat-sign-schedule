// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class PeerHmacAuthTest {

    @Test
    fun verifyRequest_acceptsValidSignatureAndRejectsTamperReplayWrongTargetAndSkew() {
        var now = 1_700_000_000_000L
        val auth = PeerHmacAuth(nowMillisProvider = { now })
        val secret = PeerHmacAuth.generateGroupSecretHex()
        val body = """{"hello":"world"}""".toByteArray()
        val nonce = PeerHmacAuth.generateNonce()
        val sig = PeerHmacAuth.computeSignature(secret, "POST", "/api/v1/peer/sync", "tv-a", "tv-b", now, nonce, body)

        assertTrue(auth.verifyRequest(secret, "POST", "/api/v1/peer/sync", "tv-b", "tv-a", "tv-b", now.toString(), nonce, sig, body, "192.168.4.10"))

        // Replay of the exact same nonce MUST be rejected
        assertFalse(auth.verifyRequest(secret, "POST", "/api/v1/peer/sync", "tv-b", "tv-a", "tv-b", now.toString(), nonce, sig, body, "192.168.4.10"))

        // Review 4 L3: Replaying to a different target deviceId MUST be rejected
        val nonceTarget = PeerHmacAuth.generateNonce()
        val sigTarget = PeerHmacAuth.computeSignature(secret, "POST", "/api/v1/peer/sync", "tv-a", "tv-b", now, nonceTarget, body)
        assertFalse(auth.verifyRequest(secret, "POST", "/api/v1/peer/sync", "tv-c", "tv-a", "tv-b", now.toString(), nonceTarget, sigTarget, body, "192.168.4.11"))

        // Tampered body MUST be rejected
        val nonce2 = PeerHmacAuth.generateNonce()
        val sig2 = PeerHmacAuth.computeSignature(secret, "POST", "/api/v1/peer/sync", "tv-a", "tv-b", now, nonce2, body)
        assertFalse(auth.verifyRequest(secret, "POST", "/api/v1/peer/sync", "tv-b", "tv-a", "tv-b", now.toString(), nonce2, sig2, """{"hello":"evil"}""".toByteArray(), "192.168.4.12"))

        // Timestamp outside ±120s window MUST be rejected
        val staleTs = now - 200_000L
        val nonce3 = PeerHmacAuth.generateNonce()
        val sig3 = PeerHmacAuth.computeSignature(secret, "POST", "/api/v1/peer/sync", "tv-a", "tv-b", staleTs, nonce3, body)
        assertFalse(auth.verifyRequest(secret, "POST", "/api/v1/peer/sync", "tv-b", "tv-a", "tv-b", staleTs.toString(), nonce3, sig3, body, "192.168.4.13"))
    }

    @Test
    fun verifyRequest_unsignedTrafficDoesNotGrowNonceStore_andResponseSigningVerifies() {
        val now = 1_700_000_000_000L
        val auth = PeerHmacAuth(nowMillisProvider = { now })
        val secret = PeerHmacAuth.generateGroupSecretHex()
        val body = "{}".toByteArray()

        // Review 4 M2: 20 unsigned requests must NOT insert anything into seenNonces
        repeat(20) { i ->
            assertFalse(
                auth.verifyRequest(
                    secret, "GET", "/api/v1/peer/sync", "tv-b", "tv-evil", "tv-b",
                    now.toString(), "fake-nonce-$i-000000", "0".repeat(64), body, "192.168.4.${50 + i}"
                )
            )
        }
        assertEquals(0, auth.seenNonceCount)

        // Review 4 H2: Response signature verifies matching body + nonce and rejects tampered body
        val reqNonce = PeerHmacAuth.generateNonce()
        val respBody = """{"configVersion":42}""".toByteArray()
        val respSig = PeerHmacAuth.computeResponseSignature(secret, "tv-b", reqNonce, now, respBody)
        assertTrue(PeerHmacAuth.verifyResponse(secret, "tv-b", reqNonce, now.toString(), respSig, respBody, now))
        assertFalse(PeerHmacAuth.verifyResponse(secret, "tv-b", reqNonce, now.toString(), respSig, """{"configVersion":999}""".toByteArray(), now))
        assertFalse(PeerHmacAuth.verifyResponse(secret, "tv-wrong", reqNonce, now.toString(), respSig, respBody, now))
    }

    @Test
    fun groupJoinManager_enforces8CharCodeGlobalInvalidationAndEcdhEncryption() {
        var now = 1_700_000_000_000L
        val mgr = GroupJoinManager(nowMillisProvider = { now }, ttlMillis = 60_000L)
        val active = mgr.generateJoinCode()
        assertEquals(8, active.code.length)

        // Review 4 H1: 5 wrong guesses across different IPs invalidate the active code globally
        repeat(GroupJoinManager.MAX_GLOBAL_FAILURES_PER_CODE) { i ->
            assertTrue(mgr.redeemCode("ZZZZZZZZ", "192.168.4.${10 + i}") is JoinCodeRedeemResult.InvalidCode)
        }
        assertNull("Code must be invalidated after 5 global failures", mgr.currentJoinCode())
        assertTrue(mgr.redeemCode(active.code, "192.168.4.200") is JoinCodeRedeemResult.NoActiveCode)

        // Fresh code succeeds once with ECDH + AES-GCM round-trip
        val fresh = mgr.generateJoinCode()
        val res = mgr.redeemCode(fresh.code, "192.168.4.200")
        assertTrue(res is JoinCodeRedeemResult.Success)
        assertTrue("Second use must fail", mgr.redeemCode(fresh.code, "192.168.4.200") is JoinCodeRedeemResult.NoActiveCode)

        val clientKp = PeerJoinCrypto.generateEphemeralKeyPair()
        val secretMsg = """{"groupSecretHex":"${"a".repeat(64)}","peers":[]}""".toByteArray(StandardCharsets.UTF_8)
        val envelope = PeerJoinCrypto.encryptForPeer(PeerJoinCrypto.encodePublicKey(clientKp), fresh.code, secretMsg)
        val decrypted = PeerJoinCrypto.decryptFromServer(
            clientKp, envelope.serverPublicKeyBase64, envelope.ivBase64, envelope.ciphertextBase64, fresh.code
        )
        assertEquals(secretMsg.toString(StandardCharsets.UTF_8), decrypted.toString(StandardCharsets.UTF_8))

        // Expiry check
        val expiring = mgr.generateJoinCode()
        now += 61_000L
        assertTrue(mgr.redeemCode(expiring.code, "192.168.4.200") is JoinCodeRedeemResult.Expired)
    }

    @Test
    fun review5_joinCodeProofEciesRotationAndPerIpSignatureLockout() {
        var now = 1_700_000_000_000L
        val mgr = GroupJoinManager(nowMillisProvider = { now })
        val active = mgr.generateJoinCode()
        val clientKp = PeerJoinCrypto.generateEphemeralKeyPair()
        val clientPub = PeerJoinCrypto.encodePublicKey(clientKp)

        // R5-2 + R6-3: Proof binds deviceId, ephemeral key, AND identityPublicKeyBase64 (tampered identity key fails!)
        val idPub = PeerJoinCrypto.encodePublicKey(PeerJoinCrypto.generateEphemeralKeyPair())
        val evilIdPub = PeerJoinCrypto.encodePublicKey(PeerJoinCrypto.generateEphemeralKeyPair())
        val validProof = PeerJoinCrypto.computeJoinCodeProof(active.code, "tv-join", clientPub, idPub)
        assertTrue(mgr.redeemByProof("tv-join", clientPub, validProof, "192.168.4.55", evilIdPub) is JoinCodeRedeemResult.InvalidCode)
        assertTrue(mgr.redeemByProof("tv-join", clientPub, validProof, "192.168.4.55", idPub) is JoinCodeRedeemResult.Success)
        assertTrue(mgr.redeemByProof("tv-join", clientPub, validProof, "192.168.4.55", idPub) is JoinCodeRedeemResult.NoActiveCode)

        // R5-1: ECIES rotation envelope can ONLY be decrypted by the intended peer's private key (not a removed TV)
        val goodPeerKp = PeerJoinCrypto.generateEphemeralKeyPair()
        val removedPeerKp = PeerJoinCrypto.generateEphemeralKeyPair()
        val rotPlain = """{"newGroupSecretHex":"${"b".repeat(64)}"}""".toByteArray(StandardCharsets.UTF_8)
        val env = PeerJoinCrypto.encryptForRecipientPublicKey(
            recipientPublicKeyBase64 = PeerJoinCrypto.encodePublicKey(goodPeerKp),
            recipientDeviceId = "tv-good",
            plaintextUtf8 = rotPlain
        )
        val decrypted = PeerJoinCrypto.decryptWithRecipientPrivateKey(
            ownPrivateKeyBase64 = PeerJoinCrypto.encodePrivateKey(goodPeerKp),
            ephemeralPublicKeyBase64 = env.serverPublicKeyBase64,
            ivBase64 = env.ivBase64,
            ciphertextBase64 = env.ciphertextBase64,
            ownDeviceId = "tv-good"
        )
        assertEquals(rotPlain.toString(StandardCharsets.UTF_8), decrypted.toString(StandardCharsets.UTF_8))

        val removedTried = runCatching {
            PeerJoinCrypto.decryptWithRecipientPrivateKey(
                ownPrivateKeyBase64 = PeerJoinCrypto.encodePrivateKey(removedPeerKp),
                ephemeralPublicKeyBase64 = env.serverPublicKeyBase64,
                ivBase64 = env.ivBase64,
                ciphertextBase64 = env.ciphertextBase64,
                ownDeviceId = "tv-good"
            )
        }
        assertTrue("Removed TV with different EC key must NOT decrypt rotated secret", removedTried.isFailure)

        // R5-6 / M2: Per-IP signature lockout after 10 bad signatures blocks even a valid signature from that IP for 60s
        val auth = PeerHmacAuth(nowMillisProvider = { now })
        val secret = PeerHmacAuth.generateGroupSecretHex()
        repeat(PeerHmacAuth.MAX_FAILED_SIGS_PER_IP) { i ->
            assertFalse(
                auth.verifyRequest(
                    secret, "GET", "/api/v1/peer/ping", "tv-b", "tv-bad", "tv-b",
                    now.toString(), "bad-nonce-$i-000000", "f".repeat(64), ByteArray(0), "192.168.4.77"
                )
            )
        }
        val goodNonce = PeerHmacAuth.generateNonce()
        val goodSig = PeerHmacAuth.computeSignature(secret, "GET", "/api/v1/peer/ping", "tv-a", "tv-b", now, goodNonce, ByteArray(0))
        assertFalse("Locked IP must be rejected even with valid signature", auth.verifyRequest(secret, "GET", "/api/v1/peer/ping", "tv-b", "tv-a", "tv-b", now.toString(), goodNonce, goodSig, ByteArray(0), "192.168.4.77"))
        now += 61_000L
        val goodNonceAfter = PeerHmacAuth.generateNonce()
        val goodSigAfter = PeerHmacAuth.computeSignature(secret, "GET", "/api/v1/peer/ping", "tv-a", "tv-b", now, goodNonceAfter, ByteArray(0))
        assertTrue("After lockout expires, valid signature succeeds", auth.verifyRequest(secret, "GET", "/api/v1/peer/ping", "tv-b", "tv-a", "tv-b", now.toString(), goodNonceAfter, goodSigAfter, ByteArray(0), "192.168.4.77"))
    }

    @Test
    fun peerAssetValidator_rejectsPathTraversalAndUnsafeNames() {
        assertTrue(PeerAssetValidator.isSafeFileName("slide_1234.jpg"))
        assertTrue(PeerAssetValidator.isSafeFileName("poster-01.webp"))
        assertFalse(PeerAssetValidator.isSafeFileName("../settings.preferences_pb"))
        assertFalse(PeerAssetValidator.isSafeFileName("sub/file.png"))
        assertFalse(PeerAssetValidator.isSafeFileName("sub\\file.png"))
        assertFalse(PeerAssetValidator.isSafeFileName(".hidden"))
        assertFalse(PeerAssetValidator.isSafeFileName(""))
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
