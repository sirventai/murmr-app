package uoc.edu.psirvent.murmr.domain.model

import kotlin.time.Instant

enum class MessageStatus { PENDING, SENDING, SENT, DELIVERED, READ, FAILED }

enum class MessageType { TEXT, PROFILE_UPDATE, MESSAGE_EDIT, MESSAGE_DELETE, READ_RECEIPT, ATTACHMENT_REF }

data class Message(
    val messageId: String,
    val sessionId: String,
    val senderDeviceId: String,
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val authTag: ByteArray,
    val plaintextLocal: String,
    val timestamp: Instant,
    val status: MessageStatus,
    val isOutgoing: Boolean,
    val type: MessageType = MessageType.TEXT,
    val replyToMessageId: String? = null,
    val edited: Boolean = false,
    val readReceiptSentAt: Instant? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Message) return false
        return messageId == other.messageId &&
            sessionId == other.sessionId &&
            senderDeviceId == other.senderDeviceId &&
            ciphertext.contentEquals(other.ciphertext) &&
            iv.contentEquals(other.iv) &&
            authTag.contentEquals(other.authTag) &&
            plaintextLocal == other.plaintextLocal &&
            timestamp == other.timestamp &&
            status == other.status &&
            isOutgoing == other.isOutgoing &&
            type == other.type &&
            replyToMessageId == other.replyToMessageId &&
            edited == other.edited &&
            readReceiptSentAt == other.readReceiptSentAt
    }

    override fun hashCode(): Int {
        var r = messageId.hashCode()
        r = 31 * r + sessionId.hashCode()
        r = 31 * r + senderDeviceId.hashCode()
        r = 31 * r + ciphertext.contentHashCode()
        r = 31 * r + iv.contentHashCode()
        r = 31 * r + authTag.contentHashCode()
        r = 31 * r + plaintextLocal.hashCode()
        r = 31 * r + timestamp.hashCode()
        r = 31 * r + status.hashCode()
        r = 31 * r + isOutgoing.hashCode()
        r = 31 * r + type.hashCode()
        r = 31 * r + (replyToMessageId?.hashCode() ?: 0)
        r = 31 * r + edited.hashCode()
        r = 31 * r + (readReceiptSentAt?.hashCode() ?: 0)
        return r
    }
}
