package uoc.edu.psirvent.murmr.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import uoc.edu.psirvent.murmr.domain.model.User

class UserRepositoryTest {

    @Test
    fun upsertAndQuery() = runTest {
        val repo = UserRepository(newInMemoryDatabase(), Dispatchers.Unconfined)

        assertNull(repo.getLocalUser())

        val user = User(
            deviceId = "dev-1",
            alias = "Pablo",
            publicKey = ByteArray(32) { 0x01 },
            privateKey = ByteArray(32) { 0x02 },
            createdAt = Clock.System.now(),
        )
        repo.upsert(user)

        val loaded = repo.getLocalUser()
        assertNotNull(loaded)
        assertEquals("dev-1", loaded.deviceId)
        assertEquals("Pablo", loaded.alias)
        assertContentEquals(user.publicKey, loaded.publicKey)
        assertContentEquals(user.privateKey, loaded.privateKey)
    }

    @Test
    fun updateAlias() = runTest {
        val repo = UserRepository(newInMemoryDatabase(), Dispatchers.Unconfined)
        val user = User("d", "Old", ByteArray(32), ByteArray(32), Clock.System.now())
        repo.upsert(user)
        repo.updateAlias("d", "NewAlias")
        assertEquals("NewAlias", repo.getById("d")?.alias)
    }

    @Test
    fun deleteAllClears() = runTest {
        val repo = UserRepository(newInMemoryDatabase(), Dispatchers.Unconfined)
        repo.upsert(User("d", "x", ByteArray(32), ByteArray(32), Clock.System.now()))
        repo.deleteAll()
        assertNull(repo.getLocalUser())
    }
}
