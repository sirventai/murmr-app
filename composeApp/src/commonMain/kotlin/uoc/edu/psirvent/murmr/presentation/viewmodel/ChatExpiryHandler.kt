package uoc.edu.psirvent.murmr.presentation.viewmodel

import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.SettingsRepository
import uoc.edu.psirvent.murmr.domain.model.EffectiveExpiry
import uoc.edu.psirvent.murmr.domain.model.SessionExpiry
import uoc.edu.psirvent.murmr.domain.usecase.BroadcastExpiryUpdateUseCase
import uoc.edu.psirvent.murmr.util.Logger

class ChatExpiryHandler(
    private val contactRepository: ContactRepository,
    private val settingsRepository: SettingsRepository,
    private val broadcastExpiryUpdate: BroadcastExpiryUpdateUseCase? = null,
) {
    private val tag = "ChatExpiryHandler"

    suspend fun applyLocal(
        contactId: String,
        value: SessionExpiry?,
        customSeconds: Long,
    ): String? {
        contactRepository.setLocalExpiry(contactId, value, customSeconds)
        runCatching { broadcastExpiryUpdate?.invoke(targetContactId = contactId) }
            .onFailure { Logger.w(tag, "broadcast EXPIRY_UPDATE fallo", it) }
        return peerWinsAlias(contactId)
    }

    private suspend fun peerWinsAlias(contactId: String): String? {
        val contact = contactRepository.getById(contactId) ?: return null
        val settings = settingsRepository.getFirst() ?: return null
        val effective = EffectiveExpiry.computeForContact(contact, settings)
        return if (effective.origin == EffectiveExpiry.Origin.PEER) contact.displayAlias else null
    }
}
