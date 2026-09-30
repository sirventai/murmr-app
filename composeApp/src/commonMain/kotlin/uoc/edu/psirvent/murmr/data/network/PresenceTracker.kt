package uoc.edu.psirvent.murmr.data.network

import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import uoc.edu.psirvent.murmr.util.Logger

class PresenceTracker(
    private val relayApi: RelayApi,
    private val connectivity: ConnectivityMonitor? = null,
    private val pollInterval: kotlin.time.Duration = 15.seconds,
    private val clock: Clock = Clock.System,
) {

    private val lastActiveAt = MutableStateFlow<Map<String, Instant>>(emptyMap())

    fun markRecentActivity(deviceId: String) {
        lastActiveAt.value = lastActiveAt.value + (deviceId to clock.now())
    }

    fun observe(remoteDeviceId: String): Flow<Boolean?> {
        val pollFlow: Flow<Boolean?> = flow {
            while (true) {
                val online: Boolean? = if (connectivity?.connected?.value == false) {
                    null
                } else {
                    runCatching {
                        relayApi.fetchPresence(remoteDeviceId)
                    }.onFailure {
                        Logger.w("Presence", "fetchPresence($remoteDeviceId) fallo: ${it.message}")
                    }.getOrNull()
                }
                Logger.i("Presence", "poll $remoteDeviceId -> $online")
                emit(online)
                delay(pollInterval)
            }
        }
        return combine(pollFlow, lastActiveAt) { polled, activityMap ->
            val lastActive = activityMap[remoteDeviceId]
            val recentlyActive = lastActive != null &&
                (clock.now() - lastActive) < RECENT_ACTIVITY_TTL
            when {
                polled == true || recentlyActive -> true
                polled == false -> false
                else -> null
            }
        }
    }

    companion object {

        private val RECENT_ACTIVITY_TTL = 90.seconds
    }
}
