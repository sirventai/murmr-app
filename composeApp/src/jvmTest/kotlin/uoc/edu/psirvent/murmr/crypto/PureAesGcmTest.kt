package uoc.edu.psirvent.murmr.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PureAesGcmTest {

    @Test
    fun nistTestCase14_256bitKey_emptyPlaintext_emptyAad() {

        val key = hex("0000000000000000000000000000000000000000000000000000000000000000")
        val iv = hex("000000000000000000000000")
        val pt = ByteArray(0)
        val expectedTag = hex("530f8afbc74536b9a963b4f1c4cb738b")

        val ct = PureAesGcm.encrypt(key, iv, pt, ByteArray(0))
        assertEquals(0, ct.ciphertext.size)
        assertContentEquals(expectedTag, ct.authTag, "tag mismatch NIST TC14")
    }

    @Test
    fun nistTestCase15_256bitKey_oneBlock() {

        val key = hex("0000000000000000000000000000000000000000000000000000000000000000")
        val iv = hex("000000000000000000000000")
        val pt = hex("00000000000000000000000000000000")
        val expectedCt = hex("cea7403d4d606b6e074ec5d3baf39d18")
        val expectedTag = hex("d0d1c8a799996bf0265b98b5d48ab919")

        val ct = PureAesGcm.encrypt(key, iv, pt, ByteArray(0))
        assertContentEquals(expectedCt, ct.ciphertext, "ciphertext mismatch NIST TC15")
        assertContentEquals(expectedTag, ct.authTag, "tag mismatch NIST TC15")
    }

    @Test
    fun roundTrip_AesGcm() {

        val key = SecureRandom.nextBytes(32)
        val iv = SecureRandom.nextBytes(12)
        val pt = "Murmr forward secrecy 🔒".encodeToByteArray()
        val aad = "session-uuid|sender→recipient".encodeToByteArray()

        val ct = PureAesGcm.encrypt(key, iv, pt, aad)
        val recovered = PureAesGcm.decrypt(key, iv, ct.ciphertext, ct.authTag, aad)
        assertContentEquals(pt, recovered)
    }

    @Test
    fun decrypt_rejectsBadTag() {
        val key = SecureRandom.nextBytes(32)
        val iv = SecureRandom.nextBytes(12)
        val ct = PureAesGcm.encrypt(key, iv, "hola".encodeToByteArray(), ByteArray(0))

        val badTag = ct.authTag.copyOf().also { it[0] = (it[0].toInt() xor 0x01).toByte() }
        assertFailsWith<AeadDecryptionFailed> {
            PureAesGcm.decrypt(key, iv, ct.ciphertext, badTag, ByteArray(0))
        }
    }

    @Test
    fun crossCompat_pureAesGcm_decrypts_jvmCipher() {

        val key = SecureRandom.nextBytes(32)
        val iv = SecureRandom.nextBytes(12)
        val plaintext = "Murmr cross-platform AES-GCM".encodeToByteArray()
        val aad = "test-aad".encodeToByteArray()

        val jvmCipher = AesGcm.encrypt(key, iv, plaintext, aad)
        val recovered = PureAesGcm.decrypt(key, iv, jvmCipher.ciphertext, jvmCipher.authTag, aad)
        assertContentEquals(plaintext, recovered, "PureAesGcm no descifra lo que produjo JVM Cipher")
    }

    @Test
    fun crossCompat_jvmCipher_decrypts_pureAesGcm() {
        val key = SecureRandom.nextBytes(32)
        val iv = SecureRandom.nextBytes(12)
        val plaintext = "Murmr cross-platform AES-GCM".encodeToByteArray()
        val aad = "test-aad".encodeToByteArray()

        val pure = PureAesGcm.encrypt(key, iv, plaintext, aad)
        val recovered = AesGcm.decrypt(key, iv, pure.ciphertext, pure.authTag, aad)
        assertContentEquals(plaintext, recovered, "JVM Cipher no descifra lo que produjo PureAesGcm")
    }

    private fun hex(s: String): ByteArray {
        val out = ByteArray(s.length / 2)
        for (i in out.indices) {
            out[i] = ((Character.digit(s[i * 2], 16) shl 4) + Character.digit(s[i * 2 + 1], 16)).toByte()
        }
        return out
    }
}
