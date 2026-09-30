package uoc.edu.psirvent.murmr.domain.usecase

import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import uoc.edu.psirvent.murmr.crypto.AeadDecryptionFailed
import uoc.edu.psirvent.murmr.crypto.AesGcm
import uoc.edu.psirvent.murmr.crypto.KeyStoreManager
import uoc.edu.psirvent.murmr.data.network.MessageEnvelope
import uoc.edu.psirvent.murmr.data.network.PresenceTracker
import uoc.edu.psirvent.murmr.data.network.TypingTracker
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.MessageRepository
import uoc.edu.psirvent.murmr.data.repository.SessionRepository
import uoc.edu.psirvent.murmr.data.repository.SettingsRepository
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.model.ContactStatus
import uoc.edu.psirvent.murmr.domain.model.EffectiveExpiry
import uoc.edu.psirvent.murmr.domain.model.Message
import uoc.edu.psirvent.murmr.domain.model.MessageStatus
import uoc.edu.psirvent.murmr.domain.model.MessageType
import uoc.edu.psirvent.murmr.domain.model.SessionStatus
import uoc.edu.psirvent.murmr.domain.protocol.ControlMessage
import uoc.edu.psirvent.murmr.util.Logger
import uoc.edu.psirvent.murmr.util.TextSanitizer

class ReceiveDecryptMessageUseCase(
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository,
    private val keyStore: KeyStoreManager,
    private val clock: Clock = Clock.System,
    private val sendEncrypted: SendEncryptedMessageUseCase? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val typingTracker: TypingTracker? = null,
    private val presenceTracker: PresenceTracker? = null,
    private val rotateSessionKey: RotateSessionKeyUseCase? = null,
) {
    private val tag = "ReceiveDecrypt"

    suspend operator fun invoke(env: MessageEnvelope): Message? {
        Logger.i(tag, "rx envelope msg=${env.messageId} sender=${env.senderDeviceId} -> recipient=${env.recipientDeviceId}")
        val me = userRepository.getLocalUser()
        if (me == null) {
            Logger.w(tag, "rx descartado: no hay usuario local")
            return null
        }
        if (env.recipientDeviceId != me.deviceId) {
            Logger.w(tag, "rx descartado: recipient=${env.recipientDeviceId} != yo=${me.deviceId}")
            return null
        }

        val contact = run {
            var found = contactRepository.getByRemote(owner = me.deviceId, remote = env.senderDeviceId)
            var waitedMs = 0L
            while (found == null && waitedMs < 5_000L) {
                delay(300L)
                waitedMs += 300L
                found = contactRepository.getByRemote(owner = me.deviceId, remote = env.senderDeviceId)
            }
            found
        } ?: run {
            Logger.w(tag, "rx descartado: sender=${env.senderDeviceId} no esta en mis contactos (yo=${me.deviceId})")
            return null
        }

        if (contact.status == ContactStatus.BLOCKED) {
            Logger.w(tag, "rx descartado: contacto ${contact.contactId} BLOCKED")
            return null
        }
        Logger.i(tag, "contacto encontrado: ${contact.displayAlias} (${contact.contactId})")
        presenceTracker?.markRecentActivity(env.senderDeviceId)

        var candidateSessions = sessionRepository.getNonRevokedForContact(contact.contactId)
            .filter { it.status == SessionStatus.ACTIVE || it.status == SessionStatus.SUPERSEDED }
        var waitedSesMs = 0L
        while (candidateSessions.isEmpty() && waitedSesMs < 5_000L) {
            delay(300L)
            waitedSesMs += 300L
            candidateSessions = sessionRepository.getNonRevokedForContact(contact.contactId)
                .filter { it.status == SessionStatus.ACTIVE || it.status == SessionStatus.SUPERSEDED }
        }
        if (candidateSessions.isEmpty()) {
            Logger.w(tag, "rx descartado: contacto sin sesiones usables tras esperar")
            return null
        }
        Logger.i(tag, "${candidateSessions.size} sesion(es) candidata(s), probando descifrado")

        val ciphertext = env.ciphertextB64.decodeBase64Bytes()
        val iv = env.ivB64.decodeBase64Bytes()
        val authTag = env.authTagB64.decodeBase64Bytes()

        val settings = settingsRepository?.getFirst()
        for (session in candidateSessions) {
            val baseInstant = session.renewedAt ?: session.createdAt
            val effCutoff = if (settings != null) {
                val effective = EffectiveExpiry.computeForContact(contact, settings)
                val effDuration = effective.duration
                val computed = if (effDuration != null) baseInstant + effDuration else null
                listOfNotNull(session.expiresAt, computed).minOrNull()
            } else {
                session.expiresAt
            }
            if (effCutoff != null && effCutoff < clock.now()) {
                Logger.w(tag, "sesion ${session.sessionId} caducada (effective), marcando EXPIRED y borrando key")
                sessionRepository.updateStatus(session.sessionId, SessionStatus.EXPIRED)
                runCatching { keyStore.deleteKey(session.sessionKeyRef) }
                continue
            }
            val key = keyStore.loadKey(session.sessionKeyRef)
            if (key == null) {
                Logger.w(tag, "sesion ${session.sessionId} sin key en KeyStore, saltando")
                continue
            }
            val aad = (session.sessionId + "|" + env.senderDeviceId + "→" + me.deviceId).encodeToByteArray()
            val plaintextBytes = try {
                AesGcm.decrypt(key, iv, ciphertext, authTag, aad)
            } catch (e: AeadDecryptionFailed) {
                Logger.w(tag, "auth tag mismatch con sesion ${session.sessionId}: ${e.message}")
                continue
            } catch (t: Throwable) {
                Logger.w(tag, "error inesperado descifrando con ${session.sessionId}: ${t.message}")
                continue
            }
            Logger.i(tag, "descifrado OK con sesion ${session.sessionId}")

            val plaintext = plaintextBytes.decodeToString()
            if (plaintext.startsWith("DELIVERY_RECEIPT|")) {
                val id = plaintext.removePrefix("DELIVERY_RECEIPT|").trim()
                if (id.isNotEmpty()) {
                    runCatching { messageRepository.updateStatus(id, MessageStatus.DELIVERED) }
                    Logger.i(tag, "DELIVERY_RECEIPT aplicado a msg=$id")
                }
                return null
            }
            (ControlMessage.parse(plaintext) as? ControlMessage.ExpiryUpdate)?.let { msg ->
                contactRepository.updatePeerExpiry(
                    contactId = contact.contactId,
                    peerSessionExpiry = msg.expiry,
                    peerCustomExpirySeconds = msg.customSeconds,
                )
                Logger.i(tag, "EXPIRY_UPDATE de ${contact.contactId}: ${msg.expiry} (${msg.customSeconds}s)")
                return null
            }
            if (plaintext == "SESSION_EXPIRED") {
                runCatching {
                    sessionRepository.getNonRevokedForContact(contact.contactId).forEach { s ->
                        sessionRepository.updateStatus(s.sessionId, SessionStatus.EXPIRED)
                        runCatching { keyStore.deleteKey(s.sessionKeyRef) }
                    }
                    Logger.i(tag, "SESSION_EXPIRED de ${contact.contactId}, sesiones marcadas EXPIRED")
                }
                return null
            }

            if (plaintext.startsWith("SESSION_ROTATE|")) {
                val expectedIdx = plaintext.removePrefix("SESSION_ROTATE|").trim().toIntOrNull()
                val rotator = rotateSessionKey
                if (expectedIdx == null || rotator == null) {
                    Logger.w(tag, "SESSION_ROTATE invalido o rotator no inyectado: '$plaintext'")
                } else {
                    rotator.applyFromPeer(session, expectedIdx)
                    Logger.i(tag, "SESSION_ROTATE aplicado: rotIdx=$expectedIdx para ${contact.contactId}")
                }
                return null
            }
            if (plaintext == "TYPING_START") {
                typingTracker?.onTypingStart(contact.contactId)
                return null
            }
            if (plaintext == "TYPING_STOP") {
                typingTracker?.onTypingStop(contact.contactId)
                return null
            }
            if (plaintext == "SESSION_REVOKED") {
                runCatching {
                    sessionRepository.getNonRevokedForContact(contact.contactId).forEach {
                        sessionRepository.updateStatus(it.sessionId, SessionStatus.REVOKED)
                    }
                    Logger.i(tag, "SESSION_REVOKED de ${contact.contactId}, sesiones marcadas REVOKED")
                }
                return null
            }
            (ControlMessage.parse(plaintext) as? ControlMessage.ReadReceipt)?.let { msg ->
                if (msg.messageIds.isNotEmpty()) {
                    messageRepository.updateStatusForIds(msg.messageIds, MessageStatus.READ)
                    Logger.i(tag, "READ_RECEIPT aplicado a ${msg.messageIds.size} mensajes")
                }
                return null
            }
            (ControlMessage.parse(plaintext) as? ControlMessage.SessionRenewed)?.let { msg ->
                val ts = Instant.fromEpochMilliseconds(msg.renewedAtMs)
                sessionRepository.markRenewed(session.sessionId, ts)
                Logger.i(tag, "SESSION_RENEWED aplicado a sesion=${session.sessionId} renewedAt=$ts")
                return null
            }
            if (plaintext == "AVATAR_REQUEST") {
                val mySelf = userRepository.getLocalUser()
                val payloadBytes = mySelf?.avatarBig ?: mySelf?.avatar
                if (sendEncrypted != null) {

                    val payload = if (payloadBytes != null) {
                        "AVATAR_BIG|" + payloadBytes.encodeBase64()
                    } else {
                        "AVATAR_BIG|"
                    }
                    runCatching {
                        sendEncrypted(
                            contactId = contact.contactId,
                            plaintext = payload,
                            type = MessageType.PROFILE_UPDATE,
                        )
                        Logger.i(tag, "AVATAR_BIG (${payloadBytes?.size ?: 0}B) enviado a ${contact.contactId}")
                    }.onFailure { Logger.w(tag, "fallo enviando AVATAR_BIG: ${it.message}") }
                } else {
                    Logger.w(tag, "AVATAR_REQUEST recibido pero sendEncrypted no inyectado")
                }
                return null
            }
            if (plaintext.startsWith("AVATAR_BIG|")) {
                val b64 = plaintext.removePrefix("AVATAR_BIG|")

                val estimatedBytes = (b64.length * 3) / 4
                if (estimatedBytes > 2 * 1024 * 1024) {
                    Logger.w(tag, "AVATAR_BIG estimado=${estimatedBytes}B > 2MB cap, rechazado de ${contact.contactId}")
                    return null
                }
                val bytes = runCatching {
                    b64.decodeBase64Bytes().takeIf { it.size in 1..(2 * 1024 * 1024) }
                }.getOrNull()
                if (bytes != null) {
                    contactRepository.updateAvatarBigFromPeer(contact.contactId, bytes)
                    Logger.i(tag, "AVATAR_BIG (${bytes.size}B) cacheado para ${contact.contactId}")
                } else {
                    Logger.w(tag, "AVATAR_BIG invalido (size fuera de rango o decode fallido) de ${contact.contactId}")
                }
                return null
            }
            (ControlMessage.parse(plaintext) as? ControlMessage.ProfileUpdate)?.let { msg ->
                when (msg.kind) {
                    ControlMessage.ProfileUpdate.Kind.ALIAS -> {

                        val sanitized = TextSanitizer.sanitize(msg.value, allowNewlines = false).trim()
                        val newAlias = sanitized.takeIf { it.isNotBlank() && it.length <= 64 }
                        if (newAlias != null) {
                            contactRepository.updateAliasFromPeer(contact.contactId, newAlias)
                            Logger.i(tag, "PROFILE_UPDATE alias=$newAlias aplicado a ${contact.contactId}")
                        } else {
                            Logger.w(tag, "PROFILE_UPDATE|ALIAS rechazado tras sanitize: '${msg.value}'")
                        }
                    }
                    ControlMessage.ProfileUpdate.Kind.AVATAR -> {

                        val avatarBytes = if (msg.value == "NONE") {
                            null
                        } else {
                            val estimatedBytes = (msg.value.length * 3) / 4
                            if (estimatedBytes > 256 * 1024) {
                                Logger.w(tag, "PROFILE_UPDATE|AVATAR estimado=${estimatedBytes}B > 256KB, rechazado")
                                null
                            } else {
                                runCatching {
                                    msg.value.decodeBase64Bytes().takeIf { it.size <= 256 * 1024 }
                                }.getOrNull()
                            }
                        }
                        contactRepository.updateAvatarFromPeer(contact.contactId, avatarBytes)
                        Logger.i(tag, "PROFILE_UPDATE avatar=${avatarBytes?.size ?: 0}B aplicado a ${contact.contactId}")
                    }
                }
                return null
            }

            val reply = ControlMessage.parse(plaintext) as? ControlMessage.MurmrReply
            val (replyTo, displayText) = if (reply != null) {
                val refMsg = messageRepository.getById(reply.refId)
                val refContactId = refMsg?.let { sessionRepository.getById(it.sessionId)?.contactId }
                val validId = when {
                    refMsg == null -> {
                        Logger.w(tag, "MURMR_REPLY ref a ID inexistente '${reply.refId}', ignorando cita")
                        null
                    }
                    refContactId != contact.contactId -> {
                        Logger.w(tag, "MURMR_REPLY ref cross-contact '${reply.refId}' (refContact=$refContactId), ignorando cita")
                        null
                    }
                    else -> reply.refId
                }
                validId to reply.text
            } else {
                null to plaintext
            }
            val message = Message(
                messageId = env.messageId,
                sessionId = session.sessionId,
                senderDeviceId = env.senderDeviceId,
                ciphertext = ciphertext,
                iv = iv,
                authTag = authTag,
                plaintextLocal = displayText,
                timestamp = Instant.fromEpochMilliseconds(env.sentAtEpochMs),
                status = MessageStatus.DELIVERED,
                isOutgoing = false,
                replyToMessageId = replyTo,
            )
            messageRepository.insert(message)
            contactRepository.incrementUnread(contact.contactId, ts = message.timestamp)
            typingTracker?.onTypingStop(contact.contactId)
            Logger.i(tag, "msg descifrado con session=${session.sessionId} status=${session.status}")

            sendEncrypted?.let { send ->
                runCatching {
                    send(
                        contactId = contact.contactId,
                        plaintext = "DELIVERY_RECEIPT|${env.messageId}",
                        type = MessageType.READ_RECEIPT,
                        replyToMessageId = null,
                    )
                }.onFailure { Logger.w(tag, "DELIVERY_RECEIPT fallo: ${it.message}") }
            }
            return message
        }

        val rotator = rotateSessionKey
        val active = candidateSessions.firstOrNull { it.status == SessionStatus.ACTIVE }
        if (rotator != null && active != null) {
            Logger.i(tag, "auto-recovery: intentando rotar localmente y reintentar descifrado")
            val rotated = runCatching {
                rotator.applyFromPeer(active, active.rotationIndex + 1)
            }.getOrNull()
            if (rotated != null) {
                val key = keyStore.loadKey(rotated.sessionKeyRef)
                if (key != null) {
                    val aad = (rotated.sessionId + "|" + env.senderDeviceId + "→" + me.deviceId).encodeToByteArray()
                    val plaintextBytes = runCatching {
                        AesGcm.decrypt(key, iv, ciphertext, authTag, aad)
                    }.getOrNull()
                    if (plaintextBytes != null) {
                        Logger.i(tag, "auto-recovery: descifrado OK tras rotacion local")

                        return invoke(env)
                    }
                }
            }
        }
        Logger.w(tag, "Auth tag invalido en todas las sesiones del contacto")
        return null
    }
}
