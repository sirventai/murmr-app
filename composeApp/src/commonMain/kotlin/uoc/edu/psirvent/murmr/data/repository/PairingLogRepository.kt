package uoc.edu.psirvent.murmr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import uoc.edu.psirvent.murmr.data.mapper.toDomain
import uoc.edu.psirvent.murmr.db.MurmrDatabase
import uoc.edu.psirvent.murmr.domain.model.PairingLogEntry

class PairingLogRepository(
    private val db: MurmrDatabase,
    private val io: CoroutineDispatcher = Dispatchers.Default,
) {
    private val queries get() = db.pairingLogQueries

    fun observeForContact(contactId: String): Flow<List<PairingLogEntry>> =
        queries.selectAllForContact(contactId)
            .asFlow()
            .mapToList(io)
            .map { list -> list.map { it.toDomain() } }

    suspend fun insert(entry: PairingLogEntry) = withContext(io) {
        queries.insert(
            logId = entry.logId,
            contactId = entry.contactId,
            eventType = entry.eventType.name,
            timestamp = entry.timestamp.toEpochMilliseconds(),
            details = entry.details,
        )
    }

    suspend fun deleteForContact(contactId: String) = withContext(io) {
        queries.deleteByContact(contactId)
    }

    suspend fun deleteAll() = withContext(io) {
        queries.deleteAll()
    }
}
