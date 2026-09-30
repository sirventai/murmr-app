package uoc.edu.psirvent.murmr.data.mapper

import kotlin.time.Instant
import uoc.edu.psirvent.murmr.db.Blocked_contacts
import uoc.edu.psirvent.murmr.db.Contacts
import uoc.edu.psirvent.murmr.db.Messages
import uoc.edu.psirvent.murmr.db.Pairing_log
import uoc.edu.psirvent.murmr.db.Sessions
import uoc.edu.psirvent.murmr.db.Settings
import uoc.edu.psirvent.murmr.db.Users
import uoc.edu.psirvent.murmr.domain.model.AppSettings
import uoc.edu.psirvent.murmr.domain.model.AutoLockTimeout
import uoc.edu.psirvent.murmr.domain.model.BlockedContact
import uoc.edu.psirvent.murmr.domain.model.Contact
import uoc.edu.psirvent.murmr.domain.model.ContactStatus
import uoc.edu.psirvent.murmr.domain.model.Message
import uoc.edu.psirvent.murmr.domain.model.MessageStatus
import uoc.edu.psirvent.murmr.domain.model.MessageType
import uoc.edu.psirvent.murmr.domain.model.PairingEvent
import uoc.edu.psirvent.murmr.domain.model.PairingLogEntry
import uoc.edu.psirvent.murmr.domain.model.Session
import uoc.edu.psirvent.murmr.domain.model.SessionExpiry
import uoc.edu.psirvent.murmr.domain.model.SessionStatus
import uoc.edu.psirvent.murmr.domain.model.User
import uoc.edu.psirvent.murmr.util.Logger

private inline fun <reified E : Enum<E>> safeEnum(value: String, default: E, field: String): E =
    runCatching { enumValueOf<E>(value) }.getOrElse {
        Logger.w("Mappers", "Valor desconocido para $field: '$value', default=$default")
        default
    }

internal fun Users.toDomain(): User = User(
    deviceId = device_id,
    alias = alias,
    publicKey = public_key,
    privateKey = private_key,
    createdAt = Instant.fromEpochMilliseconds(created_at),
    avatar = avatar,
    avatarBig = avatar_big,
)

internal fun Contacts.toDomain(): Contact = Contact(
    contactId = contact_id,
    ownerDeviceId = owner_device_id,
    remoteDeviceId = remote_device_id,
    remotePublicKey = remote_public_key,
    displayAlias = display_alias,
    status = safeEnum(status, ContactStatus.UNVERIFIED, "Contact.status"),
    pairedAt = Instant.fromEpochMilliseconds(paired_at),
    lastMessageAt = last_message_at?.let(Instant::fromEpochMilliseconds),
    unreadCount = unread_count.toInt(),
    avatar = avatar,
    avatarBig = avatar_big,
    relayUrl = relay_url,
    peerSessionExpiry = safeEnum(peer_session_expiry, SessionExpiry.NEVER, "Contact.peer_session_expiry"),
    peerCustomExpirySeconds = peer_custom_expiry_seconds,
    localSessionExpiry = local_session_expiry?.let {
        runCatching { SessionExpiry.valueOf(it) }.getOrNull()
    },
    localCustomExpirySeconds = local_custom_expiry_seconds,
)

internal fun Sessions.toDomain(): Session = Session(
    sessionId = session_id,
    contactId = contact_id,
    sessionKeyRef = session_key_ref,
    createdAt = Instant.fromEpochMilliseconds(created_at),
    expiresAt = expires_at?.let(Instant::fromEpochMilliseconds),
    sasCode = sas_code,
    fingerprint = fingerprint,
    status = safeEnum(status, SessionStatus.BROKEN, "Session.status"),
    rotationIndex = rotation_index.toInt(),
    previousSessionId = previous_session_id,
    renewedAt = renewed_at?.let(Instant::fromEpochMilliseconds),
)

internal fun Messages.toDomain(): Message = Message(
    messageId = message_id,
    sessionId = session_id,
    senderDeviceId = sender_device_id,
    ciphertext = ciphertext,
    iv = iv,
    authTag = auth_tag,
    plaintextLocal = plaintext_local,
    timestamp = Instant.fromEpochMilliseconds(timestamp),
    status = safeEnum(status, MessageStatus.FAILED, "Message.status"),
    isOutgoing = is_outgoing == 1L,
    type = safeEnum(type, MessageType.TEXT, "Message.type"),
    replyToMessageId = reply_to_message_id,
    edited = edited == 1L,
    readReceiptSentAt = read_receipt_sent_at?.let(Instant::fromEpochMilliseconds),
)

internal fun Settings.toDomain(): AppSettings = AppSettings(
    deviceId = device_id,
    sessionExpiry = safeEnum(session_expiry, SessionExpiry.NEVER, "Settings.session_expiry"),
    biometricLock = biometric_lock == 1L,
    autoLockTimeout = safeEnum(auto_lock_timeout, AutoLockTimeout.NEVER, "Settings.auto_lock_timeout"),
    notificationsEnabled = notifications_enabled == 1L,
    onboardingDone = onboarding_done == 1L,
    appVersion = app_version,
    updatedAt = Instant.fromEpochMilliseconds(updated_at),
    customExpirySeconds = custom_expiry_seconds,
    customAutoLockSeconds = custom_autolock_secs.toInt(),
    onboardingStep = onboarding_step.toInt(),
)

internal fun Blocked_contacts.toDomain(): BlockedContact = BlockedContact(
    blockId = block_id,
    ownerDeviceId = owner_device_id,
    blockedDeviceId = blocked_device_id,
    blockedAt = Instant.fromEpochMilliseconds(blocked_at),
    reason = reason,
)

internal fun Pairing_log.toDomain(): PairingLogEntry = PairingLogEntry(
    logId = log_id,
    contactId = contact_id,
    eventType = safeEnum(event_type, PairingEvent.PAIRED, "PairingLog.event_type"),
    timestamp = Instant.fromEpochMilliseconds(timestamp),
    details = details,
)
