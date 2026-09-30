package uoc.edu.psirvent.murmr.presentation.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.domain.model.ContactStatus
import uoc.edu.psirvent.murmr.domain.model.Conversation
import uoc.edu.psirvent.murmr.domain.usecase.DeleteContactSecureWipeUseCase
import uoc.edu.psirvent.murmr.presentation.components.CollapsingHeaderOverlay
import uoc.edu.psirvent.murmr.presentation.components.MurmrAvatar
import uoc.edu.psirvent.murmr.presentation.components.MurmrIcons
import uoc.edu.psirvent.murmr.presentation.components.SwipeToActionRow
import uoc.edu.psirvent.murmr.presentation.format.formatChatListTime
import uoc.edu.psirvent.murmr.presentation.screen.home.LocalBottomBarHeight
import uoc.edu.psirvent.murmr.presentation.components.IosLikeAlert
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject
import uoc.edu.psirvent.murmr.presentation.viewmodel.ChatsListUiState
import uoc.edu.psirvent.murmr.presentation.viewmodel.ChatsListViewModel

private enum class ChatFilter { ALL, UNREAD, READ }

@Composable
fun ChatsListContent(onOpenChat: (contactId: String) -> Unit) {
    val viewModel = rememberKoinInject<ChatsListViewModel>()
    val deleteContact = rememberKoinInject<DeleteContactSecureWipeUseCase>()
    val state by viewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(ChatFilter.ALL) }
    val listState = rememberLazyListState()
    val bottomBarHeight = LocalBottomBarHeight.current
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Conversation?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MurmrColors.BackgroundDark) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 24.dp, bottom = bottomBarHeight),
            ) {
                item("title") {
                    Text(
                        "Chats",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = MurmrColors.OnSurface,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }
                item("searchfilter") {
                    SearchAndFilter(
                        query = query,
                        onQueryChange = { query = it },
                        filter = filter,
                        onFilterChange = { filter = it },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                when (val s = state) {
                    ChatsListUiState.Loading -> item("loading") { CenteredText("Cargando...") }
                    ChatsListUiState.Empty -> item("empty") {
                        Box(modifier = Modifier.fillParentMaxSize()) { EmptyState() }
                    }
                    is ChatsListUiState.Loaded -> {
                        val visible = s.conversations.filter { conv ->
                            val q = query.trim().lowercase()
                            (q.isEmpty() || conv.contact.displayAlias.lowercase().contains(q) ||
                                (conv.lastMessagePreview?.lowercase()?.contains(q) == true)) &&
                                when (filter) {
                                    ChatFilter.ALL -> true
                                    ChatFilter.UNREAD -> conv.unreadCount > 0
                                    ChatFilter.READ -> conv.unreadCount == 0
                                }
                        }
                        if (visible.isEmpty()) {
                            item("nomatch") { CenteredText("Sin coincidencias") }
                        } else {
                            items(
                                count = visible.size,
                                key = { idx -> visible[idx].contact.contactId },
                            ) { index ->
                                val item = visible[index]
                                SwipeToActionRow(
                                    actionLabel = "Borrar",
                                    actionIcon = MurmrIcons.Delete,
                                    onAction = { pendingDelete = item },
                                ) {
                                    ConversationRow(item, onClick = { onOpenChat(item.contact.contactId) })
                                }
                            }
                        }
                    }
                    is ChatsListUiState.Error -> item("error") {
                        CenteredText("Error: ${s.message}", color = MurmrColors.Error)
                    }
                }
            }

            CollapsingHeaderOverlay(title = "Chats", listState = listState)

            val target = pendingDelete
            if (target != null) {
                IosLikeAlert(
                    title = "¿Borrar conversación con ${target.contact.displayAlias}?",
                    message = "Se eliminará el contacto, las claves y todo el chat. Tendrás que re-emparejar para volver a hablar.",
                    confirmLabel = "Borrar",
                    onConfirm = {
                        pendingDelete = null
                        scope.launch { deleteContact(target.contact.contactId) }
                    },
                    onDismiss = { pendingDelete = null },
                )
            }
        }
    }
}

@Composable
private fun CenteredText(text: String, color: Color = MurmrColors.OnSurfaceMuted) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color)
    }
}

@Composable
private fun SearchAndFilter(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: ChatFilter,
    onFilterChange: (ChatFilter) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MurmrColors.SurfaceDark)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                MurmrIcons.Search,
                null,
                tint = MurmrColors.OnSurfaceMuted,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                cursorBrush = SolidColor(MurmrColors.PrimaryViolet),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = MurmrColors.OnSurface,
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                "Buscar...",
                                color = MurmrColors.OnSurfaceFaint,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            )
                        }
                        inner()
                    }
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(label = "Todos", selected = filter == ChatFilter.ALL) { onFilterChange(ChatFilter.ALL) }
            FilterChip(label = "No leídos", selected = filter == ChatFilter.UNREAD) { onFilterChange(ChatFilter.UNREAD) }
            FilterChip(label = "Leídos", selected = filter == ChatFilter.READ) { onFilterChange(ChatFilter.READ) }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MurmrColors.PrimaryViolet else MurmrColors.SurfaceDark
    val fg = if (selected) Color.White else MurmrColors.OnSurfaceMuted
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun ConversationRow(item: Conversation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MurmrAvatar(initial = item.contact.displayAlias, size = 44.dp, avatar = item.contact.avatar)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.contact.displayAlias,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MurmrColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                when (item.contact.status) {
                    ContactStatus.VERIFIED -> {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            MurmrIcons.Lock,
                            contentDescription = "Verificado",
                            tint = MurmrColors.AccentCyan,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                    ContactStatus.UNVERIFIED -> {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            MurmrIcons.LockOpen,
                            contentDescription = "Sin verificar",
                            tint = MurmrColors.Error,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                    ContactStatus.BLOCKED -> {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            MurmrIcons.Block,
                            contentDescription = "Bloqueado",
                            tint = MurmrColors.Error,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                    else -> Unit
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                item.lastMessagePreview ?: "Sin mensajes todavía",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MurmrColors.OnSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatChatListTime(item.lastMessageAt),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MurmrColors.OnSurfaceFaint,
            )
            if (item.unreadCount > 0) {
                Spacer(Modifier.height(4.dp))
                UnreadBadge(item.unreadCount)
            }
        }
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MurmrColors.PrimaryViolet)
            .padding(horizontal = 7.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 99) "99+" else count.toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
            ),
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.25f))
        Column(
            modifier = Modifier.fillMaxWidth().alpha(0.18f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("S", "V", "M").forEach { initial ->
                GhostRow(initial)
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "No hay conversaciones",
            color = MurmrColors.OnSurface,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Empareja con otro teléfono para iniciar una nueva conversación",
            color = MurmrColors.OnSurfaceMuted,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(0.75f))
    }
}

@Composable
private fun GhostRow(initial: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MurmrAvatar(initial = initial, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.fillMaxWidth(0.55f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(MurmrColors.SurfaceDark),
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier.fillMaxWidth(0.85f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(MurmrColors.IncomingBubble),
            )
        }
    }
}
