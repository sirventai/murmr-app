package uoc.edu.psirvent.murmr.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.SettingsRepository
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.model.ContactStatus
import uoc.edu.psirvent.murmr.domain.model.EffectiveExpiry
import uoc.edu.psirvent.murmr.domain.model.MessageType
import uoc.edu.psirvent.murmr.domain.protocol.ControlMessage
import uoc.edu.psirvent.murmr.util.Logger

class BroadcastExpiryUpdateUseCase(
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val settingsRepository: SettingsRepository,
    private val sendEncrypted: SendEncryptedMessageUseCase,
) {
    private val tag = "ExpiryUpdate"
    private val concurrency = Semaphore(MAX_CONCURRENT_SENDS)

    suspend operator fun invoke(targetContactId: String? = null) {
        val me = userRepository.getLocalUser() ?: return
        val settings = settingsRepository.getFirst() ?: return

        if (targetContactId == null) {
            Logger.i(tag, "broadcast EXPIRY_UPDATE sin target, no-op (default global no se propaga)")
            return
        }
        val all = contactRepository.allFor(me.deviceId).filter {
            it.status == ContactStatus.VERIFIED || it.status == ContactStatus.UNVERIFIED
        }
        val targets = all.filter { it.contactId == targetContactId }
        if (targets.isEmpty()) {
            Logger.i(tag, "broadcast EXPIRY_UPDATE: 0 destinatarios, skip")
            return
        }

        coroutineScope {
            targets.map { c ->
                async {
                    concurrency.withPermit {
                        val (resolvedExpiry, resolvedSeconds) = EffectiveExpiry.resolveLocal(c, settings)
                        val payload = ControlMessage.ExpiryUpdate(resolvedExpiry, resolvedSeconds).serialize()
                        Logger.i(tag, "-> ${c.contactId}: $payload")
                        runCatching {
                            sendEncrypted(
                                contactId = c.contactId,
                                plaintext = payload,
                                type = MessageType.PROFILE_UPDATE,
                            )
                        }.onFailure {
                            Logger.w(tag, "fallo enviando EXPIRY_UPDATE a ${c.contactId}: ${it.message}")
                        }
                    }
                }
            }.forEach { it.await() }
        }
    }

    companion object {
        private const val MAX_CONCURRENT_SENDS = 5
    }
}
