package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = MinimalBlueLight,
    onPrimary = MinimalOnBlueContainer,
    primaryContainer = MinimalBlueDark,
    onPrimaryContainer = MinimalBlueContainer,
    secondary = Emerald500,
    onSecondary = Slate900,
    secondaryContainer = Emerald700,
    onSecondaryContainer = Emerald100,
    tertiary = Amber500,
    onTertiary = Slate900,
    tertiaryContainer = Amber700,
    onTertiaryContainer = Amber100,
    background = MinimalBackgroundDark,
    onBackground = Slate100,
    surface = MinimalSurfaceDark,
    onSurface = Slate100,
    surfaceVariant = Color(0xFF1E2632),
    onSurfaceVariant = Slate300,
    outline = Slate600,
    outlineVariant = Slate700,
    error = Red500,
    onError = PureWhite,
    errorContainer = Red700,
    onErrorContainer = Red100
)

private val LightColorScheme = lightColorScheme(
    primary = MinimalBlue,
    onPrimary = PureWhite,
    primaryContainer = MinimalBlueContainer,
    onPrimaryContainer = MinimalOnBlueContainer,
    secondary = Emerald600,
    onSecondary = PureWhite,
    secondaryContainer = Emerald100,
    onSecondaryContainer = Emerald700,
    tertiary = Amber600,
    onTertiary = PureWhite,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Amber700,
    background = MinimalBackground,
    onBackground = Slate900,
    surface = PureWhite,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    outline = Slate300,
    outlineVariant = Slate200,
    error = Red600,
    onError = PureWhite,
    errorContainer = Red100,
    onErrorContainer = Red700
)

val MinimalShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StockAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
        shapes = MinimalShapes,
        content = content
    )
}
