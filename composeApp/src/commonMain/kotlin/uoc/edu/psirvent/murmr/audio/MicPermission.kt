package uoc.edu.psirvent.murmr.audio

expect class MicPermission() {

    suspend fun ensure(): Boolean
}
