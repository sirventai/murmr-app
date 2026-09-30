package uoc.edu.psirvent.murmr.domain.model

import kotlin.time.Instant

enum class SessionExpiry { NEVER, ONE_DAY, SEVEN_DAYS, THIRTY_DAYS, CUSTOM }

enum class AutoLockTimeout { NEVER, FIVE_SECONDS, SIXTY_SECONDS, FIVE_MINUTES, CUSTOM }

data class AppSettings(
    val deviceId: String,
    val sessionExpiry: SessionExpiry,
    val biometricLock: Boolean,
    val autoLockTimeout: AutoLockTimeout,
    val notificationsEnabled: Boolean,
    val onboardingDone: Boolean,
    val appVersion: String,
    val updatedAt: Instant,
    val customExpirySeconds: Long = 14L * 24 * 3600,
    val customAutoLockSeconds: Int = 30,
    val onboardingStep: Int = 0,
) {
    companion object {
        const val APP_VERSION = "1.0.0"

        fun default(deviceId: String, now: Instant): AppSettings = AppSettings(
            deviceId = deviceId,

            sessionExpiry = SessionExpiry.NEVER,
            biometricLock = false,
            autoLockTimeout = AutoLockTimeout.SIXTY_SECONDS,
            notificationsEnabled = true,
            onboardingDone = false,
            appVersion = APP_VERSION,
            updatedAt = now,
        )
    }
}
