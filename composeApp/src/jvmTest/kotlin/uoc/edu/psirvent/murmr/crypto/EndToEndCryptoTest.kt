package uoc.edu.psirvent.murmr.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EndToEndCryptoTest {

    @Test
    fun fullPairingAndMessageRoundtrip() {
        val alice = X25519KeyExchange.generateKeyPair()
        val bob = X25519KeyExchange.generateKeyPair()

        val sharedAlice = X25519KeyExchange.deriveSharedSecret(alice.privateKey, bob.publicKey)
        val sharedBob = X25519KeyExchange.deriveSharedSecret(bob.privateKey, alice.publicKey)
        assertContentEquals(sharedAlice, sharedBob, "ECDH shared secret mismatch")

        val nonceA = SecureRandom.nextBytes(16)
        val nonceB = SecureRandom.nextBytes(16)
        val salt = nonceA + nonceB

        val sessionKeyA = HkdfSha256.derive(sharedAlice, salt, "murmr-session-v1".encodeToByteArray(), AES_KEY_BYTES)
        val sessionKeyB = HkdfSha256.derive(sharedBob, salt, "murmr-session-v1".encodeToByteArray(), AES_KEY_BYTES)
        assertContentEquals(sessionKeyA, sessionKeyB)

        val sasA = SasGenerator.derive(sharedAlice, salt)
        val sasB = SasGenerator.derive(sharedBob, salt)
        assertEquals(sasA.encoded(), sasB.encoded())

        val iv = newAesGcmIv()
        val plaintext = "Hola Bob, soy Alice. Test E2EE.".encodeToByteArray()
        val ct = AesGcm.encrypt(sessionKeyA, iv, plaintext, aad = nonceA)
        val recovered = AesGcm.decrypt(sessionKeyB, iv, ct.ciphertext, ct.authTag, aad = nonceA)
        assertContentEquals(plaintext, recovered)

        val tampered = ct.ciphertext.copyOf().also { it[0] = (it[0].toInt() xor 0xFF).toByte() }
        assertFailsWith<AeadDecryptionFailed> {
            AesGcm.decrypt(sessionKeyB, iv, tampered, ct.authTag, aad = nonceA)
        }
    }
}
