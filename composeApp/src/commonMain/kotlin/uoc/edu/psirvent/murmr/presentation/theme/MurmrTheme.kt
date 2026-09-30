package uoc.edu.psirvent.murmr.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MurmrDarkColors = darkColorScheme(
    primary = MurmrColors.PrimaryViolet,
    onPrimary = MurmrColors.OnSurface,
    primaryContainer = MurmrColors.PrimaryVioletDark,
    onPrimaryContainer = MurmrColors.OnSurface,
    secondary = MurmrColors.AccentCyan,
    onSecondary = MurmrColors.OnSurface,
    secondaryContainer = MurmrColors.AccentCyanDark,
    onSecondaryContainer = MurmrColors.OnSurface,
    tertiary = MurmrColors.PrimaryVioletLight,
    onTertiary = MurmrColors.OnSurface,
    background = MurmrColors.BackgroundDark,
    onBackground = MurmrColors.OnSurface,
    surface = MurmrColors.SurfaceDark,
    onSurface = MurmrColors.OnSurface,
    surfaceVariant = MurmrColors.ElevatedDark,
    onSurfaceVariant = MurmrColors.OnSurfaceMuted,
    outline = MurmrColors.DividerDark,
    error = MurmrColors.Error,
    onError = MurmrColors.OnSurface,
)

@Composable
fun MurmrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MurmrDarkColors,
        typography = MurmrTypography,
        shapes = MurmrShapes,
        content = content,
    )
}
