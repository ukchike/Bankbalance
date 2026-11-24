package com.bankscope.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppleLightColors = lightColorScheme(
    primary = AppleColors.SystemBlue,
    onPrimary = AppleColors.LightSurface,
    primaryContainer = AppleColors.SystemBlue.copy(alpha = 0.14f),
    onPrimaryContainer = AppleColors.SystemBlue,
    secondary = AppleColors.SystemIndigo,
    tertiary = AppleColors.SystemTeal,
    error = AppleColors.SystemRed,
    onError = AppleColors.LightSurface,
    background = AppleColors.LightBackground,
    onBackground = AppleColors.LightOnBackground,
    surface = AppleColors.LightSurface,
    onSurface = AppleColors.LightOnBackground,
    surfaceVariant = AppleColors.LightSurfaceVariant,
    onSurfaceVariant = AppleColors.LightOnSurfaceVariant,
    outline = AppleColors.LightSeparator,
    outlineVariant = AppleColors.LightSeparator
)

private val AppleDarkColors = darkColorScheme(
    primary = AppleColors.SystemBlue,
    onPrimary = AppleColors.DarkBackground,
    primaryContainer = AppleColors.SystemBlue.copy(alpha = 0.22f),
    onPrimaryContainer = AppleColors.SystemBlue,
    secondary = AppleColors.SystemIndigo,
    tertiary = AppleColors.SystemTeal,
    error = AppleColors.SystemRed,
    onError = AppleColors.DarkBackground,
    background = AppleColors.DarkBackground,
    onBackground = AppleColors.DarkOnBackground,
    surface = AppleColors.DarkSurface,
    onSurface = AppleColors.DarkOnBackground,
    surfaceVariant = AppleColors.DarkSurfaceVariant,
    onSurfaceVariant = AppleColors.DarkOnSurfaceVariant,
    outline = AppleColors.DarkSeparator,
    outlineVariant = AppleColors.DarkSeparator
)

/** Positive/negative semantic colors that don't map cleanly onto the Material scheme. */
object AppleSemantics {
    val Positive @Composable get() = AppleColors.SystemGreen
    val Negative @Composable get() = AppleColors.SystemRed
    val Warning @Composable get() = AppleColors.SystemOrange
}

@Composable
fun BankBalanceTheme(
    useDarkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) AppleDarkColors else AppleLightColors,
        typography = AppleTypography,
        shapes = AppleShapes,
        content = content
    )
}
