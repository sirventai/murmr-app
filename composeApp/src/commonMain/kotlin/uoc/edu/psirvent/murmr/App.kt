package uoc.edu.psirvent.murmr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransition
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.data.network.AppSession
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.presentation.components.MurmrIcons
import uoc.edu.psirvent.murmr.presentation.components.SnackbarController
import uoc.edu.psirvent.murmr.presentation.screen.splash.SplashScreen
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.theme.MurmrTheme
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject
import uoc.edu.psirvent.murmr.security.AppLockManager
import uoc.edu.psirvent.murmr.security.BiometricGate
import uoc.edu.psirvent.murmr.security.BiometricResult

@Composable
@Preview
fun App() {
    MurmrTheme {
        val snackbarController = rememberKoinInject<SnackbarController>()
        val snackbarHostState = remember { SnackbarHostState() }
        val appLock = rememberKoinInject<AppLockManager>()
        val appSession = rememberKoinInject<AppSession>()
        val userRepository = rememberKoinInject<UserRepository>()
        val locked by appLock.locked.collectAsState()
        val coroutineScope = rememberCoroutineScope()

        val currentDeviceId by userRepository.observeLocalUser()
            .collectAsState(initial = null)
        LaunchedEffect(currentDeviceId?.deviceId) {
            if (currentDeviceId != null) {
                appSession.start(coroutineScope)
            }
        }

        LaunchedEffect(Unit) {
            snackbarController.messages.collect { msg ->
                snackbarHostState.showSnackbar(msg)
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MurmrColors.BackgroundDark,
            contentColor = MurmrColors.OnSurface,
            contentWindowInsets = WindowInsets.statusBars,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            content = { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    Navigator(SplashScreen) { navigator ->

                        ScreenTransition(
                            navigator = navigator,
                            transition = {
                                when (navigator.lastEvent) {
                                    StackEvent.Pop -> {
                                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                                    }
                                    else -> {
                                        slideInHorizontally(tween(220)) { it } togetherWith
                                            slideOutHorizontally(tween(220)) { -it / 4 }
                                    }
                                }
                            },
                        )
                    }
                    if (locked) {
                        LockOverlay(onUnlocked = { appLock.unlock() })
                    }
                }
            },
        )
    }
}

@Composable
private fun LockOverlay(onUnlocked: () -> Unit) {
    val gate = rememberKoinInject<BiometricGate>()
    val scope = rememberCoroutineScope()

    var isAuthenticating by remember { mutableStateOf(false) }

    suspend fun tryAuthenticate() {
        if (isAuthenticating) return
        isAuthenticating = true
        try {
            val result = gate.authenticate("Desbloquea Murmr")
            if (result is BiometricResult.Success) onUnlocked()
        } finally {
            isAuthenticating = false
        }
    }

    LaunchedEffect(Unit) { tryAuthenticate() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MurmrColors.BackgroundDark,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MurmrColors.PrimaryViolet.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MurmrIcons.Lock,
                    contentDescription = null,
                    tint = MurmrColors.PrimaryViolet,
                    modifier = Modifier.size(44.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Murmr está bloqueada",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                color = MurmrColors.OnSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Autentícate para volver a tus mensajes.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = MurmrColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    scope.launch { tryAuthenticate() }
                },
                enabled = !isAuthenticating,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MurmrColors.PrimaryViolet,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    "Desbloquear",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}
