package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uoc.edu.psirvent.murmr.domain.model.MessageStatus
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    text: String,
    timestamp: String,
    isOutgoing: Boolean,
    status: MessageStatus,
    modifier: Modifier = Modifier,
    quotedText: String? = null,
    quotedAuthor: String? = null,
    selected: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onTap: (() -> Unit)? = null,
) {
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start
    val bubbleShape = RoundedCornerShape(18.dp)
    val baseBg: Color = if (isOutgoing) MurmrColors.PrimaryViolet else MurmrColors.IncomingBubble
    val bgColor: Color = if (selected) {
        MurmrColors.AccentCyan.copy(alpha = 0.4f).compositeOver(baseBg)
    } else {
        baseBg
    }
    val textColor = if (isOutgoing) Color.White else MurmrColors.OnSurface

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalAlignment = alignment,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bgColor)
                .let { m ->
                    if (onLongPress != null || onTap != null) {
                        m.combinedClickable(
                            onClick = { onTap?.invoke() },
                            onLongClick = { onLongPress?.invoke() },
                        )
                    } else m
                },
        ) {

            val timestampSpace = "            "
            val statusSpace = if (isOutgoing) "     " else ""
            Column(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 7.dp, bottom = 8.dp),
            ) {
                if (quotedText != null) {
                    val quoteBg = if (isOutgoing) Color.White.copy(alpha = 0.18f)
                    else MurmrColors.PrimaryViolet.copy(alpha = 0.18f)
                    val quoteAuthorColor = if (isOutgoing) Color.White
                    else MurmrColors.PrimaryViolet
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(quoteBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        androidx.compose.foundation.layout.Column {
                            if (quotedAuthor != null) {
                                Text(
                                    text = quotedAuthor,
                                    color = quoteAuthorColor,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    ),
                                    maxLines = 1,
                                )
                            }
                            Text(
                                text = quotedText,
                                color = if (isOutgoing) Color.White.copy(alpha = 0.85f) else MurmrColors.OnSurfaceMuted,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                maxLines = 2,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = text + timestampSpace + statusSpace,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    timestamp,
                    color = if (isOutgoing) Color.White.copy(alpha = 0.75f) else MurmrColors.OnSurfaceFaint,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Light),
                )
                if (isOutgoing) {
                    Spacer(Modifier.width(3.dp))
                    val statusColor = if (status == MessageStatus.READ) {
                        MurmrColors.AccentCyan
                    } else {
                        Color.White.copy(alpha = 0.85f)
                    }
                    Text(
                        text = statusGlyph(status),
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    )
                }
            }
        }
    }
}

private fun statusGlyph(status: MessageStatus): String = when (status) {
    MessageStatus.PENDING, MessageStatus.SENDING -> "-"
    MessageStatus.SENT -> "✓"
    MessageStatus.DELIVERED -> "✓✓"
    MessageStatus.READ -> "✓✓"
    MessageStatus.FAILED -> "!"
}
