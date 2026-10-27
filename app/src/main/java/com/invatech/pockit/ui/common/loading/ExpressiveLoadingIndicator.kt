package com.invatech.pockit.ui.common.loading

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.LavenderDeep
import com.invatech.pockit.ui.theme.MintDeep
import com.invatech.pockit.ui.theme.PeachDeep
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Signature Material 3 Expressive Loading Indicator.
 *
 * Implements Google's Material 3 Expressive motion physics:
 * - Fluid geometric shape morphing (Circle -> Squircle -> 4-Petal Clover -> Diamond).
 * - Breathing scale and elastic rotation.
 * - Dynamic Pockit brand color cycling (Mint -> Lavender -> Peach -> Ink).
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    customColor: Color? = null,
    showHalo: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "expressive_morph_transition")

    // Rotation with expressive easing (fast acceleration + overshoot settle)
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "expressive_rotation"
    )

    // Morph stage factor [0f to 4f]
    // 0: Circle, 1: Squircle, 2: Clover/Flower, 3: Diamond, 4: Circle
    val morphStage by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "expressive_morph_stage"
    )

    // Breathing scale [0.88f to 1.12f]
    val scaleFactor by transition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "expressive_scale"
    )

    // Expressive Brand Color Cycling
    val dynamicColor by transition.animateColor(
        initialValue = MintDeep,
        targetValue = LavenderDeep,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3600
                MintDeep at 0
                LavenderDeep at 900
                PeachDeep at 1800
                Ink at 2700
                MintDeep at 3600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "expressive_color"
    )

    val activeColor = customColor ?: dynamicColor

    Box(
        modifier = modifier
            .size(size)
            .scale(scaleFactor)
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val outerRadius = size.toPx() * 0.40f

            // Optional subtle ambient glow/halo
            if (showHalo) {
                drawCircle(
                    color = activeColor.copy(alpha = 0.15f),
                    radius = outerRadius * 1.25f,
                    center = center
                )
            }

            // Draw fluid morphing shape
            val path = createMorphingShapePath(
                center = center,
                radius = outerRadius,
                stage = morphStage
            )

            // Fill shape
            drawPath(
                path = path,
                color = activeColor
            )

            // Center counter-pulsing dot for layered depth
            val innerDotRadius = outerRadius * 0.22f
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = innerDotRadius,
                center = center
            )
        }
    }
}

/**
 * Computes a smooth mathematical path that morphs between:
 * Circle (stage ~ 0 & 4), Squircle (stage ~ 1), 4-Petal Clover (stage ~ 2), and Diamond (stage ~ 3).
 */
private fun createMorphingShapePath(
    center: Offset,
    radius: Float,
    stage: Float
): Path {
    val path = Path()
    val steps = 72
    val normalizedStage = stage % 4f

    // Morph parameters for parametric shape formula:
    // r(theta) = R * [1 + cloverFactor * cos(4*theta) + squircleFactor * cos(4*theta)]
    val cloverFactor: Float
    val pinchFactor: Float

    when {
        normalizedStage < 1f -> {
            // Circle to Squircle: subtle corner expansion
            val t = normalizedStage
            cloverFactor = 0f
            pinchFactor = 0.08f * t
        }
        normalizedStage < 2f -> {
            // Squircle to Clover/Flower: 4 petals bloom
            val t = normalizedStage - 1f
            cloverFactor = 0.26f * t
            pinchFactor = 0.08f * (1f - t)
        }
        normalizedStage < 3f -> {
            // Clover to Diamond: clover lobes taper
            val t = normalizedStage - 2f
            cloverFactor = 0.26f * (1f - t) - 0.22f * t
            pinchFactor = 0f
        }
        else -> {
            // Diamond back to Circle
            val t = normalizedStage - 3f
            cloverFactor = -0.22f * (1f - t)
            pinchFactor = 0f
        }
    }

    for (i in 0 until steps) {
        val theta = (i.toFloat() / steps) * 2f * PI.toFloat()
        val cos4Theta = cos(4f * theta)

        // Modified superellipse/polar radius
        val currentRadius = radius * (1f + cloverFactor * cos4Theta - pinchFactor * cos(8f * theta))

        val x = center.x + currentRadius * cos(theta)
        val y = center.y + currentRadius * sin(theta)

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    path.close()
    return path
}

/**
 * Material 3 Expressive Indeterminate Circular Progress Indicator.
 * Features fluid arc velocity with rounded stroke caps and a centered pulsating dot.
 */
@Composable
fun ExpressiveCircularIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    strokeWidth: Dp = 3.5.dp,
    color: Color = MintDeep
) {
    val transition = rememberInfiniteTransition(label = "circular_indeterminate_transition")

    val startAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1332, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "circular_start_angle"
    )

    val sweepAngle by transition.animateFloat(
        initialValue = 28f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circular_sweep_angle"
    )

    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Round
        )

        val padding = strokeWidth.toPx() / 2f
        val arcSize = Size(this.size.width - padding * 2, this.size.height - padding * 2)

        rotate(startAngle) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(padding, padding),
                size = arcSize,
                style = stroke
            )
        }
    }
}

/**
 * Expressive 3-Pill pulse indicator, ideal for buttons and compact status views.
 */
@Composable
fun ExpressivePulsingPills(
    modifier: Modifier = Modifier,
    color: Color = MintDeep,
    pillWidth: Dp = 6.dp,
    maxHeight: Dp = 22.dp,
    minHeight: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "expressive_pills_transition")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(0, 160, 320).forEach { delayMs ->
            val fraction by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 900
                        0f at 0
                        1f at 450
                        0f at 900
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "pill_delay_$delayMs"
            )

            val currentHeight = minHeight + (maxHeight - minHeight) * fraction

            Box(
                modifier = Modifier
                    .size(width = pillWidth, height = currentHeight)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/**
 * Expressive Fullscreen or Card Loading Overlay.
 */
@Composable
fun ExpressiveLoadingOverlay(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ExpressiveLoadingIndicator(
                size = 54.dp,
                showHalo = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
