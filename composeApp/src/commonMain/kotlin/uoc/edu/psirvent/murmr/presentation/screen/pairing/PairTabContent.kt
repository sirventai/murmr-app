package uoc.edu.psirvent.murmr.presentation.screen.pairing

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uoc.edu.psirvent.murmr.domain.model.KeyExchangePhase
import uoc.edu.psirvent.murmr.domain.model.PairingFailure
import uoc.edu.psirvent.murmr.domain.model.PairingState
import uoc.edu.psirvent.murmr.presentation.components.MurmrIcons
import uoc.edu.psirvent.murmr.presentation.screen.contacts.VerifyContactScreen
import uoc.edu.psirvent.murmr.presentation.screen.home.LocalBottomBarHeight
import uoc.edu.psirvent.murmr.presentation.screen.home.LocalRootNavigator
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.util.UiTextos
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject
import uoc.edu.psirvent.murmr.presentation.viewmodel.PairingViewModel

@Composable
fun PairTabContent() {
    val viewModel = rememberKoinInject<PairingViewModel>()
    val state by viewModel.state.collectAsState()
    val volumeWarning by viewModel.volumeWarning.collectAsState()
    val rootNavigator = LocalRootNavigator.current

    val verifiedContactId = (state as? PairingState.Verified)?.contact?.contactId
    LaunchedEffect(verifiedContactId) {
        if (verifiedContactId != null && rootNavigator != null) {
            rootNavigator.push(VerifyContactScreen(verifiedContactId))
            viewModel.reset()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MurmrColors.BackgroundDark) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 24.dp)) {
            Text(
                "Emparejar",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    letterSpacing = (-0.5).sp,
                ),
                color = MurmrColors.OnSurface,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))
            volumeWarning?.let { msg ->
                VolumeWarningBanner(
                    message = msg,
                    onDismiss = viewModel::dismissVolumeWarning,
                    modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            val bottomBarHeight = LocalBottomBarHeight.current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = bottomBarHeight),
            ) {
                when (val s = state) {
                    PairingState.Idle -> {
                        IdleContent(onStart = viewModel::start)
                    }
                    is PairingState.Discovering,
                    is PairingState.PeerFound,
                    is PairingState.ExchangingKeys,
                    is PairingState.Verified -> PairingFlowContent(
                        state = s,
                        onCancel = viewModel::reset,
                    )
                    is PairingState.Failed -> FailureContent(
                        reason = s.reason,
                        onRetry = viewModel::retryFromFailure,
                        onBack = viewModel::cancel,
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleContent(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val illustrationSize = (maxWidth * 0.7f).coerceIn(180.dp, 320.dp)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                IdleIllustration(size = illustrationSize.value.toInt())
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Acerca los teléfonos",
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
            ),
            color = MurmrColors.OnSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Pulsa Emparejar en ambos al mismo tiempo",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
            color = MurmrColors.OnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        PairButton(onClick = onStart)
    }
}

@Composable
private fun FailureContent(reason: PairingFailure, onRetry: () -> Unit, onBack: () -> Unit) {
    val message = when (reason) {
        PairingFailure.AUDIO_TIMEOUT -> "No se ha detectado al otro dispositivo. Acércalos más e inténtalo de nuevo."
        PairingFailure.DECODE_ERROR -> "Error al decodificar el mensaje acústico. Reintenta en un entorno menos ruidoso."
        PairingFailure.SAS_MISMATCH -> "Los códigos no coinciden. Por seguridad, descartamos esta sesión."
        PairingFailure.PERMISSION_DENIED -> "Necesitas permitir el acceso al micrófono."
        PairingFailure.ALREADY_PAIRED -> "Este contacto ya está emparejado."
        PairingFailure.NO_PEER_FOUND -> "No se ha encontrado ningún dispositivo. Asegúrate de que el otro está en modo emparejar."
        PairingFailure.CHANNEL_TOO_NOISY -> "El canal acústico es demasiado ruidoso. Reduce el ruido ambiente."
        PairingFailure.UNKNOWN -> "Algo salió mal. Inténtalo de nuevo."
    }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.4f))
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(MurmrColors.Error.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(MurmrIcons.Close, contentDescription = null, tint = MurmrColors.Error, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Emparejamiento fallido", color = MurmrColors.OnSurface, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            color = MurmrColors.OnSurfaceMuted,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MurmrColors.PrimaryViolet, contentColor = Color.White),
        ) { Text(UiTextos.Reintentar) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(10.dp),
        ) { Text(UiTextos.Volver, color = MurmrColors.OnSurfaceMuted) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun VolumeWarningBanner(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF3A2A10),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "🔊",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                modifier = Modifier.padding(end = 10.dp),
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = Color(0xFFFFB870),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text("OK", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = Color(0xFFFFB870))
            }
        }
    }
}

private data class PhaseInfo(val index: Int, val title: String, val subtitle: String)

private fun phaseInfoFor(state: PairingState): PhaseInfo = when (state) {
    is PairingState.Discovering -> PhaseInfo(
        index = 0,
        title = "Buscando dispositivo",
        subtitle = "Acerca los dos teléfonos.",
    )
    is PairingState.PeerFound -> PhaseInfo(
        index = 1,
        title = "Conectado con ${state.peerAlias}",
        subtitle = "Preparando el intercambio seguro de claves...",
    )
    is PairingState.ExchangingKeys -> PhaseInfo(
        index = 2,
        title = "Estableciendo con ${state.peerAlias}",
        subtitle = when (state.phase) {
            KeyExchangePhase.WAITING_HELLO_ACK -> "Esperando confirmación del peer..."
            KeyExchangePhase.SENDING_HELLO_ACK -> "Confirmación enviada, esperando claves..."
            KeyExchangePhase.WAITING_KEY_A -> "Esperando clave del peer..."
            KeyExchangePhase.WAITING_KEY_B -> "Clave enviada, esperando respuesta..."
            KeyExchangePhase.SENDING_KEY_B ->
                "Enviando clave${state.phaseDetail?.let { " ($it)" } ?: ""}..."
            KeyExchangePhase.DERIVING -> "Generando sesión segura..."
            null -> "Cifrado X25519 + AES-GCM en curso..."
        },
    )
    is PairingState.Verified -> PhaseInfo(
        index = 3,
        title = "¡Listo!",
        subtitle = "Abriendo verificación visual...",
    )
    else -> PhaseInfo(0, "", "")
}

@Composable
private fun PairingFlowContent(state: PairingState, onCancel: () -> Unit) {
    val phase = phaseInfoFor(state)
    val targetProgress = (phase.index + 1) / 4f
    val animProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "phase-progress",
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        PhaseStepper(currentPhase = phase.index, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            PhaseVisual(phase.index)
        }
        Spacer(Modifier.height(28.dp))
        Text(
            phase.title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            ),
            color = MurmrColors.OnSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            phase.subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
            color = MurmrColors.OnSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MurmrColors.SurfaceDark),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animProgress)
                    .background(MurmrColors.PrimaryViolet),
            )
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(10.dp),
        ) { Text(UiTextos.Cancelar, color = MurmrColors.OnSurfaceMuted) }
        Spacer(Modifier.height(24.dp))
    }
}
