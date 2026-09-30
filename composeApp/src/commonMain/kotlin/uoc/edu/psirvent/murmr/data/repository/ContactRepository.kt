package uoc.edu.psirvent.murmr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
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
import uoc.edu.psirvent.murmr.domain.model.Contact
import uoc.edu.psirvent.murmr.domain.model.ContactStatus
import uoc.edu.psirvent.murmr.domain.model.SessionExpiry

class ContactRepository(
    private val db: MurmrDatabase,
    private val io: CoroutineDispatcher = Dispatchers.Default,
    private val clock: Clock = Clock.System,
) {
    private val queries get() = db.contactsQueries

    fun observeAll(ownerDeviceId: String): Flow<List<Contact>> =
        queries.selectAll(ownerDeviceId)
            .asFlow()
            .mapToList(io)
            .map { list -> list.map { it.toDomain() } }

    fun observeByRelay(ownerDeviceId: String, relayUrl: String): Flow<List<Contact>> =
        queries.selectAllByRelay(ownerDeviceId, relayUrl)
            .asFlow()
            .mapToList(io)
            .map { list -> list.map { it.toDomain() } }

    fun observeById(contactId: String): Flow<Contact?> =
        queries.selectById(contactId)
            .asFlow()
            .mapToOneOrNull(io)
            .map { it?.toDomain() }

    suspend fun getById(contactId: String): Contact? = withContext(io) {
        queries.selectById(contactId).executeAsOneOrNull()?.toDomain()
    }

    suspend fun getByRemote(owner: String, remote: String): Contact? = withContext(io) {
        queries.selectByRemote(owner = owner, remote = remote).executeAsOneOrNull()?.toDomain()
    }

    suspend fun insert(contact: Contact) = withContext(io) {
        queries.insert(
            contactId = contact.contactId,
            ownerDeviceId = contact.ownerDeviceId,
            remoteDeviceId = contact.remoteDeviceId,
            remotePublicKey = contact.remotePublicKey,
            displayAlias = contact.displayAlias,
            status = contact.status.name,
            pairedAt = contact.pairedAt.toEpochMilliseconds(),
            lastMessageAt = contact.lastMessageAt?.toEpochMilliseconds(),
            unreadCount = contact.unreadCount.toLong(),
            lastAliasSyncedAt = null,
            avatar = null,
            relayUrl = contact.relayUrl,
            localSessionExpiry = contact.localSessionExpiry?.name,
            localCustomExpirySeconds = contact.localCustomExpirySeconds,
        )
    }

    suspend fun updateAlias(contactId: String, alias: String) = withContext(io) {
        queries.updateAlias(alias = alias, contactId = contactId)
    }

    suspend fun updateAliasFromPeer(contactId: String, alias: String, ts: Instant = clock.now()) = withContext(io) {
        queries.updateAliasSynced(alias = alias, ts = ts.toEpochMilliseconds(), contactId = contactId)
    }

    suspend fun updateAvatarFromPeer(contactId: String, avatar: ByteArray?) = withContext(io) {

        queries.updateAvatarRemote(avatar = avatar, contactId = contactId)
    }

    suspend fun updatePeerExpiry(
        contactId: String,
        peerSessionExpiry: SessionExpiry,
        peerCustomExpirySeconds: Long,
    ) = withContext(io) {
        queries.updatePeerExpiry(
            peerSessionExpiry = peerSessionExpiry.name,
            peerCustomExpirySeconds = peerCustomExpirySeconds,
            contactId = contactId,
        )
    }

    suspend fun setLocalExpiry(
        contactId: String,
        value: SessionExpiry?,
        customSeconds: Long,
    ) = withContext(io) {
        if (value == null) {
            queries.clearLocalExpiry(contactId)
        } else {
            queries.updateLocalExpiry(
                localSessionExpiry = value.name,
                localCustomExpirySeconds = customSeconds,
                contactId = contactId,
            )
        }
    }

    suspend fun updateAvatarBigFromPeer(contactId: String, avatarBig: ByteArray?) = withContext(io) {
        queries.updateAvatarBigRemote(avatarBig = avatarBig, contactId = contactId)
    }

    suspend fun allFor(ownerDeviceId: String): List<Contact> = withContext(io) {
        queries.selectAll(ownerDeviceId).executeAsList().map { it.toDomain() }
    }

    fun observeTotalUnread(): Flow<Int> =
        queries.totalUnread()
            .asFlow()
            .mapToOne(io)
            .map { (it as? Long)?.toInt() ?: 0 }

    suspend fun updateStatus(contactId: String, status: ContactStatus) = withContext(io) {
        queries.updateStatus(status = status.name, contactId = contactId)
    }

    suspend fun touchLastMessage(contactId: String, ts: Instant = clock.now()) = withContext(io) {
        queries.touchLastMessage(ts = ts.toEpochMilliseconds(), contactId = contactId)
    }

    suspend fun incrementUnread(contactId: String, ts: Instant = clock.now()) = withContext(io) {
        queries.incrementUnread(ts = ts.toEpochMilliseconds(), contactId = contactId)
    }

    suspend fun resetUnread(contactId: String) = withContext(io) {
        queries.resetUnread(contactId)
    }

    suspend fun delete(contactId: String) = withContext(io) {
        queries.deleteById(contactId)
    }

    suspend fun deleteAll() = withContext(io) {
        queries.deleteAll()
    }
}
