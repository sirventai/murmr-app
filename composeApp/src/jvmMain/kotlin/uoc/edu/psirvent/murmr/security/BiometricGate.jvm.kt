package uoc.edu.psirvent.murmr.security

import uoc.edu.psirvent.murmr.util.Logger

actual class BiometricGate {

    actual suspend fun authenticate(reason: String): BiometricResult {
        Logger.w("BiometricGate", "Desktop stub - no hay biometría real, devolviendo Success ($reason)")
        return BiometricResult.Success
    }

    actual fun isAvailable(): Boolean = false
}
