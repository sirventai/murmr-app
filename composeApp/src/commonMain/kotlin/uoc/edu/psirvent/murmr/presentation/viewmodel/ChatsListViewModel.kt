package uoc.edu.psirvent.murmr.presentation.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import uoc.edu.psirvent.murmr.data.network.RelayConfig
import uoc.edu.psirvent.murmr.data.network.RelayConfigStore
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.MessageRepository
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.model.Conversation

sealed interface ChatsListUiState {
    data object Loading : ChatsListUiState
    data object Empty : ChatsListUiState
    data class Loaded(val conversations: List<Conversation>) : ChatsListUiState
    data class Error(val message: String) : ChatsListUiState
}

class ChatsListViewModel(
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val relayConfigStore: RelayConfigStore? = null,
) : ScreenModel {

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ChatsListUiState> = userRepository.observeLocalUser()
        .flatMapLatest { user ->
            if (user == null) flowOf<ChatsListUiState>(ChatsListUiState.Empty)
            else {
                val relayFlow = relayConfigStore?.flow ?: flowOf(RelayConfig(""))
                relayFlow.flatMapLatest { cfg ->
                    contactRepository.observeByRelay(user.deviceId, cfg.baseUrl)
                }.flatMapLatest { contacts ->
                    if (contacts.isEmpty()) flowOf<ChatsListUiState>(ChatsListUiState.Empty)
                    else combine(contacts.map { messageRepository.observeLastByContact(it.contactId) }) { lastMessages ->
                        ChatsListUiState.Loaded(
                            conversations = contacts.mapIndexed { idx, contact ->
                                Conversation(
                                    contact = contact,
                                    lastMessagePreview = lastMessages[idx]?.plaintextLocal,
                                    lastMessageAt = lastMessages[idx]?.timestamp ?: contact.lastMessageAt,
                                    unreadCount = contact.unreadCount,
                                )
                            },
                        )
                    }
                }
            }
        }
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChatsListUiState.Loading,
        )
}
