package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors

@Composable
fun MurmrAvatar(
    initial: String? = null,
    size: Dp = 48.dp,
    avatar: ByteArray? = null,
    modifier: Modifier = Modifier,
) {

    val cachedAvatar = androidx.compose.runtime.remember(avatar?.contentHashCode()) { avatar }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (initial == null && cachedAvatar == null) MurmrColors.SurfaceDark else MurmrColors.PrimaryViolet),
        contentAlignment = Alignment.Center,
    ) {
        when {
            cachedAvatar != null -> AsyncImage(
                model = cachedAvatar,
                contentDescription = null,
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            initial == null -> Icon(
                imageVector = MurmrIcons.Person,
                contentDescription = null,
                tint = MurmrColors.OnSurfaceMuted,
                modifier = Modifier.size(size * 0.6f),
            )
            else -> {
                val sizeSp = (size.value * 0.42f).coerceAtLeast(12f)
                Text(
                    text = initial.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Color.White,
                    style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = sizeSp.sp),
                )
            }
        }
    }
}
