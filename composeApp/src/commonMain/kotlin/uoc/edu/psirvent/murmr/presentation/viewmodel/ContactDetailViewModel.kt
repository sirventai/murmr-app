package uoc.edu.psirvent.murmr.presentation.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.SessionRepository
import uoc.edu.psirvent.murmr.data.repository.SettingsRepository
import uoc.edu.psirvent.murmr.domain.model.Contact
import uoc.edu.psirvent.murmr.domain.model.EffectiveExpiry
import uoc.edu.psirvent.murmr.domain.model.Session
import uoc.edu.psirvent.murmr.domain.usecase.BroadcastProfileUpdateUseCase
import uoc.edu.psirvent.murmr.domain.usecase.RotateSessionKeyUseCase
import uoc.edu.psirvent.murmr.util.Logger
import uoc.edu.psirvent.murmr.util.ValidationResult
import uoc.edu.psirvent.murmr.util.Validators

sealed interface ContactDetailUiState {
    data object Loading : ContactDetailUiState
    data class Loaded(
        val contact: Contact,
        val activeSession: Session?,
        val sessionHistory: List<Session> = emptyList(),
        val effectiveExpiry: EffectiveExpiry? = null,
    ) : ContactDetailUiState
    data object NotFound : ContactDetailUiState
}

class ContactDetailViewModel(
    private val contactId: String,
    private val contactRepository: ContactRepository,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository? = null,
    private val broadcastProfileUpdate: BroadcastProfileUpdateUseCase? = null,
    private val rotateSessionKey: RotateSessionKeyUseCase? = null,
) : ScreenModel {

    private val settingsFlow = settingsRepository?.observeFirst() ?: flowOf(null)

    init {
        screenModelScope.launch {
            runCatching {
                broadcastProfileUpdate?.invoke(
                    BroadcastProfileUpdateUseCase.Kind.ALIAS,
                    targetContactId = contactId,
                )
                broadcastProfileUpdate?.invoke(
                    BroadcastProfileUpdateUseCase.Kind.AVATAR,
                    targetContactId = contactId,
                )
            }
        }
    }

    val state: StateFlow<ContactDetailUiState> = combine(
        contactRepository.observeById(contactId),
        sessionRepository.observeActive(contactId),
        sessionRepository.observeAllForContact(contactId),
        settingsFlow,
    ) { contact, active, all, settings ->
        if (contact == null) ContactDetailUiState.NotFound
        else {
            val effective = if (settings != null) {
                EffectiveExpiry.compute(
                    localExpiry = settings.sessionExpiry,
                    localCustomSeconds = settings.customExpirySeconds,
                    peerExpiry = contact.peerSessionExpiry,
                    peerCustomSeconds = contact.peerCustomExpirySeconds,
                )
            } else null
            ContactDetailUiState.Loaded(contact, active, all, effective)
        }
    }.stateIn(
        scope = screenModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContactDetailUiState.Loading,
    )

    fun rename(newAlias: String): ValidationResult {
        val result = Validators.alias(newAlias)
        if (result is ValidationResult.Ok) {
            screenModelScope.launch {
                contactRepository.updateAlias(contactId, newAlias.trim())
            }
        }
        return result
    }

    fun rotateKeys(onResult: (Boolean) -> Unit) {
        val rotator = rotateSessionKey
        if (rotator == null) {
            onResult(false)
            return
        }
        screenModelScope.launch {
            val active = sessionRepository.getActive(contactId)
            if (active == null) {
                Logger.w("ContactDetailVM", "rotateKeys: sin sesion activa para $contactId")
                onResult(false)
                return@launch
            }
            val ok = runCatching { rotator(active) }.isSuccess
            onResult(ok)
        }
    }
}
