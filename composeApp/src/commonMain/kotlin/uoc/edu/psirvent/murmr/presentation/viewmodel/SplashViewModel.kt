package uoc.edu.psirvent.murmr.presentation.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.data.repository.SettingsRepository

sealed interface SplashRoute {
    data object Pending : SplashRoute
    data object NeedsOnboarding : SplashRoute
    data object EnterApp : SplashRoute
}

class SplashViewModel(
    private val settingsRepository: SettingsRepository,
    private val minDelayMs: Long = 700,
) : ScreenModel {

    private val _route = MutableStateFlow<SplashRoute>(SplashRoute.Pending)
    val route: StateFlow<SplashRoute> = _route.asStateFlow()

    init {
        screenModelScope.launch {
            val settings = settingsRepository.getFirst()
            delay(minDelayMs)
            _route.value = if (settings?.onboardingDone == true) {
                SplashRoute.EnterApp
            } else {
                SplashRoute.NeedsOnboarding
            }
        }
    }
}
