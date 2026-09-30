package uoc.edu.psirvent.murmr.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForwardSecrecyTest {

    private val info = "murmr-rotate-v1".encodeToByteArray()

    private fun rotate(prev: ByteArray, sessionId: String, idx: Int): ByteArray {
        val salt = "$sessionId|$idx".encodeToByteArray()
        return HkdfSha256.derive(prev, salt, info, 32)
    }

    @Test
    fun rotationIsDeterministic() {
        val k0 = SecureRandom.nextBytes(32)
        val a = rotate(k0, "session-abc", 1)
        val b = rotate(k0, "session-abc", 1)
        assertContentEquals(a, b, "Rotación no determinista - ambos peers derivarían keys distintas")
        assertEquals(32, a.size)
    }

    @Test
    fun differentIndexProducesDifferentKey() {
        val k0 = SecureRandom.nextBytes(32)
        val a = rotate(k0, "session-abc", 1)
        val b = rotate(k0, "session-abc", 2)
        assertFalse(a.contentEquals(b), "Index distinto debe producir key distinta")
    }

    @Test
    fun differentSessionIdProducesDifferentKey() {
        val k0 = SecureRandom.nextBytes(32)
        val a = rotate(k0, "session-abc", 1)
        val b = rotate(k0, "session-xyz", 1)
        assertFalse(a.contentEquals(b), "SessionId distinto debe producir key distinta")
    }

    @Test
    fun rotationChainPreservesKeyLength() {
        var k = SecureRandom.nextBytes(32)
        for (i in 1..50) {
            k = rotate(k, "chain-test", i)
            assertEquals(32, k.size, "Rotación $i degrada longitud")
        }
    }

    @Test
    fun rotatedKeyEncryptsAndDecryptsRoundTrip() {
        val k0 = SecureRandom.nextBytes(32)
        val k1 = rotate(k0, "session-id", 1)
        val iv = newAesGcmIv()
        val plain = "mensaje tras rotación".encodeToByteArray()
        val ct = AesGcm.encrypt(k1, iv, plain)
        val recovered = AesGcm.decrypt(k1, iv, ct.ciphertext, ct.authTag)
        assertContentEquals(plain, recovered)

        var failed = false
        try {
            AesGcm.decrypt(k0, iv, ct.ciphertext, ct.authTag)
        } catch (_: Throwable) {
            failed = true
        }
        assertTrue(failed, "Key previa no debería descifrar mensajes con la nueva key")
    }
}
