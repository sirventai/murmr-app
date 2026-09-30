package uoc.edu.psirvent.murmr.presentation.screen.pairing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors

@Composable
internal fun PhaseStepper(currentPhase: Int, modifier: Modifier = Modifier) {
    val phases = listOf("Buscar", "Conectar", "Cifrar", "Listo")
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
    ) {
        phases.forEachIndexed { idx, label ->
            val active = idx <= currentPhase
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(24.dp),
                    contentAlignment = Alignment.Center,
                ) {

                    Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                        val w = size.width
                        val centerX = w / 2f
                        val lineColor = if (active) MurmrColors.PrimaryViolet else MurmrColors.SurfaceDark
                        if (idx > 0) {
                            drawLine(
                                color = lineColor,
                                start = Offset(0f, size.height / 2),
                                end = Offset(centerX - 12.dp.toPx(), size.height / 2),
                                strokeWidth = size.height,
                            )
                        }
                        if (idx < phases.size - 1) {
                            val nextActive = (idx + 1) <= currentPhase
                            drawLine(
                                color = if (nextActive) MurmrColors.PrimaryViolet else MurmrColors.SurfaceDark,
                                start = Offset(centerX + 12.dp.toPx(), size.height / 2),
                                end = Offset(w, size.height / 2),
                                strokeWidth = size.height,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (active) MurmrColors.PrimaryViolet else MurmrColors.SurfaceDark),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (idx < currentPhase) {
                            Text("✓", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                "${idx + 1}",
                                fontSize = 11.sp,
                                color = if (active) Color.White else MurmrColors.OnSurfaceFaint,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    fontSize = 10.sp,
                    color = if (active) MurmrColors.OnSurface else MurmrColors.OnSurfaceFaint,
                    fontWeight = if (idx == currentPhase) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
