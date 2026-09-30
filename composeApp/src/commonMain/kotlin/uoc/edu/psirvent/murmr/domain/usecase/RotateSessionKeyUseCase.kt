package uoc.edu.psirvent.murmr.domain.usecase

import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import uoc.edu.psirvent.murmr.crypto.AES_KEY_BYTES
import uoc.edu.psirvent.murmr.crypto.HkdfSha256
import uoc.edu.psirvent.murmr.crypto.KeyStoreManager
import uoc.edu.psirvent.murmr.crypto.bytesToUuidString
import uoc.edu.psirvent.murmr.data.repository.SessionRepository
import uoc.edu.psirvent.murmr.domain.model.Session
import uoc.edu.psirvent.murmr.domain.model.SessionStatus
import uoc.edu.psirvent.murmr.util.Logger

class RotateSessionKeyUseCase(
    private val sessionRepository: SessionRepository,
    private val keyStore: KeyStoreManager,
    private val clock: Clock = Clock.System,
) {
    private val tag = "RotateKey"

    suspend operator fun invoke(currentSession: Session): Session =
        rotate(currentSession, isLocallyInitiated = true)

    suspend fun applyFromPeer(currentSession: Session, expectedNewRotationIndex: Int): Session? {
        if (expectedNewRotationIndex != currentSession.rotationIndex + 1) {
            Logger.w(tag, "SESSION_ROTATE rotIdx=$expectedNewRotationIndex no coincide con cur=${currentSession.rotationIndex}+1 - ignoro")
            return null
        }
        return rotate(currentSession, isLocallyInitiated = false)
    }

    private suspend fun rotate(currentSession: Session, isLocallyInitiated: Boolean): Session {
        val currentKey = keyStore.loadKey(currentSession.sessionKeyRef)
            ?: error("Sesión actual sin key en KeyStore (rotación abortada)")

        val newRotationIndex = currentSession.rotationIndex + 1
        val saltKey = "${currentSession.sessionId}|$newRotationIndex".encodeToByteArray()
        val newKey = HkdfSha256.derive(
            ikm = currentKey,
            salt = saltKey,
            info = "murmr-rotate-v1".encodeToByteArray(),
            length = AES_KEY_BYTES,
        )
        val saltSid = "${currentSession.sessionId}|$newRotationIndex|sid".encodeToByteArray()
        val newSessionIdBytes = HkdfSha256.derive(
            ikm = newKey,
            salt = saltSid,
            info = "murmr-rotate-sid-v1".encodeToByteArray(),
            length = 16,
        )
        val newSessionId = bytesToUuidString(newSessionIdBytes)
        val newKeyRef = "session-$newSessionId"
        keyStore.storeKey(newKeyRef, newKey)

        val now = clock.now()
        val newSession = Session(
            sessionId = newSessionId,
            contactId = currentSession.contactId,
            sessionKeyRef = newKeyRef,
            createdAt = now,
            expiresAt = currentSession.expiresAt,
            sasCode = currentSession.sasCode,
            fingerprint = currentSession.fingerprint,
            status = SessionStatus.ACTIVE,
            rotationIndex = newRotationIndex,
            previousSessionId = currentSession.sessionId,
        )
        sessionRepository.insert(newSession)
        sessionRepository.updateStatus(currentSession.sessionId, SessionStatus.SUPERSEDED)
        sessionRepository.updateExpiry(currentSession.sessionId, now.plus(SUPERSEDED_TTL))
        val origin = if (isLocallyInitiated) "local" else "peer"
        Logger.i(tag, "rotación OK contact=${currentSession.contactId} idx=${currentSession.rotationIndex}→$newRotationIndex origin=$origin newSid=$newSessionId")
        return newSession
    }

    companion object {

        val SUPERSEDED_TTL = 48.hours
    }
}
