package uoc.edu.psirvent.murmr.crypto

import uoc.edu.psirvent.murmr.domain.model.Sas

object SasGenerator {

    private const val EMOJI_BITS = 5
    private val INFO = "murmr-sas-v1".encodeToByteArray()

    fun derive(sharedSecret: ByteArray, salt: ByteArray? = null): Sas {
        val totalBits = Sas.EMOJI_COUNT * EMOJI_BITS
        val totalBytes = (totalBits + 7) / 8
        val okm = HkdfSha256.derive(
            ikm = sharedSecret,
            salt = salt,
            info = INFO,
            length = totalBytes,
        )
        val bits = BitReader(okm)
        val emojis = (0 until Sas.EMOJI_COUNT).map { Sas.EMOJI_ALPHABET[bits.read(EMOJI_BITS)] }
        return Sas(emojis = emojis)
    }
}

private class BitReader(private val bytes: ByteArray) {
    private var offset = 0

    fun read(bits: Int): Int {
        require(bits in 1..16)
        var value = 0
        for (i in 0 until bits) {
            val byteIndex = (offset + i) ushr 3
            val bitIndex = 7 - ((offset + i) and 7)
            val bit = (bytes[byteIndex].toInt() ushr bitIndex) and 1
            value = (value shl 1) or bit
        }
        offset += bits
        return value
    }
}
