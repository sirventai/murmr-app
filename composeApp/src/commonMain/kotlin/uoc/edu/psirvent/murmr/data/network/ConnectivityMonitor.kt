package uoc.edu.psirvent.murmr.data.network

import kotlinx.coroutines.flow.StateFlow

expect class ConnectivityMonitor {
    val connected: StateFlow<Boolean>
}
