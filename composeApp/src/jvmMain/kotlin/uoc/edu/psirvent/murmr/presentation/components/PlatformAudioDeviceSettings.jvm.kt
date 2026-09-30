package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import uoc.edu.psirvent.murmr.audio.AudioDevice
import uoc.edu.psirvent.murmr.audio.AudioDeviceRegistry
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors

@Composable
actual fun PlatformAudioDeviceSettings() {
    val inputName by AudioDeviceRegistry.inputName.collectAsState()
    val outputName by AudioDeviceRegistry.outputName.collectAsState()
    val inputs = remember { AudioDeviceRegistry.availableInputs() }
    val outputs = remember { AudioDeviceRegistry.availableOutputs() }

    Spacer(Modifier.height(20.dp))
    Text(
        "Audio (Desktop)",
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        color = MurmrColors.OnSurfaceFaint,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.6.sp),
    )
    Spacer(Modifier.height(8.dp))
    SettingsGroup {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            AudioDeviceDropdown(
                label = "Micrófono",
                options = inputs,
                selectedName = inputName,
                onSelect = { AudioDeviceRegistry.selectInput(it) },
            )
            Spacer(Modifier.height(12.dp))
            AudioDeviceDropdown(
                label = "Altavoz",
                options = outputs,
                selectedName = outputName,
                onSelect = { AudioDeviceRegistry.selectOutput(it) },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Si dejas \"Sistema (predeterminado)\", Murmr usará el dispositivo " +
                    "activo de Windows. Cambia esto si tu PC tiene varios micros o " +
                    "salidas y quieres usar uno concreto para el emparejamiento " +
                    "ultrasónico.",
                color = MurmrColors.OnSurfaceMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioDeviceDropdown(
    label: String,
    options: List<AudioDevice>,
    selectedName: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedDisplay = options.firstOrNull { it.name == selectedName }?.displayName
        ?: "Sistema (predeterminado)"
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedDisplay,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = MurmrColors.OnSurfaceMuted) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MurmrColors.OnSurface,
                fontWeight = FontWeight.Medium,
            ),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MurmrColors.PrimaryViolet,
                unfocusedBorderColor = MurmrColors.DividerDark,
                focusedLabelColor = MurmrColors.PrimaryViolet,
                unfocusedLabelColor = MurmrColors.OnSurfaceMuted,
            ),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Sistema (predeterminado)", color = MurmrColors.OnSurface) },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            options.forEach { device ->
                DropdownMenuItem(
                    text = { Text(device.displayName, color = MurmrColors.OnSurface) },
                    onClick = {
                        onSelect(device.name)
                        expanded = false
                    },
                )
            }
        }
    }
}
