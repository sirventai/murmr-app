package uoc.edu.psirvent.murmr.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class AppLockManagerTest {

    private class TestClock(var now: Instant) : Clock {
        override fun now(): Instant = now
    }

    @Test
    fun startsUnlocked() = runTest {
        val mgr = AppLockManager()
        assertFalse(mgr.locked.value)
    }

    @Test
    fun foregroundShortlyAfterBackgroundDoesNotLock() = runTest {
        val clock = TestClock(Clock.System.now())
        val mgr = AppLockManager(clock = clock)
        mgr.notifyBackgrounded()
        clock.now = clock.now + 5.seconds
        mgr.notifyForegrounded(biometricEnabled = true, autoLockSeconds = 30)
        assertFalse(mgr.locked.value, "5s no debería disparar lock con timer 30s")
    }

    @Test
    fun foregroundAfterAutolockTimeoutLocks() = runTest {
        val clock = TestClock(Clock.System.now())
        val mgr = AppLockManager(clock = clock)
        mgr.notifyBackgrounded()
        clock.now = clock.now + 35.seconds
        mgr.notifyForegrounded(biometricEnabled = true, autoLockSeconds = 30)
        assertTrue(mgr.locked.value, "35s con timer 30s debería bloquear")
    }

    @Test
    fun biometricDisabledDoesNotLock() = runTest {
        val clock = TestClock(Clock.System.now())
        val mgr = AppLockManager(clock = clock)
        mgr.notifyBackgrounded()
        clock.now = clock.now + 60.seconds
        mgr.notifyForegrounded(biometricEnabled = false, autoLockSeconds = 30)
        assertFalse(mgr.locked.value, "Si biométrico desactivado, nunca bloquea")
    }

    @Test
    fun autolockZeroLocksImmediately() = runTest {
        val clock = TestClock(Clock.System.now())
        val mgr = AppLockManager(clock = clock)
        mgr.notifyBackgrounded()

        mgr.notifyForegrounded(biometricEnabled = true, autoLockSeconds = 0)
        assertTrue(mgr.locked.value, "autoLockSeconds=0 debe bloquear al instante")
    }

    @Test
    fun autolockNegativeNeverLocks() = runTest {
        val clock = TestClock(Clock.System.now())
        val mgr = AppLockManager(clock = clock)
        mgr.notifyBackgrounded()
        clock.now = clock.now + 60.seconds

        mgr.notifyForegrounded(biometricEnabled = true, autoLockSeconds = -1)
        assertFalse(mgr.locked.value, "autoLockSeconds=-1 desactiva el bloqueo")
    }

    @Test
    fun unlockResets() = runTest {
        val clock = TestClock(Clock.System.now())
        val mgr = AppLockManager(clock = clock)
        mgr.notifyBackgrounded()
        clock.now = clock.now + 60.seconds
        mgr.notifyForegrounded(biometricEnabled = true, autoLockSeconds = 30)
        assertTrue(mgr.locked.value)
        mgr.unlock()
        assertFalse(mgr.locked.value)
    }

    @Test
    fun forceLockBypassesTimer() = runTest {
        val mgr = AppLockManager()
        assertFalse(mgr.locked.value)
        mgr.forceLock()
        assertTrue(mgr.locked.value)
    }

    @Test
    fun coldStartWithBiometricOnLocks() = runTest {

        val mgr = AppLockManager()
        mgr.notifyForegrounded(biometricEnabled = true, autoLockSeconds = 30)
        assertTrue(mgr.locked.value, "Cold start con biometric=ON debe bloquear")
    }

    @Test
    fun spuriousForegroundAfterUnlockDoesNotRelock() = runTest {

        val mgr = AppLockManager()
        mgr.notifyForegrounded(biometricEnabled = true, autoLockSeconds = 30)
        assertTrue(mgr.locked.value, "Cold start bloquea como precondición")
        mgr.unlock()
        mgr.notifyForegrounded(biometricEnabled = true, autoLockSeconds = 30)
        assertFalse(mgr.locked.value, "Foreground espurio post-unlock no debe bloquear")
    }
}

class BiometricGateJvmTest {
    @Test
    fun jvmStubReturnsSuccess() = runTest {
        val gate = BiometricGate()
        val result = gate.authenticate("test reason")
        assertEquals(BiometricResult.Success, result)
    }

    @Test
    fun jvmStubReportsNotAvailable() {
        val gate = BiometricGate()
        assertFalse(gate.isAvailable(), "JVM stub no tiene biometría real")
    }
}
