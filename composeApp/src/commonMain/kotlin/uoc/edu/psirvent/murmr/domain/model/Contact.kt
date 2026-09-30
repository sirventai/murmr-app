package uoc.edu.psirvent.murmr.domain.model

import kotlin.time.Instant

enum class ContactStatus { PENDING, UNVERIFIED, VERIFIED, EXPIRED, BLOCKED }

data class Contact(
    val contactId: String,
    val ownerDeviceId: String,
    val remoteDeviceId: String,
    val remotePublicKey: ByteArray,
    val displayAlias: String,
    val status: ContactStatus,
    val pairedAt: Instant,
    val lastMessageAt: Instant?,
    val unreadCount: Int,
    val avatar: ByteArray? = null,
    val avatarBig: ByteArray? = null,
    val relayUrl: String = "",
    val peerSessionExpiry: SessionExpiry = SessionExpiry.NEVER,
    val peerCustomExpirySeconds: Long = 0L,

    val localSessionExpiry: SessionExpiry? = null,
    val localCustomExpirySeconds: Long = 0L,
) {
    val fingerprint: String
        get() = remotePublicKey.toFingerprint()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Contact) return false
        return contactId == other.contactId &&
            ownerDeviceId == other.ownerDeviceId &&
            remoteDeviceId == other.remoteDeviceId &&
            remotePublicKey.contentEquals(other.remotePublicKey) &&
            displayAlias == other.displayAlias &&
            status == other.status &&
            pairedAt == other.pairedAt &&
            lastMessageAt == other.lastMessageAt &&
            unreadCount == other.unreadCount &&
            relayUrl == other.relayUrl &&
            peerSessionExpiry == other.peerSessionExpiry &&
            peerCustomExpirySeconds == other.peerCustomExpirySeconds &&
            localSessionExpiry == other.localSessionExpiry &&
            localCustomExpirySeconds == other.localCustomExpirySeconds &&
            (avatar?.contentEquals(other.avatar) ?: (other.avatar == null)) &&
            (avatarBig?.contentEquals(other.avatarBig) ?: (other.avatarBig == null))
    }

    override fun hashCode(): Int {
        var r = contactId.hashCode()
        r = 31 * r + ownerDeviceId.hashCode()
        r = 31 * r + remoteDeviceId.hashCode()
        r = 31 * r + remotePublicKey.contentHashCode()
        r = 31 * r + displayAlias.hashCode()
        r = 31 * r + status.hashCode()
        r = 31 * r + pairedAt.hashCode()
        r = 31 * r + (lastMessageAt?.hashCode() ?: 0)
        r = 31 * r + unreadCount
        r = 31 * r + relayUrl.hashCode()
        r = 31 * r + peerSessionExpiry.hashCode()
        r = 31 * r + peerCustomExpirySeconds.hashCode()
        r = 31 * r + (localSessionExpiry?.hashCode() ?: 0)
        r = 31 * r + localCustomExpirySeconds.hashCode()
        r = 31 * r + (avatar?.contentHashCode() ?: 0)
        r = 31 * r + (avatarBig?.contentHashCode() ?: 0)
        return r
    }
}

private fun ByteArray.toFingerprint(): String {
    val hex = "0123456789ABCDEF"
    val sb = StringBuilder(this.size * 2)
    for (b in this) {
        val v = b.toInt() and 0xFF
        sb.append(hex[v ushr 4])
        sb.append(hex[v and 0x0F])
    }
    val full = sb.toString()
    return full.take(32).chunked(4).joinToString(" ")
}
