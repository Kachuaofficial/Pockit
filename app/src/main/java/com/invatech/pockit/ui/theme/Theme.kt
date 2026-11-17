package com.invatech.pockit.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp)
)

/**
 * Pockit single warm brand theme.
 * Dark mode has been intentionally removed in favor of the signature warm pastel aesthetic.
 *
 * @param themeMode optional override for backward compatibility.
 * @param palette overrides the saved colour palette (handy for previews and the palette picker).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PockitTheme(
    themeMode: ThemeMode? = null,
    palette: PockitPalette? = null,
    content: @Composable () -> Unit
) {
    val activePalette by ThemeManager.palette.collectAsState()
    val selectedPalette = palette ?: activePalette

    // Pure light warm scheme - eliminate animated 32-color interpolation overhead for optimal performance
    val dark = false
    val colorScheme = remember(dark) {
        if (dark) androidx.compose.material3.darkColorScheme(
            primary = Color(0xFF70D7C0), primaryContainer = Color(0xFF123F38), onPrimaryContainer = Color(0xFFBAF5E6),
            background = Color(0xFF101817), surface = Color(0xFF182220), onSurface = Color(0xFFE3ECE8),
            onBackground = Color(0xFFE3ECE8), outlineVariant = Color(0xFF364B45)
        ) else androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF286B5E), onPrimary = Color.White,
            primaryContainer = Color(0xFFD9E9D5), onPrimaryContainer = Color(0xFF123D33),
            secondary = Color(0xFF48675F), background = Color(0xFFFFFBEF), surface = Color(0xFFFFFEF5),
            onSurface = Color(0xFF25262A), onBackground = Color(0xFF25262A),
            onSurfaceVariant = Color(0xFF586B64), outlineVariant = Color(0xFFE5DFD0)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            // Always dark icons on light warm canvas
            insetsController.isAppearanceLightStatusBars = !dark
            insetsController.isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = PockitTypography,
        shapes = PockitShapes,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
