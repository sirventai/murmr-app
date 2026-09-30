package uoc.edu.psirvent.murmr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import uoc.edu.psirvent.murmr.data.mapper.toDomain
import uoc.edu.psirvent.murmr.db.MurmrDatabase
import uoc.edu.psirvent.murmr.domain.model.Message
import uoc.edu.psirvent.murmr.domain.model.MessageStatus

class MessageRepository(
    private val db: MurmrDatabase,
    private val io: CoroutineDispatcher = Dispatchers.Default,
) {
    private val queries get() = db.messagesQueries

    suspend fun getById(messageId: String): Message? = withContext(io) {
        queries.selectById(messageId).executeAsOneOrNull()?.toDomain()
    }

    fun observeBySession(sessionId: String): Flow<List<Message>> =
        queries.selectBySession(sessionId)
            .asFlow()
            .mapToList(io)
            .map { list -> list.map { it.toDomain() } }

    fun observeByContact(contactId: String): Flow<List<Message>> =
        queries.selectByContact(contactId)
            .asFlow()
            .mapToList(io)
            .map { list -> list.map { it.toDomain() } }

    fun observeRecentByContact(contactId: String, limit: Long = 50L): Flow<List<Message>> =
        queries.selectByContactRecent(contactId, limit)
            .asFlow()
            .mapToList(io)

            .map { list -> list.map { it.toDomain() }.asReversed() }

    suspend fun loadBefore(contactId: String, beforeMs: Long, limit: Long = 50L): List<Message> = withContext(io) {

        queries.selectByContactBefore(contactId, beforeMs, limit)
            .executeAsList()
            .map { it.toDomain() }
            .asReversed()
    }

    suspend fun countTextByContact(contactId: String): Long = withContext(io) {
        queries.countTextByContact(contactId).executeAsOne()
    }

    fun observeLastByContact(contactId: String): Flow<Message?> =
        queries.selectLastByContact(contactId)
            .asFlow()
            .mapToOneOrNull(io)
            .map { it?.toDomain() }

    suspend fun getLastByContact(contactId: String): Message? = withContext(io) {
        queries.selectLastByContact(contactId).executeAsOneOrNull()?.toDomain()
    }

    suspend fun insert(message: Message) = withContext(io) {
        queries.insert(
            messageId = message.messageId,
            sessionId = message.sessionId,
            senderDeviceId = message.senderDeviceId,
            ciphertext = message.ciphertext,
            iv = message.iv,
            authTag = message.authTag,
            plaintextLocal = message.plaintextLocal,
            timestamp = message.timestamp.toEpochMilliseconds(),
            status = message.status.name,
            isOutgoing = if (message.isOutgoing) 1L else 0L,
            type = message.type.name,
            replyToMessageId = message.replyToMessageId,
            edited = if (message.edited) 1L else 0L,
            readReceiptSentAt = message.readReceiptSentAt?.toEpochMilliseconds(),
        )
    }

    suspend fun updateStatus(messageId: String, status: MessageStatus) = withContext(io) {
        queries.updateStatus(status = status.name, messageId = messageId)
    }

    suspend fun updateStatusForIds(messageIds: Collection<String>, status: MessageStatus) = withContext(io) {
        if (messageIds.isEmpty()) return@withContext
        queries.updateStatusForIds(status.name, messageIds.toList())
    }

    suspend fun markEdited(messageId: String, newPlaintext: String) = withContext(io) {
        queries.markEdited(plaintextLocal = newPlaintext, messageId = messageId)
    }

    suspend fun markReadReceiptSent(messageId: String, ts: kotlin.time.Instant = kotlin.time.Clock.System.now()) = withContext(io) {
        queries.markReadReceiptSent(ts = ts.toEpochMilliseconds(), messageId = messageId)
    }

    suspend fun selectIncomingUnreadBySession(sessionId: String): List<Message> = withContext(io) {
        queries.selectIncomingUnreadBySession(sessionId).executeAsList().map { it.toDomain() }
    }

    suspend fun countOutgoingForSession(sessionId: String): Int = withContext(io) {
        queries.countOutgoingBySession(sessionId).executeAsOne().toInt()
    }

    suspend fun deleteByContact(contactId: String) = withContext(io) {
        queries.deleteByContact(contactId)
    }

    suspend fun deleteBySession(sessionId: String) = withContext(io) {
        queries.deleteBySession(sessionId)
    }

    suspend fun deleteAll() = withContext(io) {
        queries.deleteAll()
    }
}
