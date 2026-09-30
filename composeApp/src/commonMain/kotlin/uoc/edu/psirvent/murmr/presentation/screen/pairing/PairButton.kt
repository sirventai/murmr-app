package uoc.edu.psirvent.murmr.presentation.screen.pairing

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors

@Composable
internal fun PairButton(onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val infinite = rememberInfiniteTransition(label = "pair-pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse",
    )
    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxR = size.minDimension / 2f
            val baseR = maxR * 0.55f

            for (i in 0..1) {
                val phase = ((pulse + i * 0.5f) % 1f)
                val r = baseR + (maxR - baseR) * phase
                drawCircle(
                    color = MurmrColors.PrimaryViolet.copy(alpha = (1f - phase) * 0.35f),
                    radius = r,
                    style = Stroke(width = size.minDimension * 0.012f),
                )
            }
        }
        Surface(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            color = MurmrColors.PrimaryViolet,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "🤝",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 38.sp),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Emparejar",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 0.2.sp,
                    ),
                    color = Color.White,
                )
            }
        }
    }
}
