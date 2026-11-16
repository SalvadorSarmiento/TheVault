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
    primary = VaultPurpleDark,
    onPrimary = VaultOnPurpleDark,
    primaryContainer = VaultSelectedDark,
    onPrimaryContainer = VaultPurpleDark,
    background = VaultBackgroundDark,
    onBackground = VaultTextDark,
    surface = VaultSurfaceDark,
    onSurface = VaultTextDark,
    onSurfaceVariant = VaultSecondaryDark,
    outline = VaultBorderDark,
    error = Color(0xFFFFB2BA),
    secondary = VaultMintDark,
    onSecondary = Color(0xFF00382D),
    secondaryContainer = Color(0xFF153E36),
    onSecondaryContainer = Color(0xFF9DF2D9),
    tertiary = Color(0xFFFFD28E),
    tertiaryContainer = Color(0xFF49371C),
    onTertiaryContainer = Color(0xFFFFDBA7),
    surfaceVariant = Color(0xFF292637),
    outlineVariant = Color(0xFF353142)
)

private val LightColorScheme = lightColorScheme(
    primary = VaultPurple,
    onPrimary = VaultSurface,
    primaryContainer = VaultSelected,
    onPrimaryContainer = VaultPurple,
    background = VaultBackground,
    onBackground = VaultText,
    surface = VaultSurface,
    onSurface = VaultText,
    onSurfaceVariant = VaultSecondary,
    outline = VaultBorder,
    error = VaultError,
    secondary = VaultMint,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F5E9),
    onSecondaryContainer = Color(0xFF114F3F),
    tertiary = Color(0xFF875600),
    tertiaryContainer = Color(0xFFFFEAC9),
    onTertiaryContainer = Color(0xFF633F00),
    surfaceVariant = Color(0xFFEFECF6),
    outlineVariant = Color(0xFFE8E3EF)
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
