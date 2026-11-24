package com.invatech.pockit.ui.theme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PockitShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(30.dp)
)
private val PlayfulColors = lightColorScheme(
    primary = PockitOrangeDeep, onPrimary = Color.White,
    primaryContainer = PastelPink, onPrimaryContainer = PockitText,
    inversePrimary = PockitOrangeDeep,
    secondary = PockitPurple, onSecondary = Color.White,
    secondaryContainer = PastelLilac, onSecondaryContainer = PockitText,
    tertiary = PockitGreen, onTertiary = Color.White,
    tertiaryContainer = PastelMint, onTertiaryContainer = PockitText,
    background = PockitBackground, onBackground = PockitText,
    surface = PockitSurface, onSurface = PockitText,
    surfaceVariant = PockitElevated, onSurfaceVariant = PockitSecondaryText,
    surfaceTint = Color.Transparent,
    surfaceDim = PockitBackground, surfaceBright = PockitElevated,
    surfaceContainerLowest = PockitBackground, surfaceContainerLow = PockitSurface,
    surfaceContainer = PockitSurface, surfaceContainerHigh = PockitElevated,
    surfaceContainerHighest = PockitElevated,
    outline = PockitMutedText, outlineVariant = PockitBorder,
    error = ExpenseRed, onError = Color.White,
    errorContainer = ExpenseRedContainer, onErrorContainer = ExpenseRed,
    inverseSurface = PockitText, inverseOnSurface = PockitBackground, scrim = Color.Black
)
/** Fixed brand across screens, sheets, dialogs and platform system bars. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PockitTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(colorScheme = PlayfulColors, typography = PockitTypography,
        shapes = PockitShapes, motionScheme = MotionScheme.expressive(), content = content)
}
