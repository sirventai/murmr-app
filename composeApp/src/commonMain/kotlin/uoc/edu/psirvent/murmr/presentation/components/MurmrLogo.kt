package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.theme.MurmrPrimaryGradient

@Composable
fun MurmrLogo(
    size: Dp = 96.dp,
    waveColor: Color = MurmrColors.OnSurface,
    background: Brush = MurmrPrimaryGradient,
    cornerPercent: Int = 22,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape((size.value * cornerPercent / 100f).dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val sx = w / 100f
            val sy = h / 100f
            val pts = listOf(
                Offset(10f, 50f),
                Offset(26f, 50f),
                Offset(30f, 74f),
                Offset(42f, 26f),
                Offset(50f, 50f),
                Offset(58f, 26f),
                Offset(70f, 74f),
                Offset(74f, 50f),
                Offset(90f, 50f),
            ).map { Offset(it.x * sx, it.y * sy) }

            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
            }
            val strokePx = (size.value * 0.05f).coerceAtLeast(1.5f) * sx
            drawPath(
                path = path,
                color = waveColor,
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.cornerPathEffect(2f),
                ),
            )
        }
    }
}
