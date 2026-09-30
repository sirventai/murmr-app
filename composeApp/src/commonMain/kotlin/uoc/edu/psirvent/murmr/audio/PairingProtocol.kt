package uoc.edu.psirvent.murmr.audio

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import uoc.edu.psirvent.murmr.crypto.AES_KEY_BYTES
import uoc.edu.psirvent.murmr.crypto.HkdfSha256
import uoc.edu.psirvent.murmr.crypto.KeyStoreManager
import uoc.edu.psirvent.murmr.crypto.SasGenerator
import uoc.edu.psirvent.murmr.crypto.SecureRandom
import uoc.edu.psirvent.murmr.crypto.X25519KeyExchange
import uoc.edu.psirvent.murmr.crypto.X25519KeyPair
import uoc.edu.psirvent.murmr.crypto.bytesToUuidString
import uoc.edu.psirvent.murmr.crypto.deviceIdFromPublicKey
import uoc.edu.psirvent.murmr.domain.model.KeyExchangePhase
import uoc.edu.psirvent.murmr.domain.model.Sas
import uoc.edu.psirvent.murmr.domain.model.User
import uoc.edu.psirvent.murmr.util.Logger

sealed interface NegotiatedRole {
    data class Initiator(val prefetchedAck: PairingFrame) : NegotiatedRole
    data class Receiver(val prefetchedHello: PairingFrame) : NegotiatedRole
    data object Failed : NegotiatedRole
}

data class PairingOutcome(
    val sessionId: String,
    val sessionKeyRef: String,
    val sas: Sas,
    val sasEncoded: String,
    val fingerprint: String,
    val remoteAlias: String,
    val remotePublicKey: ByteArray,
    val nonceCombined: ByteArray,
    val telemetry: PairingTelemetry,
)

data class PairingTelemetry(
    val transportName: String,
    val totalSeconds: Int,
)

sealed interface PairingEvent {
    data class PeerDiscovered(val peerAlias: String) : PairingEvent
    data class KeyExchangeStarted(val profileName: String) : PairingEvent
    data class KeyExchangeComplete(val outcome: PairingOutcome) : PairingEvent
    data class PhaseChange(
        val phase: KeyExchangePhase,
        val detail: String? = null,
    ) : PairingEvent
}

class PairingProtocol(
    private val transport: GgwaveTransport,
    private val keyStore: KeyStoreManager,
    private val helloTimeout: Duration = 90.seconds,
    private val keyExchangeTimeout: Duration = 120.seconds,
) {

    private val tag = "PairProto"
    private val transportName: String = transport::class.simpleName ?: "GgwaveTransport"

    suspend fun initiator(
        localUser: User,
        onEvent: (PairingEvent) -> Unit = {},
        prefetchedAck: PairingFrame? = null,
    ): PairingOutcome? {
        Logger.i(tag, "INITIATOR start alias=${localUser.alias} prefetchedAck=${prefetchedAck != null}")
        val tStart = nowMs()
        val identity = X25519KeyPair(localUser.publicKey, localUser.privateKey)
        val nonceA = SecureRandom.nextBytes(16)
        Logger.i(tag, "INITIATOR identity.pubKey[0..15]=${hex16(identity.publicKey)} (deviceId=${deviceIdFromPublicKey(identity.publicKey)})")

        val ack = if (prefetchedAck != null) {
            Logger.i(tag, "FASE 0: HELLO_ACK prefetched de '${prefetchedAck.alias}'")
            prefetchedAck
        } else {
            Logger.i(tag, "FASE 0: HELLO 1 reTX + silencio 5s")
            val a = retransmitUntilReceived(
                frameToSend = PairingFrame(PairingFrameType.HELLO, localUser.alias).encode(),
                expectedReplyType = PairingFrameType.HELLO_ACK,
                timeoutMs = helloTimeout.inWholeMilliseconds,
                label = "HELLO",
                maxRetransmits = 1,
                delayBetweenMs = 0L,
                silentWindowMs = 5_000L,
            )
            if (a == null) {
                Logger.w(tag, "FASE 0 TIMEOUT, sin HELLO_ACK")
                return null
            }
            a
        }
        Logger.i(tag, "FASE 0 OK peer=${ack.alias}")
        onEvent(PairingEvent.PeerDiscovered(ack.alias))

        onEvent(PairingEvent.KeyExchangeStarted(transportName))
        onEvent(PairingEvent.PhaseChange(KeyExchangePhase.WAITING_KEY_B))
        Logger.i(tag, "FASE 1: KEY_A burst 1 reTX + silencio 5s para KEY_B")
        val keyB = retransmitUntilReceived(
            frameToSend = PairingFrame(PairingFrameType.KEY_A, localUser.alias, identity.publicKey, nonceA).encode(),
            expectedReplyType = PairingFrameType.KEY_B,
            timeoutMs = keyExchangeTimeout.inWholeMilliseconds,
            label = "KEY_A",
            maxRetransmits = 1,
            delayBetweenMs = 0L,
            silentWindowMs = 5_000L,
        )
        if (keyB == null) {
            Logger.w(tag, "FASE 1 TIMEOUT, sin KEY_B")
            return null
        }
        Logger.i(tag, "FASE 1 OK KEY_B recibido")
        onEvent(PairingEvent.PhaseChange(KeyExchangePhase.DERIVING))
        val telemetry = PairingTelemetry(
            transportName = transportName,
            totalSeconds = ((nowMs() - tStart) / 1000).toInt(),
        )
        val outcome = derive(localUser, identity, nonceA, keyB, weAreInitiator = true, telemetry)

        withContext(NonCancellable) {
            sendKeyDoneAndConfirm(localUser.alias)
        }
        onEvent(PairingEvent.KeyExchangeComplete(outcome))
        return outcome
    }

    private suspend fun sendKeyDoneAndConfirm(localAlias: String) = coroutineScope {
        val frame = PairingFrame(PairingFrameType.KEY_DONE, localAlias).encode()
        runCatching { transport.send(frame) }
            .onFailure { Logger.w(tag, "KEY_DONE TX fallo: ${it.message}") }
        Logger.i(tag, "KEY_DONE emitido (#0)")
        for (attempt in 1..MAX_KEY_DONE_REPEATS) {
            val gotMoreKeyB = withTimeoutOrNull(KEY_DONE_CONFIRM_WINDOW_MS) {
                transport.listen()
                    .firstOrNull { decode(it, PairingFrameType.KEY_B) != null }
            } != null
            if (!gotMoreKeyB) {
                Logger.i(tag, "KEY_DONE asumido entregado (sin reemision de KEY_B)")
                return@coroutineScope
            }
            Logger.w(tag, "receiver sigue emitiendo KEY_B, reenviando KEY_DONE (#$attempt)")
            runCatching { transport.send(frame) }
                .onFailure { Logger.w(tag, "KEY_DONE re-TX fallo: ${it.message}") }
        }
        Logger.w(tag, "KEY_DONE reemitido $MAX_KEY_DONE_REPEATS veces sin silenciar al peer; dejo de insistir")
    }

    suspend fun receiver(
        localUser: User,
        onEvent: (PairingEvent) -> Unit = {},
        prefetchedHello: PairingFrame? = null,
    ): PairingOutcome? {
        Logger.i(tag, "RECEIVER start alias=${localUser.alias} prefetched=${prefetchedHello != null}")
        val tStart = nowMs()
        val identity = X25519KeyPair(localUser.publicKey, localUser.privateKey)
        val nonceB = SecureRandom.nextBytes(16)
        Logger.i(tag, "RECEIVER identity.pubKey[0..15]=${hex16(identity.publicKey)} (deviceId=${deviceIdFromPublicKey(identity.publicKey)})")

        val hello = if (prefetchedHello != null) {
            Logger.i(tag, "FASE 0: HELLO prefetched de '${prefetchedHello.alias}'")
            prefetchedHello
        } else {
            Logger.i(tag, "FASE 0: esperando HELLO")
            val h = withTimeoutOrNull(helloTimeout) {
                transport.listen()
                    .firstOrNull { decode(it, PairingFrameType.HELLO) != null }
                    ?.let { decode(it, PairingFrameType.HELLO) }
            }
            if (h == null) {
                Logger.w(tag, "FASE 0 TIMEOUT, sin HELLO")
                return null
            }
            h
        }
        Logger.i(tag, "FASE 0 OK HELLO de '${hello.alias}'")
        onEvent(PairingEvent.PeerDiscovered(hello.alias))

        onEvent(PairingEvent.KeyExchangeStarted(transportName))
        onEvent(PairingEvent.PhaseChange(KeyExchangePhase.SENDING_HELLO_ACK))
        Logger.i(tag, "FASE 0 a 1: HELLO_ACK 1 reTX + silencio 5s para KEY_A del initiator")
        val keyA = retransmitUntilReceived(
            frameToSend = PairingFrame(PairingFrameType.HELLO_ACK, localUser.alias).encode(),
            expectedReplyType = PairingFrameType.KEY_A,
            timeoutMs = keyExchangeTimeout.inWholeMilliseconds,
            label = "HELLO_ACK",
            maxRetransmits = 1,
            delayBetweenMs = 0L,
            silentWindowMs = 5_000L,
        )
        if (keyA == null) {
            Logger.w(tag, "FASE 1 TIMEOUT, sin KEY_A")
            return null
        }
        Logger.i(tag, "FASE 1 OK KEY_A recibido, emitiendo KEY_B + esperando KEY_DONE")
        val keyBFrame = PairingFrame(
            PairingFrameType.KEY_B, localUser.alias, identity.publicKey, nonceB,
        ).encode()
        onEvent(PairingEvent.PhaseChange(KeyExchangePhase.SENDING_KEY_B, "esperando confirmacion"))

        val keyDone = retransmitUntilReceived(
            frameToSend = keyBFrame,
            expectedReplyType = PairingFrameType.KEY_DONE,
            timeoutMs = 30_000L,
            label = "KEY_B",
            maxRetransmits = 1,
            delayBetweenMs = 0L,
            silentWindowMs = 5_000L,
        )
        if (keyDone == null) {
            Logger.w(tag, "FASE 1 RECEIVER timeout sin KEY_DONE, abortando pairing asimetrico")
            return null
        }
        Logger.i(tag, "KEY_DONE recibido, pairing confirmado bilateralmente")
        onEvent(PairingEvent.PhaseChange(KeyExchangePhase.DERIVING))
        val telemetry = PairingTelemetry(
            transportName = transportName,
            totalSeconds = ((nowMs() - tStart) / 1000).toInt(),
        )
        val outcome = derive(localUser, identity, nonceB, keyA, weAreInitiator = false, telemetry)
        onEvent(PairingEvent.KeyExchangeComplete(outcome))
        return outcome
    }

    suspend fun negotiateRole(
        localUser: User,
        slotMs: Long = 1_800L,
        slotsPerWindow: Int = 4,
        responseTimeoutMs: Long = 4_000L,
        maxRounds: Int = 6,
        random: () -> Int = { kotlin.random.Random.nextInt() },
    ): NegotiatedRole {
        require(slotsPerWindow >= 2) { "slotsPerWindow >= 2" }
        Logger.i(tag, "AUTO negotiate alias=${localUser.alias} slot=${slotMs}ms x${slotsPerWindow} maxRounds=$maxRounds")

        val processedPeerNonces = mutableSetOf<List<Byte>>()

        var failedWinsInARow = 0

        repeat(maxRounds) { round ->
            val myNonce = SecureRandom.nextBytes(16)
            val mySlot = (random() and Int.MAX_VALUE) % slotsPerWindow
            val mySlotStartMs = mySlot * slotMs

            val windowDurationMs = (slotsPerWindow + 1) * slotMs
            Logger.i(tag, "AUTO ronda ${round + 1}/$maxRounds slot=$mySlot/$slotsPerWindow startMs=$mySlotStartMs nonce=${hex16(myNonce)}")

            val resp: PairingFrame? = coroutineScope {
                val listener = async {
                    withTimeoutOrNull(windowDurationMs) {
                        transport.listen()
                            .firstOrNull { bytes ->
                                val f = PairingFrame.decode(bytes) ?: return@firstOrNull false
                                when (f.type) {
                                    PairingFrameType.HELLO_ACK -> f.alias != localUser.alias
                                    PairingFrameType.HELLO_AUTO -> {
                                        val n = f.nonce ?: return@firstOrNull false
                                        if (n.contentEquals(myNonce)) return@firstOrNull false
                                        if (processedPeerNonces.contains(n.toList())) {
                                            Logger.i(tag, "AUTO descartando HELLO_AUTO con nonce ya visto (replay)")
                                            return@firstOrNull false
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            }
                            ?.let(PairingFrame::decode)
                    }
                }
                val sender = async {
                    delay(mySlotStartMs)

                    if (listener.isActive && !listener.isCompleted) {
                        Logger.i(tag, "AUTO slot $mySlot disparo, emitiendo HELLO_AUTO")
                        runCatching {
                            transport.send(
                                PairingFrame(PairingFrameType.HELLO_AUTO, localUser.alias, nonce = myNonce).encode(),
                            )
                        }.onFailure { Logger.w(tag, "AUTO HELLO_AUTO TX fallo: ${it.message}") }
                    } else {
                        Logger.i(tag, "AUTO listener resuelto antes del slot, NO emito HELLO_AUTO")
                    }
                }
                val r = listener.await()
                sender.cancel()
                r
            }

            when {
                resp == null -> {
                    Logger.w(tag, "AUTO ronda ${round + 1} sin trafico del peer, reintentando")
                }
                resp.type == PairingFrameType.HELLO_ACK -> {
                    Logger.i(tag, "AUTO recibido HELLO_ACK de '${resp.alias}' a INITIATOR")
                    return NegotiatedRole.Initiator(prefetchedAck = resp)
                }
                resp.type == PairingFrameType.HELLO_AUTO -> {
                    processedPeerNonces.add(resp.nonce!!.toList())
                    val cmp = compareUnsigned(myNonce, resp.nonce)
                    Logger.i(tag, "AUTO HELLO_AUTO de '${resp.alias}', cmp(my,peer)=$cmp")
                    when {
                        cmp > 0 -> {
                            Logger.i(tag, "AUTO nonce mayor, esperando HELLO_ACK del peer perdedor")
                            val ack = withTimeoutOrNull(responseTimeoutMs) {
                                transport.listen()
                                    .firstOrNull {
                                        val f = PairingFrame.decode(it)
                                        f != null &&
                                            f.type == PairingFrameType.HELLO_ACK &&
                                            f.alias != localUser.alias
                                    }
                                    ?.let(PairingFrame::decode)
                            }
                            if (ack != null) {
                                Logger.i(tag, "AUTO HELLO_ACK del peer recibido a INITIATOR")
                                failedWinsInARow = 0
                                return NegotiatedRole.Initiator(prefetchedAck = ack)
                            }
                            failedWinsInARow++
                            if (failedWinsInARow >= 2) {
                                val iYield = localUser.alias < resp.alias ||
                                    (localUser.alias == resp.alias && cmp < 0)
                                if (iYield) {
                                    Logger.w(tag, "AUTO posible deadlock, cedo por alias '${localUser.alias}' < '${resp.alias}' a RECEIVER")
                                    runCatching {
                                        transport.send(PairingFrame(PairingFrameType.HELLO_ACK, localUser.alias).encode())
                                    }.onFailure { Logger.w(tag, "AUTO HELLO_ACK yield TX fallo: ${it.message}") }
                                    return NegotiatedRole.Receiver(
                                        prefetchedHello = PairingFrame(PairingFrameType.HELLO, resp.alias),
                                    )
                                }
                                Logger.w(tag, "AUTO posible deadlock, peer '${resp.alias}' deberia ceder por alias; reintentando")
                            } else {
                                Logger.w(tag, "AUTO sin HELLO_ACK tras ganar tiebreak, reintentando ronda")
                            }
                        }
                        cmp < 0 -> {
                            Logger.i(tag, "AUTO nonce menor a RECEIVER (mando HELLO_ACK)")
                            val helloFrame = PairingFrame(PairingFrameType.HELLO, resp.alias)
                            runCatching {
                                transport.send(PairingFrame(PairingFrameType.HELLO_ACK, localUser.alias).encode())
                            }.onFailure { Logger.w(tag, "AUTO HELLO_ACK post-tiebreak TX fallo: ${it.message}") }
                            return NegotiatedRole.Receiver(prefetchedHello = helloFrame)
                        }
                        else -> {
                            Logger.w(tag, "AUTO empate de nonce (improbable), re-roll")
                        }
                    }
                }
                else -> Unit
            }
        }
        Logger.w(tag, "AUTO maxRounds agotado, negociacion fallida")
        return NegotiatedRole.Failed
    }

    suspend fun auto(localUser: User, onEvent: (PairingEvent) -> Unit = {}): PairingOutcome? {
        return when (val role = negotiateRole(localUser)) {
            is NegotiatedRole.Initiator -> initiator(localUser, onEvent, role.prefetchedAck)
            is NegotiatedRole.Receiver -> receiver(localUser, onEvent, role.prefetchedHello)
            NegotiatedRole.Failed -> null
        }
    }

    suspend fun sendSasConfirm(localAlias: String, ok: Boolean) {
        val type = if (ok) PairingFrameType.SAS_OK else PairingFrameType.SAS_NO
        val frame = PairingFrame(type, localAlias)
        Logger.i(tag, "TX SAS_${if (ok) "OK" else "NO"} (alias=$localAlias)")
        transport.send(frame.encode())
    }

    suspend fun awaitPeerSasConfirm(timeout: Duration = 60.seconds): Boolean? {
        Logger.i(tag, "esperando SAS_OK/SAS_NO del peer (timeout=$timeout)")
        val frame = withTimeoutOrNull(timeout) {
            transport.listen().firstOrNull { bytes ->
                val f = PairingFrame.decode(bytes)
                f != null && (f.type == PairingFrameType.SAS_OK || f.type == PairingFrameType.SAS_NO)
            }?.let(PairingFrame::decode)
        }
        return when (frame?.type) {
            PairingFrameType.SAS_OK -> {
                Logger.i(tag, "RX SAS_OK del peer")
                true
            }
            PairingFrameType.SAS_NO -> {
                Logger.w(tag, "RX SAS_NO del peer, abortando")
                false
            }
            else -> {
                Logger.w(tag, "timeout esperando SAS confirm del peer")
                null
            }
        }
    }

    private fun decode(bytes: ByteArray, expected: PairingFrameType): PairingFrame? {
        val frame = PairingFrame.decode(bytes) ?: return null
        if (frame.type != expected) return null

        if (expected.hasKeyPayload() && !isValidPublicKey(frame.publicKey)) {
            Logger.w(tag, "decode: ${expected.name} con publicKey invalida (low-order o nula), descartando")
            return null
        }
        return frame
    }

    private fun isValidPublicKey(pub: ByteArray?): Boolean {
        if (pub == null || pub.size != 32) return false
        if (pub.all { it == 0.toByte() }) return false
        return !LOW_ORDER_PUBLIC_KEYS.any { it.contentEquals(pub) }
    }

    private fun derive(
        localUser: User,
        myEphemeral: X25519KeyPair,
        myNonce: ByteArray,
        peerFrame: PairingFrame,
        weAreInitiator: Boolean,
        telemetry: PairingTelemetry,
    ): PairingOutcome {
        val peerPub = peerFrame.publicKey ?: error("peer frame sin publicKey")
        val peerNonce = peerFrame.nonce ?: error("peer frame sin nonce")
        Logger.i(tag, "derive: peerPub[0..15]=${hex16(peerPub)} peerDeviceId=${deviceIdFromPublicKey(peerPub)}")
        val shared = X25519KeyExchange.deriveSharedSecret(myEphemeral.privateKey, peerPub)
        val nonceCombined = if (weAreInitiator) myNonce + peerNonce else peerNonce + myNonce
        val sessionKey = HkdfSha256.derive(
            ikm = shared,
            salt = nonceCombined,
            info = "murmr-session-v1".encodeToByteArray(),
            length = AES_KEY_BYTES,
        )
        val sas = SasGenerator.derive(shared, nonceCombined)
        val sessionIdBytes = HkdfSha256.derive(
            ikm = shared,
            salt = nonceCombined,
            info = "murmr-sessionid-v1".encodeToByteArray(),
            length = 16,
        )
        val sessionId = bytesToUuidString(sessionIdBytes)
        val keyRef = "session-$sessionId"
        keyStore.storeKey(keyRef, sessionKey)
        val fingerprintBytes = HkdfSha256.derive(
            ikm = shared,
            salt = nonceCombined,
            info = "murmr-fingerprint-v1".encodeToByteArray(),
            length = 16,
        )
        Logger.i(tag) { "derive OK sessionId=$sessionId sasEmojis=${sas.emojis.joinToString("")}" }
        return PairingOutcome(
            sessionId = sessionId,
            sessionKeyRef = keyRef,
            sas = sas,
            sasEncoded = sas.encoded(),
            fingerprint = formatFingerprint(fingerprintBytes),
            remoteAlias = peerFrame.alias,

            remotePublicKey = peerPub.copyOf(),
            nonceCombined = nonceCombined,
            telemetry = telemetry,
        )
    }

    private suspend fun retransmitUntilReceived(
        frameToSend: ByteArray,
        expectedReplyType: PairingFrameType,
        timeoutMs: Long,
        label: String,
        maxRetransmits: Int? = null,
        delayBetweenMs: Long = 1_500L,
        silentWindowMs: Long = 8_000L,
    ): PairingFrame? = coroutineScope {
        val txLoop = launch {
            var i = 0
            var burstCount = 0
            while (isActive) {
                if (maxRetransmits != null && burstCount >= maxRetransmits) {
                    Logger.i(tag, "$label burst #$i completado ($burstCount reTX); silencio ${silentWindowMs}ms para que hable el peer")
                    delay(silentWindowMs)
                    burstCount = 0
                    Logger.i(tag, "$label fin de silencio, reanudando burst")
                    continue
                }
                runCatching { transport.send(frameToSend) }
                    .onFailure { Logger.w(tag, "$label TX fallo (#${i}): ${it.message}") }
                burstCount++
                Logger.i(tag, "$label retransmitido (#${i++}); esperando ${expectedReplyType.name}...")
                delay(delayBetweenMs)
            }
        }
        val reply = withTimeoutOrNull(timeoutMs) {
            transport.listen()
                .firstOrNull { decode(it, expectedReplyType) != null }
                ?.let { decode(it, expectedReplyType) }
        }
        txLoop.cancel()
        reply
    }

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private fun compareUnsigned(a: ByteArray, b: ByteArray): Int {
        val n = minOf(a.size, b.size)
        for (i in 0 until n) {
            val ai = a[i].toInt() and 0xFF
            val bi = b[i].toInt() and 0xFF
            if (ai != bi) return ai - bi
        }
        return a.size - b.size
    }

    private fun hex16(b: ByteArray): String {
        val n = minOf(b.size, 16)
        val sb = StringBuilder(n * 2)
        val hex = "0123456789abcdef"
        for (i in 0 until n) {
            val v = b[i].toInt() and 0xFF
            sb.append(hex[v ushr 4]); sb.append(hex[v and 0x0F])
        }
        return sb.toString()
    }

    companion object {

        private const val KEY_DONE_CONFIRM_WINDOW_MS = 3_000L
        private const val MAX_KEY_DONE_REPEATS = 3
    }
}

private fun formatFingerprint(bytes: ByteArray): String {
    val hex = "0123456789ABCDEF"
    val sb = StringBuilder(bytes.size * 2)
    for (b in bytes) {
        val v = b.toInt() and 0xFF
        sb.append(hex[v ushr 4]); sb.append(hex[v and 0x0F])
    }
    return sb.toString().take(32).chunked(4).joinToString(" ")
}

private val LOW_ORDER_PUBLIC_KEYS: List<ByteArray> = listOf(

    ByteArray(32),

    ByteArray(32).also { it[0] = 1 },

    byteArrayOf(
        0xe0.toByte(), 0xeb.toByte(), 0x7a.toByte(), 0x7c.toByte(),
        0x3b.toByte(), 0x41.toByte(), 0xb8.toByte(), 0xae.toByte(),
        0x16.toByte(), 0x56.toByte(), 0xe3.toByte(), 0xfa.toByte(),
        0xf1.toByte(), 0x9f.toByte(), 0xc4.toByte(), 0x6a.toByte(),
        0xda.toByte(), 0x09.toByte(), 0x8d.toByte(), 0xeb.toByte(),
        0x9c.toByte(), 0x32.toByte(), 0xb1.toByte(), 0xfd.toByte(),
        0x86.toByte(), 0x62.toByte(), 0x05.toByte(), 0x16.toByte(),
        0x5f.toByte(), 0x49.toByte(), 0xb8.toByte(), 0x00.toByte(),
    ),

    byteArrayOf(
        0xec.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
        0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
        0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
        0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
        0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
        0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
        0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
        0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte(),
    ),
)
