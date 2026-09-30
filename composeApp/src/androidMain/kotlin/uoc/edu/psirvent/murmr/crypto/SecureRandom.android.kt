package uoc.edu.psirvent.murmr.crypto

import java.security.SecureRandom as JdkSecureRandom

private val random = JdkSecureRandom()

actual object SecureRandom {
    actual fun nextBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        random.nextBytes(bytes)
        return bytes
    }
}
