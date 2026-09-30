package uoc.edu.psirvent.murmr.crypto

expect class KeyStoreManager {
    fun storeKey(alias: String, key: ByteArray)
    fun loadKey(alias: String): ByteArray?
    fun deleteKey(alias: String)
    fun hasKey(alias: String): Boolean
    fun clear()
}
