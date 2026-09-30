package uoc.edu.psirvent.murmr.domain.usecase

import uoc.edu.psirvent.murmr.crypto.KeyStoreManager
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.MessageRepository
import uoc.edu.psirvent.murmr.data.repository.PairingLogRepository
import uoc.edu.psirvent.murmr.data.repository.SessionRepository
import uoc.edu.psirvent.murmr.domain.model.MessageType
import uoc.edu.psirvent.murmr.util.Logger

class DeleteContactSecureWipeUseCase(
    private val contactRepository: ContactRepository,
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository,
    private val pairingLogRepository: PairingLogRepository,
    private val keyStore: KeyStoreManager,
    private val sendEncrypted: SendEncryptedMessageUseCase? = null,
) {
    suspend operator fun invoke(contactId: String) {
        val tag = "DeleteWipe"

        sendEncrypted?.let { send ->
            runCatching {
                val result = send(
                    contactId = contactId,
                    plaintext = "SESSION_REVOKED",
                    type = MessageType.PROFILE_UPDATE,
                    bypassExpiryCheck = true,
                )
                Logger.i(tag, "SESSION_REVOKED enviado a $contactId, status=${result.status}")
            }.onFailure {
                Logger.w(tag, "SESSION_REVOKED a $contactId NO se pudo enviar: ${it.message}")
            }
        }

        val activeOrSuperseded = runCatching { sessionRepository.getNonRevokedForContact(contactId) }
            .getOrDefault(emptyList())
        val active = runCatching { sessionRepository.getActive(contactId) }.getOrNull()
        (activeOrSuperseded + listOfNotNull(active))
            .distinctBy { it.sessionId }
            .forEach { keyStore.deleteKey(it.sessionKeyRef) }

        messageRepository.deleteByContact(contactId)
        sessionRepository.deleteAllForContact(contactId)
        pairingLogRepository.deleteForContact(contactId)
        contactRepository.delete(contactId)
    }
}
