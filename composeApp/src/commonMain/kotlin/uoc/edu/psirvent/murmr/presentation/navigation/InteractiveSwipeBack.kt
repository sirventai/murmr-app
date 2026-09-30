package uoc.edu.psirvent.murmr.presentation.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun InteractiveSwipeBack(
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    edgeWidthDp: Int = 36,
    completeRatio: Float = 0.35f,
    backgroundParallax: Float = 0.3f,
    background: @Composable () -> Unit,
    foreground: @Composable () -> Unit,
) {

    ExcludeLeftEdgeSystemGesture(edgeWidthDp)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val edgeWidthPx = with(density) { edgeWidthDp.dp.toPx() }
        val offsetX = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()

        var bgMounted by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val ratio = (offsetX.value / widthPx).coerceIn(0f, 1f)
                    translationX = -widthPx * backgroundParallax * (1f - ratio)
                    alpha = if (offsetX.value > 0f) 1f else 0f
                },
        ) {
            if (bgMounted) background()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = offsetX.value }
                .pointerInput(widthPx, edgeWidthPx) {
                    awaitEdgeSwipeGesture(
                        onTouchInsideEdge = { bgMounted = true },
                        edgeWidthPx = edgeWidthPx,
                        onDragDelta = { dx ->
                            scope.launch {
                                offsetX.snapTo((offsetX.value + dx).coerceIn(0f, widthPx))
                            }
                        },
                        onRelease = {
                            scope.launch {
                                val triggered = offsetX.value > widthPx * completeRatio
                                if (triggered) {
                                    offsetX.animateTo(widthPx, tween(180))
                                    onCompleted()
                                } else {
                                    offsetX.animateTo(0f, tween(180))
                                }
                            }
                        },
                    )
                },
        ) {
            foreground()
        }
    }
}

private suspend fun PointerInputScope.awaitEdgeSwipeGesture(
    onTouchInsideEdge: () -> Unit,
    edgeWidthPx: Float,
    onDragDelta: (Float) -> Unit,
    onRelease: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (down.position.x > edgeWidthPx) return@awaitEachGesture
        onTouchInsideEdge()

        val touchSlop = viewConfiguration.touchSlop
        var totalDx = 0f
        var totalDy = 0f
        var passedSlop = false
        val pointerId: PointerId = down.id

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change: PointerInputChange = event.changes.firstOrNull { it.id == pointerId }
                ?: break
            if (!change.pressed) {
                if (passedSlop) onRelease()
                break
            }
            val delta = change.positionChange()
            totalDx += delta.x
            totalDy += delta.y

            if (!passedSlop) {
                if (abs(totalDy) > abs(totalDx) + touchSlop) {
                    return@awaitEachGesture
                }
                if (abs(totalDx) > touchSlop) {
                    passedSlop = true

                    change.consume()
                    onDragDelta(totalDx)
                    continue
                }
            }

            if (passedSlop) {
                change.consume()
                onDragDelta(delta.x)
            }
        }
    }
}

@Composable
fun PreviousScreenBackground() {
    val navigator = LocalNavigator.current ?: return
    val items = navigator.items
    if (items.size < 2) return
    val previous = items[items.size - 2]
    previous.Content()
}
