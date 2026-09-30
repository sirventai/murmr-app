package uoc.edu.psirvent.murmr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant
import uoc.edu.psirvent.murmr.data.mapper.toDomain
import uoc.edu.psirvent.murmr.db.MurmrDatabase
import uoc.edu.psirvent.murmr.domain.model.AppSettings
import uoc.edu.psirvent.murmr.domain.model.AutoLockTimeout
import uoc.edu.psirvent.murmr.domain.model.SessionExpiry

class SettingsRepository(
    private val db: MurmrDatabase,
    private val io: CoroutineDispatcher = Dispatchers.Default,
    private val clock: Clock = Clock.System,
) {
    private val queries get() = db.settingsQueries

    fun observeFirst(): Flow<AppSettings?> =
        queries.selectFirst()
            .asFlow()
            .mapToOneOrNull(io)
            .map { it?.toDomain() }

    suspend fun getFirst(): AppSettings? = withContext(io) {
        queries.selectFirst().executeAsOneOrNull()?.toDomain()
    }

    suspend fun upsert(settings: AppSettings) = withContext(io) {
        queries.upsert(
            deviceId = settings.deviceId,
            sessionExpiry = settings.sessionExpiry.name,
            biometricLock = if (settings.biometricLock) 1L else 0L,
            autoLockTimeout = settings.autoLockTimeout.name,
            notificationsEnabled = if (settings.notificationsEnabled) 1L else 0L,
            onboardingDone = if (settings.onboardingDone) 1L else 0L,
            appVersion = settings.appVersion,
            updatedAt = settings.updatedAt.toEpochMilliseconds(),
            customExpirySeconds = settings.customExpirySeconds,
            customAutolockSecs = settings.customAutoLockSeconds.toLong(),
            onboardingStep = settings.onboardingStep.toLong(),
        )
    }

    suspend fun setOnboardingStep(deviceId: String, step: Int, ts: Instant = clock.now()) = withContext(io) {
        queries.setOnboardingStep(step = step.toLong(), ts = ts.toEpochMilliseconds(), deviceId = deviceId)
    }

    suspend fun setSessionExpiry(deviceId: String, value: SessionExpiry, ts: Instant = clock.now()) = withContext(io) {
        queries.updateSessionExpiry(value = value.name, ts = ts.toEpochMilliseconds(), deviceId = deviceId)
    }

    suspend fun setBiometric(deviceId: String, enabled: Boolean, ts: Instant = clock.now()) = withContext(io) {
        queries.updateBiometric(value = if (enabled) 1L else 0L, ts = ts.toEpochMilliseconds(), deviceId = deviceId)
    }

    suspend fun setAutoLock(deviceId: String, value: AutoLockTimeout, ts: Instant = clock.now()) = withContext(io) {
        queries.updateAutoLock(value = value.name, ts = ts.toEpochMilliseconds(), deviceId = deviceId)
    }

    suspend fun setNotifications(deviceId: String, enabled: Boolean, ts: Instant = clock.now()) = withContext(io) {
        queries.updateNotifications(value = if (enabled) 1L else 0L, ts = ts.toEpochMilliseconds(), deviceId = deviceId)
    }

    suspend fun setCustomExpirySeconds(deviceId: String, seconds: Long, ts: Instant = clock.now()) = withContext(io) {
        queries.updateCustomExpirySeconds(value = seconds, ts = ts.toEpochMilliseconds(), deviceId = deviceId)
    }

    suspend fun setCustomAutoLockSeconds(deviceId: String, secs: Int, ts: Instant = clock.now()) = withContext(io) {
        queries.updateCustomAutoLockSecs(value = secs.toLong(), ts = ts.toEpochMilliseconds(), deviceId = deviceId)
    }

    suspend fun markOnboardingDone(deviceId: String, ts: Instant = clock.now()) = withContext(io) {
        queries.markOnboardingDone(ts = ts.toEpochMilliseconds(), deviceId = deviceId)
    }

    suspend fun deleteAll() = withContext(io) {
        queries.deleteAll()
    }
}
