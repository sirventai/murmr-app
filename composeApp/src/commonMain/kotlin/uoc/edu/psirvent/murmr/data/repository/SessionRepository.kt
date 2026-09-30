package uoc.edu.psirvent.murmr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Instant
import uoc.edu.psirvent.murmr.data.mapper.toDomain
import uoc.edu.psirvent.murmr.db.MurmrDatabase
import uoc.edu.psirvent.murmr.domain.model.Session
import uoc.edu.psirvent.murmr.domain.model.SessionStatus

class SessionRepository(
    private val db: MurmrDatabase,
    private val io: CoroutineDispatcher = Dispatchers.Default,
) {
    private val queries get() = db.sessionsQueries

    fun observeAllForContact(contactId: String): Flow<List<Session>> =
        queries.selectAllForContact(contactId)
            .asFlow()
            .mapToList(io)
            .map { list -> list.map { it.toDomain() } }

    fun observeActive(contactId: String): Flow<Session?> =
        queries.selectActiveForContact(contactId)
            .asFlow()
            .mapToOneOrNull(io)
            .map { it?.toDomain() }

    suspend fun getActive(contactId: String): Session? = withContext(io) {
        queries.selectActiveForContact(contactId).executeAsOneOrNull()?.toDomain()
    }

    suspend fun getById(sessionId: String): Session? = withContext(io) {
        queries.selectById(sessionId).executeAsOneOrNull()?.toDomain()
    }

    suspend fun insert(session: Session) = withContext(io) {
        queries.insert(
            sessionId = session.sessionId,
            contactId = session.contactId,
            sessionKeyRef = session.sessionKeyRef,
            createdAt = session.createdAt.toEpochMilliseconds(),
            expiresAt = session.expiresAt?.toEpochMilliseconds(),
            sasCode = session.sasCode,
            fingerprint = session.fingerprint,
            status = session.status.name,
            rotationIndex = session.rotationIndex.toLong(),
            previousSessionId = session.previousSessionId,
        )
    }

    suspend fun getNonRevokedForContact(contactId: String): List<Session> = withContext(io) {
        queries.selectByContactNonRevoked(contactId).executeAsList().map { it.toDomain() }
    }

    suspend fun updateStatus(sessionId: String, status: SessionStatus) = withContext(io) {
        queries.updateStatus(status = status.name, sessionId = sessionId)
    }

    suspend fun updateExpiry(sessionId: String, expiresAt: Instant?) = withContext(io) {
        queries.updateExpiry(
            expiresAt = expiresAt?.toEpochMilliseconds(),
            sessionId = sessionId,
        )
    }

    suspend fun markRenewed(sessionId: String, ts: Instant) = withContext(io) {
        queries.markRenewed(renewedAt = ts.toEpochMilliseconds(), sessionId = sessionId)
    }

    suspend fun delete(sessionId: String) = withContext(io) {
        queries.deleteById(sessionId)
    }

    suspend fun deleteAllForContact(contactId: String) = withContext(io) {
        queries.deleteAllForContact(contactId)
    }
}
