package uoc.edu.psirvent.murmr.crypto

import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

actual object AesGcm {

    actual fun encrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray, aad: ByteArray): AeadCiphertext {
        require(key.size == AES_KEY_BYTES) { "key must be $AES_KEY_BYTES bytes" }
        require(iv.size == AES_GCM_IV_BYTES) { "iv must be $AES_GCM_IV_BYTES bytes" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(AES_GCM_TAG_BYTES * 8, iv))
        if (aad.isNotEmpty()) cipher.updateAAD(aad)
        val combined = cipher.doFinal(plaintext)
        val ctLen = combined.size - AES_GCM_TAG_BYTES
        return AeadCiphertext(
            ciphertext = combined.copyOfRange(0, ctLen),
            authTag = combined.copyOfRange(ctLen, combined.size),
        )
    }

    actual fun decrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray, authTag: ByteArray, aad: ByteArray): ByteArray {
        require(key.size == AES_KEY_BYTES) { "key must be $AES_KEY_BYTES bytes" }
        require(iv.size == AES_GCM_IV_BYTES) { "iv must be $AES_GCM_IV_BYTES bytes" }
        require(authTag.size == AES_GCM_TAG_BYTES) { "tag must be $AES_GCM_TAG_BYTES bytes" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(AES_GCM_TAG_BYTES * 8, iv))
        if (aad.isNotEmpty()) cipher.updateAAD(aad)
        val combined = ciphertext + authTag
        return try {
            cipher.doFinal(combined)
        } catch (t: AEADBadTagException) {
            throw AeadDecryptionFailed("authentication tag mismatch: ${t.message}")
        }
    }
}
