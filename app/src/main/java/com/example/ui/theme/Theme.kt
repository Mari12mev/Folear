package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = WarmTerracotta,
    onPrimary = DarkEspressoBg,
    primaryContainer = WarmTerracottaDark,
    onPrimaryContainer = DarkTextPrimary,
    secondary = WarmAmberGold,
    onSecondary = DarkEspressoBg,
    tertiary = SoftSageGreen,
    onTertiary = DarkEspressoBg,
    background = DarkEspressoBg,
    onBackground = DarkTextPrimary,
    surface = DarkCardSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkPillVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkPillVariant
)

private val LightColorScheme = lightColorScheme(
    primary = WarmTerracottaDark,
    onPrimary = LightCreamBg,
    primaryContainer = WarmAmberGold,
    onPrimaryContainer = LightTextPrimary,
    secondary = WarmTerracotta,
    onSecondary = LightCreamBg,
    tertiary = SoftSageGreen,
    onTertiary = LightCreamBg,
    background = LightCreamBg,
    onBackground = LightTextPrimary,
    surface = LightCardSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightPillVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightPillVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Dark mode as default for nighttime reading
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
