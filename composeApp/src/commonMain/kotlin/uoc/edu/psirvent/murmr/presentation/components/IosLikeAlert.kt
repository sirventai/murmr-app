package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.util.UiTextos

@Composable
fun IosLikeAlert(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String = UiTextos.Cancelar,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = MurmrColors.OnSurface,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                message,
                color = MurmrColors.OnSurfaceMuted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MurmrColors.Error, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel, color = MurmrColors.AccentCyan, style = MaterialTheme.typography.bodyMedium)
            }
        },
        shape = RoundedCornerShape(14.dp),
        containerColor = MurmrColors.SurfaceDark,
        titleContentColor = MurmrColors.OnSurface,
    )
}
