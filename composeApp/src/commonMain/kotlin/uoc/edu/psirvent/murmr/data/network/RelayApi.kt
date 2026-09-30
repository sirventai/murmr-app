package uoc.edu.psirvent.murmr.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class RelayApi(
    private val client: HttpClient,
    private val baseUrlProvider: () -> String,
    private val connectivity: ConnectivityMonitor? = null,
) {
    private val baseUrl: String get() = baseUrlProvider()

    constructor(client: HttpClient, baseUrl: String) : this(client, { baseUrl }, null)

    private fun ensureOnline() {
        if (connectivity?.connected?.value == false) throw OfflineException()
    }

    suspend fun registerDevice(req: RegisterDeviceRequest): RegisterDeviceResponse {
        ensureOnline()
        return client.post("$baseUrl/devices") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()
    }

    suspend fun postMessage(token: String, env: MessageEnvelope) {
        ensureOnline()

        val response = client.post("$baseUrl/messages") {
            headers { append(HttpHeaders.Authorization, "Bearer $token") }
            contentType(ContentType.Application.Json)
            setBody(env)
        }
        if (!response.status.isSuccess()) {
            val body = runCatching { response.body<String>() }.getOrDefault("")
            error("POST /messages failed: ${response.status} $body")
        }
    }

    suspend fun fetchInbox(token: String, deviceId: String): List<MessageEnvelope> {
        ensureOnline()
        return client.get("$baseUrl/messages/inbox/$deviceId") {
            headers { append(HttpHeaders.Authorization, "Bearer $token") }
        }.body<InboxResponse>().messages
    }

    suspend fun fetchPresence(deviceId: String): Boolean {
        ensureOnline()
        val response = client.get("$baseUrl/presence/$deviceId")
        if (!response.status.isSuccess()) {
            error("GET /presence failed: ${response.status}")
        }
        val resp: PresenceResponse = response.body()
        return resp.online
    }

    companion object {
        fun newClient(engineFactory: io.ktor.client.engine.HttpClientEngineFactory<*>): HttpClient =
            HttpClient(engineFactory) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }

                install(WebSockets) {
                    pingIntervalMillis = 20_000
                }
                install(HttpRequestRetry) {
                    retryOnExceptionOrServerErrors(maxRetries = 3)
                    exponentialDelay()
                }

                install(HttpTimeout) {
                    requestTimeoutMillis = 15_000
                    connectTimeoutMillis = 5_000
                    socketTimeoutMillis = 10_000
                }
                defaultRequest {
                    headers.append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }
    }
}
