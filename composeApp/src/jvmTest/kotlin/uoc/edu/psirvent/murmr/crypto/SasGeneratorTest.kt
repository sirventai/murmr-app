package uoc.edu.psirvent.murmr.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SasGeneratorTest {

    @Test
    fun deterministicForSameSecretAndSalt() {
        val secret = SecureRandom.nextBytes(32)
        val salt = SecureRandom.nextBytes(16)
        val a = SasGenerator.derive(secret, salt)
        val b = SasGenerator.derive(secret, salt)
        assertEquals(a.encoded(), b.encoded())
    }

    @Test
    fun differentSecretsProduceDifferentSas() {
        val s1 = SasGenerator.derive(ByteArray(32) { 0x01 }, byteArrayOf(0))
        val s2 = SasGenerator.derive(ByteArray(32) { 0x02 }, byteArrayOf(0))
        assertNotEquals(s1.encoded(), s2.encoded())
    }

    @Test
    fun outputShape() {
        val sas = SasGenerator.derive(ByteArray(32), byteArrayOf())
        assertEquals(5, sas.emojis.size)
        sas.emojis.forEach { e ->
            assert(e in uoc.edu.psirvent.murmr.domain.model.Sas.EMOJI_ALPHABET) {
                "Emoji $e fuera del alfabeto soportado"
            }
        }
    }
}
