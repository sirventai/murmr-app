package uoc.edu.psirvent.murmr.presentation.screen.pairing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors

@Composable
internal fun PhaseVisual(phaseIdx: Int) {
    when (phaseIdx) {
        0 -> DetectingVisual()
        1 -> ConnectedVisual()
        2 -> EncryptingVisual()
        else -> DoneVisual()
    }
}

@Composable
private fun DetectingVisual() {
    val infinite = rememberInfiniteTransition(label = "detect")
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxR = size.minDimension / 2 * 0.9f
        for (i in 0..2) {
            val phase = ((pulse + i / 3f) % 1f)
            drawCircle(
                color = MurmrColors.PrimaryViolet.copy(alpha = (1f - phase) * 0.45f),
                radius = maxR * 0.25f + maxR * 0.7f * phase,
                center = center,
                style = Stroke(width = size.minDimension * 0.014f),
            )
        }
        drawCircle(
            color = MurmrColors.PrimaryViolet,
            radius = maxR * 0.18f,
            center = center,
        )
        drawCircle(
            color = Color.White,
            radius = maxR * 0.06f,
            center = center,
        )
    }
}

@Composable
private fun ConnectedVisual() {
    val infinite = rememberInfiniteTransition(label = "conn")
    val flow by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "flow",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val left = Offset(w * 0.22f, h * 0.5f)
        val right = Offset(w * 0.78f, h * 0.5f)
        val phoneR = w * 0.11f
        drawLine(
            color = MurmrColors.PrimaryViolet.copy(alpha = 0.35f),
            start = left,
            end = right,
            strokeWidth = w * 0.006f,
            cap = StrokeCap.Round,
        )
        for (i in 0..3) {
            val t = (flow + i * 0.25f) % 1f
            val sizeFactor = (1f - abs(0.5f - t) * 1.6f).coerceIn(0.3f, 1f)
            val x = left.x + (right.x - left.x) * t
            drawCircle(
                color = MurmrColors.PrimaryViolet,
                radius = w * 0.018f * sizeFactor,
                center = Offset(x, left.y),
            )
        }
        drawCircle(MurmrColors.PrimaryViolet, radius = phoneR, center = left)
        drawCircle(Color.White, radius = phoneR * 0.4f, center = left)
        drawCircle(MurmrColors.AccentCyan, radius = phoneR, center = right)
        drawCircle(Color.White, radius = phoneR * 0.4f, center = right)
    }
}

@Composable
private fun EncryptingVisual() {
    val infinite = rememberInfiniteTransition(label = "enc")
    val rot by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rot",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerR = size.minDimension / 2 * 0.75f
        rotate(rot, pivot = center) {
            for (i in 0..5) {
                val angle = i * 60f * PI.toFloat() / 180f
                val x = center.x + outerR * cos(angle)
                val y = center.y + outerR * sin(angle)
                drawCircle(
                    color = MurmrColors.PrimaryViolet.copy(alpha = 0.35f + (i / 5f) * 0.6f),
                    radius = outerR * 0.09f,
                    center = Offset(x, y),
                )
            }
        }
        val sz = outerR * 0.55f
        drawRoundRect(
            color = MurmrColors.PrimaryViolet,
            topLeft = Offset(center.x - sz / 2, center.y - sz * 0.15f),
            size = Size(sz, sz * 0.85f),
            cornerRadius = CornerRadius(sz * 0.12f),
        )
        drawArc(
            color = MurmrColors.PrimaryViolet,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(center.x - sz * 0.32f, center.y - sz * 0.65f),
            size = Size(sz * 0.64f, sz * 0.64f),
            style = Stroke(width = sz * 0.14f, cap = StrokeCap.Round),
        )
        drawCircle(
            color = Color.White,
            radius = sz * 0.08f,
            center = Offset(center.x, center.y + sz * 0.18f),
        )
    }
}

@Composable
private fun DoneVisual() {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    val v = anim.value
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxR = size.minDimension / 2 * 0.55f
        drawCircle(
            color = MurmrColors.PrimaryViolet.copy(alpha = (1f - v) * 0.4f),
            radius = maxR * (1f + v * 0.6f),
            center = center,
        )
        val discR = maxR * v.coerceAtLeast(0.2f)
        drawCircle(
            color = MurmrColors.PrimaryViolet,
            radius = discR,
            center = center,
        )

        if (v > 0.3f) {
            val checkP = ((v - 0.3f) / 0.7f).coerceIn(0f, 1f)
            val p1 = Offset(center.x - discR * 0.45f, center.y + discR * 0.05f)
            val p2 = Offset(center.x - discR * 0.1f, center.y + discR * 0.35f)
            val p3 = Offset(center.x + discR * 0.45f, center.y - discR * 0.3f)
            val firstHalf = (checkP / 0.45f).coerceIn(0f, 1f)
            drawLine(
                color = Color.White,
                start = p1,
                end = Offset(
                    p1.x + (p2.x - p1.x) * firstHalf,
                    p1.y + (p2.y - p1.y) * firstHalf,
                ),
                strokeWidth = discR * 0.16f,
                cap = StrokeCap.Round,
            )
            val secondHalf = ((checkP - 0.45f) / 0.55f).coerceIn(0f, 1f)
            if (secondHalf > 0f) {
                drawLine(
                    color = Color.White,
                    start = p2,
                    end = Offset(
                        p2.x + (p3.x - p2.x) * secondHalf,
                        p2.y + (p3.y - p2.y) * secondHalf,
                    ),
                    strokeWidth = discR * 0.16f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
