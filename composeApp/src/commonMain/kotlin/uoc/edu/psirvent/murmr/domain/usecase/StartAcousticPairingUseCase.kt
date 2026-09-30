package uoc.edu.psirvent.murmr.domain.usecase

import com.benasher44.uuid.uuid4
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.audio.PairingOutcome
import uoc.edu.psirvent.murmr.crypto.deviceIdFromPublicKey
import uoc.edu.psirvent.murmr.data.network.RelayConfigStore
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.PairingLogRepository
import uoc.edu.psirvent.murmr.data.repository.SessionRepository
import uoc.edu.psirvent.murmr.data.repository.SettingsRepository
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.model.Contact
import uoc.edu.psirvent.murmr.domain.model.ContactStatus
import uoc.edu.psirvent.murmr.domain.model.PairingEvent
import uoc.edu.psirvent.murmr.domain.model.PairingLogEntry
import uoc.edu.psirvent.murmr.domain.model.Session
import uoc.edu.psirvent.murmr.domain.model.SessionExpiry
import uoc.edu.psirvent.murmr.domain.model.SessionStatus
import uoc.edu.psirvent.murmr.util.ApplicationScope
import uoc.edu.psirvent.murmr.util.Logger

class StartAcousticPairingUseCase(
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val sessionRepository: SessionRepository,
    private val pairingLogRepository: PairingLogRepository,
    private val settingsRepository: SettingsRepository,
    private val applicationScope: ApplicationScope,
    private val clock: Clock = Clock.System,
    private val broadcastProfileUpdate: BroadcastProfileUpdateUseCase? = null,
    private val relayConfigStore: RelayConfigStore? = null,
    private val broadcastExpiryUpdate: BroadcastExpiryUpdateUseCase? = null,
) {

    suspend fun confirm(outcome: PairingOutcome): Pair<Contact, Session> {
        val me = userRepository.getLocalUser() ?: error("Usuario local no inicializado")
        val now = clock.now()

        val remoteDeviceId = deviceIdFromPublicKey(outcome.remotePublicKey)

        if (remoteDeviceId == me.deviceId) {
            Logger.w("PairingConfirm", "self-pairing detectado (peer=me=${me.deviceId}), ABORTANDO")
            error("No puedes emparejarte contigo mismo")
        }
        val existing = contactRepository.getByRemote(me.deviceId, remoteDeviceId)
        val contactId = existing?.contactId ?: uuid4().toString()
        Logger.i(
            "PairingConfirm",
            "me=${me.deviceId} alias='${me.alias}' | peerAlias='${outcome.remoteAlias}' " +
                "peerDeviceId=$remoteDeviceId (derivado de pubkey ${outcome.remotePublicKey.size}B) | " +
                "existing=${existing != null} -> contactId=$contactId",
        )
        val currentRelay = relayConfigStore?.current()?.baseUrl.orEmpty()

        val mySettings = settingsRepository.getFirst()
        val snapshotExpiry = mySettings?.sessionExpiry
        val snapshotCustomSeconds = mySettings?.customExpirySeconds ?: 0L
        val contact = Contact(
            contactId = contactId,
            ownerDeviceId = me.deviceId,
            remoteDeviceId = remoteDeviceId,
            remotePublicKey = outcome.remotePublicKey,
            displayAlias = outcome.remoteAlias,
            status = ContactStatus.UNVERIFIED,
            pairedAt = now,
            lastMessageAt = existing?.lastMessageAt,
            unreadCount = 0,
            relayUrl = currentRelay,
            localSessionExpiry = snapshotExpiry,
            localCustomExpirySeconds = snapshotCustomSeconds,
        )
        if (existing == null) {
            contactRepository.insert(contact)
        } else {
            contactRepository.updateAlias(contactId, outcome.remoteAlias)

            contactRepository.updateStatus(contactId, ContactStatus.UNVERIFIED)
        }

        val expiresAt = settingsRepository.getFirst()?.let { settings ->
            when (settings.sessionExpiry) {
                SessionExpiry.NEVER, SessionExpiry.CUSTOM -> null
                SessionExpiry.ONE_DAY -> now.plus(1.days)
                SessionExpiry.SEVEN_DAYS -> now.plus(7.days)
                SessionExpiry.THIRTY_DAYS -> now.plus(30.days)
            }
        }

        if (existing != null) {
            sessionRepository.getNonRevokedForContact(contactId).forEach { prev ->
                sessionRepository.updateStatus(prev.sessionId, SessionStatus.REVOKED)
            }
        }

        val session = Session(
            sessionId = outcome.sessionId,
            contactId = contactId,
            sessionKeyRef = outcome.sessionKeyRef,
            createdAt = now,
            expiresAt = expiresAt,
            sasCode = outcome.sasEncoded,
            fingerprint = outcome.fingerprint,
            status = SessionStatus.ACTIVE,
            rotationIndex = 0,
            previousSessionId = null,
        )
        sessionRepository.insert(session)

        pairingLogRepository.insert(
            PairingLogEntry(
                logId = uuid4().toString(),
                contactId = contactId,
                eventType = if (existing == null) PairingEvent.PAIRED else PairingEvent.REKEYED,
                timestamp = now,
                details = "SAS: ${outcome.sasEncoded}",
            ),
        )

        applicationScope.launch {
            broadcastProfileUpdate?.let { broadcast ->
                runCatching {
                    broadcast(BroadcastProfileUpdateUseCase.Kind.ALIAS, targetContactId = contactId)
                    broadcast(BroadcastProfileUpdateUseCase.Kind.AVATAR, targetContactId = contactId)
                }.onFailure { Logger.w("PairingConfirm", "broadcast profile fallo", it) }
            }
            broadcastExpiryUpdate?.let { broadcast ->
                runCatching { broadcast(targetContactId = contactId) }
                    .onFailure { Logger.w("PairingConfirm", "broadcast expiry fallo", it) }
            }
        }

        return contact to session
    }
}

