package uoc.edu.psirvent.murmr.domain.model

import kotlin.time.Instant

enum class PairingEvent { PAIRED, REKEYED, EXPIRED, FAILED, REVOKED }

data class PairingLogEntry(
    val logId: String,
    val contactId: String,
    val eventType: PairingEvent,
    val timestamp: Instant,
    val details: String,
)
