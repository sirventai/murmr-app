package uoc.edu.psirvent.murmr.data.network

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uoc.edu.psirvent.murmr.util.ValidationResult
import uoc.edu.psirvent.murmr.util.Validators

class RelayConfigStore(
    private val settings: Settings,
    private val officialUrl: String,
) {
    private val _flow = MutableStateFlow(currentInternal())
    val flow: StateFlow<RelayConfig> = _flow.asStateFlow()

    fun current(): RelayConfig = _flow.value

    fun setOfficial() {
        settings.remove(KEY_URL)
        _flow.value = RelayConfig(officialUrl)
    }

    fun setCustom(url: String): ValidationResult {
        val cleaned = url.trim().trimEnd('/')
        val v = Validators.relayUrl(cleaned)
        if (!v.isValid) return v
        settings.putString(KEY_URL, cleaned)
        _flow.value = RelayConfig(cleaned)
        return ValidationResult.Ok
    }

    fun isOfficial(): Boolean = settings.getStringOrNull(KEY_URL) == null

    private fun currentInternal(): RelayConfig {
        val custom = settings.getStringOrNull(KEY_URL)
        return RelayConfig(custom?.takeIf { it.isNotBlank() } ?: officialUrl)
    }

    private fun Settings.getStringOrNull(key: String): String? =
        if (hasKey(key)) getString(key, "") else null

    companion object {
        private const val KEY_URL = "murmr.relay.url"
    }
}
