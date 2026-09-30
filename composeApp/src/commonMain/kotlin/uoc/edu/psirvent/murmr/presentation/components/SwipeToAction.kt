package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors

@Composable
fun SwipeToActionRow(
    actionLabel: String,
    actionIcon: ImageVector,
    onAction: () -> Unit,
    actionBackground: Color = MurmrColors.Error,
    thresholdDp: Int = 88,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { thresholdDp.dp.toPx() }
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var width by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxWidth().onSizeChanged { width = it.width }) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(actionBackground)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Icon(imageVector = actionIcon, contentDescription = actionLabel, tint = Color.White)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offset.value }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (-offset.value > thresholdPx) {
                                    offset.animateTo(-width.toFloat(), tween(180))
                                    onAction()
                                    offset.snapTo(0f)
                                } else {
                                    offset.animateTo(0f, tween(180))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(0f, tween(180)) }
                        },
                        onHorizontalDrag = { change, drag ->
                            change.consume()
                            scope.launch {
                                val target = (offset.value + drag).coerceAtMost(0f)
                                offset.snapTo(target)
                            }
                        },
                    )
                }
                .background(MurmrColors.BackgroundDark),
        ) {
            if (offset.value < -8f) {
                Text(
                    actionLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp),
                )
            }
            content()
        }
    }
}
