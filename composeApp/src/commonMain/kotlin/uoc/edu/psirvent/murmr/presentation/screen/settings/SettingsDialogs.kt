package uoc.edu.psirvent.murmr.presentation.screen.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.data.network.RelayConfigStore
import uoc.edu.psirvent.murmr.presentation.util.UiTextos
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.util.ValidationResult
import uoc.edu.psirvent.murmr.util.Validators

@Composable
internal fun AliasDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var draft by remember { mutableStateOf(current) }
    val validation = Validators.alias(draft)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar alias") },
        text = {
            Column {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 45) draft = it },
                    singleLine = true,
                    isError = !validation.isValid && draft.isNotEmpty(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = settingsTextFieldColors(),
                )
                val err = validation.errorOrNull()
                if (err != null && draft.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(err, color = MurmrColors.Error, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (validation.isValid) onSave(draft.trim()) },
                enabled = validation.isValid,
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
internal fun ValidatedNumberDialog(
    title: String,
    label: String,
    initial: String,
    validator: (String) -> ValidationResult,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    val validation = validator(draft)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 8) draft = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    label = { Text(label) },
                    isError = !validation.isValid && draft.isNotEmpty(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = settingsTextFieldColors(),
                )
                val err = validation.errorOrNull()
                if (err != null && draft.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(err, color = MurmrColors.Error, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (validation.isValid) onSave(draft) },
                enabled = validation.isValid,
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
internal fun RelayUrlDialog(onDismiss: () -> Unit) {
    val store = rememberKoinInject<RelayConfigStore>()
    val cfg by store.flow.collectAsState()
    var custom by remember { mutableStateOf(!store.isOfficial()) }
    var draft by remember { mutableStateOf(if (store.isOfficial()) "" else cfg.baseUrl) }
    val validation = Validators.relayUrl(draft)
    var globalError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(custom) { if (!custom) draft = "" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("URL del relay") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { custom = false },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = !custom,
                        onClick = { custom = false },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MurmrColors.PrimaryViolet,
                            unselectedColor = MurmrColors.OnSurfaceMuted,
                        ),
                    )
                    Column {
                        Text("Oficial", color = MurmrColors.OnSurface, fontSize = 13.sp)
                        Text(
                            "Por defecto en esta plataforma",
                            color = MurmrColors.OnSurfaceFaint,
                            fontSize = 11.sp,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { custom = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = custom,
                        onClick = { custom = true },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MurmrColors.PrimaryViolet,
                            unselectedColor = MurmrColors.OnSurfaceMuted,
                        ),
                    )
                    Text("Personalizada", color = MurmrColors.OnSurface, fontSize = 13.sp)
                }
                if (custom) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 200) draft = it },
                        singleLine = true,
                        placeholder = { Text("https://relay.example.com:8080") },
                        isError = !validation.isValid && draft.isNotEmpty(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = settingsTextFieldColors(),
                    )
                    val err = validation.errorOrNull() ?: globalError
                    if (err != null && draft.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(err, color = MurmrColors.Error, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Cambiar la URL invalida el token actual y obliga a registrar el dispositivo de nuevo.",
                        color = MurmrColors.OnSurfaceFaint,
                        fontSize = 10.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !custom || validation.isValid,
                onClick = {
                    if (custom) {
                        when (val r = store.setCustom(draft)) {
                            ValidationResult.Ok -> onDismiss()
                            is ValidationResult.Invalid -> {
                                globalError = r.message
                            }
                        }
                    } else {
                        store.setOfficial()
                        onDismiss()
                    }
                },
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
internal fun settingsTextFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = MurmrColors.OnSurface,
    unfocusedTextColor = MurmrColors.OnSurface,
    focusedContainerColor = MurmrColors.IncomingBubble,
    unfocusedContainerColor = MurmrColors.IncomingBubble,
    focusedIndicatorColor = MurmrColors.PrimaryViolet,
    unfocusedIndicatorColor = MurmrColors.DividerDark,
    cursorColor = MurmrColors.PrimaryViolet,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T : Any> ChoiceBottomSheet(
    title: String,
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSave: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MurmrColors.SurfaceDark,
        contentColor = MurmrColors.OnSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MurmrColors.OnSurfaceFaint),
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MurmrColors.OnSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                label,
                color = MurmrColors.OnSurfaceMuted,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (value, lab) ->
                    val sel = selected == value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) MurmrColors.PrimaryViolet.copy(alpha = 0.15f) else MurmrColors.IncomingBubble)
                            .clickable {
                                scope.launch { sheetState.hide() }
                                onSave(value)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            lab,
                            color = if (sel) MurmrColors.PrimaryViolet else MurmrColors.OnSurface,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        if (sel) {
                            Text(
                                "✓",
                                color = MurmrColors.PrimaryViolet,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

