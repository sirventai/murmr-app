package uoc.edu.psirvent.murmr.presentation.screen.pairing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors

@Composable
internal fun IdleIllustration(size: Int) {
    Canvas(modifier = Modifier.size(size.dp)) {
        val w = this.size.width
        val h = this.size.height
        val white = Color(0xFFEDEDED)
        val violet = MurmrColors.PrimaryViolet
        val cyan = MurmrColors.AccentCyan

        rotateAround(-15f, w * 0.27f, h * 0.55f) {
            drawRoundRect(
                color = white,
                topLeft = Offset(w * 0.12f, h * 0.30f),
                size = Size(w * 0.30f, h * 0.50f),
                cornerRadius = CornerRadius(w * 0.04f),
            )
            drawRoundRect(
                color = MurmrColors.SurfaceDark,
                topLeft = Offset(w * 0.16f, h * 0.34f),
                size = Size(w * 0.22f, h * 0.42f),
                cornerRadius = CornerRadius(w * 0.02f),
            )
            drawCircle(color = violet, radius = w * 0.05f, center = Offset(w * 0.27f, h * 0.55f))
        }

        rotateAround(15f, w * 0.73f, h * 0.55f) {
            drawRoundRect(
                color = white,
                topLeft = Offset(w * 0.58f, h * 0.30f),
                size = Size(w * 0.30f, h * 0.50f),
                cornerRadius = CornerRadius(w * 0.04f),
            )
            drawRoundRect(
                color = MurmrColors.SurfaceDark,
                topLeft = Offset(w * 0.62f, h * 0.34f),
                size = Size(w * 0.22f, h * 0.42f),
                cornerRadius = CornerRadius(w * 0.02f),
            )
            drawCircle(color = cyan, radius = w * 0.05f, center = Offset(w * 0.73f, h * 0.55f))
        }

        for (i in 0..2) {
            val r = (i + 1) * w * 0.06f
            drawArc(
                color = cyan.copy(alpha = 1f - i * 0.25f),
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(w * 0.50f - r - w * 0.02f, h * 0.55f - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = w * 0.012f),
            )
            drawArc(
                color = cyan.copy(alpha = 1f - i * 0.25f),
                startAngle = 135f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(w * 0.50f - r + w * 0.02f, h * 0.55f - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = w * 0.012f),
            )
        }
    }
}

private fun DrawScope.rotateAround(
    angleDeg: Float,
    pivotX: Float,
    pivotY: Float,
    block: DrawScope.() -> Unit,
) {
    rotate(degrees = angleDeg, pivot = Offset(pivotX, pivotY)) { block() }
}
