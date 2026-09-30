package uoc.edu.psirvent.murmr.domain.usecase

import com.benasher44.uuid.uuid4
import kotlin.time.Clock
import uoc.edu.psirvent.murmr.data.repository.BlockedContactRepository
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.model.BlockedContact
import uoc.edu.psirvent.murmr.domain.model.ContactStatus
import uoc.edu.psirvent.murmr.domain.model.MessageType

class BlockContactUseCase(
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val blockedContactRepository: BlockedContactRepository,
    private val clock: Clock = Clock.System,
    private val sendEncrypted: SendEncryptedMessageUseCase? = null,
) {
    suspend operator fun invoke(contactId: String, reason: String? = null) {
        val me = userRepository.getLocalUser() ?: error("Usuario local no inicializado")
        val contact = contactRepository.getById(contactId) ?: return

        sendEncrypted?.let { send ->
            runCatching {
                send(
                    contactId = contactId,
                    plaintext = "SESSION_REVOKED",
                    type = MessageType.PROFILE_UPDATE,
                )
            }
        }
        blockedContactRepository.block(
            BlockedContact(
                blockId = uuid4().toString(),
                ownerDeviceId = me.deviceId,
                blockedDeviceId = contact.remoteDeviceId,
                blockedAt = clock.now(),
                reason = reason,
            ),
        )
        contactRepository.updateStatus(contactId, ContactStatus.BLOCKED)
    }
}
