package uoc.edu.psirvent.murmr.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uoc.edu.psirvent.murmr.util.Logger

actual class ConnectivityMonitor(context: Context) {

    private val _connected = MutableStateFlow(true)
    actual val connected: StateFlow<Boolean> = _connected.asStateFlow()

    init {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (cm != null) {

            _connected.value = runCatching { cm.isCurrentlyValidated() }.getOrDefault(true)
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()
            runCatching {
                cm.registerNetworkCallback(
                    request,
                    object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Logger.i(TAG, "network available")
                        _connected.value = true
                    }

                    override fun onLost(network: Network) {
                        Logger.i(TAG, "network lost")
                        _connected.value = cm.isCurrentlyValidated()
                    }

                    override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                        val online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                        if (_connected.value != online) {
                            Logger.i(TAG, "capabilities online=$online")
                            _connected.value = online
                        }
                    }
                    },
                )
            }.onFailure { Logger.w(TAG, "registerNetworkCallback fallo: ${it.message}") }
        } else {
            Logger.w(TAG, "ConnectivityManager no disponible, asumiendo online")
        }
    }

    private fun ConnectivityManager.isCurrentlyValidated(): Boolean {
        val net = activeNetwork ?: return false
        val caps = getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private companion object {
        const val TAG = "Connectivity"
    }
}
