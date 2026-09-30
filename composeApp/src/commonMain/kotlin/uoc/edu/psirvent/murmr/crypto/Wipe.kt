package uoc.edu.psirvent.murmr.crypto

fun ByteArray.zeroize() {
    for (i in indices) this[i] = 0
}
