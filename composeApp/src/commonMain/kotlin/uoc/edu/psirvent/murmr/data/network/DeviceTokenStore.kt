package uoc.edu.psirvent.murmr.data.network

import com.russhwolf.settings.Settings

class DeviceTokenStore(private val settings: Settings) {

    fun get(): String? = settings.getStringOrNull(KEY_TOKEN)

    fun put(token: String) {
        settings.putString(KEY_TOKEN, token)
    }

    fun clear() {
        settings.remove(KEY_TOKEN)
    }

    private fun Settings.getStringOrNull(key: String): String? =
        if (hasKey(key)) getString(key, "") else null

    companion object {
        private const val KEY_TOKEN = "murmr.relay.token"
    }
}
