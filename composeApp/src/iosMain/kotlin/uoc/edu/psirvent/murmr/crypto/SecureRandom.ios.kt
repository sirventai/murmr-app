package uoc.edu.psirvent.murmr.crypto

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault

@OptIn(ExperimentalForeignApi::class)
actual object SecureRandom {
    actual fun nextBytes(size: Int): ByteArray {
        if (size == 0) return ByteArray(0)
        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            val ptr = pinned.addressOf(0)
            val rc = SecRandomCopyBytes(kSecRandomDefault, size.convert(), ptr)
            require(rc == errSecSuccess) { "SecRandomCopyBytes failed: $rc" }
        }
        return bytes
    }
}
