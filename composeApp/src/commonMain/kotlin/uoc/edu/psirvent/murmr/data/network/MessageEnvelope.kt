package uoc.edu.psirvent.murmr.data.network

import kotlinx.serialization.Serializable

@Serializable
data class MessageEnvelope(
    val messageId: String,
    val senderDeviceId: String,
    val recipientDeviceId: String,
    val ciphertextB64: String,
    val ivB64: String,
    val authTagB64: String,
    val sentAtEpochMs: Long,
)

@Serializable
data class RegisterDeviceRequest(
    val deviceId: String,
    val publicKeyB64: String,
    val alias: String?,
)

@Serializable
data class RegisterDeviceResponse(
    val deviceId: String,
    val token: String,
)

@Serializable
data class InboxResponse(val messages: List<MessageEnvelope>)

@Serializable
data class PresenceResponse(
    val deviceId: String,
    val online: Boolean,
)
