package uoc.edu.psirvent.murmr.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AesGcmTest {

    private fun hex(s: String): ByteArray =
        s.replace(" ", "").chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun nist_case14_emptyMessage() {
        val key = hex("00000000000000000000000000000000" + "00000000000000000000000000000000")
        val iv = hex("000000000000000000000000")
        val plaintext = ByteArray(0)
        val expectedTag = hex("530f8afbc74536b9a963b4f1c4cb738b")

        val out = AesGcm.encrypt(key, iv, plaintext)
        assertEquals(0, out.ciphertext.size)
        assertContentEquals(expectedTag, out.authTag)

        val recovered = AesGcm.decrypt(key, iv, out.ciphertext, out.authTag)
        assertContentEquals(plaintext, recovered)
    }

    @Test
    fun roundTripRandomMessage() {
        val key = SecureRandom.nextBytes(AES_KEY_BYTES)
        val iv = newAesGcmIv()
        val plain = "Hola Murmr! 🔒 mensaje cifrado.".encodeToByteArray()
        val aad = "session-1".encodeToByteArray()

        val out = AesGcm.encrypt(key, iv, plain, aad)
        assertEquals(AES_GCM_TAG_BYTES, out.authTag.size)

        assertTrue(!out.ciphertext.contentEquals(plain))

        val recovered = AesGcm.decrypt(key, iv, out.ciphertext, out.authTag, aad)
        assertContentEquals(plain, recovered)
    }

    @Test
    fun decryptFailsOnTamperedCiphertext() {
        val key = SecureRandom.nextBytes(AES_KEY_BYTES)
        val iv = newAesGcmIv()
        val out = AesGcm.encrypt(key, iv, "secreto".encodeToByteArray())
        val tampered = out.ciphertext.copyOf().also { it[0] = (it[0].toInt() xor 0x01).toByte() }
        assertFailsWith<AeadDecryptionFailed> {
            AesGcm.decrypt(key, iv, tampered, out.authTag)
        }
    }

    @Test
    fun decryptFailsOnTamperedAad() {
        val key = SecureRandom.nextBytes(AES_KEY_BYTES)
        val iv = newAesGcmIv()
        val out = AesGcm.encrypt(key, iv, "msg".encodeToByteArray(), aad = "context-A".encodeToByteArray())
        assertFailsWith<AeadDecryptionFailed> {
            AesGcm.decrypt(key, iv, out.ciphertext, out.authTag, aad = "context-B".encodeToByteArray())
        }
    }

    @Test
    fun rejectsWrongKeySize() {
        assertFailsWith<IllegalArgumentException> {
            AesGcm.encrypt(ByteArray(16), newAesGcmIv(), ByteArray(0))
        }
    }
}
