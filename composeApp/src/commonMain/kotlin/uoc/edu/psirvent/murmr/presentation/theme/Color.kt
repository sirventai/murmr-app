package uoc.edu.psirvent.murmr.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object MurmrColors {
    val PrimaryViolet = Color(0xFF7C3AED)
    val PrimaryVioletLight = Color(0xFF8B5CF6)
    val PrimaryVioletDark = Color(0xFF6D28D9)

    val AccentCyan = Color(0xFF06B6D4)
    val AccentCyanDark = Color(0xFF0891B2)

    val SurfaceDark = Color(0xFF262624)
    val BackgroundDark = Color(0xFF111111)
    val ElevatedDark = Color(0xFF1F1F1D)
    val DividerDark = Color(0xFF2F2F2D)

    val OnSurface = Color(0xFFF8FAFC)
    val OnSurfaceMuted = Color(0xFF9CA3AF)
    val OnSurfaceFaint = Color(0xFF6B7280)

    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)

    val IncomingBubble = Color(0xFF2A2A28)
}

val MurmrPrimaryGradient: Brush
    get() = Brush.linearGradient(
        colors = listOf(MurmrColors.PrimaryVioletLight, MurmrColors.PrimaryVioletDark),
    )

val MurmrLogoGradient: Brush
    get() = Brush.linearGradient(
        colors = listOf(MurmrColors.PrimaryViolet, MurmrColors.AccentCyan),
    )
