package uoc.edu.psirvent.murmr.presentation.components

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RelayDialogBus {
    private val _open = MutableStateFlow(0)
    val open: StateFlow<Int> = _open.asStateFlow()

    fun fire() {
        _open.value = _open.value + 1
    }
}
