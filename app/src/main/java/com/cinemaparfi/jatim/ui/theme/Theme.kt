package com.cinemaparfi.jatim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppDarkColorScheme = darkColorScheme(
    primary = PremiumGold,
    onPrimary = DeepBlack,
    primaryContainer = PremiumGold.copy(alpha = 0.18f),
    onPrimaryContainer = PremiumGold,
    secondary = SoftGold,
    onSecondary = DeepBlack,
    secondaryContainer = SoftGold.copy(alpha = 0.15f),
    onSecondaryContainer = SoftGold,
    tertiary = AccentBlue,
    background = DeepBlack,
    surface = SoftDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondary,
    outline = OutlineColor
)

@Composable
fun CinemaParfiJatimTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        typography = Typography,
        content = content
    )
}
