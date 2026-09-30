package uoc.edu.psirvent.murmr.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import uoc.edu.psirvent.murmr.domain.model.Contact
import uoc.edu.psirvent.murmr.domain.model.ContactStatus

class ContactRepositoryTest {

    private fun sampleContact(id: String, owner: String = "owner-1", remote: String = "remote-${id}"): Contact = Contact(
        contactId = id,
        ownerDeviceId = owner,
        remoteDeviceId = remote,
        remotePublicKey = ByteArray(32) { 0x42 },
        displayAlias = "alias-$id",
        status = ContactStatus.PENDING,
        pairedAt = Clock.System.now(),
        lastMessageAt = null,
        unreadCount = 0,
    )

    @Test
    fun insertAndFetch() = runTest {
        val repo = ContactRepository(newInMemoryDatabase(), Dispatchers.Unconfined)
        val c = sampleContact("c1")
        repo.insert(c)
        val fetched = repo.getById("c1")
        assertEquals("c1", fetched?.contactId)
        assertEquals(ContactStatus.PENDING, fetched?.status)
    }

    @Test
    fun updateStatusAndUnread() = runTest {
        val repo = ContactRepository(newInMemoryDatabase(), Dispatchers.Unconfined)
        repo.insert(sampleContact("c1"))
        repo.updateStatus("c1", ContactStatus.VERIFIED)
        assertEquals(ContactStatus.VERIFIED, repo.getById("c1")?.status)

        repo.incrementUnread("c1")
        repo.incrementUnread("c1")
        assertEquals(2, repo.getById("c1")?.unreadCount)

        repo.resetUnread("c1")
        assertEquals(0, repo.getById("c1")?.unreadCount)
    }

    @Test
    fun listOrderByLastMessage() = runTest {
        val repo = ContactRepository(newInMemoryDatabase(), Dispatchers.Unconfined)
        repo.insert(sampleContact("oldest"))
        repo.insert(sampleContact("middle"))
        repo.insert(sampleContact("recent"))

        repo.touchLastMessage("middle")
        repo.touchLastMessage("recent")

        val ordered = repo.observeAll("owner-1").first().map { it.contactId }
        assertTrue(ordered.indexOf("oldest") > ordered.indexOf("middle"))
        assertTrue(ordered.indexOf("oldest") > ordered.indexOf("recent"))
    }

    @Test
    fun deleteCascade() = runTest {
        val repo = ContactRepository(newInMemoryDatabase(), Dispatchers.Unconfined)
        repo.insert(sampleContact("c1"))
        repo.delete("c1")
        assertNull(repo.getById("c1"))
    }
}
