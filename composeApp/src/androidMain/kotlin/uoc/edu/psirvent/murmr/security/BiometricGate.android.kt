package uoc.edu.psirvent.murmr.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.mp.KoinPlatform
import android.content.Context

actual class BiometricGate {

    actual suspend fun authenticate(reason: String): BiometricResult {
        val activity = BiometricActivityRegistry.activity
            ?: return BiometricResult.NotAvailable("FragmentActivity no registrada")

        if (activity.isFinishing || activity.isDestroyed) {
            BiometricActivityRegistry.activity = null
            return BiometricResult.NotAvailable("FragmentActivity destruida")
        }

        return suspendCancellableCoroutine { cont ->
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (cont.isActive) cont.resume(BiometricResult.Success)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val res = if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) BiometricResult.Cancelled else BiometricResult.Error("$errorCode: $errString")
                    if (cont.isActive) cont.resume(res)
                }
                override fun onAuthenticationFailed() {

                }
            }
            val prompt = BiometricPrompt(activity, executor, callback)

            val mgr = BiometricManager.from(activity)
            val biometricOk = mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS

            val info = if (biometricOk) {
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Murmr")
                    .setSubtitle(reason)
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                    )
                    .build()
            } else {
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Murmr")
                    .setSubtitle(reason)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()
            }
            prompt.authenticate(info)
        }
    }

    actual fun isAvailable(): Boolean {
        val ctx = runCatching { KoinPlatform.getKoin().get<Context>() }.getOrNull() ?: return false
        val mgr = BiometricManager.from(ctx)
        return mgr.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
}

object BiometricActivityRegistry {
    @Volatile var activity: FragmentActivity? = null
}
