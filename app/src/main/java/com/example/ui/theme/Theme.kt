package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SkyPrimaryDark,
    onPrimary = SkyOnPrimaryDark,
    primaryContainer = SkyPrimaryContainerDark,
    onPrimaryContainer = SkyPrimaryContainer,
    secondary = StatusDoneDark,
    onSecondary = SkyOnPrimaryDark,
    tertiary = StatusPartialDark,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkTextMain,
    onSurface = DarkTextMain,
    onSurfaceVariant = DarkTextSec,
    outline = DarkTextSec.copy(alpha = 0.3f)
)

private val LightColorScheme = lightColorScheme(
    primary = SkyPrimary,
    onPrimary = SkyOnPrimary,
    primaryContainer = SkyPrimaryContainer,
    onPrimaryContainer = SkyOnPrimaryContainer,
    secondary = StatusDone,
    onSecondary = SkyOnPrimary,
    tertiary = StatusPartial,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightTextMain,
    onSurface = LightTextMain,
    onSurfaceVariant = LightTextSec,
    outline = LightOutline
)

@Composable
fun RotinaVivaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our branded sky & green palette by default
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
