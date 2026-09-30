package uoc.edu.psirvent.murmr.crypto

const val AES_KEY_BYTES = 32
const val AES_GCM_IV_BYTES = 12
const val AES_GCM_TAG_BYTES = 16

data class AeadCiphertext(val ciphertext: ByteArray, val authTag: ByteArray) {
    init {
        require(authTag.size == AES_GCM_TAG_BYTES) { "tag must be $AES_GCM_TAG_BYTES bytes" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AeadCiphertext) return false
        return ciphertext.contentEquals(other.ciphertext) && authTag.contentEquals(other.authTag)
    }

    override fun hashCode(): Int = 31 * ciphertext.contentHashCode() + authTag.contentHashCode()
}

class AeadDecryptionFailed(message: String) : RuntimeException(message)

expect object AesGcm {
    fun encrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray, aad: ByteArray = ByteArray(0)): AeadCiphertext
    fun decrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray, authTag: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray
}

fun newAesGcmIv(): ByteArray = SecureRandom.nextBytes(AES_GCM_IV_BYTES)
