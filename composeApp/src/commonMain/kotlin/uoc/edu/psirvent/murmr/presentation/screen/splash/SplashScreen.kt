package uoc.edu.psirvent.murmr.presentation.screen.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import uoc.edu.psirvent.murmr.domain.usecase.RegisterDeviceUseCase
import uoc.edu.psirvent.murmr.presentation.components.MurmrLogo
import uoc.edu.psirvent.murmr.presentation.screen.home.HomeScreen
import uoc.edu.psirvent.murmr.presentation.screen.onboarding.OnboardingScreen
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject
import uoc.edu.psirvent.murmr.presentation.viewmodel.SplashRoute
import uoc.edu.psirvent.murmr.presentation.viewmodel.SplashViewModel
import uoc.edu.psirvent.murmr.util.Autostart
import uoc.edu.psirvent.murmr.util.Logger

object SplashScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberKoinInject<SplashViewModel>()
        val registerDevice = rememberKoinInject<RegisterDeviceUseCase>()
        val route by viewModel.route.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(route) {
            when (route) {
                SplashRoute.NeedsOnboarding -> {
                    val autoAlias = Autostart.alias
                    if (autoAlias != null) {
                        Logger.i("Autostart", "registrando alias automáticamente: '$autoAlias'")
                        runCatching { registerDevice(autoAlias) }
                            .onFailure { Logger.e("Autostart", "fallo registro automático", it) }
                        navigator.replaceAll(HomeScreen)
                    } else {
                        navigator.replaceAll(OnboardingScreen)
                    }
                }
                SplashRoute.EnterApp -> navigator.replaceAll(HomeScreen)
                SplashRoute.Pending -> Unit
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MurmrColors.BackgroundDark) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MurmrLogo(size = 132.dp)
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = "Murmr",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-2).sp,
                        ),
                        color = MurmrColors.OnSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "SECURE MESSAGING BY SOUND",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 4.sp,
                            fontWeight = FontWeight.Light,
                        ),
                        color = MurmrColors.OnSurfaceFaint,
                    )
                }
            }
        }
    }
}
