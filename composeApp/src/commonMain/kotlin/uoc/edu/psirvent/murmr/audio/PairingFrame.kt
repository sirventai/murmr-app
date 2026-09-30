package uoc.edu.psirvent.murmr.audio

data class PairingFrame(
    val type: PairingFrameType,
    val alias: String,
    val publicKey: ByteArray? = null,
    val nonce: ByteArray? = null,

    val negotiatedProfileId: Byte? = null,
) {
    init {
        require(alias.encodeToByteArray().size <= 255) { "alias too long" }
        if (type.hasKeyPayload()) {
            requireNotNull(publicKey) { "type=$type requires publicKey" }
            requireNotNull(nonce) { "type=$type requires nonce" }
            require(publicKey.size == 32) { "publicKey must be 32 bytes" }
            require(nonce.size == 16) { "nonce must be 16 bytes" }
        }
        if (type == PairingFrameType.HELLO_AUTO) {
            requireNotNull(nonce) { "HELLO_AUTO requires nonce" }
            require(nonce.size == 16) { "HELLO_AUTO nonce must be 16 bytes" }
        }
    }

    fun encode(): ByteArray {
        val aliasBytes = alias.encodeToByteArray()
        val keySize = if (type.hasKeyPayload()) 48 else 0
        val ackSize = if (type == PairingFrameType.HELLO_ACK) 1 else 0
        val autoNonceSize = if (type == PairingFrameType.HELLO_AUTO) 16 else 0
        val out = ByteArray(4 + 1 + 1 + aliasBytes.size + keySize + ackSize + autoNonceSize)
        var i = 0
        out[i++] = 'M'.code.toByte()
        out[i++] = 'M'.code.toByte()
        out[i++] = 'R'.code.toByte()
        out[i++] = '2'.code.toByte()
        out[i++] = type.value
        out[i++] = aliasBytes.size.toByte()
        aliasBytes.copyInto(out, i); i += aliasBytes.size
        if (type.hasKeyPayload()) {
            publicKey!!.copyInto(out, i); i += 32
            nonce!!.copyInto(out, i); i += 16
        }
        if (type == PairingFrameType.HELLO_ACK) {
            out[i++] = negotiatedProfileId ?: 0
        }
        if (type == PairingFrameType.HELLO_AUTO) {
            nonce!!.copyInto(out, i); i += 16
        }
        return out
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PairingFrame) return false
        return type == other.type &&
            alias == other.alias &&
            ((publicKey == null && other.publicKey == null) ||
                (publicKey != null && other.publicKey != null && publicKey.contentEquals(other.publicKey))) &&
            ((nonce == null && other.nonce == null) ||
                (nonce != null && other.nonce != null && nonce.contentEquals(other.nonce))) &&
            negotiatedProfileId == other.negotiatedProfileId
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + alias.hashCode()
        result = 31 * result + (publicKey?.contentHashCode() ?: 0)
        result = 31 * result + (nonce?.contentHashCode() ?: 0)
        result = 31 * result + (negotiatedProfileId?.toInt() ?: 0)
        return result
    }

    companion object {
        fun decode(bytes: ByteArray): PairingFrame? {
            if (bytes.size < 6) return null
            if (bytes[0] != 'M'.code.toByte() ||
                bytes[1] != 'M'.code.toByte() ||
                bytes[2] != 'R'.code.toByte() ||
                bytes[3] != '2'.code.toByte()
            ) return null
            val type = PairingFrameType.fromValue(bytes[4]) ?: return null
            val aliasLen = bytes[5].toInt() and 0xFF
            val keyBytes = if (type.hasKeyPayload()) 48 else 0
            val ackBytes = if (type == PairingFrameType.HELLO_ACK) 1 else 0
            val autoBytes = if (type == PairingFrameType.HELLO_AUTO) 16 else 0
            val needed = 6 + aliasLen + keyBytes + ackBytes + autoBytes
            if (bytes.size < needed) return null
            val alias = bytes.copyOfRange(6, 6 + aliasLen).decodeToString()
            var off = 6 + aliasLen
            val pub = if (type.hasKeyPayload()) bytes.copyOfRange(off, off + 32).also { off += 32 } else null
            val keyNonce = if (type.hasKeyPayload()) bytes.copyOfRange(off, off + 16).also { off += 16 } else null
            val profileId = if (type == PairingFrameType.HELLO_ACK) bytes[off].also { off += 1 } else null
            val autoNonce = if (type == PairingFrameType.HELLO_AUTO) bytes.copyOfRange(off, off + 16) else null
            return PairingFrame(type, alias, pub, keyNonce ?: autoNonce, profileId)
        }
    }
}

enum class PairingFrameType(val value: Byte) {
    HELLO(0x10),
    HELLO_ACK(0x11),
    CALIBRATE(0x12),

    HELLO_AUTO(0x13),
    KEY_A(0x20),
    KEY_B(0x21),

    KEY_DONE(0x22),

    SAS_OK(0x30),

    SAS_NO(0x31);

    fun hasKeyPayload(): Boolean = this == KEY_A || this == KEY_B

    companion object {
        fun fromValue(b: Byte): PairingFrameType? = entries.firstOrNull { it.value == b }
    }
}
