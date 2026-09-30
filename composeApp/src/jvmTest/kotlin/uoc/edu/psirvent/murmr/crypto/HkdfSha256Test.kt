package uoc.edu.psirvent.murmr.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class HkdfSha256Test {

    private fun hex(s: String): ByteArray =
        s.replace(" ", "").chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun rfc5869_case1() {
        val ikm = hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b")
        val salt = hex("000102030405060708090a0b0c")
        val info = hex("f0f1f2f3f4f5f6f7f8f9")
        val l = 42
        val expectedPrk = hex("077709362c2e32df0ddc3f0dc47bba6390b6c73bb50f9c3122ec844ad7c2b3e5")
        val expectedOkm = hex(
            "3cb25f25faacd57a90434f64d0362f2a" +
                "2d2d0a90cf1a5a4c5db02d56ecc4c5bf" +
                "34007208d5b887185865",
        )

        val prk = HkdfSha256.extract(salt, ikm)
        assertContentEquals(expectedPrk, prk, "PRK mismatch")

        val okm = HkdfSha256.expand(prk, info, l)
        assertContentEquals(expectedOkm, okm, "OKM mismatch")

        assertContentEquals(expectedOkm, HkdfSha256.derive(ikm, salt, info, l))
    }

    @Test
    fun rfc5869_case3() {
        val ikm = hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b")
        val salt = ByteArray(0)
        val info = ByteArray(0)
        val l = 42
        val expectedOkm = hex(
            "8da4e775a563c18f715f802a063c5a31" +
                "b8a11f5c5ee1879ec3454e5f3c738d2d" +
                "9d201395faa4b61a96c8",
        )
        assertContentEquals(expectedOkm, HkdfSha256.derive(ikm, salt, info, l))
    }

    @Test
    fun expandLengthRespected() {
        val prk = ByteArray(32) { 0x42 }
        for (l in 1..255 * 32 step 31) {
            assertEquals(l, HkdfSha256.expand(prk, byteArrayOf(), l).size)
        }
    }
}
