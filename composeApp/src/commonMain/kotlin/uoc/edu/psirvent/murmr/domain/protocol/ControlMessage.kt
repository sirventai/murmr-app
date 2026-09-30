package uoc.edu.psirvent.murmr.domain.protocol

import uoc.edu.psirvent.murmr.domain.model.SessionExpiry

sealed interface ControlMessage {

    fun serialize(): String

    data class ExpiryUpdate(val expiry: SessionExpiry, val customSeconds: Long) : ControlMessage {
        override fun serialize(): String = "$PREFIX${expiry.name}|$customSeconds"
        companion object { const val PREFIX = "EXPIRY_UPDATE|" }
    }

    data class ReadReceipt(val messageIds: List<String>) : ControlMessage {
        override fun serialize(): String = PREFIX + messageIds.joinToString(",")
        companion object { const val PREFIX = "READ_RECEIPT|" }
    }

    data class ProfileUpdate(val kind: Kind, val value: String) : ControlMessage {
        enum class Kind { ALIAS, AVATAR }
        override fun serialize(): String = "$PREFIX${kind.name}|$value"
        companion object { const val PREFIX = "PROFILE_UPDATE|" }
    }

    data class MurmrReply(val refId: String, val text: String) : ControlMessage {
        override fun serialize(): String = "$PREFIX$refId|$text"
        companion object { const val PREFIX = "MURMR_REPLY|" }
    }

    data class SessionRenewed(val renewedAtMs: Long) : ControlMessage {
        override fun serialize(): String = "$PREFIX$renewedAtMs"
        companion object { const val PREFIX = "SESSION_RENEWED|" }
    }

    companion object {
        fun parse(text: String): ControlMessage? = when {
            text.startsWith(ExpiryUpdate.PREFIX) -> {
                val parts = text.removePrefix(ExpiryUpdate.PREFIX).split("|", limit = 2)
                val enumName = parts.getOrNull(0)?.trim().orEmpty()
                val customSeconds = parts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
                val expiry = runCatching { SessionExpiry.valueOf(enumName) }
                    .getOrDefault(SessionExpiry.NEVER)
                ExpiryUpdate(expiry, customSeconds)
            }
            text.startsWith(ReadReceipt.PREFIX) -> {
                val ids = text.removePrefix(ReadReceipt.PREFIX)
                    .split(",")
                    .filter { it.isNotBlank() }
                ReadReceipt(ids)
            }
            text.startsWith(ProfileUpdate.PREFIX) -> {
                val parts = text.removePrefix(ProfileUpdate.PREFIX).split("|", limit = 2)
                val kindName = parts.getOrNull(0)
                val value = parts.getOrNull(1).orEmpty()
                val kind = runCatching { ProfileUpdate.Kind.valueOf(kindName.orEmpty()) }.getOrNull()
                kind?.let { ProfileUpdate(it, value) }
            }
            text.startsWith(MurmrReply.PREFIX) -> {
                val parts = text.removePrefix(MurmrReply.PREFIX).split("|", limit = 2)
                val refId = parts.getOrNull(0)?.takeIf { it.isNotBlank() }
                val msg = parts.getOrNull(1).orEmpty()
                refId?.let { MurmrReply(it, msg) }
            }
            text.startsWith(SessionRenewed.PREFIX) -> {
                val ms = text.removePrefix(SessionRenewed.PREFIX).trim().toLongOrNull()
                ms?.let { SessionRenewed(it) }
            }
            else -> null
        }
    }
}
