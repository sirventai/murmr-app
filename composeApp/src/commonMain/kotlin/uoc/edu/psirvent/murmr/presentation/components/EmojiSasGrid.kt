package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import uoc.edu.psirvent.murmr.domain.model.Sas
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors

@Composable
fun EmojiSelectedRow(selected: List<String>, expectedSize: Int = Sas.EMOJI_COUNT) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        for (i in 0 until expectedSize) {
            val emoji = selected.getOrNull(i)
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(56.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MurmrColors.SurfaceDark),
                contentAlignment = Alignment.Center,
            ) {
                if (emoji != null) {
                    Text(emoji, style = MaterialTheme.typography.headlineMedium)
                } else {
                    Text("•", color = MurmrColors.OnSurfaceFaint)
                }
            }
        }
    }
}

@Composable
fun EmojiPickerGrid(
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
    ) {
        items(Sas.EMOJI_ALPHABET.size) { index ->
            val emoji = Sas.EMOJI_ALPHABET[index]
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MurmrColors.IncomingBubble)
                    .clickable(enabled = enabled) { onPick(emoji) },
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.items(
    count: Int,
    itemContent: @Composable androidx.compose.foundation.lazy.grid.LazyGridItemScope.(Int) -> Unit,
) {
    for (i in 0 until count) item { itemContent(i) }
}
