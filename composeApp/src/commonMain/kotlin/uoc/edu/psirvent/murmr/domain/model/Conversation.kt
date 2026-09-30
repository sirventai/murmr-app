package uoc.edu.psirvent.murmr.domain.model

import kotlin.time.Instant

data class Conversation(
    val contact: Contact,
    val lastMessagePreview: String?,
    val lastMessageAt: Instant?,
    val unreadCount: Int,
)
