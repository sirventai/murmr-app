package uoc.edu.psirvent.murmr.crypto

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFArrayCreate
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeArrayCallBacks
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import uoc.edu.psirvent.murmr.util.Logger

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class KeyStoreManager {

    private val tag = "KeyStoreIOS"
    private val service = "uoc.edu.psirvent.murmr.keys"

    actual fun storeKey(alias: String, key: ByteArray) {
        deleteKey(alias)
        memScoped {
            val serviceRef = CFBridgingRetain(service) as CFTypeRef
            val accountRef = CFBridgingRetain(alias) as CFTypeRef
            val dataRef = CFBridgingRetain(key.toNSData()) as CFTypeRef
            try {
                val query = createQueryDict(
                    listOf(kSecClass, kSecAttrService, kSecAttrAccount,
                        kSecValueData, kSecAttrAccessible),
                    listOf(kSecClassGenericPassword, serviceRef, accountRef,
                        dataRef, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly),
                )
                val status = SecItemAdd(query, null)
                if (status != errSecSuccess) {
                    Logger.w(tag, "SecItemAdd($alias) status=$status")
                }
                CFBridgingRelease(query)
            } finally {
                CFBridgingRelease(serviceRef)
                CFBridgingRelease(accountRef)
                CFBridgingRelease(dataRef)
            }
        }
    }

    actual fun loadKey(alias: String): ByteArray? = memScoped {
        val serviceRef = CFBridgingRetain(service) as CFTypeRef
        val accountRef = CFBridgingRetain(alias) as CFTypeRef
        try {
            val query = createQueryDict(
                listOf(kSecClass, kSecAttrService, kSecAttrAccount,
                    kSecReturnData, kSecMatchLimit),
                listOf(kSecClassGenericPassword, serviceRef, accountRef,
                    kCFBooleanTrue, kSecMatchLimitOne),
            )
            val resultRef = allocPointerTo<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, resultRef.ptr.reinterpret())
            CFBridgingRelease(query)
            when (status) {
                errSecSuccess -> {
                    val nsData = CFBridgingRelease(resultRef.value) as? NSData
                    nsData?.toByteArray()
                }
                errSecItemNotFound -> null
                else -> {
                    Logger.w(tag, "SecItemCopyMatching($alias) status=$status")
                    null
                }
            }
        } finally {
            CFBridgingRelease(serviceRef)
            CFBridgingRelease(accountRef)
        }
    }

    actual fun deleteKey(alias: String) {
        memScoped {
            val serviceRef = CFBridgingRetain(service) as CFTypeRef
            val accountRef = CFBridgingRetain(alias) as CFTypeRef
            try {
                val query = createQueryDict(
                    listOf(kSecClass, kSecAttrService, kSecAttrAccount),
                    listOf(kSecClassGenericPassword, serviceRef, accountRef),
                )
                val status = SecItemDelete(query)
                CFBridgingRelease(query)
                if (status != errSecSuccess && status != errSecItemNotFound) {
                    Logger.w(tag, "SecItemDelete($alias) status=$status")
                }
            } finally {
                CFBridgingRelease(serviceRef)
                CFBridgingRelease(accountRef)
            }
        }
    }

    actual fun hasKey(alias: String): Boolean = loadKey(alias) != null

    actual fun clear() {
        memScoped {
            val serviceRef = CFBridgingRetain(service) as CFTypeRef
            try {
                val query = createQueryDict(
                    listOf(kSecClass, kSecAttrService),
                    listOf(kSecClassGenericPassword, serviceRef),
                )
                val status = SecItemDelete(query)
                CFBridgingRelease(query)
                if (status != errSecSuccess && status != errSecItemNotFound) {
                    Logger.w(tag, "SecItemDelete(all) status=$status")
                }
            } finally {
                CFBridgingRelease(serviceRef)
            }
        }
    }

    private fun createQueryDict(keys: List<CFTypeRef?>, values: List<CFTypeRef?>): CFDictionaryRef {
        require(keys.size == values.size) { "keys/values size mismatch" }
        memScoped {
            val n = keys.size
            val keysArrayPtr = allocArray<COpaquePointerVar>(n)
            val valuesArrayPtr = allocArray<COpaquePointerVar>(n)
            for (i in 0 until n) {
                keysArrayPtr[i] = keys[i]
                valuesArrayPtr[i] = values[i]
            }
            return CFDictionaryCreate(
                kCFAllocatorDefault,
                keysArrayPtr.reinterpret(),
                valuesArrayPtr.reinterpret(),
                n.toLong(),
                kCFTypeDictionaryKeyCallBacks.ptr,
                kCFTypeDictionaryValueCallBacks.ptr,
            ) ?: error("CFDictionaryCreate devolvió null")
        }
    }

    private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
    }

    private fun NSData.toByteArray(): ByteArray {
        val len = this.length.toInt()
        if (len == 0) return ByteArray(0)
        val out = ByteArray(len)
        out.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), this.bytes, len.toULong())
        }
        return out
    }
}

