package uoc.edu.psirvent.murmr.presentation.screen.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.SessionRepository
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.model.ContactStatus
import uoc.edu.psirvent.murmr.domain.model.Sas
import uoc.edu.psirvent.murmr.domain.usecase.DeleteContactSecureWipeUseCase
import uoc.edu.psirvent.murmr.presentation.components.MurmrIcons
import uoc.edu.psirvent.murmr.presentation.navigation.InteractiveSwipeBack
import uoc.edu.psirvent.murmr.presentation.navigation.PreviousScreenBackground
import uoc.edu.psirvent.murmr.presentation.screen.chat.ChatScreen
import uoc.edu.psirvent.murmr.presentation.screen.home.HomeScreen
import uoc.edu.psirvent.murmr.presentation.components.IosLikeAlert
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.util.UiTextos
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject
import uoc.edu.psirvent.murmr.util.Logger

class VerifyContactScreen(
    private val contactId: String,
    private val popOnSuccess: Boolean = false,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val finishSuccess: () -> Unit = {
            if (popOnSuccess) navigator.pop() else navigator.replace(ChatScreen(contactId))
        }
        val sessionRepo = rememberKoinInject<SessionRepository>()
        val contactRepo = rememberKoinInject<ContactRepository>()
        val userRepo = rememberKoinInject<UserRepository>()
        val deleteContact = rememberKoinInject<DeleteContactSecureWipeUseCase>()
        val scope = rememberCoroutineScope()

        var emojis by remember { mutableStateOf<List<String>?>(null) }
        var alias by remember { mutableStateOf<String?>(null) }
        var isVerifier by remember { mutableStateOf<Boolean?>(null) }

        var error by remember { mutableStateOf<String?>(null) }
        var showSkipConfirm by remember { mutableStateOf(false) }
        var showRejectConfirm by remember { mutableStateOf(false) }

        LaunchedEffect(contactId) {
            val contact = contactRepo.getById(contactId)
            val me = userRepo.getLocalUser()
            alias = contact?.displayAlias
            isVerifier = if (contact != null && me != null) me.deviceId < contact.remoteDeviceId else null
            Logger.i(
                "VerifySAS",
                "me=${me?.deviceId} peer=${contact?.remoteDeviceId} alias=${contact?.displayAlias} isVerifier=$isVerifier",
            )
            val active = sessionRepo.getActive(contactId)
            if (active == null) {
                error = "No hay sesión activa con este contacto"
                return@LaunchedEffect
            }
            val sas = Sas.fromEncoded(active.sasCode)
            if (sas == null) {
                error = "No se pudo recuperar el código SAS"
                return@LaunchedEffect
            }
            emojis = sas.emojis
        }

        InteractiveSwipeBack(
            onCompleted = { navigator.pop() },
            background = { PreviousScreenBackground() },
            foreground = {
                Surface(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                    color = MurmrColors.BackgroundDark,
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "Verificar identidad",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                            color = MurmrColors.OnSurface,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        when {
                            error != null -> Text(error!!, color = MurmrColors.Error)
                            emojis == null || isVerifier == null -> Text("Cargando...", color = MurmrColors.OnSurfaceMuted)
                            isVerifier == true -> VerifierBody(
                                peerAlias = alias ?: "el contacto",
                                expectedEmojis = emojis!!,
                                onMatch = {
                                    scope.launch {
                                        contactRepo.updateStatus(contactId, ContactStatus.VERIFIED)
                                        finishSuccess()
                                    }
                                },
                                onReject = { showRejectConfirm = true },
                                onSkip = { showSkipConfirm = true },
                            )
                            else -> ReaderBody(
                                peerAlias = alias ?: "el contacto",
                                emojis = emojis!!,
                                onPeerVerified = {
                                    scope.launch {
                                        contactRepo.updateStatus(contactId, ContactStatus.VERIFIED)
                                        finishSuccess()
                                    }
                                },
                                onSkip = { showSkipConfirm = true },
                            )
                        }
                    }
                }
            },
        )

        if (showSkipConfirm) {
            IosLikeAlert(
                title = "¿Saltar verificación?",
                message = "La sesión NO quedará verificada. El chat funciona pero no has " +
                    "confirmado en persona que nadie ha interceptado la conexión. " +
                    "Puedes verificar más tarde desde el detalle del contacto.",
                confirmLabel = "Saltar",
                cancelLabel = UiTextos.Volver,
                onConfirm = {
                    showSkipConfirm = false
                    finishSuccess()
                },
                onDismiss = { showSkipConfirm = false },
            )
        }
        if (showRejectConfirm) {
            IosLikeAlert(
                title = "¿Los emojis no coinciden?",
                message = "Si confirmas, este contacto y toda la sesión se borrarán de TU " +
                    "dispositivo. La otra persona no se enterará automáticamente - avísale " +
                    "por otro canal de que ha habido un fallo de verificación.",
                confirmLabel = "Borrar contacto",
                cancelLabel = UiTextos.Volver,
                onConfirm = {
                    showRejectConfirm = false
                    scope.launch {
                        deleteContact(contactId)
                        navigator.popUntil { it === HomeScreen }
                    }
                },
                onDismiss = { showRejectConfirm = false },
            )
        }
    }
}

@Composable
private fun VerifierBody(
    peerAlias: String,
    expectedEmojis: List<String>,
    onMatch: () -> Unit,
    onReject: () -> Unit,
    onSkip: () -> Unit,
) {
    var entered by remember { mutableStateOf<List<String>>(emptyList()) }
    var showConfirmMatch by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val complete = entered.size == Sas.EMOJI_COUNT
    val matches = complete && entered == expectedEmojis
    val mismatches = complete && !matches
    LaunchedEffect(matches, mismatches) {
        when {
            matches -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            mismatches -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Text(
        "Pídele a $peerAlias que te lea sus 5 emojis y escríbelos aquí en el mismo orden.",
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
        color = MurmrColors.OnSurfaceMuted,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(Sas.EMOJI_COUNT) { i ->
            val filled = i < entered.size
            val borderColor = when {
                !complete -> MurmrColors.DividerDark
                matches -> MurmrColors.AccentCyan
                else -> MurmrColors.Error
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MurmrColors.SurfaceDark)
                    .border(1.5.dp, borderColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (filled) {
                    Text(entered[i], style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp))
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    when {
        mismatches -> Text(
            "Los emojis no coinciden con los de mi pantalla. Revisa la lectura o cancela.",
            color = MurmrColors.Error,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            textAlign = TextAlign.Center,
        )
        matches -> Text(
            "Los 5 emojis coinciden - confirma para verificar.",
            color = MurmrColors.AccentCyan,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            textAlign = TextAlign.Center,
        )
        else -> Text(
            "${entered.size} / ${Sas.EMOJI_COUNT}",
            color = MurmrColors.OnSurfaceFaint,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        )
    }
    Spacer(Modifier.height(12.dp))

    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = Modifier.fillMaxWidth().height(220.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(Sas.EMOJI_ALPHABET) { e ->
            EmojiKey(emoji = e, enabled = entered.size < Sas.EMOJI_COUNT) {
                if (entered.size < Sas.EMOJI_COUNT) entered = entered + e
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        IconButton(
            onClick = { if (entered.isNotEmpty()) entered = entered.dropLast(1) },
            enabled = entered.isNotEmpty(),
        ) {
            Icon(MurmrIcons.Backspace, contentDescription = "Borrar", tint = MurmrColors.OnSurface, modifier = Modifier.size(22.dp))
        }
        TextButton(onClick = { entered = emptyList() }, enabled = entered.isNotEmpty()) {
            Text("Limpiar", color = MurmrColors.OnSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
    Spacer(Modifier.height(12.dp))

    Button(
        onClick = { showConfirmMatch = true },
        enabled = matches,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MurmrColors.PrimaryViolet,
            contentColor = Color.White,
        ),
    ) {
        Text(
            "Verificar",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
    Spacer(Modifier.height(6.dp))
    OutlinedButton(
        onClick = onReject,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text("No coinciden - borrar contacto", color = MurmrColors.Error, style = MaterialTheme.typography.bodySmall)
    }
    TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
        Text("Saltar verificación", color = MurmrColors.OnSurfaceMuted, style = MaterialTheme.typography.bodySmall)
    }

    if (showConfirmMatch) {
        IosLikeAlert(
            title = "¿Confirmas la verificación?",
            message = "¿Estás seguro de que estos son los 5 emojis que te ha leído $peerAlias? " +
                "Una vez verificada, esta sesión queda marcada como segura.",
            confirmLabel = "Sí, verificar",
            cancelLabel = UiTextos.Cancelar,
            onConfirm = {
                showConfirmMatch = false
                onMatch()
            },
            onDismiss = { showConfirmMatch = false },
        )
    }
}

@Composable
private fun EmojiKey(emoji: String, enabled: Boolean, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) MurmrColors.SurfaceDark else MurmrColors.SurfaceDark.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp))
    }
}

@Composable
private fun ReaderBody(
    peerAlias: String,
    emojis: List<String>,
    onPeerVerified: () -> Unit,
    onSkip: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    Text(
        "Léele estos 5 emojis a $peerAlias en este mismo orden. $peerAlias los introducirá en su dispositivo para verificar.",
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
        color = MurmrColors.OnSurfaceMuted,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(28.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MurmrColors.SurfaceDark)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            emojis.forEach { e ->
                Text(e, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp))
            }
        }
    }
    Spacer(Modifier.height(28.dp))
    Text(
        "Cuando $peerAlias te confirme verbalmente que ha verificado en su lado, " +
            "puedes confirmarlo también aquí.",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
        color = MurmrColors.OnSurfaceFaint,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(40.dp))

    Button(
        onClick = { showConfirm = true },
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MurmrColors.PrimaryViolet,
            contentColor = Color.White,
        ),
    ) {
        Text(
            "$peerAlias me ha confirmado que coinciden",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onSkip,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text("Cerrar", color = MurmrColors.OnSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
    }

    if (showConfirm) {
        IosLikeAlert(
            title = "¿Confirmas?",
            message = "¿Estás seguro de que $peerAlias te ha dicho en persona que los 5 emojis " +
                "han coincidido con los suyos? Esta sesión quedará marcada como verificada en tu lado.",
            confirmLabel = "Sí, confirmar",
            cancelLabel = UiTextos.Cancelar,
            onConfirm = {
                showConfirm = false
                onPeerVerified()
            },
            onDismiss = { showConfirm = false },
        )
    }
}
