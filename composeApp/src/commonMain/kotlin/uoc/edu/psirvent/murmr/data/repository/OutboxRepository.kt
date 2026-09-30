package uoc.edu.psirvent.murmr.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant
import uoc.edu.psirvent.murmr.db.MurmrDatabase

class OutboxRepository(
    private val db: MurmrDatabase,
    private val io: CoroutineDispatcher = Dispatchers.Default,
    private val clock: Clock = Clock.System,
) {
    private val queries get() = db.outboxQueries

    suspend fun enqueue(messageId: String, payloadJson: String, nextRetryAt: Instant = clock.now()) = withContext(io) {
        queries.upsert(
            messageId = messageId,
            payloadJson = payloadJson,
            attempts = 0L,
            nextRetryAt = nextRetryAt.toEpochMilliseconds(),
            createdAt = clock.now().toEpochMilliseconds(),
            lastError = null,
        )
    }

    data class Entry(
        val messageId: String,
        val payloadJson: String,
        val attempts: Int,
        val nextRetryAt: Instant,
        val createdAt: Instant,
        val lastError: String? = null,
    )

    suspend fun due(now: Instant = clock.now(), limit: Int = 32): List<Entry> = withContext(io) {
        queries.selectDue(now.toEpochMilliseconds(), limit.toLong())
            .executeAsList()
            .map {
                Entry(
                    messageId = it.message_id,
                    payloadJson = it.payload_json,
                    attempts = it.attempts.toInt(),
                    nextRetryAt = Instant.fromEpochMilliseconds(it.next_retry_at),
                    createdAt = Instant.fromEpochMilliseconds(it.created_at),
                    lastError = it.last_error,
                )
            }
    }

    suspend fun bumpAttempts(
        messageId: String,
        nextRetryAt: Instant,
        lastError: String? = null,
    ) = withContext(io) {
        queries.bumpAttempts(
            nextRetryAt = nextRetryAt.toEpochMilliseconds(),
            messageId = messageId,
            lastError = lastError?.take(200),
        )
    }

    suspend fun delete(messageId: String) = withContext(io) {
        queries.deleteById(messageId)
    }

    suspend fun count(): Int = withContext(io) {
        queries.count().executeAsOne().toInt()
    }

    suspend fun clear() = withContext(io) {
        queries.deleteAll()
    }
}
