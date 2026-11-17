package com.invatech.pockit.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * A colour personality for the app. Palettes are independent of [ThemeMode]:
 * every palette ships a light AND a dark scheme, so System / Light / Dark keeps working
 * on top of whichever palette the user picked.
 *
 * Persist by [id] (stable), never by enum name or ordinal.
 */
enum class PockitPalette(val id: String, val title: String, val subtitle: String) {
    POCKIT("pockit", "Pockit", "Signature ink & soft pastels"),
    OCEAN("ocean", "Ocean", "Calm blues & fresh teal"),
    SUNSET("sunset", "Sunset", "Warm coral & amber glow"),
    FOREST("forest", "Forest", "Fresh greens & earthy tones"),
    BERRY("berry", "Berry", "Rich plum & raspberry"),
    HONEY("honey", "Honey", "Golden warmth on soft cream");

    companion object {
        val Default = POCKIT

        fun fromId(id: String?): PockitPalette = entries.firstOrNull { it.id == id } ?: Default
    }
}

/** The Material 3 colour scheme for this palette in the requested brightness. */
fun PockitPalette.colorScheme(isDark: Boolean = false): ColorScheme = when (this) {
    PockitPalette.POCKIT -> PockitLightScheme
    PockitPalette.OCEAN -> OceanLightScheme
    PockitPalette.SUNSET -> SunsetLightScheme
    PockitPalette.FOREST -> ForestLightScheme
    PockitPalette.BERRY -> BerryLightScheme
    PockitPalette.HONEY -> HoneyLightScheme
}

// ---------------------------------------------------------------------------------------------
// Default Pockit warm brand palette
// ---------------------------------------------------------------------------------------------

private val PockitLightScheme = lightColorScheme(
    primary = CocoaEspresso,
    onPrimary = Color.White,
    primaryContainer = PastelMint,
    onPrimaryContainer = CocoaEspresso,
    secondary = CocoaFab,
    onSecondary = Color.White,
    secondaryContainer = PastelPeach,
    onSecondaryContainer = CocoaEspresso,
    tertiary = CocoaSoft,
    onTertiary = Color.White,
    tertiaryContainer = PastelApricot,
    onTertiaryContainer = CocoaEspresso,
    background = WarmCream,
    onBackground = CocoaEspresso,
    surface = WarmCreamSurface,
    onSurface = CocoaEspresso,
    surfaceVariant = WarmCreamMuted,
    onSurfaceVariant = CocoaMuted,
    outline = WarmBorder,
    outlineVariant = WarmDivider,
    error = ErrorRed,
    errorContainer = ErrorSoft,
    onErrorContainer = ErrorRed
)

private val PockitDarkScheme = darkColorScheme(
    primary = MintDark,
    onPrimary = DarkBackground,
    primaryContainer = MintContainerDark,
    onPrimaryContainer = MintDark,
    secondary = LavenderDark,
    onSecondary = DarkBackground,
    secondaryContainer = LavenderContainerDark,
    onSecondaryContainer = LavenderDark,
    tertiary = PeachDark,
    onTertiary = DarkBackground,
    tertiaryContainer = PeachContainerDark,
    onTertiaryContainer = PeachDark,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ErrorRed,
    errorContainer = Color(0xFF451E1E),
    onErrorContainer = Color(0xFFFFB4AB)
)

// ---------------------------------------------------------------------------------------------
// Ocean: calm blues & teal
// ---------------------------------------------------------------------------------------------

private val OceanLightScheme = paletteScheme(
    isDark = false,
    primary = Color(0xFF0A5CA8), onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8FA), onPrimaryContainer = Color(0xFF07294A),
    secondary = Color(0xFF0F7C90), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2F0F5), onSecondaryContainer = Color(0xFF063A44),
    tertiary = Color(0xFF4A5BC4), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E4FA), onTertiaryContainer = Color(0xFF151D5C),
    background = Color(0xFFF2F7FB), surface = Color.White,
    surfaceVariant = Color(0xFFE3ECF4),
    onSurface = Color(0xFF0D1B2A), onSurfaceVariant = Color(0xFF4A5B6D),
    outline = Color(0xFFD5E0EA), outlineVariant = Color(0xFFE6EDF3),
    inversePrimary = Color(0xFF6DB6FF)
)

private val OceanDarkScheme = paletteScheme(
    isDark = true,
    primary = Color(0xFF6DB6FF), onPrimary = Color(0xFF06223F),
    primaryContainer = Color(0xFF0F3A63), onPrimaryContainer = Color(0xFFCFE5FF),
    secondary = Color(0xFF5CD0E6), onSecondary = Color(0xFF032A33),
    secondaryContainer = Color(0xFF0C3A44), onSecondaryContainer = Color(0xFFBFEFF8),
    tertiary = Color(0xFF9AA8FF), onTertiary = Color(0xFF111A5E),
    tertiaryContainer = Color(0xFF232B66), onTertiaryContainer = Color(0xFFDDE1FF),
    background = Color(0xFF0A121C), surface = Color(0xFF111C29),
    surfaceVariant = Color(0xFF1B2938),
    onSurface = Color(0xFFE6EEF7), onSurfaceVariant = Color(0xFF93A4B8),
    outline = Color(0xFF2B3C4F), outlineVariant = Color(0xFF1D2A39),
    inversePrimary = Color(0xFF0A5CA8)
)

// ---------------------------------------------------------------------------------------------
// Sunset: warm coral & amber
// ---------------------------------------------------------------------------------------------

private val SunsetLightScheme = paletteScheme(
    isDark = false,
    primary = Color(0xFFC2410C), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1D1), onPrimaryContainer = Color(0xFF4A1604),
    secondary = Color(0xFFD6336C), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDE8), onSecondaryContainer = Color(0xFF4A0F25),
    tertiary = Color(0xFFA16207), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCEBC0), onTertiaryContainer = Color(0xFF3D2A04),
    background = Color(0xFFFFF6EF), surface = Color.White,
    surfaceVariant = Color(0xFFF8E7DA),
    onSurface = Color(0xFF2A1810), onSurfaceVariant = Color(0xFF6E5042),
    outline = Color(0xFFF0DACB), outlineVariant = Color(0xFFF6E6DA),
    inversePrimary = Color(0xFFFF9B6A)
)

private val SunsetDarkScheme = paletteScheme(
    isDark = true,
    primary = Color(0xFFFF9B6A), onPrimary = Color(0xFF4A1A04),
    primaryContainer = Color(0xFF6B2A0C), onPrimaryContainer = Color(0xFFFFDBCB),
    secondary = Color(0xFFFF8FB1), onSecondary = Color(0xFF4A0F25),
    secondaryContainer = Color(0xFF5C1A33), onSecondaryContainer = Color(0xFFFFD9E4),
    tertiary = Color(0xFFF5C451), onTertiary = Color(0xFF3A2800),
    tertiaryContainer = Color(0xFF4F3A05), onTertiaryContainer = Color(0xFFFFE9A8),
    background = Color(0xFF16100D), surface = Color(0xFF201813),
    surfaceVariant = Color(0xFF2D221B),
    onSurface = Color(0xFFF7EBE3), onSurfaceVariant = Color(0xFFB59F92),
    outline = Color(0xFF43332A), outlineVariant = Color(0xFF2E241D),
    inversePrimary = Color(0xFFC2410C)
)

// ---------------------------------------------------------------------------------------------
// Forest: fresh greens & earth
// ---------------------------------------------------------------------------------------------

private val ForestLightScheme = paletteScheme(
    isDark = false,
    primary = Color(0xFF1F7A4D), onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F0DF), onPrimaryContainer = Color(0xFF0A3320),
    secondary = Color(0xFF587A35), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EFCF), onSecondaryContainer = Color(0xFF223312),
    tertiary = Color(0xFF8A6A2F), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2E6CC), onTertiaryContainer = Color(0xFF382A0B),
    background = Color(0xFFF3F7F2), surface = Color.White,
    surfaceVariant = Color(0xFFE2EBDF),
    onSurface = Color(0xFF122017), onSurfaceVariant = Color(0xFF4F6054),
    outline = Color(0xFFD3E0D1), outlineVariant = Color(0xFFE4ECE2),
    inversePrimary = Color(0xFF6FD79A)
)

private val ForestDarkScheme = paletteScheme(
    isDark = true,
    primary = Color(0xFF6FD79A), onPrimary = Color(0xFF07301B),
    primaryContainer = Color(0xFF14502F), onPrimaryContainer = Color(0xFFC9F2D8),
    secondary = Color(0xFFA9D17A), onSecondary = Color(0xFF1C2E08),
    secondaryContainer = Color(0xFF2D4416), onSecondaryContainer = Color(0xFFDCF0BE),
    tertiary = Color(0xFFE2C275), onTertiary = Color(0xFF352607),
    tertiaryContainer = Color(0xFF4B3810), onTertiaryContainer = Color(0xFFF6E3B4),
    background = Color(0xFF0B140F), surface = Color(0xFF121E16),
    surfaceVariant = Color(0xFF1C2C21),
    onSurface = Color(0xFFE6F0E8), onSurfaceVariant = Color(0xFF92A898),
    outline = Color(0xFF2A3E30), outlineVariant = Color(0xFF1D2D22),
    inversePrimary = Color(0xFF1F7A4D)
)

// ---------------------------------------------------------------------------------------------
// Berry: rich plum & raspberry
// ---------------------------------------------------------------------------------------------

private val BerryLightScheme = paletteScheme(
    isDark = false,
    primary = Color(0xFF9D2A7C), onPrimary = Color.White,
    primaryContainer = Color(0xFFF9DCEE), onPrimaryContainer = Color(0xFF4A0D38),
    secondary = Color(0xFF6D3FC0), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DDFB), onSecondaryContainer = Color(0xFF2C1259),
    tertiary = Color(0xFFC13B62), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCE5), onTertiaryContainer = Color(0xFF4A0F22),
    background = Color(0xFFFAF3F8), surface = Color.White,
    surfaceVariant = Color(0xFFEEE1EA),
    onSurface = Color(0xFF24101E), onSurfaceVariant = Color(0xFF664D5E),
    outline = Color(0xFFE6D3E0), outlineVariant = Color(0xFFF0E4EC),
    inversePrimary = Color(0xFFF29AD6)
)

private val BerryDarkScheme = paletteScheme(
    isDark = true,
    primary = Color(0xFFF29AD6), onPrimary = Color(0xFF4A0D38),
    primaryContainer = Color(0xFF6E1E55), onPrimaryContainer = Color(0xFFFFD9F1),
    secondary = Color(0xFFB79BFF), onSecondary = Color(0xFF2C1259),
    secondaryContainer = Color(0xFF402478), onSecondaryContainer = Color(0xFFE8DDFF),
    tertiary = Color(0xFFFF93AE), onTertiary = Color(0xFF4A0F22),
    tertiaryContainer = Color(0xFF62213A), onTertiaryContainer = Color(0xFFFFD9E1),
    background = Color(0xFF130C12), surface = Color(0xFF1D131C),
    surfaceVariant = Color(0xFF2A1D28),
    onSurface = Color(0xFFF6E9F2), onSurfaceVariant = Color(0xFFB7A0B1),
    outline = Color(0xFF402E3D), outlineVariant = Color(0xFF2C1F2A),
    inversePrimary = Color(0xFF9D2A7C)
)

// ---------------------------------------------------------------------------------------------
// Honey: golden warmth on soft cream
// ---------------------------------------------------------------------------------------------

private val HoneyLightScheme = paletteScheme(
    isDark = false,
    // Deep gold keeps primary text/icons readable on cream; the bright honey shows up in containers.
    primary = Color(0xFF946600), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE9A6), onPrimaryContainer = Color(0xFF3B2C00),
    secondary = Color(0xFF7A6420), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E9C6), onSecondaryContainer = Color(0xFF2F2606),
    tertiary = Color(0xFF8C5A2B), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF5E0CC), onTertiaryContainer = Color(0xFF3A2210),
    background = Color(0xFFFBF7EC), surface = Color.White,
    surfaceVariant = Color(0xFFF0E9D2),
    onSurface = Color(0xFF231D0E), onSurfaceVariant = Color(0xFF62583B),
    outline = Color(0xFFE8DFC2), outlineVariant = Color(0xFFF1EAD3),
    inversePrimary = Color(0xFFF5C84B)
)

private val HoneyDarkScheme = paletteScheme(
    isDark = true,
    primary = Color(0xFFF5C84B), onPrimary = Color(0xFF2E2200),
    primaryContainer = Color(0xFF594200), onPrimaryContainer = Color(0xFFFFE8A3),
    secondary = Color(0xFFD9C27C), onSecondary = Color(0xFF2F2606),
    secondaryContainer = Color(0xFF433710), onSecondaryContainer = Color(0xFFF3E7BC),
    tertiary = Color(0xFFE5B58A), onTertiary = Color(0xFF3A2210),
    tertiaryContainer = Color(0xFF563619), onTertiaryContainer = Color(0xFFF7DFC8),
    background = Color(0xFF14110A), surface = Color(0xFF1E1A10),
    surfaceVariant = Color(0xFF2B2618),
    onSurface = Color(0xFFF5EFDD), onSurfaceVariant = Color(0xFFB5AB8C),
    outline = Color(0xFF41391F), outlineVariant = Color(0xFF2D2818),
    inversePrimary = Color(0xFF946600)
)

// ---------------------------------------------------------------------------------------------
// Builder
// ---------------------------------------------------------------------------------------------

/**
 * Builds a full scheme from the 21 tokens that actually define a palette and derives the rest
 * (surface containers, inverse colours, surface tint) so no baseline-purple Material defaults
 * leak into dialogs, nav bars, sheets or snackbars.
 */
private fun paletteScheme(
    isDark: Boolean,
    primary: Color, onPrimary: Color, primaryContainer: Color, onPrimaryContainer: Color,
    secondary: Color, onSecondary: Color, secondaryContainer: Color, onSecondaryContainer: Color,
    tertiary: Color, onTertiary: Color, tertiaryContainer: Color, onTertiaryContainer: Color,
    background: Color, surface: Color, surfaceVariant: Color,
    onSurface: Color, onSurfaceVariant: Color,
    outline: Color, outlineVariant: Color,
    inversePrimary: Color
): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    val containerMid = lerp(if (isDark) surface else background, surfaceVariant, 0.5f)
    val containerHighest = lerp(surfaceVariant, outline, 0.5f)

    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        inverseSurface = onSurface,
        inverseOnSurface = background,
        outline = outline,
        outlineVariant = outlineVariant,
        error = ErrorRed,
        errorContainer = if (isDark) Color(0xFF451E1E) else ErrorSoft,
        onErrorContainer = if (isDark) Color(0xFFFFB4AB) else ErrorRed,
        surfaceBright = if (isDark) surfaceVariant else surface,
        surfaceDim = if (isDark) background else surfaceVariant,
        surfaceContainerLowest = if (isDark) background else surface,
        surfaceContainerLow = if (isDark) surface else background,
        surfaceContainer = containerMid,
        surfaceContainerHigh = surfaceVariant,
        surfaceContainerHighest = containerHighest
    )
}
