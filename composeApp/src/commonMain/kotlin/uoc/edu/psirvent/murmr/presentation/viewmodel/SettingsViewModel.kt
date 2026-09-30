package uoc.edu.psirvent.murmr.presentation.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uoc.edu.psirvent.murmr.data.network.AppSession
import uoc.edu.psirvent.murmr.data.repository.SettingsRepository
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.model.AppSettings
import uoc.edu.psirvent.murmr.domain.model.AutoLockTimeout
import uoc.edu.psirvent.murmr.domain.model.SessionExpiry
import uoc.edu.psirvent.murmr.domain.model.User
import uoc.edu.psirvent.murmr.domain.usecase.BroadcastExpiryUpdateUseCase
import uoc.edu.psirvent.murmr.domain.usecase.BroadcastProfileUpdateUseCase
import uoc.edu.psirvent.murmr.domain.usecase.DeleteAccountUseCase
import uoc.edu.psirvent.murmr.presentation.components.SnackbarController
import uoc.edu.psirvent.murmr.security.BiometricGate
import uoc.edu.psirvent.murmr.security.BiometricResult
import uoc.edu.psirvent.murmr.util.ApplicationScope
import uoc.edu.psirvent.murmr.util.Logger

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Loaded(val user: User, val settings: AppSettings) : SettingsUiState
    data object Empty : SettingsUiState
}

class SettingsViewModel(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val broadcastProfileUpdate: BroadcastProfileUpdateUseCase,
    private val applicationScope: ApplicationScope,
    private val appSession: AppSession,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val biometricGate: BiometricGate? = null,
    private val snackbar: SnackbarController? = null,
    private val broadcastExpiryUpdate: BroadcastExpiryUpdateUseCase? = null,
) : ScreenModel {

    val state: StateFlow<SettingsUiState> = combine(
        userRepository.observeLocalUser(),
        settingsRepository.observeFirst(),
    ) { user, settings ->
        when {
            user == null -> SettingsUiState.Empty
            settings == null -> SettingsUiState.Empty
            else -> SettingsUiState.Loaded(user, settings)
        }
    }.stateIn(
        scope = screenModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState.Loading,
    )

    fun updateAlias(newAlias: String) {
        val current = (state.value as? SettingsUiState.Loaded) ?: return
        screenModelScope.launch {
            userRepository.updateAlias(current.user.deviceId, newAlias.trim())
        }
    }

    fun updateSessionExpiry(value: SessionExpiry) {
        val current = (state.value as? SettingsUiState.Loaded) ?: run {
            Logger.w("SettingsVM", "updateSessionExpiry($value) ignorado: state=${state.value::class.simpleName}")
            return
        }
        Logger.i("SettingsVM", "updateSessionExpiry($value) device=${current.user.deviceId}")
        screenModelScope.launch {
            withContext(NonCancellable) {

                settingsRepository.setSessionExpiry(current.user.deviceId, value)
                Logger.i("SettingsVM", "setSessionExpiry persisted=$value (solo afecta a NUEVOS chats)")
            }
        }
    }

    fun updateBiometric(enabled: Boolean) {
        val current = (state.value as? SettingsUiState.Loaded) ?: return
        screenModelScope.launch {
            if (!enabled) {
                settingsRepository.setBiometric(current.user.deviceId, false)
                return@launch
            }

            val gate = biometricGate
            if (gate == null) {
                settingsRepository.setBiometric(current.user.deviceId, true)
                return@launch
            }
            val result = runCatching { gate.authenticate("Confirma tu biometría") }
                .getOrElse { BiometricResult.Error(it.message ?: "fallo") }
            when (result) {
                BiometricResult.Success -> {
                    settingsRepository.setBiometric(current.user.deviceId, true)
                    snackbar?.show("Biometría activada")
                }
                else -> {
                    settingsRepository.setBiometric(current.user.deviceId, false)
                    snackbar?.show("No se pudo activar el biométrico")
                }
            }
        }
    }

    fun updateAutoLock(value: AutoLockTimeout) {
        val current = (state.value as? SettingsUiState.Loaded) ?: run {
            Logger.w("SettingsVM", "updateAutoLock($value) ignorado: state=${state.value::class.simpleName}")
            return
        }
        Logger.i("SettingsVM", "updateAutoLock($value) device=${current.user.deviceId}")
        screenModelScope.launch {
            withContext(NonCancellable) {
                settingsRepository.setAutoLock(current.user.deviceId, value)
                Logger.i("SettingsVM", "setAutoLock persisted=$value")
            }
        }
    }

    fun updateNotifications(enabled: Boolean) {
        val current = (state.value as? SettingsUiState.Loaded) ?: return
        screenModelScope.launch {
            settingsRepository.setNotifications(current.user.deviceId, enabled)
        }
    }

    fun updateCustomExpirySeconds(seconds: Long) {
        val current = (state.value as? SettingsUiState.Loaded) ?: return
        Logger.i("SettingsVM", "updateCustomExpirySeconds(${seconds}s) (solo afecta a NUEVOS chats)")
        screenModelScope.launch {
            withContext(NonCancellable) {
                settingsRepository.setCustomExpirySeconds(current.user.deviceId, seconds)
                settingsRepository.setSessionExpiry(current.user.deviceId, SessionExpiry.CUSTOM)
            }
        }
    }

    fun updateCustomAutoLockSeconds(secs: Int) {
        val current = (state.value as? SettingsUiState.Loaded) ?: return
        Logger.i("SettingsVM", "updateCustomAutoLockSeconds($secs)")
        screenModelScope.launch {
            withContext(NonCancellable) {
                settingsRepository.setCustomAutoLockSeconds(current.user.deviceId, secs)
                settingsRepository.setAutoLock(current.user.deviceId, AutoLockTimeout.CUSTOM)
            }
        }
    }

    fun updateAvatar(avatar: ByteArray?, avatarBig: ByteArray?) {
        val current = (state.value as? SettingsUiState.Loaded) ?: return
        screenModelScope.launch {
            userRepository.updateAvatar(current.user.deviceId, avatar)
            userRepository.updateAvatarBig(current.user.deviceId, avatarBig)
        }
    }

    fun deleteAccount(onComplete: () -> Unit, onError: (String) -> Unit) {
        applicationScope.launch {
            try {
                appSession.stop()
                deleteAccountUseCase()
                withContext(Dispatchers.Main) { onComplete() }
            } catch (t: Throwable) {
                Logger.e("SettingsVM", "delete-account fallo", t)
                withContext(Dispatchers.Main) { onError(t.message ?: "Error eliminando cuenta") }
            }
        }
    }
}
