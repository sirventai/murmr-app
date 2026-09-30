package uoc.edu.psirvent.murmr.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import uoc.edu.psirvent.murmr.crypto.KeyStoreManager
import uoc.edu.psirvent.murmr.crypto.X25519KeyExchange
import uoc.edu.psirvent.murmr.domain.model.User

class AutoPairingNegotiationTest {

    private fun newPair(): Pair<PerPeerTransport, PerPeerTransport> {
        val a = PerPeerTransport()
        val b = PerPeerTransport()
        a.peer = b
        b.peer = a
        return a to b
    }

    @Test
    fun differentSlotsAEarliestBecomesReceiverPathOrInitiator() = kotlinx.coroutines.test.runTest {
        val (txA, txB) = newPair()
        val protoA = PairingProtocol(txA, KeyStoreManager())
        val protoB = PairingProtocol(txB, KeyStoreManager())
        val alice = User("a-id", "Alice", ByteArray(32), ByteArray(32), Clock.System.now())
        val bob = User("b-id", "Bob", ByteArray(32), ByteArray(32), Clock.System.now())

        coroutineScope {
            val deferA = async {
                protoA.negotiateRole(
                    alice,
                    slotMs = 200,
                    slotsPerWindow = 5,
                    responseTimeoutMs = 2_000,
                    random = { 0 },
                )
            }
            val deferB = async {
                protoB.negotiateRole(
                    bob,
                    slotMs = 200,
                    slotsPerWindow = 5,
                    responseTimeoutMs = 2_000,
                    random = { 3 },
                )
            }
            val rA = deferA.await()
            val rB = deferB.await()
            assertTrue(rA is NegotiatedRole.Initiator, "A esperado INITIATOR, fue $rA")
            assertTrue(rB is NegotiatedRole.Receiver, "B esperado RECEIVER, fue $rB")
            assertEquals("Bob", rA.prefetchedAck.alias)
            assertEquals("Alice", rB.prefetchedHello.alias)
        }
    }

    @Test
    fun differentSlotsBEarliestBecomesInitiator() = kotlinx.coroutines.test.runTest {
        val (txA, txB) = newPair()
        val protoA = PairingProtocol(txA, KeyStoreManager())
        val protoB = PairingProtocol(txB, KeyStoreManager())
        val alice = User("a-id", "Alice", ByteArray(32), ByteArray(32), Clock.System.now())
        val bob = User("b-id", "Bob", ByteArray(32), ByteArray(32), Clock.System.now())

        coroutineScope {
            val deferA = async {
                protoA.negotiateRole(
                    alice, slotMs = 200, slotsPerWindow = 5, responseTimeoutMs = 2_000, random = { 3 },
                )
            }
            val deferB = async {
                protoB.negotiateRole(
                    bob, slotMs = 200, slotsPerWindow = 5, responseTimeoutMs = 2_000, random = { 0 },
                )
            }
            val rA = deferA.await()
            val rB = deferB.await()
            assertTrue(rA is NegotiatedRole.Receiver, "A esperado RECEIVER, fue $rA")
            assertTrue(rB is NegotiatedRole.Initiator, "B esperado INITIATOR, fue $rB")
        }
    }

    @Test
    fun sameSlotCollisionConvergesViaTiebreakOrRetry() = kotlinx.coroutines.test.runTest {
        val (txA, txB) = newPair()
        val protoA = PairingProtocol(txA, KeyStoreManager())
        val protoB = PairingProtocol(txB, KeyStoreManager())
        val alice = User("a-id", "Alice", ByteArray(32), ByteArray(32), Clock.System.now())
        val bob = User("b-id", "Bob", ByteArray(32), ByteArray(32), Clock.System.now())

        coroutineScope {
            val deferA = async {
                protoA.negotiateRole(
                    alice, slotMs = 200, slotsPerWindow = 5, responseTimeoutMs = 4_000, random = { 0 },
                )
            }
            val deferB = async {
                protoB.negotiateRole(
                    bob, slotMs = 200, slotsPerWindow = 5, responseTimeoutMs = 4_000, random = { 0 },
                )
            }
            val rA = deferA.await()
            val rB = deferB.await()
            val complementary = (rA is NegotiatedRole.Initiator && rB is NegotiatedRole.Receiver) ||
                (rA is NegotiatedRole.Receiver && rB is NegotiatedRole.Initiator)
            assertTrue(complementary, "roles no complementarios: A=$rA B=$rB")
        }
    }

    @Test
    fun fullAutoEndToEndConverges() = kotlinx.coroutines.test.runTest {
        val (txA, txB) = newPair()
        val ksA = KeyStoreManager()
        val ksB = KeyStoreManager()
        val protoA = PairingProtocol(txA, ksA)
        val protoB = PairingProtocol(txB, ksB)

        val pairA = X25519KeyExchange.generateKeyPair()
        val pairB = X25519KeyExchange.generateKeyPair()
        val alice = User("a-id", "Alice", pairA.publicKey, pairA.privateKey, Clock.System.now())
        val bob = User("b-id", "Bob", pairB.publicKey, pairB.privateKey, Clock.System.now())

        val outcomeA: PairingOutcome?
        val outcomeB: PairingOutcome?
        coroutineScope {
            val deferA = async { protoA.auto(alice) }
            val deferB = async { protoB.auto(bob) }
            outcomeA = deferA.await()
            outcomeB = deferB.await()
        }
        assertNotNull(outcomeA, "A no completo pairing")
        assertNotNull(outcomeB, "B no completo pairing")
        assertEquals(outcomeA.sasEncoded, outcomeB.sasEncoded, "SAS divergente")
        val keyA = ksA.loadKey(outcomeA.sessionKeyRef)
        val keyB = ksB.loadKey(outcomeB.sessionKeyRef)
        assertNotNull(keyA); assertNotNull(keyB)
        assertTrue(keyA.contentEquals(keyB), "sessionKey divergente")
    }
}

private class PerPeerTransport : GgwaveTransport {
    var peer: PerPeerTransport? = null
    private val incoming = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)

    override suspend fun send(payload: ByteArray) {

        val peerInbox = peer?.incoming ?: return
        peerInbox.subscriptionCount.first { it > 0 }
        peerInbox.emit(payload)
    }

    override fun listen(): Flow<ByteArray> = incoming.asSharedFlow()

    override fun amplitudes(): Flow<List<Float>> = flow { }

    override suspend fun stop() {}
}
