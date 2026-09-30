package uoc.edu.psirvent.murmr.presentation.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import uoc.edu.psirvent.murmr.data.network.RelayConfigStore
import uoc.edu.psirvent.murmr.presentation.util.UiTextos
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject
import uoc.edu.psirvent.murmr.domain.model.AppSettings
import uoc.edu.psirvent.murmr.domain.model.AutoLockTimeout
import uoc.edu.psirvent.murmr.domain.model.SessionExpiry
import uoc.edu.psirvent.murmr.domain.model.User
import uoc.edu.psirvent.murmr.presentation.components.MurmrAvatar
import uoc.edu.psirvent.murmr.presentation.components.MurmrIcons
import uoc.edu.psirvent.murmr.presentation.components.PlatformAudioDeviceSettings
import uoc.edu.psirvent.murmr.presentation.components.SettingsDivider
import uoc.edu.psirvent.murmr.presentation.components.SettingsGroup
import uoc.edu.psirvent.murmr.presentation.components.SettingsRow
import uoc.edu.psirvent.murmr.presentation.components.SettingsToggleRow
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.viewmodel.SettingsViewModel

@Composable
internal fun Loaded(
    user: User,
    settings: AppSettings,
    vm: SettingsViewModel,
    avatarFeedback: String?,
    onPickAvatar: () -> Unit,
    onClearAvatar: () -> Unit,
    onAlias: () -> Unit,
    onExpiry: () -> Unit,
    onAutoLock: () -> Unit,
    onRelayUrl: () -> Unit,
    onDelete: () -> Unit,
    onVersion: () -> Unit,
) {
    var showAvatarMenu by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.clickable {
                if (user.avatar != null) showAvatarMenu = true else onPickAvatar()
            }) {
                MurmrAvatar(initial = user.alias, size = 96.dp, avatar = user.avatar)
            }
            if (avatarFeedback != null) {
                Text(
                    avatarFeedback,
                    color = MurmrColors.OnSurfaceMuted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                user.alias,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                color = MurmrColors.OnSurface,
            )
        }
        Spacer(Modifier.height(28.dp))
        SectionLabel("Ajustes generales")
        Spacer(Modifier.height(8.dp))

        SettingsGroup {
            SettingsRow(icon = MurmrIcons.Edit, title = "Alias", value = user.alias, onClick = onAlias)
            SettingsDivider()
            SettingsRow(icon = MurmrIcons.Delete, title = "Eliminar cuenta", danger = true, onClick = onDelete)
        }
        Spacer(Modifier.height(20.dp))

        SettingsGroup {
            SettingsRow(
                icon = MurmrIcons.Schedule,
                title = "Expiración de sesiones",
                value = expiryLabel(settings.sessionExpiry, settings.customExpirySeconds),
                onClick = onExpiry,
            )
            SettingsDivider()
            SettingsToggleRow(
                icon = MurmrIcons.Fingerprint,
                title = "Acceso biométrico",
                checked = settings.biometricLock,
                onChange = vm::updateBiometric,
            )
            SettingsDivider()
            SettingsRow(
                icon = MurmrIcons.Lock,
                title = "Bloqueo automático",
                value = if (settings.biometricLock) {
                    autoLockLabel(settings.autoLockTimeout, settings.customAutoLockSeconds)
                } else {
                    "Activa biométrico"
                },
                enabled = settings.biometricLock,
                onClick = onAutoLock,
            )
            SettingsDivider()
            SettingsToggleRow(
                icon = MurmrIcons.Notifications,
                title = "Notificaciones",
                checked = settings.notificationsEnabled,
                onChange = vm::updateNotifications,
            )
            SettingsDivider()
            SettingsRow(
                icon = MurmrIcons.InfoOutline,
                title = "Versión",
                value = AppSettings.APP_VERSION,
                onClick = onVersion,
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Servidor relay")
        Spacer(Modifier.height(8.dp))
        SettingsGroup {
            val store = rememberKoinInject<RelayConfigStore>()
            val cfg by store.flow.collectAsState()
            SettingsRow(
                icon = MurmrIcons.InfoOutline,
                title = "URL del relay",
                value = cfg.baseUrl,
                onClick = onRelayUrl,
            )
        }

        PlatformAudioDeviceSettings()
    }

    if (showAvatarMenu) {
        AlertDialog(
            onDismissRequest = { showAvatarMenu = false },
            title = { Text("Foto de perfil", color = MurmrColors.OnSurface) },
            text = {
                Text(
                    "¿Qué quieres hacer con tu avatar?",
                    color = MurmrColors.OnSurfaceMuted,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAvatarMenu = false
                    onPickAvatar()
                }) {
                    Text("Cambiar", color = MurmrColors.PrimaryViolet)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showAvatarMenu = false
                        onClearAvatar()
                    }) {
                        Text("Eliminar", color = MurmrColors.Error)
                    }
                    TextButton(onClick = { showAvatarMenu = false }) {
                        Text(UiTextos.Cancelar, color = MurmrColors.OnSurfaceMuted)
                    }
                }
            },
            containerColor = MurmrColors.SurfaceDark,
            titleContentColor = MurmrColors.OnSurface,
        )
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        color = MurmrColors.OnSurfaceFaint,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.6.sp),
    )
}

internal fun expiryLabel(value: SessionExpiry, customSeconds: Long): String = when (value) {
    SessionExpiry.NEVER -> "Nunca"
    SessionExpiry.ONE_DAY -> "1 día"
    SessionExpiry.SEVEN_DAYS -> "7 días"
    SessionExpiry.THIRTY_DAYS -> "30 días"
    SessionExpiry.CUSTOM -> "Custom - ${formatDuration(customSeconds)}"
}

internal fun formatDuration(seconds: Long): String {
    if (seconds <= 0L) return "0s"
    return when {
        seconds % 86400L == 0L -> "${seconds / 86400L}d"
        seconds % 3600L == 0L -> "${seconds / 3600L}h"
        seconds % 60L == 0L -> "${seconds / 60L}m"
        else -> "${seconds}s"
    }
}

internal fun autoLockLabel(value: AutoLockTimeout, customSecs: Int): String = when (value) {
    AutoLockTimeout.NEVER -> "Nunca"
    AutoLockTimeout.FIVE_SECONDS -> "5 seg"
    AutoLockTimeout.SIXTY_SECONDS -> "60 seg"
    AutoLockTimeout.FIVE_MINUTES -> "5 min"
    AutoLockTimeout.CUSTOM -> "Custom - ${customSecs}s"
}
