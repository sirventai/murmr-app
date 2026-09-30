package uoc.edu.psirvent.murmr.domain.model

import kotlin.time.Instant

data class User(
    val deviceId: String,
    val alias: String,
    val publicKey: ByteArray,
    val privateKey: ByteArray,
    val createdAt: Instant,
    val avatar: ByteArray? = null,
    val avatarBig: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return deviceId == other.deviceId &&
            alias == other.alias &&
            publicKey.contentEquals(other.publicKey) &&
            privateKey.contentEquals(other.privateKey) &&
            createdAt == other.createdAt &&
            (avatar?.contentEquals(other.avatar ?: ByteArray(0)) ?: (other.avatar == null)) &&
            (avatarBig?.contentEquals(other.avatarBig ?: ByteArray(0)) ?: (other.avatarBig == null))
    }

    override fun hashCode(): Int {
        var result = deviceId.hashCode()
        result = 31 * result + alias.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + privateKey.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (avatar?.contentHashCode() ?: 0)
        result = 31 * result + (avatarBig?.contentHashCode() ?: 0)
        return result
    }
}
