package com.invatech.pockit.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val PockitShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(32.dp)
)

private const val THEME_TRANSITION_MS = 350

/**
 * @param themeMode overrides the saved System / Light / Dark choice (handy for previews).
 * @param palette overrides the saved colour palette (handy for previews and the palette picker).
 */
@Composable
fun PockitTheme(
    themeMode: ThemeMode? = null,
    palette: PockitPalette? = null,
    content: @Composable () -> Unit
) {
    val activeMode by ThemeManager.themeMode.collectAsState()
    val activePalette by ThemeManager.palette.collectAsState()
    val mode = themeMode ?: activeMode
    val selectedPalette = palette ?: activePalette
    val systemDark = isSystemInDarkTheme()

    val isDark = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val targetScheme = remember(selectedPalette, isDark) { selectedPalette.colorScheme(isDark) }
    val colorScheme = targetScheme.animated()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PockitTypography,
        shapes = PockitShapes,
        content = content
    )
}

/**
 * Cross-fades every colour that components read, so switching palette or light/dark
 * glides instead of snapping. On first composition the colours start at their target,
 * so there is no animation on app launch.
 */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    val spec = tween<Color>(durationMillis = THEME_TRANSITION_MS)

    @Composable
    fun fade(target: Color): Color = animateColorAsState(
        targetValue = target,
        animationSpec = spec,
        label = "theme_color"
    ).value

    return copy(
        primary = fade(primary),
        onPrimary = fade(onPrimary),
        primaryContainer = fade(primaryContainer),
        onPrimaryContainer = fade(onPrimaryContainer),
        inversePrimary = fade(inversePrimary),
        secondary = fade(secondary),
        onSecondary = fade(onSecondary),
        secondaryContainer = fade(secondaryContainer),
        onSecondaryContainer = fade(onSecondaryContainer),
        tertiary = fade(tertiary),
        onTertiary = fade(onTertiary),
        tertiaryContainer = fade(tertiaryContainer),
        onTertiaryContainer = fade(onTertiaryContainer),
        background = fade(background),
        onBackground = fade(onBackground),
        surface = fade(surface),
        onSurface = fade(onSurface),
        surfaceVariant = fade(surfaceVariant),
        onSurfaceVariant = fade(onSurfaceVariant),
        surfaceTint = fade(surfaceTint),
        inverseSurface = fade(inverseSurface),
        inverseOnSurface = fade(inverseOnSurface),
        errorContainer = fade(errorContainer),
        onErrorContainer = fade(onErrorContainer),
        outline = fade(outline),
        outlineVariant = fade(outlineVariant),
        surfaceBright = fade(surfaceBright),
        surfaceDim = fade(surfaceDim),
        surfaceContainerLowest = fade(surfaceContainerLowest),
        surfaceContainerLow = fade(surfaceContainerLow),
        surfaceContainer = fade(surfaceContainer),
        surfaceContainerHigh = fade(surfaceContainerHigh),
        surfaceContainerHighest = fade(surfaceContainerHighest)
    )
}