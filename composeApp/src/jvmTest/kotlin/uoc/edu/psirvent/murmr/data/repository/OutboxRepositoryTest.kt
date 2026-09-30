package uoc.edu.psirvent.murmr.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Clock
import kotlin.time.Instant

class OutboxRepositoryTest {

    @Test
    fun enqueueAndCount() = runTest {
        val db = newInMemoryDatabase()
        val repo = OutboxRepository(db)
        repo.enqueue("msg-1", "{\"foo\":1}")
        repo.enqueue("msg-2", "{\"foo\":2}")
        assertEquals(2, repo.count())
    }

    @Test
    fun dueOnlyReturnsEntriesPastNextRetry() = runTest {
        val db = newInMemoryDatabase()
        val now = Clock.System.now()
        val repo = OutboxRepository(db, clock = FixedClock(now))
        repo.enqueue("now-1", "{}", nextRetryAt = now)
        repo.enqueue("future-1", "{}", nextRetryAt = now + 5.minutes)
        val due = repo.due(now = now)
        assertEquals(1, due.size)
        assertEquals("now-1", due.first().messageId)
    }

    @Test
    fun deleteRemovesEntry() = runTest {
        val db = newInMemoryDatabase()
        val repo = OutboxRepository(db)
        repo.enqueue("msg-1", "{}")
        assertEquals(1, repo.count())
        repo.delete("msg-1")
        assertEquals(0, repo.count())
    }

    @Test
    fun bumpAttemptsIncrementsAndPostpones() = runTest {
        val db = newInMemoryDatabase()
        val now = Clock.System.now()
        val later = now + 1.minutes
        val repo = OutboxRepository(db)
        repo.enqueue("msg-1", "{}", nextRetryAt = now)
        repo.bumpAttempts("msg-1", nextRetryAt = later)

        val due = repo.due(now = now)
        assertTrue(due.none { it.messageId == "msg-1" }, "Tras bump, no debería estar due en `now`")

        val dueLater = repo.due(now = later)
        assertEquals(1, dueLater.size)
        assertEquals(1, dueLater.first().attempts, "attempts debería incrementar")
    }

    @Test
    fun upsertReplacesExisting() = runTest {
        val db = newInMemoryDatabase()
        val repo = OutboxRepository(db)
        repo.enqueue("msg-1", "{\"v\":1}")
        repo.enqueue("msg-1", "{\"v\":2}")
        assertEquals(1, repo.count(), "Mismo messageId no debe duplicar")
        val due = repo.due(now = Clock.System.now())
        assertEquals("{\"v\":2}", due.first().payloadJson)
    }

    private class FixedClock(private val now: Instant) : Clock {
        override fun now(): Instant = now
    }
}
