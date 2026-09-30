package uoc.edu.psirvent.murmr.domain.usecase

import com.benasher44.uuid.uuid4
import io.ktor.util.encodeBase64
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import uoc.edu.psirvent.murmr.crypto.AesGcm
import uoc.edu.psirvent.murmr.crypto.KeyStoreManager
import uoc.edu.psirvent.murmr.crypto.newAesGcmIv
import uoc.edu.psirvent.murmr.data.network.MessageEnvelope
import uoc.edu.psirvent.murmr.data.network.RelayApi
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.MessageRepository
import uoc.edu.psirvent.murmr.data.repository.OutboxRepository
import uoc.edu.psirvent.murmr.data.repository.SessionRepository
import uoc.edu.psirvent.murmr.data.repository.SettingsRepository
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.model.ContactStatus
import uoc.edu.psirvent.murmr.domain.model.EffectiveExpiry
import uoc.edu.psirvent.murmr.domain.model.Message
import uoc.edu.psirvent.murmr.domain.model.MessageStatus
import uoc.edu.psirvent.murmr.domain.model.MessageType
import uoc.edu.psirvent.murmr.domain.model.Session
import uoc.edu.psirvent.murmr.domain.model.SessionStatus
import uoc.edu.psirvent.murmr.domain.protocol.ControlMessage
import uoc.edu.psirvent.murmr.util.Logger

class SendEncryptedMessageUseCase(
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository,
    private val keyStore: KeyStoreManager,
    private val rotateSessionKey: RotateSessionKeyUseCase,
    private val relayApi: RelayApi? = null,
    private val outbox: OutboxRepository? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val clock: Clock = Clock.System,
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend operator fun invoke(
        contactId: String,
        plaintext: String,
        type: MessageType = MessageType.TEXT,
        replyToMessageId: String? = null,
        bypassExpiryCheck: Boolean = false,
    ): Message {
        require(plaintext.isNotBlank() || type != MessageType.TEXT) { "mensaje vacío" }
        val me = userRepository.getLocalUser() ?: error("Usuario local no inicializado")
        val contact = contactRepository.getById(contactId) ?: error("Contacto inexistente")
        if (contact.status == ContactStatus.BLOCKED) {
            error("Contacto bloqueado - envío rechazado")
        }
        var session = sessionRepository.getActive(contactId) ?: error("No hay sesión activa con $contactId")

        if (replyToMessageId != null) {
            val refMsg = messageRepository.getById(replyToMessageId)
            require(refMsg != null) { "replyToMessageId '$replyToMessageId' no existe" }
            val refSession = sessionRepository.getById(refMsg.sessionId)
            require(refSession != null && refSession.contactId == contactId) {
                "replyToMessageId pertenece a otro contacto (sessionId=${refMsg.sessionId})"
            }
        }

        val now = clock.now()
        val baseInstant = session.renewedAt ?: session.createdAt
        val effectiveCutoff = run {
            val settings = settingsRepository?.getFirst()
            if (settings == null) {
                session.expiresAt
            } else {
                val effective = EffectiveExpiry.computeForContact(contact, settings)
                val effDuration = effective.duration
                val effCutoff = if (effDuration != null) baseInstant + effDuration else null
                listOfNotNull(session.expiresAt, effCutoff).minOrNull()
            }
        }
        if (effectiveCutoff != null && effectiveCutoff < now && !bypassExpiryCheck) {

            runCatching {
                invoke(
                    contactId = contactId,
                    plaintext = "SESSION_EXPIRED",
                    type = MessageType.PROFILE_UPDATE,
                    bypassExpiryCheck = true,
                )
            }
            sessionRepository.updateStatus(session.sessionId, SessionStatus.EXPIRED)
            error("Sesión caducada con $contactId - re-empareja para enviar")
        }

        if (!bypassExpiryCheck && type != MessageType.PROFILE_UPDATE) {
            val initiatesRotation = me.deviceId < contact.remoteDeviceId
            if (initiatesRotation) {
                val outgoingCount = countOutgoing(session)
                if (outgoingCount > 0 && outgoingCount % ROTATE_EVERY == 0) {
                    val newRotIdx = session.rotationIndex + 1
                    runCatching {
                        invoke(
                            contactId = contactId,
                            plaintext = "SESSION_ROTATE|$newRotIdx",
                            type = MessageType.PROFILE_UPDATE,
                            bypassExpiryCheck = true,
                        )
                    }
                    session = rotateSessionKey(session)
                }
            }
        }

        val key = keyStore.loadKey(session.sessionKeyRef)
            ?: run {
                sessionRepository.updateStatus(session.sessionId, SessionStatus.BROKEN)
                error("Clave de sesión no encontrada - KeyStore corrupto")
            }

        val iv = newAesGcmIv()
        val aad = (session.sessionId + "|" + me.deviceId + "→" + contact.remoteDeviceId).encodeToByteArray()

        val wirePlaintext = if (replyToMessageId != null && type == MessageType.TEXT) {
            ControlMessage.MurmrReply(replyToMessageId, plaintext).serialize()
        } else {
            plaintext
        }
        val ct = AesGcm.encrypt(key, iv, wirePlaintext.encodeToByteArray(), aad)
        val messageId = uuid4().toString()

        val record = Message(
            messageId = messageId,
            sessionId = session.sessionId,
            senderDeviceId = me.deviceId,
            ciphertext = ct.ciphertext,
            iv = iv,
            authTag = ct.authTag,
            plaintextLocal = plaintext,
            timestamp = now,
            status = MessageStatus.SENDING,
            isOutgoing = true,
            type = type,
            replyToMessageId = replyToMessageId,
        )
        messageRepository.insert(record)
        contactRepository.touchLastMessage(contactId, now)

        val envelope = MessageEnvelope(
            messageId = messageId,
            senderDeviceId = me.deviceId,
            recipientDeviceId = contact.remoteDeviceId,
            ciphertextB64 = ct.ciphertext.encodeBase64(),
            ivB64 = iv.encodeBase64(),
            authTagB64 = ct.authTag.encodeBase64(),
            sentAtEpochMs = now.toEpochMilliseconds(),
        )

        Logger.i(
            "SendMsg",
            "tx msg=$messageId | contactId=$contactId alias='${contact.displayAlias}' status=${contact.status} | " +
                "sender=${me.deviceId} -> recipient=${contact.remoteDeviceId} (ciphertext=${ct.ciphertext.size}B)",
        )

        val newStatus = if (relayApi != null) {
            try {
                relayApi.postMessage(token = me.deviceId, env = envelope)
                Logger.i("SendMsg", "POST /messages OK msg=$messageId")
                MessageStatus.SENT
            } catch (t: Throwable) {
                Logger.w(
                    "SendMsg",
                    "POST /messages FALLO msg=$messageId, encolando en outbox: ${t.message}",
                )
                outbox?.let {
                    runCatching {
                        it.enqueue(messageId, json.encodeToString(MessageEnvelope.serializer(), envelope))
                    }
                }
                MessageStatus.SENDING
            }
        } else {
            Logger.w("SendMsg", "RelayApi no inyectado, marcando SENT local")
            MessageStatus.SENT
        }
        messageRepository.updateStatus(messageId, newStatus)
        return record.copy(status = newStatus)
    }

    private suspend fun countOutgoing(session: Session): Int =
        messageRepository.countOutgoingForSession(session.sessionId)

    companion object {
        const val ROTATE_EVERY = 20
    }
}
