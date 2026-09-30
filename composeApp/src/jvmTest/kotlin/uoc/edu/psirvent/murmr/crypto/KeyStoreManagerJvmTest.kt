package uoc.edu.psirvent.murmr.crypto

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeyStoreManagerJvmTest {

    private val home = File(System.getProperty("user.home"), ".murmr")
    private val vault = File(home, "vault.txt")

    @BeforeTest
    fun cleanup() {
        if (vault.exists()) vault.delete()
    }

    @AfterTest
    fun teardown() {
        if (vault.exists()) vault.delete()
    }

    @Test
    fun storeAndLoad() {
        val mgr = KeyStoreManager()
        val key = SecureRandom.nextBytes(32)
        mgr.storeKey("session-1", key)
        assertTrue(mgr.hasKey("session-1"))
        assertContentEquals(key, mgr.loadKey("session-1"))
    }

    @Test
    fun deleteRemovesKey() {
        val mgr = KeyStoreManager()
        mgr.storeKey("k", ByteArray(32) { 0x42 })
        mgr.deleteKey("k")
        assertFalse(mgr.hasKey("k"))
        assertNull(mgr.loadKey("k"))
    }

    @Test
    fun multipleKeys() {
        val mgr = KeyStoreManager()
        val a = SecureRandom.nextBytes(32)
        val b = SecureRandom.nextBytes(32)
        mgr.storeKey("a", a)
        mgr.storeKey("b", b)
        assertContentEquals(a, mgr.loadKey("a"))
        assertContentEquals(b, mgr.loadKey("b"))
    }

    @Test
    fun overwriteAlias() {
        val mgr = KeyStoreManager()
        mgr.storeKey("k", ByteArray(32) { 0x01 })
        mgr.storeKey("k", ByteArray(32) { 0x02 })
        assertContentEquals(ByteArray(32) { 0x02 }, mgr.loadKey("k"))
    }
}
