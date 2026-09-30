package uoc.edu.psirvent.murmr.data.network

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_global_queue
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import uoc.edu.psirvent.murmr.util.Logger

@OptIn(ExperimentalForeignApi::class)
actual class ConnectivityMonitor {

    private val _connected = MutableStateFlow(true)
    actual val connected: StateFlow<Boolean> = _connected.asStateFlow()

    init {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_queue(
            monitor,
            dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u),
        )
        nw_path_monitor_set_update_handler(monitor) { path ->
            val online = nw_path_get_status(path) == nw_path_status_satisfied
            if (_connected.value != online) {
                Logger.i(TAG, "path online=$online")
                _connected.value = online
            }
        }
        nw_path_monitor_start(monitor)
    }

    private companion object {
        const val TAG = "Connectivity"
    }
}
