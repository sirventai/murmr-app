package uoc.edu.psirvent.murmr.presentation.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.util.Validators

sealed interface OnboardingUiState {
    data class InProgress(
        val pageIndex: Int,
        val alias: String,
        val aliasError: String?,
        val avatar: ByteArray? = null,
        val avatarBig: ByteArray? = null,
    ) : OnboardingUiState
    data object Submitting : OnboardingUiState
    data object Completed : OnboardingUiState
    data class Error(val message: String) : OnboardingUiState
}

class OnboardingViewModel(
    private val registerDevice: suspend (String, ByteArray?, ByteArray?) -> Unit,
) : ScreenModel {

    private val _state = MutableStateFlow<OnboardingUiState>(
        OnboardingUiState.InProgress(pageIndex = 0, alias = "", aliasError = null),
    )
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun onPageChange(index: Int) {
        val s = _state.value as? OnboardingUiState.InProgress ?: return
        _state.value = s.copy(pageIndex = index)
    }

    fun onAliasChange(alias: String) {
        val s = _state.value as? OnboardingUiState.InProgress ?: return
        _state.value = s.copy(alias = alias, aliasError = validateAlias(alias))
    }

    fun onAvatarChange(avatar: ByteArray?, avatarBig: ByteArray? = null) {
        val s = _state.value as? OnboardingUiState.InProgress ?: return
        _state.value = s.copy(avatar = avatar, avatarBig = avatarBig)
    }

    fun resetFromError() {
        if (_state.value !is OnboardingUiState.Error) return
        _state.value = OnboardingUiState.InProgress(
            pageIndex = 0,
            alias = "",
            aliasError = null,
        )
    }

    fun submit() {
        val s = _state.value as? OnboardingUiState.InProgress ?: return
        val alias = s.alias.trim()
        val err = validateAlias(alias)
        if (err != null) {
            _state.value = s.copy(aliasError = err)
            return
        }
        _state.value = OnboardingUiState.Submitting
        screenModelScope.launch {
            try {
                registerDevice(alias, s.avatar, s.avatarBig)
                _state.value = OnboardingUiState.Completed
            } catch (t: Throwable) {
                _state.value = OnboardingUiState.Error(t.message ?: "Error al registrar usuario")
            }
        }
    }

    private fun validateAlias(alias: String): String? = Validators.alias(alias).errorOrNull()
}
