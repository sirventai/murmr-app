package uoc.edu.psirvent.murmr.presentation.navigation

import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

@Composable
actual fun ExcludeLeftEdgeSystemGesture(widthDp: Int) {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
    val view = LocalView.current
    val density = LocalDensity.current
    val widthPx = with(density) { widthDp.dp.roundToPx() }

    DisposableEffect(view, widthPx) {
        fun apply() {
            if (view.width <= 0 || view.height <= 0) return
            view.systemGestureExclusionRects = listOf(Rect(0, 0, widthPx, view.height))
        }

        apply()
        val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> apply() }
        view.addOnLayoutChangeListener(listener)
        onDispose {
            view.removeOnLayoutChangeListener(listener)
            view.systemGestureExclusionRects = emptyList()
        }
    }
}
