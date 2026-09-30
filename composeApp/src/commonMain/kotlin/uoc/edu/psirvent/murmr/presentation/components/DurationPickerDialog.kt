package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.util.UiTextos

enum class DurationUnit(val label: String, val seconds: Long, val maxValue: Int) {
    SECONDS("Segundos", 1L, 86400),
    MINUTES("Minutos", 60L, 10080),
    HOURS("Horas", 3600L, 8760),
    DAYS("Días", 86400L, 365),
}

fun unitForSeconds(totalSeconds: Long): Pair<DurationUnit, Long> = when {
    totalSeconds <= 0L -> DurationUnit.DAYS to 1L
    totalSeconds % 86400L == 0L -> DurationUnit.DAYS to (totalSeconds / 86400L)
    totalSeconds % 3600L == 0L -> DurationUnit.HOURS to (totalSeconds / 3600L)
    totalSeconds % 60L == 0L -> DurationUnit.MINUTES to (totalSeconds / 60L)
    else -> DurationUnit.SECONDS to totalSeconds
}

@Composable
fun DurationPickerDialog(
    title: String,
    initialSeconds: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    val (initialUnit, initialValue) = remember(initialSeconds) { unitForSeconds(initialSeconds) }
    var unit by remember { mutableStateOf(initialUnit) }
    var draft by remember { mutableStateOf(initialValue.toString()) }
    val numeric = draft.toLongOrNull()
    val isValid = numeric != null && numeric in 1..unit.maxValue.toLong()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { txt ->
                        if (txt.length <= 6) draft = txt.filter { it.isDigit() }
                    },
                    singleLine = true,
                    label = { Text("Cantidad") },
                    isError = !isValid && draft.isNotEmpty(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = durationTextFieldColors(),
                )
                Spacer(Modifier.height(12.dp))
                Text("Unidad", color = MurmrColors.OnSurfaceMuted, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DurationUnit.entries.forEach { u ->
                        val selected = u == unit
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) MurmrColors.PrimaryViolet
                                    else MurmrColors.ElevatedDark,
                                )
                                .clickable { unit = u }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                u.label,
                                color = if (selected) MurmrColors.OnSurface else MurmrColors.OnSurfaceMuted,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
                if (numeric != null && numeric > unit.maxValue) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Máximo ${unit.maxValue} ${unit.label.lowercase()}",
                        color = MurmrColors.Error,
                        fontSize = 11.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid && numeric != null) onSave(numeric * unit.seconds) },
                enabled = isValid,
            ) { Text(UiTextos.Guardar, color = MurmrColors.PrimaryViolet) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(UiTextos.Cancelar, color = MurmrColors.OnSurfaceMuted) }
        },
        containerColor = MurmrColors.SurfaceDark,
        titleContentColor = MurmrColors.OnSurface,
    )
}

@Composable
private fun durationTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MurmrColors.ElevatedDark,
    unfocusedContainerColor = MurmrColors.ElevatedDark,
    focusedTextColor = MurmrColors.OnSurface,
    unfocusedTextColor = MurmrColors.OnSurface,
    focusedIndicatorColor = MurmrColors.PrimaryViolet,
    unfocusedIndicatorColor = MurmrColors.DividerDark,
    cursorColor = MurmrColors.PrimaryViolet,
)
