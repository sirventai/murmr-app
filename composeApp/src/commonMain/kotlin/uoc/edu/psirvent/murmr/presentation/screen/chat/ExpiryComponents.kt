package uoc.edu.psirvent.murmr.presentation.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Instant
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.domain.model.EffectiveExpiry
import uoc.edu.psirvent.murmr.domain.model.SessionExpiry
import uoc.edu.psirvent.murmr.presentation.components.DurationPickerDialog
import uoc.edu.psirvent.murmr.presentation.components.MurmrIcons
import uoc.edu.psirvent.murmr.presentation.format.formatChatListTime
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors

@Composable
internal fun ExpiryBar(
    effective: EffectiveExpiry?,
    expiresAt: Instant?,
    hasOverride: Boolean,
    contactAlias: String,
    onClick: () -> Unit,
    onRenew: (() -> Unit)? = null,
) {
    val expired = effective?.duration != null
    val main = expiryMainText(effective)
    val sub = expirySubText(effective, expiresAt, hasOverride, contactAlias)
    val accent = if (expired) MurmrColors.AccentCyan else MurmrColors.OnSurfaceFaint

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MurmrColors.ElevatedDark)
                .border(
                    width = 0.5.dp,
                    color = MurmrColors.DividerDark,
                    shape = RoundedCornerShape(20.dp),
                )
                .clickable(onClick = onClick)
                .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MurmrIcons.Schedule,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(7.dp))
            Column(modifier = Modifier.padding(end = 2.dp)) {
                Text(
                    main,
                    color = MurmrColors.OnSurface,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (sub != null) {
                    Text(
                        sub,
                        color = MurmrColors.OnSurfaceFaint,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            lineHeight = 11.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (onRenew != null) {
                Spacer(Modifier.width(6.dp))
                PillIconButton(
                    icon = MurmrIcons.Refresh,
                    tint = MurmrColors.AccentCyan,
                    contentDesc = "Renovar plazo",
                    onClick = onRenew,
                )
            }
        }
    }
}

@Composable
private fun PillIconButton(
    icon: ImageVector,
    tint: Color,
    contentDesc: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = tint,
            modifier = Modifier.size(13.dp),
        )
    }
}

private fun expiryMainText(effective: EffectiveExpiry?): String {
    if (effective?.duration == null) return "Sin caducidad"
    return expiryEnumLabel(effective.expiry, effective.customSeconds)
}

private fun expirySubText(
    effective: EffectiveExpiry?,
    expiresAt: Instant?,
    hasOverride: Boolean,
    contactAlias: String,
): String? {
    if (effective?.duration == null) return null
    val origin = when (effective.origin) {
        EffectiveExpiry.Origin.LOCAL -> if (hasOverride) "tú - este chat" else "tú"
        EffectiveExpiry.Origin.PEER -> contactAlias
        EffectiveExpiry.Origin.BOTH -> null
    }
    val whenStr = expiresAt?.let { formatChatListTime(it) }
    return when {
        whenStr != null && origin != null -> "Hasta $whenStr - $origin"
        whenStr != null -> "Hasta $whenStr"
        origin != null -> origin
        else -> null
    }
}

private fun expiryEnumLabel(expiry: SessionExpiry, customSeconds: Long): String = when (expiry) {
    SessionExpiry.NEVER -> "Nunca"
    SessionExpiry.ONE_DAY -> "1 día"
    SessionExpiry.SEVEN_DAYS -> "7 días"
    SessionExpiry.THIRTY_DAYS -> "30 días"
    SessionExpiry.CUSTOM -> formatExpirySecondsLong(customSeconds)
}

private fun formatExpirySecondsLong(seconds: Long): String {
    if (seconds <= 0L) return "0s"
    return when {
        seconds % 86400L == 0L -> {
            val d = seconds / 86400L; if (d == 1L) "1 día" else "$d días"
        }
        seconds % 3600L == 0L -> {
            val h = seconds / 3600L; if (h == 1L) "1 hora" else "$h horas"
        }
        seconds % 60L == 0L -> {
            val m = seconds / 60L; if (m == 1L) "1 minuto" else "$m minutos"
        }
        else -> if (seconds == 1L) "1 segundo" else "$seconds segundos"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpiryChooserSheet(
    current: SessionExpiry?,
    currentCustomSeconds: Long,
    hasOverride: Boolean,
    onDismiss: () -> Unit,
    onChoose: (SessionExpiry, Long) -> Unit,
    onResetToDefault: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showCustomDialog by remember { mutableStateOf(false) }

    val opciones = listOf(
        SessionExpiry.NEVER to "Nunca",
        SessionExpiry.ONE_DAY to "1 día",
        SessionExpiry.SEVEN_DAYS to "7 días",
        SessionExpiry.THIRTY_DAYS to "30 días",
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MurmrColors.SurfaceDark,
        contentColor = MurmrColors.OnSurface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                "Expiración de esta sesión",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MurmrColors.OnSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Solo afecta a este chat. Tu configuración global de Ajustes no cambia. " +
                    "El lado más restrictivo manda: si tu contacto tiene una caducidad más " +
                    "corta, esa será la que aplique.",
                color = MurmrColors.OnSurfaceMuted,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                opciones.forEach { (value, label) ->
                    ExpiryOptionRow(
                        label = label,
                        selected = current == value && current != SessionExpiry.CUSTOM,
                        onClick = {
                            scope.launch { sheetState.hide() }
                            onChoose(value, 0L)
                        },
                    )
                }
                ExpiryOptionRow(
                    label = if (current == SessionExpiry.CUSTOM) {
                        "Personalizado - ${formatExpirySecondsLong(currentCustomSeconds)}"
                    } else {
                        "Personalizado-"
                    },
                    selected = current == SessionExpiry.CUSTOM,
                    onClick = { showCustomDialog = true },
                )
            }
            if (hasOverride) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch { sheetState.hide() }
                        onResetToDefault()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        "Restaurar default global",
                        color = MurmrColors.OnSurfaceMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showCustomDialog) {
        DurationPickerDialog(
            title = "Expiración personalizada",
            initialSeconds = if (currentCustomSeconds > 0L) currentCustomSeconds else 86400L,
            onDismiss = { showCustomDialog = false },
            onSave = { secs ->
                showCustomDialog = false
                scope.launch { sheetState.hide() }
                onChoose(SessionExpiry.CUSTOM, secs)
            },
        )
    }
}

@Composable
private fun ExpiryOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MurmrColors.PrimaryViolet.copy(alpha = 0.15f) else MurmrColors.IncomingBubble)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (selected) MurmrColors.PrimaryViolet else MurmrColors.OnSurface,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Text("✓", color = MurmrColors.PrimaryViolet, style = MaterialTheme.typography.titleMedium)
        }
    }
}
