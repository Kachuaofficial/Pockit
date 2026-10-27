package com.invatech.pockit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Cloud,
    primaryContainer = Mint,
    onPrimaryContainer = Ink,
    secondary = LavenderDeep,
    onSecondary = Ink,
    secondaryContainer = Lavender,
    onSecondaryContainer = Ink,
    tertiary = PeachDeep,
    onTertiary = Ink,
    tertiaryContainer = Peach,
    onTertiaryContainer = Ink,
    background = Cloud,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = CloudDeep,
    onSurfaceVariant = InkMuted,
    error = ErrorRed,
    errorContainer = ErrorSoft,
    onErrorContainer = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Ink,
    primaryContainer = InkSoft,
    onPrimaryContainer = Mint,
    secondary = LavenderDeep,
    onSecondary = Ink,
    secondaryContainer = InkSoft,
    onSecondaryContainer = Lavender,
    tertiary = PeachDeep,
    onTertiary = Ink,
    tertiaryContainer = InkSoft,
    onTertiaryContainer = Peach,
    background = Ink,
    onBackground = Cloud,
    surface = InkSoft,
    onSurface = Cloud,
    surfaceVariant = InkSoft,
    onSurfaceVariant = InkMuted,
    error = ErrorRed,
    errorContainer = ErrorSoft,
    onErrorContainer = ErrorRed
)

private val PockitShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(32.dp)
)

@Composable
fun PockitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PockitTypography,
        shapes = PockitShapes,
        content = content
    )
}