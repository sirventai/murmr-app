package uoc.edu.psirvent.murmr.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidatorsTest {

    @Test fun aliasOk() {
        assertTrue(Validators.alias("Pablo").isValid)
        assertTrue(Validators.alias("Maria_García").isValid)
        assertTrue(Validators.alias("Joao 1").isValid)
    }

    @Test fun aliasReject() {
        assertFalse(Validators.alias("").isValid)
        assertFalse(Validators.alias("ab").isValid)

        assertFalse(Validators.alias("a".repeat(46)).isValid)
        assertFalse(Validators.alias("Hola@!").isValid)
    }

    @Test fun relayUrl() {
        assertTrue(Validators.relayUrl("http://localhost:8080").isValid)
        assertTrue(Validators.relayUrl("https://relay.murmr.app").isValid)
        assertTrue(Validators.relayUrl("http://10.0.2.2:8080/v1").isValid)
        assertFalse(Validators.relayUrl("ftp://relay.murmr.app").isValid)
        assertFalse(Validators.relayUrl("relay.murmr.app").isValid)
        assertFalse(Validators.relayUrl("").isValid)
    }

    @Test fun customNumbers() {
        assertTrue(Validators.autoLockSeconds("30").isValid)
        assertFalse(Validators.autoLockSeconds("0").isValid)
        assertFalse(Validators.autoLockSeconds("99999").isValid)
    }

    @Test fun chatMessage() {
        assertTrue(Validators.chatMessage("hola").isValid)
        assertFalse(Validators.chatMessage("").isValid)
        assertFalse(Validators.chatMessage("a".repeat(4_001)).isValid)
    }

    @Test fun avatarMagicBytes() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) +
            ByteArray(8) { 0 }
        assertTrue(Validators.avatarBytes(jpeg).isValid)
        val png = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte()) +
            ByteArray(8) { 0 }
        assertTrue(Validators.avatarBytes(png).isValid)
        val webp = "RIFF".encodeToByteArray() + ByteArray(4) { 0 } + "WEBP".encodeToByteArray()
        assertTrue(Validators.avatarBytes(webp).isValid)
        val unknown = ByteArray(20) { 0x42 }
        assertFalse(Validators.avatarBytes(unknown).isValid)
        val tooBig = ByteArray(64 * 1024 + 1) { 0xFF.toByte() }
        assertFalse(Validators.avatarBytes(tooBig).isValid)
        assertTrue(Validators.avatarBytes(ByteArray(0)).isValid)
    }

    @Test fun resultHelpers() {
        val ok: ValidationResult = ValidationResult.Ok
        assertTrue(ok.isValid)
        assertEquals(null, ok.errorOrNull())
        val bad: ValidationResult = ValidationResult.Invalid("nope")
        assertFalse(bad.isValid)
        assertEquals("nope", bad.errorOrNull())
    }
}
