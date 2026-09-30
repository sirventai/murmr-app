package uoc.edu.psirvent.murmr.crypto

expect object SecureRandom {
    fun nextBytes(size: Int): ByteArray
}
