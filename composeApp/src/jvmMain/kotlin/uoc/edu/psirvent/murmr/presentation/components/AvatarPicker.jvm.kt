package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberAvatarPicker(onResult: (ByteArray?) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onResult)
    return {
        val chooser = JFileChooser().apply {
            dialogTitle = "Selecciona avatar"
            fileFilter = FileNameExtensionFilter("Imágenes (JPG/PNG/WebP)", "jpg", "jpeg", "png", "webp")
            isAcceptAllFileFilterUsed = false
        }
        when (chooser.showOpenDialog(null)) {
            JFileChooser.APPROVE_OPTION -> {
                val bytes = runCatching { chooser.selectedFile.readBytes() }.getOrNull()
                callback.value(bytes)
            }
            else -> callback.value(null)
        }
    }
}
