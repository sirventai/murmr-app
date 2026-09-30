package uoc.edu.psirvent.murmr.domain.usecase

import uoc.edu.psirvent.murmr.crypto.KeyStoreManager
import uoc.edu.psirvent.murmr.data.network.DeviceTokenStore
import uoc.edu.psirvent.murmr.data.repository.BlockedContactRepository
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.MessageRepository
import uoc.edu.psirvent.murmr.data.repository.OutboxRepository
import uoc.edu.psirvent.murmr.data.repository.PairingLogRepository
import uoc.edu.psirvent.murmr.data.repository.SessionRepository
import uoc.edu.psirvent.murmr.data.repository.SettingsRepository
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.model.MessageType

class DeleteAccountUseCase(
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository,
    private val settingsRepository: SettingsRepository,
    private val blockedContactRepository: BlockedContactRepository,
    private val pairingLogRepository: PairingLogRepository,
    private val keyStore: KeyStoreManager,
    private val tokenStore: DeviceTokenStore,
    private val outboxRepository: OutboxRepository,
    private val sendEncrypted: SendEncryptedMessageUseCase? = null,
) {
    suspend operator fun invoke() {
        sendEncrypted?.let { send ->
            val me = userRepository.getLocalUser()
            if (me != null) {
                val all = runCatching { contactRepository.allFor(me.deviceId) }
                    .getOrDefault(emptyList())
                all.forEach { contact ->
                    runCatching {
                        send(
                            contactId = contact.contactId,
                            plaintext = "SESSION_REVOKED",
                            type = MessageType.PROFILE_UPDATE,
                            bypassExpiryCheck = true,
                        )
                    }
                }
            }
        }
        keyStore.clear()
        runCatching { tokenStore.clear() }
        runCatching { outboxRepository.clear() }
        messageRepository.deleteAll()
        pairingLogRepository.deleteAll()
        blockedContactRepository.deleteAll()
        contactRepository.deleteAll()
        settingsRepository.deleteAll()
        userRepository.deleteAll()
    }
}
