package uoc.edu.psirvent.murmr.security

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorAuthenticationFailed
import platform.LocalAuthentication.LAErrorPasscodeNotSet
import platform.LocalAuthentication.LAErrorSystemCancel
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAErrorUserFallback
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import uoc.edu.psirvent.murmr.util.Logger
import kotlin.coroutines.resume

actual class BiometricGate {

    private val tag = "BiometricGate"

    @OptIn(ExperimentalForeignApi::class)
    actual fun isAvailable(): Boolean {
        val ctx = LAContext()
        val canEvaluate = ctx.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)
        Logger.i(tag, "isAvailable=$canEvaluate")
        return canEvaluate
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual suspend fun authenticate(reason: String): BiometricResult = withContext(Dispatchers.Main) {
        val ctx = LAContext()
        if (!ctx.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)) {
            Logger.w(tag, "Biométrico no disponible (sin Face ID/Touch ID configurado)")
            return@withContext BiometricResult.NotAvailable(
                "Face ID o Touch ID no disponible en este dispositivo",
            )
        }

        suspendCancellableCoroutine<BiometricResult> { cont ->
            ctx.evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = reason,
            ) { success, error ->
                val outcome: BiometricResult = when {
                    success -> {
                        Logger.i(tag, "Auth OK")
                        BiometricResult.Success
                    }
                    error == null -> {
                        Logger.w(tag, "Auth fallo sin NSError")
                        BiometricResult.Error("Fallo desconocido")
                    }
                    else -> {
                        val code = error.code.toInt()
                        Logger.w(tag, "Auth fallo code=$code msg=${error.localizedDescription}")
                        when (code) {
                            LAErrorUserCancel.toInt(),
                            LAErrorSystemCancel.toInt(),
                            LAErrorUserFallback.toInt(),
                            -> BiometricResult.Cancelled
                            LAErrorPasscodeNotSet.toInt(),
                            -> BiometricResult.NotAvailable("Passcode no configurado")
                            LAErrorAuthenticationFailed.toInt(),
                            -> BiometricResult.Error("Autenticación fallida")
                            else -> BiometricResult.Error(error.localizedDescription)
                        }
                    }
                }
                cont.resume(outcome)
            }
        }
    }
}
