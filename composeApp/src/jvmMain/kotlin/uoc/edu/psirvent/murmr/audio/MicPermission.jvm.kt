package uoc.edu.psirvent.murmr.audio

actual class MicPermission actual constructor() {
    actual suspend fun ensure(): Boolean = true
}
