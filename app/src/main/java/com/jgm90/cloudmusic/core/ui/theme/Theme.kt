package com.jgm90.cloudmusic.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

private val lightScheme =
    lightColorScheme(
        primary = primaryLight,
        onPrimary = onPrimaryLight,
        primaryContainer = primaryContainerLight,
        onPrimaryContainer = onPrimaryContainerLight,
        secondary = secondaryLight,
        onSecondary = onSecondaryLight,
        secondaryContainer = secondaryContainerLight,
        onSecondaryContainer = onSecondaryContainerLight,
        tertiary = tertiaryLight,
        onTertiary = onTertiaryLight,
        tertiaryContainer = tertiaryContainerLight,
        onTertiaryContainer = onTertiaryContainerLight,
        error = errorLight,
        onError = onErrorLight,
        errorContainer = errorContainerLight,
        onErrorContainer = onErrorContainerLight,
        background = backgroundLight,
        onBackground = onBackgroundLight,
        surface = surfaceLight,
        onSurface = onSurfaceLight,
        surfaceVariant = surfaceVariantLight,
        onSurfaceVariant = onSurfaceVariantLight,
        outline = outlineLight,
        outlineVariant = outlineVariantLight,
        scrim = scrimLight,
        inverseSurface = inverseSurfaceLight,
        inverseOnSurface = inverseOnSurfaceLight,
        inversePrimary = inversePrimaryLight,
        surfaceDim = surfaceDimLight,
        surfaceBright = surfaceBrightLight,
        surfaceContainerLowest = surfaceContainerLowestLight,
        surfaceContainerLow = surfaceContainerLowLight,
        surfaceContainer = surfaceContainerLight,
        surfaceContainerHigh = surfaceContainerHighLight,
        surfaceContainerHighest = surfaceContainerHighestLight
    )

private val darkScheme =
    darkColorScheme(
        primary = primaryDark,
        onPrimary = onPrimaryDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = onPrimaryContainerDark,
        secondary = secondaryDark,
        onSecondary = onSecondaryDark,
        secondaryContainer = secondaryContainerDark,
        onSecondaryContainer = onSecondaryContainerDark,
        tertiary = tertiaryDark,
        onTertiary = onTertiaryDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = onTertiaryContainerDark,
        error = errorDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = backgroundDark,
        onBackground = onBackgroundDark,
        surface = surfaceDark,
        onSurface = onSurfaceDark,
        surfaceVariant = surfaceVariantDark,
        onSurfaceVariant = onSurfaceVariantDark,
        outline = outlineDark,
        outlineVariant = outlineVariantDark,
        scrim = scrimDark,
        inverseSurface = inverseSurfaceDark,
        inverseOnSurface = inverseOnSurfaceDark,
        inversePrimary = inversePrimaryDark,
        surfaceDim = surfaceDimDark,
        surfaceBright = surfaceBrightDark,
        surfaceContainerLowest = surfaceContainerLowestDark,
        surfaceContainerLow = surfaceContainerLowDark,
        surfaceContainer = surfaceContainerDark,
        surfaceContainerHigh = surfaceContainerHighDark,
        surfaceContainerHighest = surfaceContainerHighestDark
    )

@Composable
fun CloudMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val seedColor by ThemeController.seedColor.collectAsState()
    val baseScheme = if (darkTheme) darkScheme else lightScheme

    val colorScheme = seedColor?.let { seed ->
        createDynamicColorScheme(baseScheme, seed, darkTheme)
    } ?: baseScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

private fun createDynamicColorScheme(base: ColorScheme, seed: Color, darkTheme: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    
    val primaryHsl = floatArrayOf(
        hsl[0],
        hsl[1].coerceIn(0.3f, 0.9f),
        if (darkTheme) 0.7f else 0.5f
    )
    val primary = Color(ColorUtils.HSLToColor(primaryHsl))
    
    val primaryContainerHsl = floatArrayOf(
        hsl[0],
        hsl[1].coerceIn(0.3f, 0.9f),
        if (darkTheme) 0.3f else 0.9f
    )
    val primaryContainer = Color(ColorUtils.HSLToColor(primaryContainerHsl))
    
    val secondaryHsl = floatArrayOf(
        (hsl[0] + 30f) % 360f,
        (hsl[1] * 0.8f).coerceIn(0.3f, 0.9f),
        if (darkTheme) 0.65f else 0.55f
    )
    val secondary = Color(ColorUtils.HSLToColor(secondaryHsl))
    
    val secondaryContainerHsl = floatArrayOf(
        (hsl[0] + 30f) % 360f,
        (hsl[1] * 0.8f).coerceIn(0.3f, 0.9f),
        if (darkTheme) 0.25f else 0.85f
    )
    val secondaryContainer = Color(ColorUtils.HSLToColor(secondaryContainerHsl))
    
    val tertiaryHsl = floatArrayOf(
        (hsl[0] + 60f) % 360f,
        (hsl[1] * 0.7f).coerceIn(0.3f, 0.9f),
        if (darkTheme) 0.6f else 0.6f
    )
    val tertiary = Color(ColorUtils.HSLToColor(tertiaryHsl))
    
    val tertiaryContainerHsl = floatArrayOf(
        (hsl[0] + 60f) % 360f,
        (hsl[1] * 0.7f).coerceIn(0.3f, 0.9f),
        if (darkTheme) 0.2f else 0.8f
    )
    val tertiaryContainer = Color(ColorUtils.HSLToColor(tertiaryContainerHsl))

    val onPrimary = if (primary.luminance() > 0.5f) Color.Black else Color.White
    val onPrimaryContainer = if (primaryContainer.luminance() > 0.5f) Color.Black else Color.White
    val onSecondary = if (secondary.luminance() > 0.5f) Color.Black else Color.White
    val onSecondaryContainer = if (secondaryContainer.luminance() > 0.5f) Color.Black else Color.White
    val onTertiary = if (tertiary.luminance() > 0.5f) Color.Black else Color.White
    val onTertiaryContainer = if (tertiaryContainer.luminance() > 0.5f) Color.Black else Color.White

    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer
    )
}
