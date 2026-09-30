package uoc.edu.psirvent.murmr.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

actual class KeyStoreManager(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    actual fun storeKey(alias: String, key: ByteArray) {
        require(alias.isNotBlank()) { "alias must not be blank" }
        prefs.edit().putString(alias, encodeBase64(key)).apply()
    }

    actual fun loadKey(alias: String): ByteArray? =
        prefs.getString(alias, null)?.let(::decodeBase64)

    actual fun deleteKey(alias: String) {
        prefs.edit().remove(alias).apply()
    }

    actual fun hasKey(alias: String): Boolean = prefs.contains(alias)

    actual fun clear() {
        prefs.edit().clear().apply()
    }

    private fun encodeBase64(bytes: ByteArray): String =
        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

    private fun decodeBase64(s: String): ByteArray =
        android.util.Base64.decode(s, android.util.Base64.NO_WRAP)

    private companion object {
        const val MASTER_KEY_ALIAS = "murmr_master_key"
        const val FILE_NAME = "murmr_keys.prefs"
    }
}
