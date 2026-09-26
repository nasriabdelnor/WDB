package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyanLight,
    onPrimary = IndustrialNavy900,
    primaryContainer = IndustrialNavy700,
    onPrimaryContainer = ElectricCyanLight,
    secondary = WeldAmber,
    onSecondary = IndustrialNavy900,
    secondaryContainer = IndustrialNavy600,
    onSecondaryContainer = WeldAmberContainer,
    tertiary = ApprovedGreen,
    background = SurfaceDark,
    surface = SurfaceCardDark,
    surfaceVariant = IndustrialNavy700,
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = SurfaceBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = SteelBlue,
    onPrimary = Color.White,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = SteelBlue,
    secondary = WeldAmberDark,
    onSecondary = Color.White,
    secondaryContainer = WeldAmberContainer,
    onSecondaryContainer = IndustrialNavy900,
    tertiary = ApprovedGreenDark,
    background = SurfaceLight,
    surface = SurfaceCardLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    outline = SurfaceBorderLight
)

@Composable
fun WeldTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent industrial palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
