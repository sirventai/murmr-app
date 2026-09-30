package uoc.edu.psirvent.murmr.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class ConnectivityMonitor {

    private val _connected = MutableStateFlow(true)
    actual val connected: StateFlow<Boolean> = _connected.asStateFlow()
}
