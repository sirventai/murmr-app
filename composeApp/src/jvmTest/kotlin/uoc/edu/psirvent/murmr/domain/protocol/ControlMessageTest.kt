package uoc.edu.psirvent.murmr.domain.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import uoc.edu.psirvent.murmr.domain.model.SessionExpiry

class ControlMessageTest {

    @Test
    fun `expiry round trip`() {
        val msg = ControlMessage.ExpiryUpdate(SessionExpiry.SEVEN_DAYS, 0L)
        val parsed = ControlMessage.parse(msg.serialize())
        assertEquals(msg, parsed)
    }

    @Test
    fun `expiry custom seconds round trip`() {
        val msg = ControlMessage.ExpiryUpdate(SessionExpiry.CUSTOM, 90L * 60)
        val parsed = ControlMessage.parse(msg.serialize())
        assertEquals(msg, parsed)
    }

    @Test
    fun `expiry unknown enum falls back to NEVER`() {
        val parsed = ControlMessage.parse("EXPIRY_UPDATE|NOT_AN_ENUM|0")
        assertEquals(ControlMessage.ExpiryUpdate(SessionExpiry.NEVER, 0L), parsed)
    }

    @Test
    fun `read receipt round trip`() {
        val msg = ControlMessage.ReadReceipt(listOf("id-1", "id-2", "id-3"))
        val parsed = ControlMessage.parse(msg.serialize()) as ControlMessage.ReadReceipt
        assertEquals(msg.messageIds, parsed.messageIds)
    }

    @Test
    fun `read receipt drops blank ids`() {
        val parsed = ControlMessage.parse("READ_RECEIPT|a,,b") as ControlMessage.ReadReceipt
        assertEquals(listOf("a", "b"), parsed.messageIds)
    }

    @Test
    fun `profile alias round trip`() {
        val msg = ControlMessage.ProfileUpdate(ControlMessage.ProfileUpdate.Kind.ALIAS, "Pablo S.")
        val parsed = ControlMessage.parse(msg.serialize())
        assertEquals(msg, parsed)
    }

    @Test
    fun `profile avatar NONE round trip`() {
        val msg = ControlMessage.ProfileUpdate(ControlMessage.ProfileUpdate.Kind.AVATAR, "NONE")
        val parsed = ControlMessage.parse(msg.serialize())
        assertEquals(msg, parsed)
    }

    @Test
    fun `profile unknown kind returns null`() {
        val parsed = ControlMessage.parse("PROFILE_UPDATE|UNKNOWN|x")
        assertNull(parsed)
    }

    @Test
    fun `murmr reply round trip`() {
        val msg = ControlMessage.MurmrReply("msg-uuid-1", "hola, que tal")
        val parsed = ControlMessage.parse(msg.serialize())
        assertEquals(msg, parsed)
    }

    @Test
    fun `murmr reply preserves pipes in body`() {

        val msg = ControlMessage.MurmrReply("ref", "linea 1 | con pipes | aqui")
        val parsed = ControlMessage.parse(msg.serialize()) as ControlMessage.MurmrReply
        assertEquals(msg.text, parsed.text)
    }

    @Test
    fun `murmr reply empty refId returns null`() {
        val parsed = ControlMessage.parse("MURMR_REPLY||texto")
        assertNull(parsed)
    }

    @Test
    fun `session renewed round trip`() {
        val msg = ControlMessage.SessionRenewed(1764777600000L)
        val parsed = ControlMessage.parse(msg.serialize())
        assertEquals(msg, parsed)
    }

    @Test
    fun `session renewed invalid number returns null`() {
        assertNull(ControlMessage.parse("SESSION_RENEWED|not-a-number"))
    }

    @Test
    fun `non-control plaintext returns null`() {
        assertNull(ControlMessage.parse("hola, esto es un mensaje normal"))
        assertNull(ControlMessage.parse(""))
        assertNull(ControlMessage.parse("DELIVERY_RECEIPT|x"))
    }

    @Test
    fun `serialize formats match wire protocol`() {

        assertEquals(
            "EXPIRY_UPDATE|SEVEN_DAYS|0",
            ControlMessage.ExpiryUpdate(SessionExpiry.SEVEN_DAYS, 0L).serialize(),
        )
        assertEquals(
            "READ_RECEIPT|a,b,c",
            ControlMessage.ReadReceipt(listOf("a", "b", "c")).serialize(),
        )
        assertEquals(
            "PROFILE_UPDATE|ALIAS|Pablo",
            ControlMessage.ProfileUpdate(ControlMessage.ProfileUpdate.Kind.ALIAS, "Pablo").serialize(),
        )
        assertTrue(
            ControlMessage.MurmrReply("rid", "msg").serialize() == "MURMR_REPLY|rid|msg",
        )
    }
}
