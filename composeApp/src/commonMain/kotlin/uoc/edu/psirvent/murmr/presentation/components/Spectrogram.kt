package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors

private const val BAR_COUNT = 36

@Composable
fun Spectrogram(
    bins: List<Float> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val animated = remember { mutableStateOf(emptyList<Float>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(bins) {
        if (bins.isEmpty()) {
            scope.launch {
                var phase = 0f
                while (true) {
                    val list = (0 until BAR_COUNT).map { i ->
                        val n = i.toFloat() / BAR_COUNT
                        val s = (sin((phase + n * 6) * 2f * PI.toFloat()) * 0.4f) + 0.5f
                        s.coerceIn(0.05f, 1f)
                    }
                    animated.value = list
                    phase += 0.04f
                    delay(50)
                }
            }
        } else {
            animated.value = bins
        }
    }

    val data by animated
    Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
        if (data.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val barWidth = w / (data.size * 1.6f)
        val gap = barWidth * 0.6f
        var x = (w - (data.size * (barWidth + gap) - gap)) / 2f
        val cy = h / 2f
        data.forEach { amp ->
            val barH = (amp * h * 0.85f).coerceAtLeast(2f)
            val brush = Brush.verticalGradient(
                colors = listOf(MurmrColors.PrimaryViolet, MurmrColors.AccentCyan),
                startY = cy - barH / 2f,
                endY = cy + barH / 2f,
            )
            drawRoundedBar(brush, Offset(x, cy - barH / 2f), barWidth, barH)
            x += barWidth + gap
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundedBar(
    brush: Brush,
    topLeft: Offset,
    width: Float,
    height: Float,
) {
    val radius = androidx.compose.ui.geometry.CornerRadius(width / 2f, width / 2f)
    drawRoundRect(brush = brush, topLeft = topLeft, size = androidx.compose.ui.geometry.Size(width, height), cornerRadius = radius)
}
