package uoc.edu.psirvent.murmr.crypto

object HkdfSha256 {

    fun extract(salt: ByteArray?, ikm: ByteArray): ByteArray {

        val effectiveSalt = if (salt == null || salt.isEmpty()) ByteArray(SHA256_OUTPUT_BYTES) else salt
        return HmacSha256.mac(effectiveSalt, ikm)
    }

    fun expand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length > 0 && length <= 255 * SHA256_OUTPUT_BYTES) {
            "Length out of range: $length"
        }
        val n = (length + SHA256_OUTPUT_BYTES - 1) / SHA256_OUTPUT_BYTES
        val output = ByteArray(length)
        var prev = ByteArray(0)
        var copied = 0
        for (i in 1..n) {
            val data = ByteArray(prev.size + info.size + 1)
            prev.copyInto(data, 0)
            info.copyInto(data, prev.size)
            data[data.size - 1] = i.toByte()
            val t = HmacSha256.mac(prk, data)
            val toCopy = minOf(SHA256_OUTPUT_BYTES, length - copied)
            t.copyInto(output, copied, 0, toCopy)
            copied += toCopy
            prev = t
        }
        return output
    }

    fun derive(ikm: ByteArray, salt: ByteArray?, info: ByteArray, length: Int): ByteArray {
        val prk = extract(salt, ikm)
        return expand(prk, info, length)
    }
}
