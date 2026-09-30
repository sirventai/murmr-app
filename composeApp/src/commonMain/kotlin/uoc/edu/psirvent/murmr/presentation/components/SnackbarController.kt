package uoc.edu.psirvent.murmr.presentation.components

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import uoc.edu.psirvent.murmr.util.Logger

class SnackbarController {
    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun show(text: String) {
        val emitted = _messages.tryEmit(text)
        if (!emitted) {
            Logger.w("Snackbar", "Mensaje descartado: '$text'")
        }
    }
}
