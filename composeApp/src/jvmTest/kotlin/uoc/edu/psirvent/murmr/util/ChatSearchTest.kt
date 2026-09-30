package uoc.edu.psirvent.murmr.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatSearchTest {

    private fun filter(messages: List<String>, query: String): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return messages
        return messages.filter { it.lowercase().contains(q) }
    }

    @Test
    fun emptyQueryReturnsAll() {
        val messages = listOf("hola", "qué tal", "todo bien")
        assertEquals(messages, filter(messages, ""))
        assertEquals(messages, filter(messages, "   "))
    }

    @Test
    fun caseInsensitive() {
        val messages = listOf("Hola María", "que tal", "MARÍA")
        val r = filter(messages, "maría")
        assertEquals(2, r.size)
        assertTrue(r.contains("Hola María"))
        assertTrue(r.contains("MARÍA"))
    }

    @Test
    fun substringMatch() {
        val messages = listOf("vamos al parque luego", "el parque es bonito", "no recuerdo")
        val r = filter(messages, "parque")
        assertEquals(2, r.size)
    }

    @Test
    fun noMatchesReturnsEmpty() {
        val messages = listOf("hola", "qué tal")
        assertEquals(emptyList(), filter(messages, "xyz123"))
    }

    @Test
    fun trimsWhitespace() {
        val messages = listOf("hola mundo")
        assertEquals(messages, filter(messages, "  hola  "))
    }

    @Test
    fun supportsAccentsLiterally() {

        val messages = listOf("café con María", "cafe con Maria")
        assertEquals(1, filter(messages, "maría").size)
        assertEquals(1, filter(messages, "maria").size)
    }
}
