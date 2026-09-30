package uoc.edu.psirvent.murmr.domain.usecase

import uoc.edu.psirvent.murmr.data.repository.BlockedContactRepository
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.model.ContactStatus

class UnblockContactUseCase(
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val blockedContactRepository: BlockedContactRepository,
) {
    suspend operator fun invoke(contactId: String) {
        val me = userRepository.getLocalUser() ?: error("Usuario local no inicializado")
        val contact = contactRepository.getById(contactId) ?: return
        blockedContactRepository.unblock(owner = me.deviceId, remote = contact.remoteDeviceId)
        contactRepository.updateStatus(contactId, ContactStatus.EXPIRED)
    }
}
