package uoc.edu.psirvent.murmr.domain.usecase

import uoc.edu.psirvent.murmr.domain.model.MessageType
import uoc.edu.psirvent.murmr.util.Logger

class RequestPeerAvatarBigUseCase(
    private val sendEncrypted: SendEncryptedMessageUseCase,
) {
    private val tag = "AvatarRequest"

    suspend operator fun invoke(contactId: String) {
        runCatching {
            sendEncrypted(
                contactId = contactId,
                plaintext = "AVATAR_REQUEST",
                type = MessageType.PROFILE_UPDATE,
            )
            Logger.i(tag, "AVATAR_REQUEST enviado a $contactId")
        }.onFailure { Logger.w(tag, "fallo enviando AVATAR_REQUEST a $contactId: ${it.message}") }
    }
}
