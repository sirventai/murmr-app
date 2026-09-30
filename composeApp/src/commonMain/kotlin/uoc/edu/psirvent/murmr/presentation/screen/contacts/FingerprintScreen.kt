package uoc.edu.psirvent.murmr.presentation.screen.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.core.parameter.parametersOf
import uoc.edu.psirvent.murmr.domain.model.Sas
import uoc.edu.psirvent.murmr.presentation.components.MurmrIcons
import uoc.edu.psirvent.murmr.presentation.navigation.InteractiveSwipeBack
import uoc.edu.psirvent.murmr.presentation.navigation.PreviousScreenBackground
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject
import uoc.edu.psirvent.murmr.presentation.viewmodel.ContactDetailUiState
import uoc.edu.psirvent.murmr.presentation.viewmodel.ContactDetailViewModel

class FingerprintScreen(private val contactId: String) : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberKoinInject<ContactDetailViewModel> { parametersOf(contactId) }
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        InteractiveSwipeBack(
            onCompleted = { navigator.pop() },
            background = { PreviousScreenBackground() },
            foreground = {
                Surface(modifier = Modifier.fillMaxSize(), color = MurmrColors.BackgroundDark) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Header(onBack = { navigator.pop() })
                        when (val s = state) {
                            is ContactDetailUiState.Loaded -> {
                                val fingerprint = s.activeSession?.fingerprint
                                    ?: "Sin sesión activa"
                                FingerprintBody(
                                    alias = s.contact.displayAlias,
                                    fingerprint = fingerprint,
                                )
                            }
                            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Cargando...", color = MurmrColors.OnSurfaceMuted)
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                MurmrIcons.ArrowBack,
                contentDescription = "Atrás",
                tint = MurmrColors.OnSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            "Huella de la sesión",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            color = MurmrColors.OnSurface,
        )
    }
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(MurmrColors.DividerDark))
}

@Composable
private fun FingerprintBody(alias: String, fingerprint: String) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Sesión con $alias",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            color = MurmrColors.OnSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Compara este código por un canal alternativo si quieres asegurar que nadie se está haciendo pasar por $alias.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
            color = MurmrColors.OnSurfaceMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))
        SectionLabel("Hexadecimal")
        Spacer(Modifier.height(8.dp))
        FingerprintHexBox(fingerprint = fingerprint)

        Spacer(Modifier.height(24.dp))
        SectionLabel("Emoji")
        Spacer(Modifier.height(8.dp))
        FingerprintEmojiRow(fingerprint = fingerprint)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
        color = MurmrColors.OnSurfaceMuted,
    )
}

@Composable
private fun FingerprintHexBox(fingerprint: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MurmrColors.SurfaceDark)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            fingerprint,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                letterSpacing = 1.sp,
            ),
            color = MurmrColors.OnSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FingerprintEmojiRow(fingerprint: String) {
    val emojis = remember(fingerprint) { fingerprintToEmojis(fingerprint, count = 8) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MurmrColors.SurfaceDark)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            emojis.forEach { e ->
                Text(e, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp))
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Derivado de los primeros bits del fingerprint sobre el alfabeto de 64 emojis del SAS.",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
        color = MurmrColors.OnSurfaceMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    )
}

private fun fingerprintToEmojis(fingerprint: String, count: Int): List<String> {
    val cleaned = fingerprint.filter { it != ' ' }
    if (cleaned.isEmpty()) return List(count) { "❓" }
    val bytes = ByteArray(cleaned.length / 2) { i ->
        val hi = hexDigit(cleaned[2 * i])
        val lo = hexDigit(cleaned[2 * i + 1])
        ((hi shl 4) or lo).toByte()
    }
    val out = mutableListOf<String>()
    var bitIdx = 0
    val totalBits = bytes.size * 8
    while (out.size < count && bitIdx + 5 <= totalBits) {
        var v = 0
        for (k in 0 until 5) {
            val bit = (bytes[(bitIdx + k) ushr 3].toInt() ushr (7 - ((bitIdx + k) and 7))) and 1
            v = (v shl 1) or bit
        }
        out.add(Sas.EMOJI_ALPHABET[v % Sas.EMOJI_ALPHABET.size])
        bitIdx += 5
    }
    while (out.size < count) out.add("❓")
    return out
}

private fun hexDigit(c: Char): Int = when (c) {
    in '0'..'9' -> c.code - '0'.code
    in 'a'..'f' -> c.code - 'a'.code + 10
    in 'A'..'F' -> c.code - 'A'.code + 10
    else -> 0
}
