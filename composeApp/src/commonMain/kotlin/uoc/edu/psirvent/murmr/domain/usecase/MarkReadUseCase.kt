package uoc.edu.psirvent.murmr.domain.usecase

import kotlin.time.Clock
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.MessageRepository
import uoc.edu.psirvent.murmr.data.repository.SessionRepository
import uoc.edu.psirvent.murmr.domain.model.MessageType
import uoc.edu.psirvent.murmr.domain.protocol.ControlMessage
import uoc.edu.psirvent.murmr.util.Logger

class MarkReadUseCase(
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val sendEncrypted: SendEncryptedMessageUseCase,
    private val clock: Clock = Clock.System,
) {
    private val tag = "MarkRead"

    suspend operator fun invoke(contactId: String) {
        val session = sessionRepository.getActive(contactId) ?: return
        val unread = messageRepository.selectIncomingUnreadBySession(session.sessionId)
        if (unread.isEmpty()) {
            contactRepository.resetUnread(contactId)
            return
        }

        val now = clock.now()
        val ids = unread.map { it.messageId }
        ids.forEach { messageRepository.markReadReceiptSent(it, now) }
        contactRepository.resetUnread(contactId)
        Logger.i(tag, "marcados ${ids.size} msg como leídos en contacto $contactId")

        runCatching {
            sendEncrypted(
                contactId = contactId,
                plaintext = ControlMessage.ReadReceipt(ids).serialize(),
                type = MessageType.READ_RECEIPT,
            )
        }.onFailure {
            Logger.w(tag, "READ_RECEIPT no enviado: ${it.message}")
        }
    }
}
