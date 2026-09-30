package uoc.edu.psirvent.murmr.util

expect fun compressAvatar(
    bytes: ByteArray,
    maxSidePx: Int = 256,
    quality: Int = 85,
): ByteArray
