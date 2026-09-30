package uoc.edu.psirvent.murmr.domain.usecase

import io.ktor.util.encodeBase64
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.model.ContactStatus
import uoc.edu.psirvent.murmr.domain.model.MessageType
import uoc.edu.psirvent.murmr.domain.protocol.ControlMessage
import uoc.edu.psirvent.murmr.util.Logger

class BroadcastProfileUpdateUseCase(
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val sendEncrypted: SendEncryptedMessageUseCase,
) {
    private val tag = "ProfileUpdate"
    private val concurrency = Semaphore(MAX_CONCURRENT_SENDS)

    enum class Kind { ALIAS, AVATAR }

    suspend operator fun invoke(kind: Kind, targetContactId: String? = null) {
        val me = userRepository.getLocalUser() ?: return

        val allTargets = contactRepository.allFor(me.deviceId).filter {
            it.status == ContactStatus.VERIFIED || it.status == ContactStatus.UNVERIFIED
        }
        val targets = if (targetContactId != null) {
            allTargets.filter { it.contactId == targetContactId }
        } else {
            allTargets
        }
        if (targets.isEmpty()) {
            Logger.i(tag, "broadcast $kind: 0 destinatarios - skip")
            return
        }

        val payload = when (kind) {
            Kind.ALIAS -> ControlMessage.ProfileUpdate(
                ControlMessage.ProfileUpdate.Kind.ALIAS,
                me.alias,
            ).serialize()
            Kind.AVATAR -> ControlMessage.ProfileUpdate(
                ControlMessage.ProfileUpdate.Kind.AVATAR,
                me.avatar?.encodeBase64() ?: "NONE",
            ).serialize()
        }
        Logger.i(
            tag,
            "broadcast $kind a ${targets.size} contactos verificados (payload=${payload.length}B)",
        )

        coroutineScope {
            targets.map { c ->
                async {
                    concurrency.withPermit {
                        runCatching {
                            sendEncrypted(
                                contactId = c.contactId,
                                plaintext = payload,
                                type = MessageType.PROFILE_UPDATE,
                            )
                        }.onFailure {
                            Logger.w(tag, "fallo a contacto=${c.contactId}: ${it.message}")
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
