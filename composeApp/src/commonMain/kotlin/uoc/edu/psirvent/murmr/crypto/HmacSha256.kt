package uoc.edu.psirvent.murmr.crypto

expect object HmacSha256 {
    fun mac(key: ByteArray, data: ByteArray): ByteArray
}

const val SHA256_BLOCK_BYTES = 64
const val SHA256_OUTPUT_BYTES = 32
