package com.pulsemusic.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SpotifyGreen = Color(0xFF1DB954)
private val SpotifyGreenLight = Color(0xFF1ED760)
private val SpotifyBlack = Color(0xFF121212)
private val SpotifyDarkGray = Color(0xFF181818)
private val SpotifyCardGray = Color(0xFF282828)
private val SpotifyLightGray = Color(0xFFB3B3B3)
private val SpotifyWhite = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = SpotifyBlack,
    primaryContainer = SpotifyGreenLight.copy(alpha = 0.2f),
    secondary = Color(0xFFBB86FC),
    onSecondary = SpotifyBlack,
    secondaryContainer = Color(0xFF3700B3).copy(alpha = 0.2f),
    tertiary = Color(0xFF03DAC6),
    background = SpotifyBlack,
    onBackground = SpotifyWhite,
    surface = SpotifyDarkGray,
    onSurface = SpotifyWhite,
    surfaceVariant = SpotifyCardGray,
    onSurfaceVariant = SpotifyLightGray,
    outline = Color(0xFF535353)
)

private val LightColorScheme = lightColorScheme(
    primary = SpotifyGreen,
    onPrimary = SpotifyWhite,
    primaryContainer = SpotifyGreenLight.copy(alpha = 0.2f),
    secondary = Color(0xFF6200EE),
    onSecondary = SpotifyWhite,
    secondaryContainer = Color(0xFF3700B3).copy(alpha = 0.2f),
    tertiary = Color(0xFF03DAC6),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1A1A1A),
    surface = SpotifyWhite,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF616161),
    outline = Color(0xFFBDBDBD)
)

@Composable
fun PulseMusicTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PulseTypography,
        content = content
    )
}
