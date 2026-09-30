package uoc.edu.psirvent.murmr.presentation.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.core.parameter.parametersOf
import uoc.edu.psirvent.murmr.domain.model.Message
import uoc.edu.psirvent.murmr.domain.model.SessionExpiry
import uoc.edu.psirvent.murmr.domain.usecase.SendEncryptedMessageUseCase
import uoc.edu.psirvent.murmr.presentation.components.ChatBubble
import uoc.edu.psirvent.murmr.presentation.components.MurmrIcons
import uoc.edu.psirvent.murmr.presentation.util.UiTextos
import uoc.edu.psirvent.murmr.presentation.format.formatHm
import uoc.edu.psirvent.murmr.presentation.navigation.InteractiveSwipeBack
import uoc.edu.psirvent.murmr.presentation.navigation.PreviousScreenBackground
import uoc.edu.psirvent.murmr.presentation.screen.avatar.AvatarFullScreen
import uoc.edu.psirvent.murmr.presentation.screen.contacts.ContactDetailScreen
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject
import uoc.edu.psirvent.murmr.presentation.viewmodel.ChatUiState
import uoc.edu.psirvent.murmr.presentation.viewmodel.ChatViewModel

class ChatScreen(private val contactId: String) : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberKoinInject<ChatViewModel> { parametersOf(contactId) }
        val state by viewModel.state.collectAsState()
        val online by viewModel.online.collectAsState()
        val peerTyping by viewModel.peerTyping.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        val msgCount = (state as? ChatUiState.Loaded)?.messages?.size ?: 0
        LaunchedEffect(msgCount) { viewModel.onMarkRead() }

        InteractiveSwipeBack(
            onCompleted = { navigator.pop() },
            background = { PreviousScreenBackground() },
            foreground = {
                ChatScreenForeground(
                    state = state,
                    online = online,
                    peerTyping = peerTyping,
                    contactId = contactId,
                    viewModel = viewModel,
                    navigator = navigator,
                )
            },
        )
    }
}

@Composable
private fun ChatScreenForeground(
    state: ChatUiState,
    online: Boolean?,
    peerTyping: Boolean,
    contactId: String,
    viewModel: ChatViewModel,
    navigator: Navigator,
) {
    val sendEncrypted = rememberKoinInject<SendEncryptedMessageUseCase>()
    Surface(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        color = MurmrColors.BackgroundDark,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val s = state) {
                ChatUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cargando...", color = MurmrColors.OnSurfaceMuted)
                }
                is ChatUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.message, color = MurmrColors.Error)
                }
                is ChatUiState.Loaded -> {
                    val clipboard = LocalClipboardManager.current
                    ChatLoaded(
                        state = s,
                        online = online,
                        peerTyping = peerTyping,
                        onBack = { navigator.pop() },
                        onOpenDetail = {
                            navigator.push(ContactDetailScreen(contactId = contactId, cameFromChat = true))
                        },
                        onOpenAvatar = { navigator.push(AvatarFullScreen(contactId)) },
                        onDraft = viewModel::onDraftChange,
                        onSend = {
                            viewModel.onSend { text, replyToId ->
                                try {
                                    sendEncrypted(
                                        contactId = contactId,
                                        plaintext = text,
                                        replyToMessageId = replyToId,
                                    )
                                } catch (_: Throwable) {
                                }
                            }
                        },
                        onLoadMore = viewModel::loadMore,
                        onReply = viewModel::setReplyTo,
                        onClearReply = viewModel::clearReplyTo,
                        onToggleSelect = viewModel::toggleSelection,
                        onClearSelection = viewModel::clearSelection,
                        onCopySelection = {
                            val txt = viewModel.selectedAsClipboardText()
                            if (txt.isNotEmpty()) clipboard.setText(AnnotatedString(txt))
                            viewModel.clearSelection()
                        },
                        onChangeExpiry = { value, customSeconds ->
                            viewModel.setLocalExpiry(value, customSeconds)
                        },
                        onRenewSession = { viewModel.renewSession() },
                        resolveQuoted = viewModel::resolveQuotedMessage,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatLoaded(
    state: ChatUiState.Loaded,
    online: Boolean?,
    peerTyping: Boolean,
    onBack: () -> Unit,
    onOpenDetail: () -> Unit,
    onOpenAvatar: () -> Unit,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onLoadMore: () -> Unit,
    onReply: (Message) -> Unit,
    onClearReply: () -> Unit,
    onToggleSelect: (String) -> Unit,
    onClearSelection: () -> Unit,
    onCopySelection: () -> Unit,
    onChangeExpiry: ((SessionExpiry?, Long) -> Unit)? = null,
    onRenewSession: (() -> Unit)? = null,
    resolveQuoted: (suspend (String) -> Message?)? = null,
) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var expiryChooserOpen by remember { mutableStateOf(false) }

    val visibleMessages = remember(state.messages, searchQuery, searchOpen) {
        if (searchOpen && searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            state.messages.filter { it.plaintextLocal.lowercase().contains(q) }
        } else {
            state.messages
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        if (state.selected.isNotEmpty()) {
            SelectionBar(
                count = state.selected.size,
                onClearSelection = onClearSelection,
                onCopySelection = onCopySelection,
            )
        } else {
            ChatHeader(
                state = state,
                online = online,
                peerTyping = peerTyping,
                searchOpen = searchOpen,
                searchQuery = searchQuery,
                onBack = onBack,
                onOpenDetail = onOpenDetail,
                onOpenAvatar = onOpenAvatar,
                onToggleSearch = {
                    searchOpen = !searchOpen
                    if (!searchOpen) searchQuery = ""
                },
                onSearchChange = { searchQuery = it },
            )
        }
        Spacer(Modifier.height(8.dp))
        if (!searchOpen && onChangeExpiry != null) {
            ExpiryBar(
                effective = state.effectiveExpiry,
                expiresAt = state.sessionExpiresAt,
                hasOverride = state.expiryHasOverride,
                contactAlias = state.contact.displayAlias,
                onClick = { expiryChooserOpen = true },
                onRenew = if (state.effectiveExpiry?.duration != null) onRenewSession else null,
            )
        }
        if (!searchOpen) TodayPill()
        if (expiryChooserOpen && onChangeExpiry != null) {
            ExpiryChooserSheet(
                current = state.contact.localSessionExpiry,
                currentCustomSeconds = state.contact.localCustomExpirySeconds,
                hasOverride = state.expiryHasOverride,
                onDismiss = { expiryChooserOpen = false },
                onChoose = { value, customSeconds ->
                    expiryChooserOpen = false
                    onChangeExpiry(value, customSeconds)
                },
                onResetToDefault = {
                    expiryChooserOpen = false
                    onChangeExpiry(null, 0L)
                },
            )
        }
        if (searchOpen && searchQuery.isNotBlank() && visibleMessages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Sin coincidencias para \"$searchQuery\"",
                    color = MurmrColors.OnSurfaceMuted,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                )
            }
        } else {
            MessagesList(
                state = state,
                visibleMessages = visibleMessages,
                searchOpen = searchOpen,
                onLoadMore = onLoadMore,
                onReply = onReply,
                onToggleSelect = onToggleSelect,
                resolveQuoted = resolveQuoted,
                modifier = Modifier.weight(1f),
            )
        }
        if (!state.sessionActive) {
            SessionClosedBanner(peerAlias = state.contact.displayAlias)
        } else {
            state.replyTo?.let { rt ->
                val author = if (rt.isOutgoing) "Tú" else state.contact.displayAlias
                ReplyPreviewBar(author = author, text = rt.plaintextLocal, onCancel = onClearReply)
            }
            ChatInputBar(state = state, onDraft = onDraft, onSend = onSend)
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onClearSelection: () -> Unit,
    onCopySelection: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MurmrColors.SurfaceDark)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClearSelection) {
            Icon(MurmrIcons.Close, UiTextos.Cancelar, tint = MurmrColors.OnSurface)
        }
        Spacer(Modifier.width(4.dp))
        Text(
            "$count seleccionado${if (count == 1) "" else "s"}",
            color = MurmrColors.OnSurface,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            ),
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onCopySelection) {
            Text(
                "Copiar",
                color = MurmrColors.PrimaryViolet,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(MurmrColors.DividerDark))
}

@Composable
private fun MessagesList(
    state: ChatUiState.Loaded,
    visibleMessages: List<Message>,
    searchOpen: Boolean,
    onLoadMore: () -> Unit,
    onReply: (Message) -> Unit,
    onToggleSelect: (String) -> Unit,
    resolveQuoted: (suspend (String) -> Message?)?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    val lastMessageId = visibleMessages.lastOrNull()?.messageId
    val lastIsOutgoing = visibleMessages.lastOrNull()?.isOutgoing == true
    LaunchedEffect(lastMessageId, searchOpen) {
        if (visibleMessages.isEmpty()) return@LaunchedEffect
        val lastVisibleIdx = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        val firstFrame = lastVisibleIdx == null
        val nearBottom = lastVisibleIdx != null &&
            lastVisibleIdx >= visibleMessages.size - 3
        when {
            firstFrame -> listState.scrollToItem(visibleMessages.lastIndex)
            lastIsOutgoing || nearBottom ->
                listState.animateScrollToItem(visibleMessages.lastIndex)
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { idx -> if (idx == 0 && state.hasMore && !state.loadingMore) onLoadMore() }
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        state = listState,
    ) {
        if (state.loadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MurmrColors.PrimaryViolet,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        items(
            count = visibleMessages.size,
            key = { idx -> visibleMessages[idx].messageId },
        ) { i ->
            val m = visibleMessages[i]
            val inMem = m.replyToMessageId?.let { id ->
                visibleMessages.firstOrNull { it.messageId == id }
            }

            val fetched = produceState<Message?>(initialValue = null, m.replyToMessageId, inMem) {
                value = if (inMem != null || m.replyToMessageId == null) null
                else resolveQuoted?.invoke(m.replyToMessageId)
            }
            val quoted = inMem ?: fetched.value
            val quotedAuthor = quoted?.let { q ->
                if (q.isOutgoing) "Tú" else state.contact.displayAlias
            }
            val isSelected = m.messageId in state.selected
            val inSelectionMode = state.selected.isNotEmpty()
            SwipeToReply(onReply = { onReply(m) }) {
                ChatBubble(
                    text = m.plaintextLocal,
                    timestamp = formatHm(m.timestamp),
                    isOutgoing = m.isOutgoing,
                    status = m.status,
                    quotedText = quoted?.plaintextLocal
                        ?: m.replyToMessageId?.let { "Mensaje citado" },
                    quotedAuthor = quotedAuthor,
                    selected = isSelected,
                    onLongPress = { onToggleSelect(m.messageId) },
                    onTap = if (inSelectionMode) {
                        { onToggleSelect(m.messageId) }
                    } else null,
                )
            }
        }
    }
}
