package uoc.edu.psirvent.murmr.domain.model

import kotlin.time.Instant

data class BlockedContact(
    val blockId: String,
    val ownerDeviceId: String,
    val blockedDeviceId: String,
    val blockedAt: Instant,
    val reason: String?,
)
