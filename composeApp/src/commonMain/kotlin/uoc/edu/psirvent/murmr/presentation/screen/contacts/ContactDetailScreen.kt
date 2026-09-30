package uoc.edu.psirvent.murmr.presentation.screen.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import uoc.edu.psirvent.murmr.domain.model.Contact
import uoc.edu.psirvent.murmr.domain.model.ContactStatus
import uoc.edu.psirvent.murmr.domain.model.EffectiveExpiry
import uoc.edu.psirvent.murmr.domain.model.Session
import uoc.edu.psirvent.murmr.domain.model.SessionExpiry
import uoc.edu.psirvent.murmr.domain.usecase.BlockContactUseCase
import uoc.edu.psirvent.murmr.domain.usecase.DeleteContactSecureWipeUseCase
import uoc.edu.psirvent.murmr.domain.usecase.UnblockContactUseCase
import uoc.edu.psirvent.murmr.presentation.components.MurmrAvatar
import uoc.edu.psirvent.murmr.presentation.components.MurmrIcons
import uoc.edu.psirvent.murmr.presentation.format.formatChatListTime
import uoc.edu.psirvent.murmr.presentation.navigation.InteractiveSwipeBack
import uoc.edu.psirvent.murmr.presentation.navigation.PreviousScreenBackground
import uoc.edu.psirvent.murmr.presentation.screen.avatar.AvatarFullScreen
import uoc.edu.psirvent.murmr.presentation.screen.chat.ChatScreen
import uoc.edu.psirvent.murmr.presentation.components.SnackbarController
import uoc.edu.psirvent.murmr.presentation.components.IosLikeAlert
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject
import uoc.edu.psirvent.murmr.presentation.viewmodel.ContactDetailUiState
import uoc.edu.psirvent.murmr.presentation.viewmodel.ContactDetailViewModel

class ContactDetailScreen(
    private val contactId: String,
    private val cameFromChat: Boolean = false,
) : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberKoinInject<ContactDetailViewModel> { parametersOf(contactId) }
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        var showDeleteConfirm by remember { mutableStateOf(false) }
        var showBlockConfirm by remember { mutableStateOf(false) }
        var showRekeyConfirm by remember { mutableStateOf(false) }

        InteractiveSwipeBack(
            onCompleted = { navigator.pop() },
            background = { PreviousScreenBackground() },
            foreground = {
                ContactDetailForeground(
                    state = state,
                    viewModel = viewModel,
                    contactId = contactId,
                    cameFromChat = cameFromChat,
                    navigator = navigator,
                    scope = scope,
                    showDeleteConfirm = showDeleteConfirm,
                    onShowDeleteConfirm = { showDeleteConfirm = it },
                    showBlockConfirm = showBlockConfirm,
                    onShowBlockConfirm = { showBlockConfirm = it },
                    showRekeyConfirm = showRekeyConfirm,
                    onShowRekeyConfirm = { showRekeyConfirm = it },
                )
            },
        )
    }
}

@Composable
private fun ContactDetailForeground(
    state: ContactDetailUiState,
    viewModel: ContactDetailViewModel,
    contactId: String,
    cameFromChat: Boolean,
    navigator: Navigator,
    scope: CoroutineScope,
    showDeleteConfirm: Boolean,
    onShowDeleteConfirm: (Boolean) -> Unit,
    showBlockConfirm: Boolean,
    onShowBlockConfirm: (Boolean) -> Unit,
    showRekeyConfirm: Boolean,
    onShowRekeyConfirm: (Boolean) -> Unit,
) {
    val deleteContact = rememberKoinInject<DeleteContactSecureWipeUseCase>()
    val unblockContact = rememberKoinInject<UnblockContactUseCase>()
    val blockContact = rememberKoinInject<BlockContactUseCase>()
    val snackbar = rememberKoinInject<SnackbarController>()
    Surface(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        color = MurmrColors.BackgroundDark,
    ) {
        when (val s = state) {
            ContactDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Cargando...", color = MurmrColors.OnSurfaceMuted)
            }

            ContactDetailUiState.NotFound -> {
                LaunchedEffect(Unit) { navigator.pop() }
                Box(Modifier.fillMaxSize()) {}
            }
            is ContactDetailUiState.Loaded -> Loaded(
                contact = s.contact,
                session = s.activeSession,
                history = s.sessionHistory,
                effectiveExpiry = s.effectiveExpiry,
                showChatButton = !cameFromChat,
                onBack = { navigator.pop() },
                onChat = { navigator.replace(ChatScreen(contactId)) },
                onVerify = { navigator.push(VerifyContactScreen(contactId, popOnSuccess = true)) },
                onRekey = { onShowRekeyConfirm(true) },
                onBlock = { onShowBlockConfirm(true) },
                onDelete = { onShowDeleteConfirm(true) },
                onOpenAvatar = { navigator.push(AvatarFullScreen(contactId)) },
            )
        }
    }

    if (showDeleteConfirm) {
        IosLikeAlert(
            title = "¿Estás seguro de que quieres eliminar este contacto y purgar el chat?",
            message = "Esto eliminará toda interacción con este contacto, tanto su usuario como su chat. Esto implica que deberás volver a emparejarte con él.",
            confirmLabel = "Borrar todo",
            onConfirm = {
                onShowDeleteConfirm(false)
                scope.launch {
                    deleteContact(contactId)
                    navigator.pop()
                }
            },
            onDismiss = { onShowDeleteConfirm(false) },
        )
    }
    if (showBlockConfirm) {
        val s = state as? ContactDetailUiState.Loaded
        val alreadyBlocked = s?.contact?.status == ContactStatus.BLOCKED
        if (alreadyBlocked) {
            IosLikeAlert(
                title = "¿Desbloquear contacto?",
                message = "Tendrás que volver a emparejarte para chatear (la sesión cripto " +
                    "anterior se cerró al bloquear).",
                confirmLabel = "Desbloquear",
                onConfirm = {
                    onShowBlockConfirm(false)
                    scope.launch { unblockContact(contactId) }
                },
                onDismiss = { onShowBlockConfirm(false) },
            )
        } else {
            IosLikeAlert(
                title = "¿Bloquear contacto?",
                message = "No recibirás más mensajes de este contacto. La sesión cripto se " +
                    "cerrará en ambos lados; si lo desbloqueas más tarde, deberás re-emparejar.",
                confirmLabel = "Bloquear",
                onConfirm = {
                    onShowBlockConfirm(false)
                    scope.launch { blockContact(contactId) }
                },
                onDismiss = { onShowBlockConfirm(false) },
            )
        }
    }
    if (showRekeyConfirm) {
        IosLikeAlert(
            title = "¿Renovar claves?",
            message = "Genera una nueva clave AES de sesion derivada de la actual (forward " +
                "secrecy). El peer se sincroniza automaticamente al recibir el siguiente " +
                "mensaje. Mantienes el chat y el contacto.",
            confirmLabel = "Renovar",
            onConfirm = {
                onShowRekeyConfirm(false)
                viewModel.rotateKeys { ok ->
                    snackbar.show(if (ok) "Claves renovadas" else "No se pudieron renovar (sin sesion activa)")
                }
            },
            onDismiss = { onShowRekeyConfirm(false) },
        )
    }
}

@Composable
private fun Loaded(
    contact: Contact,
    session: Session?,
    history: List<Session>,
    effectiveExpiry: EffectiveExpiry?,
    showChatButton: Boolean,
    onBack: () -> Unit,
    onChat: () -> Unit,
    onVerify: () -> Unit,
    onRekey: () -> Unit,
    onBlock: () -> Unit,
    onDelete: () -> Unit,
    onOpenAvatar: () -> Unit,
) {
    var showHistory by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(MurmrIcons.ArrowBack, contentDescription = "Atrás", tint = MurmrColors.OnSurface, modifier = Modifier.size(22.dp))
            }
            Text(
                "Información de usuario",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                color = MurmrColors.OnSurface,
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(MurmrColors.DividerDark))

        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.clickable(onClick = onOpenAvatar)) {
                MurmrAvatar(initial = contact.displayAlias, size = 90.dp, avatar = contact.avatar)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                contact.displayAlias,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
                color = MurmrColors.OnSurface,
            )
            Spacer(Modifier.height(8.dp))
            Column(modifier = Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                InfoLine("Última vez:", formatChatListTime(contact.lastMessageAt ?: contact.pairedAt))
                InfoLine("Último emparejamiento:", formatChatListTime(contact.pairedAt))
                val hasOverride = contact.localSessionExpiry != null
                val expiryText = expiryDescription(effectiveExpiry, contact.displayAlias, hasOverride)
                InfoLine("Expiración de la sesión:", expiryText)
                if (session != null && effectiveExpiry?.duration != null) {
                    val base = session.renewedAt ?: session.createdAt
                    val cutoff = base + effectiveExpiry.duration
                    InfoLine("Caduca el:", formatChatListTime(cutoff))
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BoxAction(icon = MurmrIcons.Lock, label = "Verificar sesión", onClick = onVerify, modifier = Modifier.weight(1f))
            BoxAction(icon = MurmrIcons.Refresh, label = "Renovar claves", onClick = onRekey, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(28.dp))
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            val isBlocked = contact.status == ContactStatus.BLOCKED
            DangerLink(
                icon = MurmrIcons.Block,
                label = if (isBlocked) "Desbloquear contacto" else "Bloquear contacto",
                onClick = onBlock,
            )
            Spacer(Modifier.height(14.dp))
            DangerLink(icon = MurmrIcons.Delete, label = "Borrar contacto y purgar chat", onClick = onDelete)
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showHistory = !showHistory }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Histórico de sesiones (${history.size})",
                        color = MurmrColors.OnSurface,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (showHistory) "Ocultar" else "Mostrar",
                        color = MurmrColors.AccentCyan,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    )
                }
                if (showHistory) {
                    Spacer(Modifier.height(8.dp))
                    history.forEach { s -> SessionHistoryRow(s) }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        if (showChatButton) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                BoxAction(
                    icon = MurmrIcons.ChatBubbleOutline,
                    label = "Chatear",
                    onClick = onChat,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun expiryDescription(
    eff: EffectiveExpiry?,
    contactAlias: String,
    hasOverride: Boolean,
): String {
    if (eff == null) return "-"
    val durLabel = when (eff.expiry) {
        SessionExpiry.NEVER -> "Nunca"
        SessionExpiry.ONE_DAY -> "1 día"
        SessionExpiry.SEVEN_DAYS -> "7 días"
        SessionExpiry.THIRTY_DAYS -> "30 días"
        SessionExpiry.CUSTOM -> formatCustomSeconds(eff.customSeconds)
    }
    if (eff.duration == null) return "Nunca (ambos)"
    val origin = when (eff.origin) {
        EffectiveExpiry.Origin.LOCAL -> if (hasOverride) "ti (este chat)" else "ti"
        EffectiveExpiry.Origin.PEER -> contactAlias
        EffectiveExpiry.Origin.BOTH -> "ambos"
    }
    return "$durLabel - fijado por $origin"
}

private fun formatCustomSeconds(seconds: Long): String {
    if (seconds <= 0L) return "0 s"
    return when {
        seconds % 86400L == 0L -> {
            val d = seconds / 86400L; if (d == 1L) "1 día" else "$d días"
        }
        seconds % 3600L == 0L -> {
            val h = seconds / 3600L; if (h == 1L) "1 hora" else "$h horas"
        }
        seconds % 60L == 0L -> {
            val m = seconds / 60L; if (m == 1L) "1 min" else "$m min"
        }
        else -> "$seconds s"
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MurmrColors.OnSurfaceMuted, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.width(4.dp))
        Text(value, color = MurmrColors.OnSurfaceMuted, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
    }
}

@Composable
private fun BoxAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MurmrColors.SurfaceDark)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = MurmrColors.OnSurface, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = MurmrColors.OnSurface, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
    }
}

@Composable
private fun SessionHistoryRow(s: Session) {
    val statusColor = when (s.status.name) {
        "ACTIVE" -> MurmrColors.AccentCyan
        "SUPERSEDED" -> MurmrColors.OnSurfaceMuted
        "EXPIRED" -> MurmrColors.Error
        "REVOKED" -> MurmrColors.Error
        "BROKEN" -> MurmrColors.Error
        else -> MurmrColors.OnSurfaceMuted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MurmrColors.SurfaceDark)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(statusColor))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${s.status.name} - rot ${s.rotationIndex}",
                color = MurmrColors.OnSurface,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            )
            Text(
                "${formatChatListTime(s.createdAt)} - fp ${s.fingerprint.take(12)}...",
                color = MurmrColors.OnSurfaceMuted,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun DangerLink(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MurmrColors.Error, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = MurmrColors.Error, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp))
    }
}
