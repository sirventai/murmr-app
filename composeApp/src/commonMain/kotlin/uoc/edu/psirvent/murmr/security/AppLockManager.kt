package uoc.edu.psirvent.murmr.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.Instant
import uoc.edu.psirvent.murmr.util.Logger

class AppLockManager(
    private val clock: Clock = Clock.System,
) {
    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private val lastBackgroundedAt = MutableStateFlow<Instant?>(null)

    private var hasNotifiedForegroundOnce = false

    fun notifyBackgrounded() {
        lastBackgroundedAt.value = clock.now()
    }

    fun notifyForegrounded(biometricEnabled: Boolean, autoLockSeconds: Int) {

        if (_locked.value) {
            Logger.i("AppLock", "foregrounded: ya bloqueado, no re-disparar")
            return
        }
        if (!biometricEnabled) {
            Logger.i("AppLock", "foregrounded: biometric=OFF, NO bloqueo")
            return
        }
        if (autoLockSeconds < 0) {
            Logger.i("AppLock", "foregrounded: autoLock=NEVER, NO bloqueo")
            return
        }
        val last = lastBackgroundedAt.value
        val isColdStart = !hasNotifiedForegroundOnce && last == null
        hasNotifiedForegroundOnce = true
        if (isColdStart) {
            Logger.i("AppLock", "foregrounded: cold start con biometric=ON, BLOQUEO")
            _locked.value = true
            return
        }
        if (last == null) {

            Logger.i("AppLock", "foregrounded: post-unlock sin background entremedio, NO bloqueo")
            return
        }
        val elapsed = (clock.now() - last).inWholeSeconds
        if (autoLockSeconds == 0 || elapsed >= autoLockSeconds) {
            Logger.i("AppLock", "foregrounded: elapsed=${elapsed}s >= ${autoLockSeconds}s, BLOQUEO")
            _locked.value = true
        } else {
            Logger.i("AppLock", "foregrounded: elapsed=${elapsed}s < ${autoLockSeconds}s, NO bloqueo aun")
        }
    }

    fun unlock() {
        _locked.value = false
        lastBackgroundedAt.value = null
    }

    fun forceLock() {
        _locked.value = true
    }
}
