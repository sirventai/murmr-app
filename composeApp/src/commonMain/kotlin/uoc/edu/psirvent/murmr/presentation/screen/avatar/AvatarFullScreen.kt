package uoc.edu.psirvent.murmr.presentation.screen.avatar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.domain.usecase.RequestPeerAvatarBigUseCase
import uoc.edu.psirvent.murmr.presentation.components.MurmrIcons
import uoc.edu.psirvent.murmr.presentation.navigation.InteractiveSwipeBack
import uoc.edu.psirvent.murmr.presentation.navigation.PreviousScreenBackground
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject

class AvatarFullScreen(private val contactId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val contactRepo = rememberKoinInject<ContactRepository>()
        val requestBig = rememberKoinInject<RequestPeerAvatarBigUseCase>()
        val scope = rememberCoroutineScope()

        val contact by contactRepo.observeById(contactId).collectAsState(initial = null)

        val hasBig = contact?.avatarBig != null
        var timedOut by remember(contactId) { mutableStateOf(false) }
        var secondsRemaining by remember(contactId) { mutableStateOf(0) }
        LaunchedEffect(contactId, hasBig) {
            if (contact != null && !hasBig) {
                timedOut = false
                secondsRemaining = 20
                scope.launch { requestBig(contactId) }
                while (secondsRemaining > 0 && !hasBig) {
                    delay(1_000)
                    secondsRemaining -= 1
                }
                if (!hasBig) timedOut = true
            }
        }

        InteractiveSwipeBack(
            onCompleted = { navigator.pop() },
            background = { PreviousScreenBackground() },
            foreground = {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        IconButton(onClick = { navigator.pop() }, modifier = Modifier.padding(8.dp)) {
                            Icon(
                                imageVector = MurmrIcons.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            val c = contact
                            val timeoutLabel = "${c?.displayAlias ?: "El contacto"} no tiene foto disponible"
                            when {
                                c == null -> CircularProgressIndicator(color = MurmrColors.PrimaryViolet)
                                c.avatarBig != null -> AsyncImage(
                                    model = c.avatarBig,
                                    contentDescription = "Avatar de ${c.displayAlias}",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Fit,
                                )
                                c.avatar != null -> {
                                    Box(contentAlignment = Alignment.Center) {
                                        AsyncImage(
                                            model = c.avatar,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxWidth(),
                                            contentScale = ContentScale.Fit,
                                        )
                                        if (timedOut) {
                                            Text(
                                                timeoutLabel,
                                                color = Color.White.copy(alpha = 0.85f),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            )
                                        } else {
                                            Loading(label = "Pidiendo a ${c.displayAlias}... (${secondsRemaining}s)")
                                        }
                                    }
                                }
                                timedOut -> Text(
                                    timeoutLabel,
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                )
                                else -> Loading(label = "Pidiendo a ${c.displayAlias}... (${secondsRemaining}s)")
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun Loading(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = MurmrColors.PrimaryViolet)
        Spacer(Modifier.height(12.dp))
        Text(
            label,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}
