package uoc.edu.psirvent.murmr.audio

expect class SystemVolume() {
    fun currentFraction(): Float

    fun tryRaiseToMax(): Boolean
}
