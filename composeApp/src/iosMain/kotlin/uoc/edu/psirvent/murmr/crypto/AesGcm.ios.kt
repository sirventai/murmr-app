package uoc.edu.psirvent.murmr.crypto

actual object AesGcm {
    actual fun encrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray, aad: ByteArray): AeadCiphertext =
        PureAesGcm.encrypt(key, iv, plaintext, aad)

    actual fun decrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray, authTag: ByteArray, aad: ByteArray): ByteArray =
        PureAesGcm.decrypt(key, iv, ciphertext, authTag, aad)
}
