package uoc.edu.psirvent.murmr.domain.model

data class Sas(
    val emojis: List<String>,
) {
    init {
        require(emojis.size == EMOJI_COUNT) { "Sas necesita $EMOJI_COUNT emojis" }
    }

    fun encoded(): String = emojis.joinToString("")

    companion object {
        const val EMOJI_COUNT = 5

        fun fromEncoded(encoded: String): Sas? {
            val out = mutableListOf<String>()
            var i = 0
            while (i < encoded.length && out.size < EMOJI_COUNT) {
                val match = EMOJI_ALPHABET.firstOrNull { encoded.startsWith(it, i) }
                    ?: return null
                out.add(match)
                i += match.length
            }
            return if (out.size == EMOJI_COUNT && i == encoded.length) Sas(out) else null
        }

        val EMOJI_ALPHABET: List<String> = listOf(
            "🐶", "🐱", "🐰", "🐼", "🦁", "🐷", "🐸", "🐵",
            "🐔", "🐧", "🐍", "🐳", "🦋", "🌸", "🌻", "🌷",
            "🍀", "🍎", "🍌", "🍓", "🍉", "🍕", "🍔", "🍩",
            "☕", "⚽", "🏀", "🎸", "🚗", "✈️", "🚀", "⭐",
        )
    }
}
