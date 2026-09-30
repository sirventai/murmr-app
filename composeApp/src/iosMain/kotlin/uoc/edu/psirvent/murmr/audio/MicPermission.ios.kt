package uoc.edu.psirvent.murmr.audio

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVAudioSessionRecordPermissionUndetermined
import uoc.edu.psirvent.murmr.util.Logger
import kotlin.coroutines.resume

actual class MicPermission actual constructor() {

    private val tag = "MicPermission"

    actual suspend fun ensure(): Boolean {
        val session = AVAudioSession.sharedInstance()
        when (session.recordPermission) {
            AVAudioSessionRecordPermissionGranted -> {
                Logger.i(tag, "RECORD permission already granted")
                return true
            }
            AVAudioSessionRecordPermissionDenied -> {
                Logger.w(tag, "RECORD permission previously denied; el usuario debe ir a Ajustes")
                return false
            }
            AVAudioSessionRecordPermissionUndetermined -> {
                Logger.i(tag, "RECORD permission undetermined, lanzando prompt al usuario")
            }
        }
        return suspendCancellableCoroutine { cont ->
            session.requestRecordPermission { granted ->
                Logger.i(tag, "RECORD permission grant=$granted")
                cont.resume(granted)
            }
        }
    }
}
