package uoc.edu.psirvent.murmr.data.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class PresenceResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun roundTripOnline() {
        val original = PresenceResponse(deviceId = "alice", online = true)
        val encoded = json.encodeToString(PresenceResponse.serializer(), original)
        assertTrue(encoded.contains("\"online\":true"))
        val decoded = json.decodeFromString(PresenceResponse.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun roundTripOffline() {
        val original = PresenceResponse(deviceId = "bob", online = false)
        val encoded = json.encodeToString(PresenceResponse.serializer(), original)
        val decoded = json.decodeFromString(PresenceResponse.serializer(), encoded)
        assertEquals("bob", decoded.deviceId)
        assertFalse(decoded.online)
    }

    @Test
    fun decodesIgnoringExtraFields() {
        val raw = """{"deviceId":"x","online":true,"someExtra":"futureField"}"""
        val decoded = json.decodeFromString(PresenceResponse.serializer(), raw)
        assertEquals("x", decoded.deviceId)
        assertTrue(decoded.online)
    }
}
