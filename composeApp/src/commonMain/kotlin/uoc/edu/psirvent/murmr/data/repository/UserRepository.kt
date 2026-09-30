package uoc.edu.psirvent.murmr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import uoc.edu.psirvent.murmr.data.mapper.toDomain
import uoc.edu.psirvent.murmr.db.MurmrDatabase
import uoc.edu.psirvent.murmr.domain.model.User

class UserRepository(
    private val db: MurmrDatabase,
    private val io: CoroutineDispatcher = Dispatchers.Default,
) {
    private val queries get() = db.usersQueries

    fun observeLocalUser(): Flow<User?> =
        queries.selectLocal()
            .asFlow()
            .mapToOneOrNull(io)
            .map { row -> row?.toDomain() }

    suspend fun getLocalUser(): User? = withContext(io) {
        queries.selectLocal().executeAsOneOrNull()?.toDomain()
    }

    suspend fun getById(deviceId: String): User? = withContext(io) {
        queries.selectById(deviceId).executeAsOneOrNull()?.toDomain()
    }

    suspend fun upsert(user: User) = withContext(io) {
        queries.upsert(
            deviceId = user.deviceId,
            alias = user.alias,
            createdAt = user.createdAt.toEpochMilliseconds(),
            publicKey = user.publicKey,
            privateKey = user.privateKey,
            avatar = user.avatar,
            avatarBig = user.avatarBig,
        )
    }

    suspend fun updateAlias(deviceId: String, alias: String) = withContext(io) {
        queries.updateAlias(alias = alias, deviceId = deviceId)
    }

    suspend fun updateAvatar(deviceId: String, avatar: ByteArray?) = withContext(io) {
        queries.updateAvatar(avatar = avatar, deviceId = deviceId)
    }

    suspend fun updateAvatarBig(deviceId: String, avatarBig: ByteArray?) = withContext(io) {
        queries.updateAvatarBig(avatarBig = avatarBig, deviceId = deviceId)
    }

    suspend fun deleteAll() = withContext(io) {
        queries.deleteAll()
    }
}
