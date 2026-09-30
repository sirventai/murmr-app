package uoc.edu.psirvent.murmr.domain.model

import kotlin.time.Instant

enum class SessionStatus { ACTIVE, SUPERSEDED, EXPIRED, REVOKED, BROKEN }

data class Session(
    val sessionId: String,
    val contactId: String,
    val sessionKeyRef: String,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val sasCode: String,
    val fingerprint: String,
    val status: SessionStatus,
    val rotationIndex: Int = 0,
    val previousSessionId: String? = null,

    val renewedAt: Instant? = null,
)
