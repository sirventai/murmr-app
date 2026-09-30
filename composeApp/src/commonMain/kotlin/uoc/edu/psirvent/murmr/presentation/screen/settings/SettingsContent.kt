package uoc.edu.psirvent.murmr.presentation.screen.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import uoc.edu.psirvent.murmr.domain.model.AppSettings
import uoc.edu.psirvent.murmr.domain.model.AutoLockTimeout
import uoc.edu.psirvent.murmr.domain.model.SessionExpiry
import uoc.edu.psirvent.murmr.presentation.components.CollapsingHeaderOverlay
import uoc.edu.psirvent.murmr.presentation.components.DurationPickerDialog
import uoc.edu.psirvent.murmr.presentation.components.IosLikeAlert
import uoc.edu.psirvent.murmr.presentation.components.RelayDialogBus
import uoc.edu.psirvent.murmr.presentation.components.SnackbarController
import uoc.edu.psirvent.murmr.presentation.components.rememberAvatarPicker
import uoc.edu.psirvent.murmr.presentation.screen.home.LocalBottomBarHeight
import uoc.edu.psirvent.murmr.presentation.screen.splash.SplashScreen
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject
import uoc.edu.psirvent.murmr.presentation.viewmodel.SettingsUiState
import uoc.edu.psirvent.murmr.presentation.viewmodel.SettingsViewModel
import uoc.edu.psirvent.murmr.util.ValidationResult
import uoc.edu.psirvent.murmr.util.Validators
import uoc.edu.psirvent.murmr.util.compressAvatar

@Composable
fun SettingsContent() {
    val viewModel = rememberKoinInject<SettingsViewModel>()
    val state by viewModel.state.collectAsState()
    val rootNavigator = LocalNavigator.currentOrThrow

    var showAliasDialog by remember { mutableStateOf(false) }
    var showExpirySheet by remember { mutableStateOf(false) }
    var showAutoLockSheet by remember { mutableStateOf(false) }
    var showCustomExpiry by remember { mutableStateOf(false) }
    var showCustomAutoLock by remember { mutableStateOf(false) }
    var showRelayUrl by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var avatarFeedback by remember { mutableStateOf<String?>(null) }

    val relayBus = rememberKoinInject<RelayDialogBus>()
    val initialRelaySignal = remember { relayBus.open.value }
    val relaySignal by relayBus.open.collectAsState()
    LaunchedEffect(relaySignal) {
        if (relaySignal > initialRelaySignal) showRelayUrl = true
    }

    val avatarPicker = rememberAvatarPicker { bytes ->
        if (bytes == null) return@rememberAvatarPicker
        when (val v = Validators.avatarBytes(bytes)) {
            ValidationResult.Ok -> {
                val thumbnail = compressAvatar(bytes, maxSidePx = 256, quality = 85)
                val big = compressAvatar(bytes, maxSidePx = 1024, quality = 88)
                viewModel.updateAvatar(thumbnail, big)
                avatarFeedback = null
            }
            is ValidationResult.Invalid -> {
                avatarFeedback = v.message
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MurmrColors.BackgroundDark) {
        Box(modifier = Modifier.fillMaxSize()) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp)
                    .verticalScroll(scrollState),
            ) {
                Text(
                    "Ajustes",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = MurmrColors.OnSurface,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(20.dp))
                when (val s = state) {
                    SettingsUiState.Loading,
                    SettingsUiState.Empty -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Cargando...", color = MurmrColors.OnSurfaceMuted)
                    }
                    is SettingsUiState.Loaded -> Loaded(
                        user = s.user,
                        settings = s.settings,
                        vm = viewModel,
                        avatarFeedback = avatarFeedback,
                        onPickAvatar = { avatarFeedback = null; avatarPicker() },
                        onClearAvatar = { viewModel.updateAvatar(null, null); avatarFeedback = null },
                        onAlias = { showAliasDialog = true },
                        onExpiry = { showExpirySheet = true },
                        onAutoLock = { showAutoLockSheet = true },
                        onRelayUrl = { showRelayUrl = true },
                        onDelete = { showDeleteAccount = true },
                        onVersion = { showVersionDialog = true },
                    )
                }
                val bottomBarHeight = LocalBottomBarHeight.current
                Spacer(Modifier.height(40.dp + bottomBarHeight))
            }
            CollapsingHeaderOverlay(title = "Ajustes", scrollState = scrollState)
        }
    }

    val current = state as? SettingsUiState.Loaded
    if (showAliasDialog && current != null) {
        AliasDialog(
            current = current.user.alias,
            onDismiss = { showAliasDialog = false },
            onSave = { viewModel.updateAlias(it); showAliasDialog = false },
        )
    }
    if (showExpirySheet && current != null) {
        ChoiceBottomSheet(
            title = "Expiración de sesiones",
            label = "Chats",
            options = SessionExpiry.entries.map { it to expiryLabel(it, current.settings.customExpirySeconds) },
            selected = current.settings.sessionExpiry,
            onSave = {
                if (it == SessionExpiry.CUSTOM) {
                    showExpirySheet = false
                    showCustomExpiry = true
                } else {
                    viewModel.updateSessionExpiry(it)
                    showExpirySheet = false
                }
            },
            onDismiss = { showExpirySheet = false },
        )
    }
    if (showCustomExpiry && current != null) {
        DurationPickerDialog(
            title = "Expiración personalizada",
            initialSeconds = current.settings.customExpirySeconds,
            onDismiss = { showCustomExpiry = false },
            onSave = { secs ->
                viewModel.updateCustomExpirySeconds(secs)
                showCustomExpiry = false
            },
        )
    }
    if (showAutoLockSheet && current != null) {
        ChoiceBottomSheet(
            title = "Bloqueo automático",
            label = "¿Cuándo debería bloquearse el teléfono?",
            options = AutoLockTimeout.entries.map { it to autoLockLabel(it, current.settings.customAutoLockSeconds) },
            selected = current.settings.autoLockTimeout,
            onSave = {
                if (it == AutoLockTimeout.CUSTOM) {
                    showAutoLockSheet = false
                    showCustomAutoLock = true
                } else {
                    viewModel.updateAutoLock(it)
                    showAutoLockSheet = false
                }
            },
            onDismiss = { showAutoLockSheet = false },
        )
    }
    if (showCustomAutoLock && current != null) {
        ValidatedNumberDialog(
            title = "Bloqueo personalizado",
            label = "Segundos",
            initial = current.settings.customAutoLockSeconds.toString(),
            validator = Validators::autoLockSeconds,
            onDismiss = { showCustomAutoLock = false },
            onSave = { txt ->
                viewModel.updateCustomAutoLockSeconds(txt.trim().toInt())
                showCustomAutoLock = false
            },
        )
    }
    if (showRelayUrl) {
        RelayUrlDialog(onDismiss = { showRelayUrl = false })
    }
    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text("Versión") },
            text = { Text("Murmr v${AppSettings.APP_VERSION}", color = MurmrColors.OnSurfaceMuted) },
            confirmButton = {
                TextButton(onClick = { showVersionDialog = false }) {
                    Text("Descartar", color = MurmrColors.PrimaryViolet)
                }
            },
            containerColor = MurmrColors.SurfaceDark,
            titleContentColor = MurmrColors.OnSurface,
        )
    }
    if (showDeleteAccount) {
        val snackbar = rememberKoinInject<SnackbarController>()
        IosLikeAlert(
            title = "¿Estás seguro de que quieres eliminar tu cuenta?",
            message = "Esta acción es irreversible. Borra todas las claves, mensajes, contactos y perfil del dispositivo.",
            confirmLabel = "Borrar todo",
            onConfirm = {
                showDeleteAccount = false
                viewModel.deleteAccount(
                    onComplete = { rootNavigator.replaceAll(SplashScreen) },
                    onError = { msg -> snackbar.show("No se pudo eliminar la cuenta: $msg") },
                )
            },
            onDismiss = { showDeleteAccount = false },
        )
    }
}
