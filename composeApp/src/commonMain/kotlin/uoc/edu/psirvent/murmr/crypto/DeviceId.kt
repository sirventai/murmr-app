package uoc.edu.psirvent.murmr.crypto

fun deviceIdFromPublicKey(publicKey: ByteArray): String {
    require(publicKey.size >= 16) { "publicKey debe tener al menos 16 bytes" }
    val hex = "0123456789abcdef"
    val sb = StringBuilder()
    for (i in 0 until 16) {
        val v = publicKey[i].toInt() and 0xFF
        sb.append(hex[v ushr 4])
        sb.append(hex[v and 0x0F])
    }
    return "pk-${sb.substring(0, 8)}-${sb.substring(8, 16)}-${sb.substring(16, 32)}"
}

fun bytesToUuidString(bytes: ByteArray): String {
    require(bytes.size == 16) { "UUID requires 16 bytes" }
    val hex = "0123456789abcdef"
    val sb = StringBuilder(36)
    for (i in 0 until 16) {
        val v = bytes[i].toInt() and 0xFF
        sb.append(hex[v ushr 4]); sb.append(hex[v and 0x0F])
        if (i == 3 || i == 5 || i == 7 || i == 9) sb.append('-')
    }
    return sb.toString()
}
