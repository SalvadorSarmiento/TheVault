package com.example.thevault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
    error = VaultError
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
    error = VaultError
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
        content = content
    )
}
