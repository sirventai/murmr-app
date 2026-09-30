package uoc.edu.psirvent.murmr.presentation.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import uoc.edu.psirvent.murmr.data.network.RelayConfig
import uoc.edu.psirvent.murmr.data.network.RelayConfigStore
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.model.Contact

sealed interface ContactsUiState {
    data object Loading : ContactsUiState
    data object Empty : ContactsUiState
    data class Loaded(val contacts: List<Contact>) : ContactsUiState
}

class ContactsViewModel(
    userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val relayConfigStore: RelayConfigStore? = null,
) : ScreenModel {

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ContactsUiState> = userRepository.observeLocalUser()
        .flatMapLatest { user ->
            if (user == null) flowOf<ContactsUiState>(ContactsUiState.Empty)
            else {
                val relayFlow = relayConfigStore?.flow ?: flowOf(RelayConfig(""))
                relayFlow.flatMapLatest { cfg ->
                    contactRepository.observeByRelay(user.deviceId, cfg.baseUrl)
                }.flatMapLatest { list ->
                    if (list.isEmpty()) flowOf<ContactsUiState>(ContactsUiState.Empty)
                    else flowOf<ContactsUiState>(ContactsUiState.Loaded(list))
                }
            }
        }
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ContactsUiState.Loading,
        )
}
