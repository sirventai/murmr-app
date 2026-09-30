package uoc.edu.psirvent.murmr.audio

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock
import uoc.edu.psirvent.murmr.crypto.KeyStoreManager
import uoc.edu.psirvent.murmr.crypto.X25519KeyExchange
import uoc.edu.psirvent.murmr.domain.model.User

class PairingProtocolTest {

    @Test
    fun loopbackInitiatorAndReceiverConverge() = runTest {
        val keyStoreA = KeyStoreManager()
        val keyStoreB = KeyStoreManager()
        val transportA = LoopbackGgwaveTransport.forProcess("alice")
        val transportB = LoopbackGgwaveTransport.forProcess("bob")

        val protocolA = PairingProtocol(transportA, keyStoreA)
        val protocolB = PairingProtocol(transportB, keyStoreB)

        val pairAlice = X25519KeyExchange.generateKeyPair()
        val pairBob = X25519KeyExchange.generateKeyPair()
        val alice = User("alice-id", "Alice", pairAlice.publicKey, pairAlice.privateKey, Clock.System.now())
        val bob = User("bob-id", "Bob", pairBob.publicKey, pairBob.privateKey, Clock.System.now())

        val outcomeA: PairingOutcome?
        val outcomeB: PairingOutcome?
        coroutineScope {
            val deferB = async { protocolB.receiver(bob) }

            kotlinx.coroutines.delay(20)
            val deferA = async { protocolA.initiator(alice) }
            outcomeA = deferA.await()
            outcomeB = deferB.await()
        }

        assertNotNull(outcomeA, "Iniciador no completó pairing")
        assertNotNull(outcomeB, "Receptor no completó pairing")

        assertEquals(outcomeA.sasEncoded, outcomeB.sasEncoded, "SAS no coincide entre las dos partes")

        val keyA = keyStoreA.loadKey(outcomeA.sessionKeyRef)
        val keyB = keyStoreB.loadKey(outcomeB.sessionKeyRef)
        assertNotNull(keyA); assertNotNull(keyB)
        assertContentEquals(keyA, keyB, "sessionKey divergente entre las dos partes")
    }
}
