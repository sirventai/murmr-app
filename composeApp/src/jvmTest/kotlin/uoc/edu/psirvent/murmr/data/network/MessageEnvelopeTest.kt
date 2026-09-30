package uoc.edu.psirvent.murmr.data.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class MessageEnvelopeTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun roundTrip() {
        val original = MessageEnvelope(
            messageId = "abc-123",
            senderDeviceId = "alice",
            recipientDeviceId = "bob",
            ciphertextB64 = "zNZBg==",
            ivB64 = "AAAA",
            authTagB64 = "AAECAwQFBgcICQoLDA0ODw==",
            sentAtEpochMs = 1714560000000,
        )
        val encoded = json.encodeToString(MessageEnvelope.serializer(), original)
        assertTrue(encoded.contains("\"messageId\":\"abc-123\""))
        assertTrue(encoded.contains("\"senderDeviceId\":\"alice\""))
        assertTrue(encoded.contains("\"recipientDeviceId\":\"bob\""))
        val decoded = json.decodeFromString(MessageEnvelope.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun decodesWithExtraFieldsTolerantly() {
        val raw = """{
            "messageId":"x",
            "senderDeviceId":"a",
            "recipientDeviceId":"b",
            "ciphertextB64":"AA==",
            "ivB64":"BB==",
            "authTagB64":"CC==",
            "sentAtEpochMs":0,
            "futureField":"ignored"
        }"""
        val tolerant = Json { ignoreUnknownKeys = true }
        val decoded = tolerant.decodeFromString(MessageEnvelope.serializer(), raw)
        assertEquals("x", decoded.messageId)
    }

    @Test
    fun registerDeviceRequestRoundTrip() {
        val original = RegisterDeviceRequest(
            deviceId = "alice",
            publicKeyB64 = "abcd",
            alias = "Alice",
        )
        val encoded = json.encodeToString(RegisterDeviceRequest.serializer(), original)
        val decoded = json.decodeFromString(RegisterDeviceRequest.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun registerDeviceRequestSupportsNullAlias() {
        val original = RegisterDeviceRequest(
            deviceId = "anon",
            publicKeyB64 = "xy",
            alias = null,
        )
        val encoded = json.encodeToString(RegisterDeviceRequest.serializer(), original)
        val decoded = json.decodeFromString(RegisterDeviceRequest.serializer(), encoded)
        assertEquals(null, decoded.alias)
    }
}
