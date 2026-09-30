package uoc.edu.psirvent.murmr.crypto

import io.github.andreypfau.curve25519.x25519.X25519

const val X25519_KEY_BYTES = 32

data class X25519KeyPair(val publicKey: ByteArray, val privateKey: ByteArray) {
    init {
        require(publicKey.size == X25519_KEY_BYTES) { "publicKey must be $X25519_KEY_BYTES bytes" }
        require(privateKey.size == X25519_KEY_BYTES) { "privateKey must be $X25519_KEY_BYTES bytes" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is X25519KeyPair) return false
        return publicKey.contentEquals(other.publicKey) && privateKey.contentEquals(other.privateKey)
    }

    override fun hashCode(): Int =
        31 * publicKey.contentHashCode() + privateKey.contentHashCode()
}

object X25519KeyExchange {

    fun generateKeyPair(): X25519KeyPair {
        val priv = SecureRandom.nextBytes(X25519_KEY_BYTES)
        val pub = X25519.x25519(scalar = priv)
        return X25519KeyPair(publicKey = pub, privateKey = priv)
    }

    fun deriveSharedSecret(privateKey: ByteArray, peerPublicKey: ByteArray): ByteArray {
        require(privateKey.size == X25519_KEY_BYTES) { "privateKey must be $X25519_KEY_BYTES bytes" }
        require(peerPublicKey.size == X25519_KEY_BYTES) { "peerPublicKey must be $X25519_KEY_BYTES bytes" }

        val privCopy = privateKey.copyOf()
        val pubCopy = peerPublicKey.copyOf()
        return X25519.x25519(scalar = privCopy, point = pubCopy)
    }

    fun publicFromPrivate(privateKey: ByteArray): ByteArray {
        require(privateKey.size == X25519_KEY_BYTES) { "privateKey must be $X25519_KEY_BYTES bytes" }
        return X25519.x25519(scalar = privateKey)
    }
}
