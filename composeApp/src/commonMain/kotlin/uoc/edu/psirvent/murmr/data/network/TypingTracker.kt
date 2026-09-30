package uoc.edu.psirvent.murmr.data.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class TypingTracker(
    private val clock: Clock = Clock.System,
) {
    private val expiresAtByContact = MutableStateFlow<Map<String, Instant>>(emptyMap())

    fun onTypingStart(contactId: String) {
        val expires = clock.now().plus(TTL)
        expiresAtByContact.value = expiresAtByContact.value + (contactId to expires)
    }

    fun onTypingStop(contactId: String) {
        expiresAtByContact.value = expiresAtByContact.value - contactId
    }

    fun observe(contactId: String): Flow<Boolean> = flow {
        var lastEmitted: Boolean? = null
        while (true) {
            val expires = expiresAtByContact.value[contactId]
            val active = expires != null && expires > clock.now()
            if (active != lastEmitted) {
                emit(active)
                lastEmitted = active
            }
            if (expires != null && !active) {
                expiresAtByContact.value = expiresAtByContact.value - contactId
            }
            delay(500.milliseconds)
        }
    }

    companion object {
        val TTL = 7_000.milliseconds
    }
}
