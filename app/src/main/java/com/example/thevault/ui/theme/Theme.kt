package com.example.thevault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = VaultActionDark,
    onPrimary = VaultOnAction,
    primaryContainer = VaultSelectedDark,
    onPrimaryContainer = VaultOnAction,
    background = VaultBackgroundDark,
    onBackground = VaultTextDark,
    surface = VaultSurfaceDark,
    onSurface = VaultTextDark,
    onSurfaceVariant = VaultSecondaryDark,
    outline = VaultBorderDark,
    error = Color(0xFFFFB2BA),
    secondary = VaultAccentDark,
    onSecondary = VaultOnAction,
    secondaryContainer = Color(0xFF4D3058),
    onSecondaryContainer = Color.White,
    tertiary = Color.White,
    onTertiary = VaultOnAction,
    tertiaryContainer = Color(0xFF4D3058),
    onTertiaryContainer = Color.White,
    surfaceVariant = Color(0xFF432B4D),
    outlineVariant = Color(0xFF62456E),
    surfaceContainerLowest = VaultBackgroundDark,
    surfaceContainerLow = VaultSurfaceDark,
    surfaceContainer = VaultSurfaceDark,
    surfaceContainerHigh = Color(0xFF432B4D),
    surfaceContainerHighest = Color(0xFF51375C),
    surfaceDim = VaultBackgroundDark,
    surfaceBright = Color(0xFF51375C),
    surfaceTint = VaultBackgroundDark,
    inverseSurface = Color.White,
    inverseOnSurface = VaultOnAction,
    inversePrimary = VaultOnAction
)

private val LightColorScheme = lightColorScheme(
    primary = VaultAction,
    onPrimary = VaultOnAction,
    primaryContainer = VaultSelected,
    onPrimaryContainer = VaultOnAction,
    background = VaultBackground,
    onBackground = VaultText,
    surface = VaultSurface,
    onSurface = VaultText,
    onSurfaceVariant = VaultSecondary,
    outline = VaultBorder,
    error = VaultError,
    onError = Color(0xFF591B2C),
    errorContainer = Color(0xFF713C52),
    onErrorContainer = Color(0xFFFFE9EC),
    secondary = VaultAccent,
    onSecondary = VaultOnAction,
    secondaryContainer = Color(0xFF61406D),
    onSecondaryContainer = Color(0xFFF7EAFC),
    tertiary = Color.White,
    onTertiary = VaultOnAction,
    tertiaryContainer = Color(0xFF61406D),
    onTertiaryContainer = Color.White,
    surfaceVariant = Color(0xFF75567F),
    outlineVariant = Color(0xFF84638E),
    surfaceContainerLowest = VaultBackground,
    surfaceContainerLow = VaultSurface,
    surfaceContainer = VaultSurface,
    surfaceContainerHigh = Color(0xFF70507C),
    surfaceContainerHighest = Color(0xFF795985),
    surfaceDim = Color(0xFF482C54),
    surfaceBright = Color(0xFF795985),
    surfaceTint = VaultBackgroundDark,
    inverseSurface = Color.White,
    inverseOnSurface = VaultOnAction,
    inversePrimary = VaultOnAction
)

@Composable
fun TheVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(32.dp)
        )
    ) {
        CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground, content = content)
    }
}
