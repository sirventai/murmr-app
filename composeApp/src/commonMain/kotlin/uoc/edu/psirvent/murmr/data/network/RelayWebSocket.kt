package uoc.edu.psirvent.murmr.data.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import uoc.edu.psirvent.murmr.util.Logger

class RelayWebSocket(
    private val client: HttpClient,
    private val baseUrlProvider: () -> String,
    private val connectivity: ConnectivityMonitor? = null,
) {
    constructor(client: HttpClient, baseUrl: String) : this(client, { baseUrl }, null)

    private val baseUrl: String get() = baseUrlProvider()

    private val tag = "RelayWS"
    private val _incoming = MutableSharedFlow<MessageEnvelope>(extraBufferCapacity = 64)
    val incoming: SharedFlow<MessageEnvelope> = _incoming.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private var connectionJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun connect(scope: CoroutineScope, deviceId: String, token: String) {
        connectionJob?.cancel()
        connectionJob = scope.launch(Dispatchers.Default) {
            var backoffMs = 1000L
            val rng = kotlin.random.Random.Default
            while (isActive) {

                connectivity?.let { mon ->
                    if (!mon.connected.value) {
                        Logger.i(tag, "offline, esperando conectividad antes de WS")
                        mon.connected.first { it }
                        Logger.i(tag, "online recuperado, abriendo WS")
                        backoffMs = 1000L
                    }
                }
                try {
                    val wsUrl = baseUrl
                        .replace("https://", "wss://")
                        .replace("http://", "ws://")
                    Logger.i(tag, "Conectando WS a $wsUrl/relay/$deviceId")
                    client.webSocket(
                        urlString = "$wsUrl/relay/$deviceId?token=$token",
                    ) {
                        backoffMs = 1000L
                        _connected.value = true
                        Logger.i(tag, "WS abierto deviceId=$deviceId")
                        try {
                            for (frame in incoming) {
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    Logger.i(tag, "rx frame ${text.length} chars")
                                    runCatching {
                                        json.decodeFromString(MessageEnvelope.serializer(), text)
                                    }.fold(
                                        onSuccess = { env ->
                                            Logger.i(tag, "rx envelope OK msg=${env.messageId} sender=${env.senderDeviceId}")
                                            _incoming.emit(env)
                                        },
                                        onFailure = { e ->
                                            Logger.w(tag, "frame no parseable como envelope: ${e.message}")
                                        },
                                    )
                                }
                            }
                        } finally {
                            _connected.value = false
                            Logger.i(tag, "WS cerrado deviceId=$deviceId")
                        }
                    }
                } catch (t: Throwable) {
                    _connected.value = false
                    Logger.w(tag, "WS fallo: ${t.message}")
                }
                val jitter = rng.nextLong(0L, backoffMs / 2 + 1)
                delay(backoffMs + jitter)
                backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
            }
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        _connected.value = false
    }
}
