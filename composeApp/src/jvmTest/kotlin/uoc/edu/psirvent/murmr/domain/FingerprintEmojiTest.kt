package uoc.edu.psirvent.murmr.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import uoc.edu.psirvent.murmr.domain.model.Sas

class FingerprintEmojiTest {

    private fun hexDigit(c: Char): Int = when (c) {
        in '0'..'9' -> c.code - '0'.code
        in 'a'..'f' -> c.code - 'a'.code + 10
        in 'A'..'F' -> c.code - 'A'.code + 10
        else -> 0
    }

    private fun fingerprintToEmojis(fingerprint: String, count: Int): List<String> {
        val cleaned = fingerprint.filter { it != ' ' }
        if (cleaned.isEmpty()) return List(count) { "❓" }
        val bytes = ByteArray(cleaned.length / 2) { i ->
            val hi = hexDigit(cleaned[2 * i])
            val lo = hexDigit(cleaned[2 * i + 1])
            ((hi shl 4) or lo).toByte()
        }
        val out = mutableListOf<String>()
        var bitIdx = 0
        val totalBits = bytes.size * 8
        while (out.size < count && bitIdx + 5 <= totalBits) {
            var v = 0
            for (k in 0 until 5) {
                val bit = (bytes[(bitIdx + k) ushr 3].toInt() ushr (7 - ((bitIdx + k) and 7))) and 1
                v = (v shl 1) or bit
            }
            out.add(Sas.EMOJI_ALPHABET[v % Sas.EMOJI_ALPHABET.size])
            bitIdx += 5
        }
        while (out.size < count) out.add("❓")
        return out
    }

    @Test
    fun deterministic() {
        val fp = "AB CD EF 01 23 45 67 89"
        val a = fingerprintToEmojis(fp, 8)
        val b = fingerprintToEmojis(fp, 8)
        assertEquals(a, b)
    }

    @Test
    fun returnsRequestedLength() {
        val fp = "AB CD EF 01 23 45 67 89"
        assertEquals(8, fingerprintToEmojis(fp, 8).size)
        assertEquals(4, fingerprintToEmojis(fp, 4).size)
    }

    @Test
    fun differentFingerprintsDifferentEmojis() {
        val fp1 = "AB CD EF 01 23 45 67 89"
        val fp2 = "FF FF FF FF FF FF FF FF"
        assertNotEquals(fingerprintToEmojis(fp1, 8), fingerprintToEmojis(fp2, 8))
    }

    @Test
    fun emptyFingerprintFallsBackToQuestionMarks() {
        val emojis = fingerprintToEmojis("", 5)
        assertEquals(5, emojis.size)
        assertTrue(emojis.all { it == "❓" })
    }

    @Test
    fun caseInsensitiveHex() {
        val a = fingerprintToEmojis("ABCDEF", 4)
        val b = fingerprintToEmojis("abcdef", 4)
        assertEquals(a, b)
    }

    @Test
    fun allEmojisFromSasAlphabet() {
        val fp = "00 11 22 33 44 55 66 77 88 99 AA BB CC DD EE FF"
        val emojis = fingerprintToEmojis(fp, 8)
        assertTrue(emojis.all { it in Sas.EMOJI_ALPHABET || it == "❓" })
    }
}
