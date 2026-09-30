package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors

@Composable
fun CollapsingHeaderOverlay(title: String, listState: LazyListState) {
    val collapseProgress by remember(listState) {
        derivedStateOf {
            val firstIdx = listState.firstVisibleItemIndex
            if (firstIdx > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 80f).coerceIn(0f, 1f)
        }
    }
    CollapsingHeaderInner(title = title, alpha = collapseProgress)
}

@Composable
fun CollapsingHeaderOverlay(title: String, scrollState: ScrollState) {
    val collapseProgress by remember(scrollState) {
        derivedStateOf { (scrollState.value / 80f).coerceIn(0f, 1f) }
    }
    CollapsingHeaderInner(title = title, alpha = collapseProgress)
}

@Composable
private fun CollapsingHeaderInner(title: String, alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .background(MurmrColors.BackgroundDark.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
            ),
            color = MurmrColors.OnSurface,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MurmrColors.DividerDark.copy(alpha = 0.6f)),
        )
    }
}
