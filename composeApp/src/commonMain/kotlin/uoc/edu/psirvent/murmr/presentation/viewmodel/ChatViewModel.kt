package uoc.edu.psirvent.murmr.presentation.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uoc.edu.psirvent.murmr.data.network.PresenceTracker
import uoc.edu.psirvent.murmr.data.network.TypingTracker
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.MessageRepository
import uoc.edu.psirvent.murmr.data.repository.SessionRepository
import uoc.edu.psirvent.murmr.data.repository.SettingsRepository
import uoc.edu.psirvent.murmr.domain.model.AppSettings
import uoc.edu.psirvent.murmr.domain.model.Contact
import uoc.edu.psirvent.murmr.domain.model.EffectiveExpiry
import uoc.edu.psirvent.murmr.domain.model.Message
import uoc.edu.psirvent.murmr.domain.model.MessageType
import uoc.edu.psirvent.murmr.domain.model.Session
import uoc.edu.psirvent.murmr.domain.model.SessionExpiry
import uoc.edu.psirvent.murmr.domain.protocol.ControlMessage
import uoc.edu.psirvent.murmr.domain.usecase.BroadcastProfileUpdateUseCase
import uoc.edu.psirvent.murmr.domain.usecase.MarkReadUseCase
import uoc.edu.psirvent.murmr.domain.usecase.SendEncryptedMessageUseCase
import uoc.edu.psirvent.murmr.presentation.components.SnackbarController
import uoc.edu.psirvent.murmr.util.Logger
import uoc.edu.psirvent.murmr.util.TextSanitizer
import uoc.edu.psirvent.murmr.util.Validators

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data class Loaded(
        val contact: Contact,
        val messages: List<Message>,
        val draft: String,
        val sending: Boolean,
        val hasMore: Boolean = false,
        val loadingMore: Boolean = false,
        val replyTo: Message? = null,
        val selected: Set<String> = emptySet(),
        val effectiveExpiry: EffectiveExpiry? = null,
        val sessionExpiresAt: Instant? = null,
        val expiryHasOverride: Boolean = false,
        val sessionActive: Boolean = true,
    ) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

class ChatViewModel(
    private val contactId: String,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val snackbar: SnackbarController? = null,
    private val markRead: MarkReadUseCase? = null,
    private val presenceTracker: PresenceTracker? = null,
    private val settingsRepository: SettingsRepository,
    private val expiryHandler: ChatExpiryHandler? = null,
    private val sessionRepository: SessionRepository,
    private val typingTracker: TypingTracker? = null,
    private val sendEncrypted: SendEncryptedMessageUseCase? = null,
    private val broadcastProfileUpdate: BroadcastProfileUpdateUseCase? = null,
) : ScreenModel {

    companion object {
        const val PAGE_SIZE = 50
        const val MAX_OLDER_IN_MEMORY = 500
        const val TYPING_START_THROTTLE_MS = 3_000L
        const val TYPING_STOP_AFTER_MS = 5_000L
    }

    private val draft = MutableStateFlow("")
    private val sending = MutableStateFlow(false)
    private val olderPages = MutableStateFlow<List<Message>>(emptyList())
    private val loadingMore = MutableStateFlow(false)
    private val replyTo = MutableStateFlow<Message?>(null)
    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    private var cachedTotal: Int = Int.MAX_VALUE
    private var typingActive = false
    private var lastTypingStartAt: Instant? = null
    private var typingStopJob: Job? = null

    private data class InternalState(
        val older: List<Message>,
        val draft: String,
        val sending: Boolean,
        val loadingMore: Boolean,
        val replyTo: Message?,
    )

    private val settingsFlow = settingsRepository.observeFirst()
    private val activeSessionFlow = sessionRepository.observeActive(contactId)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ChatUiState> = combine(
        contactRepository.observeById(contactId),
        messageRepository.observeRecentByContact(contactId, PAGE_SIZE.toLong()),
        combine(olderPages, draft, sending, loadingMore, replyTo) { o, d, s, lm, rt ->
            InternalState(o, d, s, lm, rt)
        },
        _selected,
        settingsFlow,
        activeSessionFlow,
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val contact = args[0] as Contact?
        @Suppress("UNCHECKED_CAST")
        val recent = args[1] as List<Message>
        val internal = args[2] as InternalState
        @Suppress("UNCHECKED_CAST")
        val sel = args[3] as Set<String>
        val settings = args[4] as AppSettings?
        val activeSession = args[5] as Session?
        if (contact == null) {
            ChatUiState.Error("Contacto no encontrado")
        } else {
            val merged = if (internal.older.isEmpty()) recent else internal.older + recent
            val effective = settings?.let { EffectiveExpiry.computeForContact(contact, it) }
            val cutoff = if (activeSession != null && effective?.duration != null) {
                (activeSession.renewedAt ?: activeSession.createdAt) + effective.duration
            } else null
            ChatUiState.Loaded(
                contact = contact,
                messages = merged,
                draft = internal.draft,
                sending = internal.sending,
                hasMore = recent.size == PAGE_SIZE && merged.size < cachedTotal,
                loadingMore = internal.loadingMore,
                replyTo = internal.replyTo,
                selected = sel,
                effectiveExpiry = effective,
                sessionExpiresAt = cutoff,
                expiryHasOverride = contact.localSessionExpiry != null,
                sessionActive = activeSession != null,
            )
        }
    }.stateIn(
        scope = screenModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState.Loading,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val online: StateFlow<Boolean?> = contactRepository.observeById(contactId)
        .flatMapLatest { contact ->
            val tracker = presenceTracker
            if (contact == null || tracker == null) flowOf(null)
            else tracker.observe(contact.remoteDeviceId)
        }
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val peerTyping: StateFlow<Boolean> = (typingTracker?.observe(contactId) ?: flowOf(false))
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    init {
        screenModelScope.launch {
            cachedTotal = runCatching {
                messageRepository.countTextByContact(contactId).toInt()
            }.getOrDefault(Int.MAX_VALUE)
            runCatching {
                val c = contactRepository.getById(contactId)
                Logger.i(
                    "ChatVM",
                    "abierto contactId=$contactId | alias='${c?.displayAlias}' " +
                        "remoteDeviceId=${c?.remoteDeviceId} status=${c?.status} " +
                        "pairedAt=${c?.pairedAt}",
                )
            }
            runCatching {
                broadcastProfileUpdate?.invoke(BroadcastProfileUpdateUseCase.Kind.ALIAS, targetContactId = contactId)
                broadcastProfileUpdate?.invoke(BroadcastProfileUpdateUseCase.Kind.AVATAR, targetContactId = contactId)
            }
        }
    }

    fun toggleSelection(messageId: String) {
        _selected.value = _selected.value.let {
            if (messageId in it) it - messageId else it + messageId
        }
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun selectedAsClipboardText(): String {
        val ids = _selected.value
        if (ids.isEmpty()) return ""
        val loaded = state.value as? ChatUiState.Loaded ?: return ""
        return loaded.messages
            .filter { it.messageId in ids }
            .joinToString("\n") { it.plaintextLocal }
    }

    fun loadMore() {
        if (loadingMore.value) return
        val current = (state.value as? ChatUiState.Loaded) ?: return
        if (!current.hasMore) return
        val oldestTs = current.messages.firstOrNull()?.timestamp?.toEpochMilliseconds() ?: return
        loadingMore.value = true
        screenModelScope.launch {
            try {
                val older = messageRepository.loadBefore(contactId, oldestTs, PAGE_SIZE.toLong())
                if (older.isNotEmpty()) {
                    val combined = older + olderPages.value
                    olderPages.value = if (combined.size > MAX_OLDER_IN_MEMORY) {
                        combined.takeLast(MAX_OLDER_IN_MEMORY)
                    } else {
                        combined
                    }
                }
                cachedTotal = messageRepository.countTextByContact(contactId).toInt()
            } catch (t: Throwable) {
                snackbar?.show("No se pudo cargar el histórico: ${t.message}")
            } finally {
                loadingMore.value = false
            }
        }
    }

    fun onDraftChange(value: String) {
        val sanitized = TextSanitizer.sanitize(value, allowNewlines = true)
        draft.value = sanitized
        handleTypingSignal(sanitized)
    }

    fun setReplyTo(message: Message) {
        replyTo.value = message
    }

    fun clearReplyTo() {
        replyTo.value = null
    }

    suspend fun resolveQuotedMessage(messageId: String): Message? =
        runCatching { messageRepository.getById(messageId) }.getOrNull()

    fun setLocalExpiry(value: SessionExpiry?, customSeconds: Long = 0L) {
        val handler = expiryHandler
        screenModelScope.launch {
            withContext(NonCancellable) {
                if (handler != null) {
                    val peerAlias = handler.applyLocal(contactId, value, customSeconds)
                    if (peerAlias != null) {
                        snackbar?.show("Aplicado lo de $peerAlias: es más restrictivo")
                    }
                } else {
                    contactRepository.setLocalExpiry(contactId, value, customSeconds)
                }
            }
        }
    }

    fun renewSession() {
        screenModelScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    val active = sessionRepository.getActive(contactId) ?: return@runCatching
                    val now = Clock.System.now()
                    sessionRepository.markRenewed(active.sessionId, now)

                    sendEncrypted?.let { send ->
                        runCatching {
                            send(
                                contactId = contactId,
                                plaintext = ControlMessage.SessionRenewed(now.toEpochMilliseconds()).serialize(),
                                type = MessageType.PROFILE_UPDATE,
                            )
                        }.onFailure { Logger.w("ChatVM", "SESSION_RENEWED no enviado", it) }
                    }
                }
            }
        }
    }

    fun onMarkRead() {
        screenModelScope.launch {
            if (markRead != null) {
                runCatching { markRead.invoke(contactId) }
            } else {
                contactRepository.resetUnread(contactId)
            }
        }
    }

    fun onSend(send: suspend (text: String, replyToMessageId: String?) -> Unit) {
        val text = draft.value.trim()
        val v = Validators.chatMessage(text)
        if (!v.isValid) {
            v.errorOrNull()?.let { snackbar?.show(it) }
            return
        }
        if (sending.value) return
        sending.value = true
        val reply = replyTo.value
        cancelTypingTimer()
        screenModelScope.launch {
            try {
                send(text, reply?.messageId)
                draft.value = ""
                replyTo.value = null
            } catch (t: Throwable) {
                snackbar?.show("Error al enviar: ${t.message ?: "no se pudo enviar"}")
            } finally {
                sending.value = false
            }
        }
    }

    private fun handleTypingSignal(text: String) {
        val send = sendEncrypted ?: return
        val now = Clock.System.now()
        if (text.isBlank()) {
            typingStopJob?.cancel()
            if (typingActive) {
                typingActive = false
                screenModelScope.launch {
                    runCatching {
                        send(contactId = contactId, plaintext = "TYPING_STOP", type = MessageType.PROFILE_UPDATE)
                    }
                }
            }
            return
        }
        val elapsed = lastTypingStartAt?.let { (now - it).inWholeMilliseconds } ?: Long.MAX_VALUE
        if (!typingActive || elapsed >= TYPING_START_THROTTLE_MS) {
            typingActive = true
            lastTypingStartAt = now
            screenModelScope.launch {
                runCatching {
                    send(contactId = contactId, plaintext = "TYPING_START", type = MessageType.PROFILE_UPDATE)
                }
            }
        }
        typingStopJob?.cancel()
        typingStopJob = screenModelScope.launch {
            delay(TYPING_STOP_AFTER_MS)
            if (typingActive) {
                typingActive = false
                runCatching {
                    send(contactId = contactId, plaintext = "TYPING_STOP", type = MessageType.PROFILE_UPDATE)
                }
            }
        }
    }

    private fun cancelTypingTimer() {
        typingStopJob?.cancel()
        typingActive = false
    }
}
