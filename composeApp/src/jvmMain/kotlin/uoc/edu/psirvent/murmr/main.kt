package uoc.edu.psirvent.murmr

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import uoc.edu.psirvent.murmr.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Murmr",
        ) {
            App()
        }
    }
}
