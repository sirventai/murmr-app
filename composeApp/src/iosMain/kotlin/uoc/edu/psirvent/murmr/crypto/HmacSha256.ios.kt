package uoc.edu.psirvent.murmr.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.kCCHmacAlgSHA256

@OptIn(ExperimentalForeignApi::class)
actual object HmacSha256 {
    actual fun mac(key: ByteArray, data: ByteArray): ByteArray {
        val output = ByteArray(SHA256_OUTPUT_BYTES)
        val keyBuf = if (key.isEmpty()) ByteArray(1) else key
        val dataBuf = if (data.isEmpty()) ByteArray(1) else data
        keyBuf.usePinned { kp ->
            dataBuf.usePinned { dp ->
                output.usePinned { op ->
                    CCHmac(
                        algorithm = kCCHmacAlgSHA256,
                        key = if (key.isEmpty()) null else kp.addressOf(0),
                        keyLength = key.size.convert(),
                        data = if (data.isEmpty()) null else dp.addressOf(0),
                        dataLength = data.size.convert(),
                        macOut = op.addressOf(0),
                    )
                }
            }
        }
        return output
    }
}
