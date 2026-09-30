package uoc.edu.psirvent.murmr.data.network

import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.MessageRepository
import uoc.edu.psirvent.murmr.data.repository.OutboxRepository
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.model.MessageStatus
import uoc.edu.psirvent.murmr.util.Logger

class OutboxWorker(
    private val outbox: OutboxRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val relayApi: RelayApi,
    private val clock: Clock = Clock.System,
    private val contactRepository: ContactRepository? = null,
    private val relayConfigStore: RelayConfigStore? = null,
    private val connectivity: ConnectivityMonitor? = null,
) {
    private val tag = "OutboxWorker"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var workerJob: Job? = null

    private val concurrency = Semaphore(5)

    fun start(scope: CoroutineScope) {
        workerJob?.cancel()
        workerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                connectivity?.let { mon ->
                    if (!mon.connected.value) {
                        Logger.i(tag, "offline, esperando conectividad")
                        mon.connected.first { it }
                        Logger.i(tag, "online, drenando outbox")
                    }
                }
                tick()
                delay(2.seconds)
            }
        }
    }

    fun stop() {
        workerJob?.cancel()
        workerJob = null
    }

    private suspend fun tick() {
        val due = runCatching { outbox.due(clock.now()) }.getOrNull().orEmpty()
        if (due.isEmpty()) return
        val me = userRepository.getLocalUser() ?: return
        coroutineScope {
            due.map { entry ->
                async { concurrency.withPermit { processEntry(me.deviceId, entry) } }
            }.forEach { it.await() }
        }
    }

    private suspend fun processEntry(deviceId: String, entry: OutboxRepository.Entry) {
        val env = runCatching {
            json.decodeFromString(MessageEnvelope.serializer(), entry.payloadJson)
        }.getOrNull()
        if (env == null) {
            Logger.w(tag, "envelope JSON corrupto en outbox para ${entry.messageId} - descartando")
            outbox.delete(entry.messageId)
            messageRepository.updateStatus(entry.messageId, MessageStatus.FAILED)
            return
        }

        if (contactRepository != null && relayConfigStore != null) {
            val contact = contactRepository.getByRemote(deviceId, env.recipientDeviceId)
            val currentRelay = relayConfigStore.current().baseUrl
            if (contact != null && contact.relayUrl.isNotEmpty() && contact.relayUrl != currentRelay) {
                Logger.w(
                    tag,
                    "outbox ${entry.messageId}: relay del contacto=${contact.relayUrl} != actual=$currentRelay - FAIL",
                )
                outbox.delete(entry.messageId)
                messageRepository.updateStatus(entry.messageId, MessageStatus.FAILED)
                return
            }
        }
        val sendResult = runCatching { relayApi.postMessage(token = deviceId, env = env) }
        if (sendResult.isSuccess) {
            outbox.delete(entry.messageId)
            messageRepository.updateStatus(entry.messageId, MessageStatus.SENT)
            Logger.i(tag, "delivered ${entry.messageId} (attempts=${entry.attempts})")
        } else {
            val errMsg = sendResult.exceptionOrNull()?.message?.take(200) ?: "unknown"
            val nextDelayMs = nextRetryDelayMs(entry.attempts)
            val next = clock.now().plus(nextDelayMs.milliseconds)
            if (entry.attempts >= MAX_ATTEMPTS_BEFORE_DROP) {
                outbox.delete(entry.messageId)
                messageRepository.updateStatus(entry.messageId, MessageStatus.FAILED)
                Logger.w(tag, "dropped ${entry.messageId} after ${entry.attempts} attempts: $errMsg")
            } else {
                outbox.bumpAttempts(entry.messageId, next, errMsg)
                Logger.i(tag, "retry ${entry.messageId} att=${entry.attempts + 1} nextIn=${nextDelayMs}ms err=$errMsg")
            }
        }
    }

    companion object {

        const val MAX_ATTEMPTS_BEFORE_DROP = 10

        internal fun nextRetryDelayMs(attempts: Int): Long =
            (1000L shl attempts.coerceAtMost(8)).coerceAtMost(5 * 60_000L)
    }
}
