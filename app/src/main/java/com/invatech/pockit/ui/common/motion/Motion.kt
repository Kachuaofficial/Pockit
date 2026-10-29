package com.invatech.pockit.ui.common.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

/**
 * Material Design 3 Expressive Motion System
 *
 * Official References:
 * - https://m3.material.io/styles/motion/transitions/transition-patterns
 * - https://m3.material.io/styles/motion/easing-and-duration/applying-easing-and-duration
 *
 * Combines:
 * 1. M3 Expressive Motion Physics System (spring-based spatial & effects tokens with intentional overshoot)
 * 2. M3 Easing Curves (Emphasized and Standard sets)
 * 3. M3 Duration Tokens (Short 1-4, Medium 1-4, Long 1-4, Extra-long 1-4)
 */

object M3Duration {
    // Short durations (micro-interactions, selection pills, icon transforms)
    const val Short1 = 50
    const val Short2 = 100
    const val Short3 = 150
    const val Short4 = 200

    // Medium durations (transitions, sheet peek, dialog reveals)
    const val Medium1 = 250
    const val Medium2 = 300
    const val Medium3 = 350
    const val Medium4 = 400

    // Long durations (forward/backward page drill-down, container transforms)
    const val Long1 = 450
    const val Long2 = 500
    const val Long3 = 550
    const val Long4 = 600

    // Extra long durations (modal container transitions, full-screen reveals)
    const val ExtraLong1 = 700
    const val ExtraLong2 = 800
    const val ExtraLong3 = 900
    const val ExtraLong4 = 1000
}

object M3Easing {
    /**
     * Emphasized set: Captures the signature M3 expressive character.
     * Snappy take-offs and very soft landings.
     */
    val Emphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f) // Incoming elements entering screen
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f) // Outgoing elements exiting screen

    /**
     * Standard set: Functional, utilitarian motion for subtle helpers.
     */
    val Standard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val StandardDecelerate = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val StandardAccelerate = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)
}

// Direct top-level access for convenience
val Emphasized = M3Easing.Emphasized
val EmphasizedDecelerate = M3Easing.EmphasizedDecelerate
val EmphasizedAccelerate = M3Easing.EmphasizedAccelerate
val Standard = M3Easing.Standard
val StandardDecelerate = M3Easing.StandardDecelerate
val StandardAccelerate = M3Easing.StandardAccelerate

/**
 * Material 3 Expressive Motion Physics System
 *
 * Spring composite tokens combining damping and stiffness.
 * Deliberately employs subtle, tuned overshoots for expressive responsiveness.
 */
object M3Spring {
    /** Fast spatial transitions (small UI elements, switches, toggles). */
    fun <T> fastSpatial(): SpringSpec<T> = spring(
        dampingRatio = 0.70f,
        stiffness = Spring.StiffnessMedium
    )

    /** Default spatial transitions (cards, dialogs, bottom sheets, navigation pill). */
    fun <T> defaultSpatial(): SpringSpec<T> = spring(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessLow
    )

    /** Slow spatial transitions (large hero containers, complex layout reorders). */
    fun <T> slowSpatial(): SpringSpec<T> = spring(
        dampingRatio = 0.80f,
        stiffness = Spring.StiffnessVeryLow
    )

    /** Fast effects transitions (opacity, alpha, color cross-fades). */
    fun <T> fastEffects(): SpringSpec<T> = spring(
        dampingRatio = 0.90f,
        stiffness = Spring.StiffnessMedium
    )

    /** Default effects transitions (elevation, subtle lighting sweeps). */
    fun <T> defaultEffects(): SpringSpec<T> = spring(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Expressive bouncy spring with playful overshoot
     * (used for active tab icon pop, count-up numbers, badge pulses).
     */
    fun <T> bouncy(): SpringSpec<T> = spring(
        dampingRatio = 0.62f,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Tactile touch feedback spring: snappy compression and elastic rebound. */
    fun <T> touchFeedback(): SpringSpec<T> = spring(
        dampingRatio = 0.68f,
        stiffness = 1100f
    )

    /** Smooth placement spring for LazyColumn and LazyRow item additions/deletions. */
    fun <T> itemPlacement(): SpringSpec<T> = spring(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMediumLow
    )
}

/**
 * Standard duration-paired tween helper based on M3 motion guidelines:
 * - Entering: EmphasizedDecelerate + 400ms
 * - Exiting: EmphasizedAccelerate + 200ms
 * - Within screen: Emphasized + 500ms
 */
object M3Tween {
    fun <T> entering(duration: Int = M3Duration.Medium4): TweenSpec<T> = tween(
        durationMillis = duration,
        easing = M3Easing.EmphasizedDecelerate
    )

    fun <T> exiting(duration: Int = M3Duration.Short4): TweenSpec<T> = tween(
        durationMillis = duration,
        easing = M3Easing.EmphasizedAccelerate
    )

    fun <T> inside(duration: Int = M3Duration.Long2): TweenSpec<T> = tween(
        durationMillis = duration,
        easing = M3Easing.Emphasized
    )
}

/**
 * Tactile touch feedback modifier implementing M3 Expressive physics.
 * Compresses fluidly under touch and bounds back with a subtle overshoot.
 */
fun Modifier.expressivePressable(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.95f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = M3Spring.touchFeedback(),
        label = "expressive_press_scale"
    )
    this.scale(scale)
}

/**
 * Springy press feedback state helper.
 */
@Composable
fun rememberExpressivePressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.95f
): State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = M3Spring.touchFeedback(),
        label = "expressive_press_scale"
    )
}

/** Backwards-compatible alias for existing call sites. */
@Composable
fun rememberPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.94f
): State<Float> = rememberExpressivePressScale(interactionSource, pressedScale)
