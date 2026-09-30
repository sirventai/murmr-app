package uoc.edu.psirvent.murmr.util

object TextSanitizer {
    fun sanitize(s: String, allowNewlines: Boolean = true): String {
        if (s.isEmpty()) return s
        val sb = StringBuilder(s.length)
        for (c in s) {
            val code = c.code
            val zeroWidth = code in 0x200B..0x200D || code == 0xFEFF
            val rtlOverride = code in 0x202A..0x202E || code in 0x2066..0x2069
            val control = code < 0x20 && (!allowNewlines || (c != '\n' && c != '\t'))
            if (zeroWidth || rtlOverride || control) continue
            sb.append(c)
        }
        return sb.toString()
    }
}
