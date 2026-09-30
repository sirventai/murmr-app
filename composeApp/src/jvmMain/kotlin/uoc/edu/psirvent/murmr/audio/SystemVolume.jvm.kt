package uoc.edu.psirvent.murmr.audio

actual class SystemVolume actual constructor() {
    actual fun currentFraction(): Float = 1.0f
    actual fun tryRaiseToMax(): Boolean = false
}
