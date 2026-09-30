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
import uoc.edu.psirvent.murmr.domain.model.BlockedContact

class BlockedContactRepository(
    private val db: MurmrDatabase,
    private val io: CoroutineDispatcher = Dispatchers.Default,
) {
    private val queries get() = db.blockedContactsQueries

    fun observeAll(owner: String): Flow<List<BlockedContact>> =
        queries.selectAll(owner)
            .asFlow()
            .mapToList(io)
            .map { list -> list.map { it.toDomain() } }

    suspend fun isBlocked(owner: String, remote: String): Boolean = withContext(io) {
        queries.isBlocked(owner = owner, remote = remote).executeAsOne()
    }

    suspend fun block(item: BlockedContact) = withContext(io) {
        queries.insert(
            blockId = item.blockId,
            ownerDeviceId = item.ownerDeviceId,
            blockedDeviceId = item.blockedDeviceId,
            blockedAt = item.blockedAt.toEpochMilliseconds(),
            reason = item.reason,
        )
    }

    suspend fun unblock(owner: String, remote: String) = withContext(io) {
        queries.deleteByDevice(owner = owner, remote = remote)
    }

    suspend fun deleteAll() = withContext(io) {
        queries.deleteAll()
    }
}
