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
    onPrimary = VaultOnActionDark,
    primaryContainer = VaultSelectedDark,
    onPrimaryContainer = Color(0xFFB0F5EB),
    background = VaultBackgroundDark,
    onBackground = VaultTextDark,
    surface = VaultSurfaceDark,
    onSurface = VaultTextDark,
    onSurfaceVariant = VaultSecondaryDark,
    outline = VaultBorderDark,
    error = VaultErrorDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFB8CCC8),
    onSecondary = Color(0xFF213431),
    secondaryContainer = Color(0xFF314B47),
    onSecondaryContainer = Color(0xFFD4E6E0),
    tertiary = Color(0xFFA7D8CB),
    onTertiary = Color(0xFF13382E),
    tertiaryContainer = Color(0xFF2B5146),
    onTertiaryContainer = Color(0xFFC6F0E2),
    surfaceVariant = Color(0xFF243640),
    outlineVariant = Color(0xFF354852),
    surfaceContainerLowest = VaultBackgroundDark,
    surfaceContainerLow = VaultSurfaceDark,
    surfaceContainer = VaultSurfaceDark,
    surfaceContainerHigh = Color(0xFF243640),
    surfaceContainerHighest = Color(0xFF30424D),
    surfaceDim = VaultBackgroundDark,
    surfaceBright = Color(0xFF30424D),
    surfaceTint = VaultActionDark,
    inverseSurface = Color(0xFFE0E9EC),
    inverseOnSurface = Color(0xFF263139),
    inversePrimary = VaultAction
)

private val LightColorScheme = lightColorScheme(
    primary = VaultAction,
    onPrimary = VaultOnAction,
    primaryContainer = VaultSelected,
    onPrimaryContainer = Color(0xFF053D38),
    background = VaultBackground,
    onBackground = VaultText,
    surface = VaultSurface,
    onSurface = VaultText,
    onSurfaceVariant = VaultSecondary,
    outline = VaultBorder,
    error = VaultError,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    secondary = Color(0xFF47665F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE8E0),
    onSecondaryContainer = Color(0xFF102E27),
    tertiary = Color(0xFF2C6B53),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB3F0D1),
    onTertiaryContainer = Color(0xFF002114),
    surfaceVariant = Color(0xFFE3EDED),
    outlineVariant = Color(0xFFCFDCDF),
    surfaceContainerLowest = VaultSurface,
    surfaceContainerLow = Color(0xFFF8FAFA),
    surfaceContainer = Color(0xFFECF2F2),
    surfaceContainerHigh = Color(0xFFE3EDED),
    surfaceContainerHighest = Color(0xFFDAE5E5),
    surfaceDim = Color(0xFFD7E2E2),
    surfaceBright = Color(0xFFFAFCFC),
    surfaceTint = VaultAction,
    inverseSurface = VaultSurfaceDark,
    inverseOnSurface = VaultTextDark,
    inversePrimary = VaultActionDark
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
