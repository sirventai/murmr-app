package uoc.edu.psirvent.murmr.presentation.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uoc.edu.psirvent.murmr.domain.model.ContactStatus
import uoc.edu.psirvent.murmr.presentation.components.MurmrAvatar
import uoc.edu.psirvent.murmr.presentation.components.MurmrIcons
import uoc.edu.psirvent.murmr.presentation.format.formatChatListTime
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.viewmodel.ChatUiState

@Composable
internal fun SessionClosedBanner(peerAlias: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MurmrColors.SurfaceDark)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = MurmrIcons.Lock,
            contentDescription = null,
            tint = MurmrColors.Error,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Sesión cerrada",
            color = MurmrColors.OnSurface,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$peerAlias ha terminado esta sesión o la caducidad venció. " +
                "Para volver a chatear, tienes que re-emparejar.",
            color = MurmrColors.OnSurfaceMuted,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun ChatHeader(
    state: ChatUiState.Loaded,
    online: Boolean?,
    peerTyping: Boolean,
    searchOpen: Boolean,
    searchQuery: String,
    onBack: () -> Unit,
    onOpenDetail: () -> Unit,
    onOpenAvatar: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(MurmrIcons.ArrowBack, contentDescription = "Atrás", tint = MurmrColors.OnSurface, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(2.dp))
            if (searchOpen) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = {
                        Text(
                            "Buscar en este chat...",
                            color = MurmrColors.OnSurfaceFaint,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MurmrColors.OnSurface,
                        unfocusedTextColor = MurmrColors.OnSurface,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MurmrColors.PrimaryViolet,
                    ),
                )
            } else {
                Box(modifier = Modifier.clickable(onClick = onOpenAvatar)) {
                    MurmrAvatar(initial = state.contact.displayAlias, size = 36.dp, avatar = state.contact.avatar)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenDetail)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.contact.displayAlias,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                            color = MurmrColors.OnSurface,
                        )
                        when (state.contact.status) {
                            ContactStatus.VERIFIED -> {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    MurmrIcons.Lock,
                                    contentDescription = "Verificado",
                                    tint = MurmrColors.AccentCyan,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                            ContactStatus.UNVERIFIED -> {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "-",
                                    color = MurmrColors.OnSurfaceMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "sin verificar",
                                    color = MurmrColors.OnSurfaceMuted,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                )
                            }
                            ContactStatus.BLOCKED -> {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    MurmrIcons.Block,
                                    contentDescription = "Bloqueado",
                                    tint = MurmrColors.Error,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    "bloqueado",
                                    color = MurmrColors.Error,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                )
                            }
                            else -> Unit
                        }
                    }
                    val isBlocked = state.contact.status == ContactStatus.BLOCKED
                    val isExpiredContact = state.contact.status == ContactStatus.EXPIRED
                    val sessionDead = !state.sessionActive
                    val effectiveOnline = peerTyping || online == true
                    val lastSeenInstant = state.contact.lastMessageAt ?: state.contact.pairedAt
                    val presenceLabel: String
                    val presenceColor: Color
                    when {
                        isBlocked -> {
                            presenceLabel = "bloqueado"
                            presenceColor = MurmrColors.Error
                        }
                        isExpiredContact || sessionDead -> {
                            presenceLabel = "sesión cerrada"
                            presenceColor = MurmrColors.Error
                        }
                        peerTyping -> {
                            presenceLabel = "escribiendo..."
                            presenceColor = MurmrColors.AccentCyan
                        }
                        effectiveOnline -> {
                            presenceLabel = "En línea"
                            presenceColor = MurmrColors.AccentCyan
                        }
                        else -> {
                            presenceLabel = "última vez ${formatChatListTime(lastSeenInstant)}"
                            presenceColor = MurmrColors.OnSurfaceMuted
                        }
                    }
                    Text(
                        presenceLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = presenceColor,
                    )
                }
            }
            IconButton(onClick = onToggleSearch) {
                Icon(
                    if (searchOpen) MurmrIcons.Close else MurmrIcons.Search,
                    contentDescription = if (searchOpen) "Cerrar búsqueda" else "Buscar",
                    tint = MurmrColors.OnSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (!searchOpen) {
                IconButton(onClick = onOpenDetail) {
                    Icon(MurmrIcons.InfoOutline, contentDescription = "Información del contacto", tint = MurmrColors.OnSurfaceMuted, modifier = Modifier.size(22.dp))
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(MurmrColors.DividerDark))
    }
}

@Composable
internal fun ReplyPreviewBar(author: String, text: String, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MurmrColors.SurfaceDark)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(28.dp)
                .background(MurmrColors.PrimaryViolet),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Respondiendo a $author",
                color = MurmrColors.PrimaryViolet,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            )
            Text(
                text,
                color = MurmrColors.OnSurfaceMuted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onCancel) {
            Icon(MurmrIcons.Close, contentDescription = "Cancelar respuesta", tint = MurmrColors.OnSurfaceMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
internal fun TodayPill() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MurmrColors.PrimaryViolet.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                "Today",
                color = MurmrColors.OnSurfaceMuted,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}

@Composable
internal fun ChatInputBar(
    state: ChatUiState.Loaded,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(color = MurmrColors.BackgroundDark) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(MurmrColors.DividerDark))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MurmrColors.SurfaceDark)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    if (state.draft.isEmpty()) {
                        Text(
                            "Mensaje",
                            color = MurmrColors.OnSurfaceFaint,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        )
                    }
                    BasicTextField(
                        value = state.draft,
                        onValueChange = onDraft,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 20.dp, max = 100.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            color = MurmrColors.OnSurface,
                        ),
                        cursorBrush = SolidColor(MurmrColors.PrimaryViolet),
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    )
                }
                Spacer(Modifier.width(6.dp))
                val active = state.draft.isNotBlank() && !state.sending
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (active) MurmrColors.PrimaryViolet else MurmrColors.SurfaceDark)
                        .clickable(enabled = active) { onSend() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MurmrIcons.Send,
                        contentDescription = "Enviar",
                        tint = if (active) Color.White else MurmrColors.OnSurfaceFaint,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}
