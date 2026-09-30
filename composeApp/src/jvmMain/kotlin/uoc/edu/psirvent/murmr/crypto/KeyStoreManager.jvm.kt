package uoc.edu.psirvent.murmr.crypto

import java.io.File
import java.util.Base64

actual class KeyStoreManager {

    private val vaultFile: File by lazy {
        val home = System.getProperty("user.home") ?: "."
        val dir = File(home, ".murmr").apply { mkdirs() }
        File(dir, "vault.txt").apply { if (!exists()) createNewFile() }
    }

    private val masterKey: ByteArray by lazy {
        val identity = listOf(
            System.getProperty("user.name") ?: "anon",
            System.getProperty("os.name") ?: "unknown",
            System.getProperty("os.arch") ?: "unknown",
        ).joinToString("|").encodeToByteArray()
        HkdfSha256.derive(
            ikm = identity,
            salt = "murmr-vault-v1".encodeToByteArray(),
            info = "vault-master".encodeToByteArray(),
            length = AES_KEY_BYTES,
        )
    }

    private fun loadAll(): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        if (!vaultFile.exists()) return map
        vaultFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains('=') }
            .forEach {
                val (k, v) = it.split('=', limit = 2)
                map[k] = v
            }
        return map
    }

    private fun saveAll(map: Map<String, String>) {
        vaultFile.writeText(map.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }

    actual fun storeKey(alias: String, key: ByteArray) {
        require(alias.isNotBlank())
        val iv = newAesGcmIv()
        val ct = AesGcm.encrypt(masterKey, iv, key)
        val payload = iv + ct.ciphertext + ct.authTag
        val map = loadAll()
        map[alias] = Base64.getEncoder().encodeToString(payload)
        saveAll(map)
    }

    actual fun loadKey(alias: String): ByteArray? {
        val raw = loadAll()[alias] ?: return null
        val payload = Base64.getDecoder().decode(raw)
        if (payload.size < AES_GCM_IV_BYTES + AES_GCM_TAG_BYTES) return null
        val iv = payload.copyOfRange(0, AES_GCM_IV_BYTES)
        val tag = payload.copyOfRange(payload.size - AES_GCM_TAG_BYTES, payload.size)
        val ct = payload.copyOfRange(AES_GCM_IV_BYTES, payload.size - AES_GCM_TAG_BYTES)
        return try {
            AesGcm.decrypt(masterKey, iv, ct, tag)
        } catch (_: AeadDecryptionFailed) {
            null
        }
    }

    actual fun deleteKey(alias: String) {
        val map = loadAll()
        map.remove(alias)
        saveAll(map)
    }

    actual fun hasKey(alias: String): Boolean = alias in loadAll()

    actual fun clear() {
        if (vaultFile.exists()) vaultFile.writeText("")
    }
}
