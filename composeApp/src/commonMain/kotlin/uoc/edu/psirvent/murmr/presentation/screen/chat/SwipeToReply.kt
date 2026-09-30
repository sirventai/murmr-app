package uoc.edu.psirvent.murmr.presentation.screen.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
internal fun SwipeToReply(
    onReply: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { 56.dp.toPx() }
    val maxOffsetPx = with(density) { 80.dp.toPx() }

    val edgeWidthPx = with(density) { 36.dp.toPx() }
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val triggered = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = offset.value }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                    val startedInEdge = down.position.x < edgeWidthPx
                    val touchSlop = viewConfiguration.touchSlop
                    var totalDx = 0f
                    var totalDy = 0f
                    var passedSlop = false
                    var accepted = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            triggered.value = false
                            scope.launch { offset.animateTo(0f, tween(160)) }
                            break
                        }
                        val delta = change.positionChange()
                        totalDx += delta.x
                        totalDy += delta.y
                        if (!passedSlop) {
                            if (abs(totalDy) > abs(totalDx) + touchSlop) break
                            if (abs(totalDx) > touchSlop) {
                                passedSlop = true
                                if (totalDx > 0f && startedInEdge) break
                                accepted = true
                            }
                        }
                        if (accepted) {
                            change.consume()
                            scope.launch {
                                val limited = (offset.value + delta.x).coerceIn(-maxOffsetPx, maxOffsetPx)
                                offset.snapTo(limited)
                                if (!triggered.value && abs(limited) > thresholdPx) {
                                    triggered.value = true
                                    onReply()
                                }
                            }
                        }
                    }
                }
            },
    ) {
        content()
    }
}
