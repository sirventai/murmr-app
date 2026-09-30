package uoc.edu.psirvent.murmr.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InputSanitizationTest {

    private fun sanitize(s: String): String {
        if (s.isEmpty()) return s
        val sb = StringBuilder(s.length)
        for (c in s) {
            val code = c.code
            val zeroWidth = code in 0x200B..0x200D || code == 0xFEFF
            val rtlOverride = code in 0x202A..0x202E || code in 0x2066..0x2069
            val control = code < 0x20 && c != '\n' && c != '\t'
            if (zeroWidth || rtlOverride || control) continue
            sb.append(c)
        }
        return sb.toString()
    }

    @Test
    fun normalAsciiPassthrough() {
        val input = "Hola Bob, ¿qué tal? Esto es un mensaje normal."
        assertEquals(input, sanitize(input))
    }

    @Test
    fun emojiAndAccentsPassthrough() {
        val input = "café ñoño 🎉 🌍 中文 - funciona"
        assertEquals(input, sanitize(input))
    }

    @Test
    fun preservesNewlineAndTab() {
        val input = "linea 1\nlinea 2\tcol2"
        assertEquals(input, sanitize(input))
    }

    @Test
    fun stripsZeroWidthSpaces() {
        val input = "vis​ib‌le‍word﻿here"

        assertEquals("visiblewordhere", sanitize(input))
    }

    @Test
    fun stripsBidiOverrideAttacks() {

        val input = "balance: 100‮‭ evil"
        val out = sanitize(input)
        assertFalse(out.contains('‮'))
        assertFalse(out.contains('‭'))
    }

    @Test
    fun stripsControlChars() {
        val input = "test"
        assertEquals("test", sanitize(input))
    }

    @Test
    fun emptyStringIsEmpty() {
        assertEquals("", sanitize(""))
    }

    @Test
    fun mixedAttackString() {
        val input = "‮Evil​Mensaje\nlegitimo"
        val out = sanitize(input)
        assertTrue(out.contains("Evil"))
        assertTrue(out.contains("Mensaje"))
        assertTrue(out.contains("\n"))
        assertFalse(out.contains('‮'))
        assertFalse(out.contains('​'))
        assertFalse(out.contains(''))
    }
}
