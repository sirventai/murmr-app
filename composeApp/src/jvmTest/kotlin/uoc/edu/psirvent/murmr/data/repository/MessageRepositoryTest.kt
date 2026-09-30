package uoc.edu.psirvent.murmr.data.repository

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import uoc.edu.psirvent.murmr.domain.model.Contact
import uoc.edu.psirvent.murmr.domain.model.ContactStatus
import uoc.edu.psirvent.murmr.domain.model.Message
import uoc.edu.psirvent.murmr.domain.model.MessageStatus
import uoc.edu.psirvent.murmr.domain.model.Session
import uoc.edu.psirvent.murmr.domain.model.SessionStatus

class MessageRepositoryTest {

    @Test
    fun insertAndListBySession() = runTest {
        val db = newInMemoryDatabase()
        val contacts = ContactRepository(db, Dispatchers.Unconfined)
        val sessions = SessionRepository(db, Dispatchers.Unconfined)
        val messages = MessageRepository(db, Dispatchers.Unconfined)
        val now = Clock.System.now()

        contacts.insert(
            Contact(
                contactId = "c1",
                ownerDeviceId = "me",
                remoteDeviceId = "them",
                remotePublicKey = ByteArray(32),
                displayAlias = "Bob",
                status = ContactStatus.VERIFIED,
                pairedAt = now,
                lastMessageAt = null,
                unreadCount = 0,
            ),
        )
        sessions.insert(
            Session(
                sessionId = "s1",
                contactId = "c1",
                sessionKeyRef = "ref1",
                createdAt = now,
                expiresAt = null,
                sasCode = "SAS",
                fingerprint = "fp",
                status = SessionStatus.ACTIVE,
            ),
        )

        val payload1 = Message(
            messageId = "m1",
            sessionId = "s1",
            senderDeviceId = "me",
            ciphertext = byteArrayOf(1, 2, 3),
            iv = ByteArray(12),
            authTag = ByteArray(16),
            plaintextLocal = "hola",
            timestamp = now,
            status = MessageStatus.SENT,
            isOutgoing = true,
        )
        val payload2 = payload1.copy(
            messageId = "m2",
            plaintextLocal = "adiós",
            timestamp = now.plus(1.seconds),
            isOutgoing = false,
            status = MessageStatus.DELIVERED,
        )

        messages.insert(payload1)
        messages.insert(payload2)

        val sessionMessages = messages.observeBySession("s1").first()
        assertEquals(2, sessionMessages.size)
        assertEquals("hola", sessionMessages[0].plaintextLocal)
        assertEquals("adiós", sessionMessages[1].plaintextLocal)
        assertContentEquals(byteArrayOf(1, 2, 3), sessionMessages[0].ciphertext)
    }

    @Test
    fun updateStatus() = runTest {
        val db = newInMemoryDatabase()
        val contacts = ContactRepository(db, Dispatchers.Unconfined)
        val sessions = SessionRepository(db, Dispatchers.Unconfined)
        val messages = MessageRepository(db, Dispatchers.Unconfined)
        val now = Clock.System.now()
        contacts.insert(
            Contact("c", "me", "them", ByteArray(32), "B", ContactStatus.VERIFIED, now, null, 0),
        )
        sessions.insert(Session("s", "c", "ref", now, null, "SAS", "fp", SessionStatus.ACTIVE))

        messages.insert(
            Message("m1", "s", "me", ByteArray(0), ByteArray(12), ByteArray(16), "x", now, MessageStatus.SENDING, true),
        )
        messages.updateStatus("m1", MessageStatus.DELIVERED)
        val list = messages.observeBySession("s").first()
        assertEquals(MessageStatus.DELIVERED, list.first().status)
    }
}
