package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors

@Composable
fun IllustrationLockedPhone(size: Dp = 200.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val violet = MurmrColors.PrimaryViolet
        val violetLight = MurmrColors.PrimaryVioletLight
        val violetDark = MurmrColors.PrimaryVioletDark
        val cyan = MurmrColors.AccentCyan

        val cx = w / 2f
        val cy = h / 2f

        for (i in 4 downTo 1) {
            val r = w * (0.30f + i * 0.06f)
            drawCircle(
                color = violet.copy(alpha = 0.05f * i),
                radius = r,
                center = Offset(cx, cy),
            )
        }

        val bubbleW = w * 0.58f
        val bubbleH = h * 0.46f
        val bubbleX = cx - bubbleW / 2f
        val bubbleY = cy - bubbleH / 2f - h * 0.02f
        val bubblePath = Path().apply {
            val r = bubbleW * 0.18f
            moveTo(bubbleX + r, bubbleY)
            lineTo(bubbleX + bubbleW - r, bubbleY)
            quadraticTo(bubbleX + bubbleW, bubbleY, bubbleX + bubbleW, bubbleY + r)
            lineTo(bubbleX + bubbleW, bubbleY + bubbleH - r)
            quadraticTo(
                bubbleX + bubbleW, bubbleY + bubbleH,
                bubbleX + bubbleW - r, bubbleY + bubbleH,
            )

            lineTo(bubbleX + bubbleW * 0.36f, bubbleY + bubbleH)
            lineTo(bubbleX + bubbleW * 0.22f, bubbleY + bubbleH + h * 0.08f)
            lineTo(bubbleX + bubbleW * 0.28f, bubbleY + bubbleH)
            lineTo(bubbleX + r, bubbleY + bubbleH)
            quadraticTo(bubbleX, bubbleY + bubbleH, bubbleX, bubbleY + bubbleH - r)
            lineTo(bubbleX, bubbleY + r)
            quadraticTo(bubbleX, bubbleY, bubbleX + r, bubbleY)
            close()
        }
        drawPath(
            path = bubblePath,
            brush = Brush.linearGradient(
                colors = listOf(violetLight, violetDark),
                start = Offset(bubbleX, bubbleY),
                end = Offset(bubbleX + bubbleW, bubbleY + bubbleH),
            ),
        )

        val padW = bubbleW * 0.28f
        val padH = bubbleH * 0.36f
        val padX = cx - padW / 2f
        val padY = bubbleY + bubbleH * 0.40f

        val arcSize = padW * 0.62f
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - arcSize / 2f, padY - arcSize * 0.55f),
            size = Size(arcSize, arcSize),
            style = Stroke(width = w * 0.022f),
        )

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(padX, padY),
            size = Size(padW, padH),
            cornerRadius = CornerRadius(padW * 0.18f),
        )

        drawCircle(
            color = violet,
            radius = padW * 0.10f,
            center = Offset(cx, padY + padH * 0.42f),
        )
        drawRoundRect(
            color = violet,
            topLeft = Offset(cx - padW * 0.04f, padY + padH * 0.42f),
            size = Size(padW * 0.08f, padH * 0.32f),
            cornerRadius = CornerRadius(padW * 0.04f),
        )

        drawCircle(
            color = cyan,
            radius = w * 0.020f,
            center = Offset(bubbleX + bubbleW - bubbleW * 0.10f, bubbleY + bubbleH * 0.18f),
        )
    }
}

@Composable
fun IllustrationSoundExchange(size: Dp = 200.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val violet = MurmrColors.PrimaryViolet
        val violetLight = MurmrColors.PrimaryVioletLight
        val violetDark = MurmrColors.PrimaryVioletDark
        val cyan = MurmrColors.AccentCyan

        val cx = w / 2f
        val cy = h * 0.52f

        for (i in 4 downTo 1) {
            drawCircle(
                color = cyan.copy(alpha = 0.04f * i),
                radius = w * (0.10f + i * 0.06f),
                center = Offset(cx, cy),
            )
        }

        val phoneW = w * 0.22f
        val phoneH = h * 0.46f
        val tilt = 12f
        val gap = w * 0.16f

        translate(left = cx - gap - phoneW, top = cy - phoneH / 2f) {
            rotate(degrees = -tilt, pivot = Offset(phoneW, phoneH / 2f)) {
                drawPhone(phoneW, phoneH, violet, violetLight, violetDark, cyan, dotColor = violetLight)
            }
        }

        translate(left = cx + gap, top = cy - phoneH / 2f) {
            rotate(degrees = tilt, pivot = Offset(0f, phoneH / 2f)) {
                drawPhone(phoneW, phoneH, violet, violetLight, violetDark, cyan, dotColor = cyan)
            }
        }

        val waveStroke = w * 0.013f
        for (i in 0..4) {
            val r = w * (0.05f + i * 0.05f)
            val alpha = 0.85f - i * 0.16f

            drawArc(
                color = cyan.copy(alpha = alpha),
                startAngle = 60f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = waveStroke),
            )
            drawArc(
                color = cyan.copy(alpha = alpha),
                startAngle = -120f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = waveStroke),
            )
        }

        drawCircle(color = cyan, radius = w * 0.014f, center = Offset(cx, cy - h * 0.04f))
        drawCircle(color = cyan, radius = w * 0.014f, center = Offset(cx, cy + h * 0.04f))
    }
}

private fun DrawScope.drawPhone(
    w: Float,
    h: Float,
    border: Color,
    light: Color,
    dark: Color,
    cyan: Color,
    dotColor: Color,
) {

    drawRoundRect(
        brush = Brush.linearGradient(colors = listOf(light, dark)),
        topLeft = Offset(0f, 0f),
        size = Size(w, h),
        cornerRadius = CornerRadius(w * 0.18f),
    )

    val padX = w * 0.10f
    val padY = h * 0.07f
    drawRoundRect(
        color = MurmrColors.SurfaceDark,
        topLeft = Offset(padX, padY),
        size = Size(w - padX * 2, h - padY * 2),
        cornerRadius = CornerRadius(w * 0.10f),
    )

    drawRoundRect(
        color = MurmrColors.BackgroundDark,
        topLeft = Offset(w / 2f - w * 0.16f, padY),
        size = Size(w * 0.32f, h * 0.04f),
        cornerRadius = CornerRadius(h * 0.02f),
    )

    drawCircle(
        color = dotColor,
        radius = w * 0.16f,
        center = Offset(w / 2f, h / 2f + h * 0.04f),
    )
    drawCircle(
        color = dotColor.copy(alpha = 0.30f),
        radius = w * 0.26f,
        center = Offset(w / 2f, h / 2f + h * 0.04f),
    )
}

private val sasPreviewEmojis = listOf("🐱", "🍉", "🐔", "🐶", "🚀")

@Composable
fun IllustrationVerifiedShield(size: Dp = 200.dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val violet = MurmrColors.PrimaryViolet
            val violetLight = MurmrColors.PrimaryVioletLight
            val cyan = MurmrColors.AccentCyan
            val success = MurmrColors.Success

            val cx = w / 2f
            val cy = h / 2f

            for (i in 4 downTo 1) {
                drawCircle(
                    color = violet.copy(alpha = 0.05f * i),
                    radius = w * (0.28f + i * 0.05f),
                    center = Offset(cx, cy),
                )
            }

            val cardW = w * 0.78f
            val cardH = h * 0.42f
            val cardX = cx - cardW / 2f
            val cardY = cy - cardH / 2f

            drawRoundRect(
                color = Color(0x40000000),
                topLeft = Offset(cardX + w * 0.01f, cardY + h * 0.018f),
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(w * 0.06f),
            )
            drawRoundRect(
                color = MurmrColors.ElevatedDark,
                topLeft = Offset(cardX, cardY),
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(w * 0.06f),
            )

            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(violetLight, cyan),
                    start = Offset(cardX, cardY),
                    end = Offset(cardX + cardW, cardY + cardH),
                ),
                topLeft = Offset(cardX, cardY),
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(w * 0.06f),
                style = Stroke(width = w * 0.006f),
            )

            drawRoundRect(
                color = MurmrColors.OnSurfaceMuted.copy(alpha = 0.25f),
                topLeft = Offset(cardX + cardW * 0.20f, cardY + cardH * 0.80f),
                size = Size(cardW * 0.60f, h * 0.008f),
                cornerRadius = CornerRadius(h * 0.004f),
            )

            val shieldCx = cardX + cardW - w * 0.04f
            val shieldCy = cardY + h * 0.02f
            val shieldR = w * 0.11f
            drawCircle(
                color = Color(0x66000000),
                radius = shieldR + w * 0.008f,
                center = Offset(shieldCx + w * 0.005f, shieldCy + w * 0.008f),
            )
            val shieldPath = Path().apply {
                moveTo(shieldCx - shieldR * 0.85f, shieldCy - shieldR * 0.6f)
                lineTo(shieldCx + shieldR * 0.85f, shieldCy - shieldR * 0.6f)
                lineTo(shieldCx + shieldR * 0.85f, shieldCy + shieldR * 0.2f)
                quadraticTo(
                    shieldCx, shieldCy + shieldR * 1.20f,
                    shieldCx - shieldR * 0.85f, shieldCy + shieldR * 0.2f,
                )
                close()
            }
            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(success, Color(0xFF15803D)),
                    start = Offset(shieldCx - shieldR, shieldCy - shieldR),
                    end = Offset(shieldCx + shieldR, shieldCy + shieldR),
                ),
            )
            val checkStroke = shieldR * 0.22f
            drawLine(
                color = Color.White,
                start = Offset(shieldCx - shieldR * 0.40f, shieldCy + shieldR * 0.05f),
                end = Offset(shieldCx - shieldR * 0.10f, shieldCy + shieldR * 0.35f),
                strokeWidth = checkStroke,
            )
            drawLine(
                color = Color.White,
                start = Offset(shieldCx - shieldR * 0.10f, shieldCy + shieldR * 0.35f),
                end = Offset(shieldCx + shieldR * 0.50f, shieldCy - shieldR * 0.30f),
                strokeWidth = checkStroke,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(size * 0.025f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sasPreviewEmojis.forEach { emoji ->
                Text(
                    text = emoji,
                    fontSize = (size.value * 0.115f).sp,
                )
            }
        }
    }
}
