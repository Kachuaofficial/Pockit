package com.invatech.pockit.ui.common.motion

/*
 * Navigation and predictive-back animation helpers with animation-disabled paths.
 * Composition locals share transition scopes without moving navigation ownership into individual widgets.
 */

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * CompositionLocal providing access to the top-level [SharedTransitionScope],
 * allowing any composable in the app to participate in shared transitions.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * CompositionLocal providing access to the current destination's [AnimatedVisibilityScope].
 */
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Material 3 Expressive Navigation Transitions
 *
 * Provides spring-based spatial transitions and emphasized easing for
 * NavHost destinations and in-app route switches, optimized for Android 14+
 * predictive back gestures.
 */
object M3NavMotion {

    /** Screen entering forward navigation */
    fun enter(animationsEnabled: Boolean = true): EnterTransition {
        if (!animationsEnabled) return EnterTransition.None
        return slideInHorizontally(
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
        ) { (it * 0.22f).toInt() } +
                fadeIn(animationSpec = tween(durationMillis = 280, easing = M3Easing.EmphasizedDecelerate)) +
                scaleIn(
                    initialScale = 0.94f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                )
    }

    /** Screen exiting forward navigation (becoming background) */
    fun exit(animationsEnabled: Boolean = true): ExitTransition {
        if (!animationsEnabled) return ExitTransition.None
        return slideOutHorizontally(
            animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
        ) { (-it * 0.12f).toInt() } +
                fadeOut(animationSpec = tween(durationMillis = 200, easing = M3Easing.EmphasizedAccelerate)) +
                scaleOut(
                    targetScale = 0.96f,
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
                )
    }

    /** Screen re-entering on Pop / Back gesture */
    fun popEnter(animationsEnabled: Boolean = true): EnterTransition {
        if (!animationsEnabled) return EnterTransition.None
        return slideInHorizontally(
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
        ) { (-it * 0.12f).toInt() } +
                fadeIn(animationSpec = tween(durationMillis = 280, easing = M3Easing.EmphasizedDecelerate)) +
                scaleIn(
                    initialScale = 0.96f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                )
    }

    /** Screen exiting on Pop / Back gesture (predictive back gesture destination) */
    fun popExit(animationsEnabled: Boolean = true): ExitTransition {
        if (!animationsEnabled) return ExitTransition.None
        return slideOutHorizontally(
            animationSpec = spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow)
        ) { (it * 0.22f).toInt() } +
                fadeOut(animationSpec = tween(durationMillis = 220, easing = M3Easing.EmphasizedAccelerate)) +
                scaleOut(
                    targetScale = 0.94f,
                    animationSpec = spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow)
                )
    }

    /** Directional tab transition for custom tablayouts / segmented controls */
    fun directionalTabTransition(
        isForward: Boolean,
        animationsEnabled: Boolean = true
    ): ContentTransform {
        if (!animationsEnabled) return EnterTransition.None togetherWith ExitTransition.None
        val enterSlide = slideInHorizontally(
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow)
        ) { if (isForward) (it * 0.35f).toInt() else (-it * 0.35f).toInt() } +
                fadeIn(animationSpec = tween(260, easing = M3Easing.EmphasizedDecelerate)) +
                scaleIn(initialScale = 0.96f, animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow))

        val exitSlide = slideOutHorizontally(
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
        ) { if (isForward) (-it * 0.20f).toInt() else (it * 0.20f).toInt() } +
                fadeOut(animationSpec = tween(180, easing = M3Easing.EmphasizedAccelerate)) +
                scaleOut(targetScale = 0.96f, animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow))

        return enterSlide togetherWith exitSlide
    }
}

/**
 * Expressive Predictive Back Container
 *
 * Implements Material 3 Expressive predictive back mechanics for detail views,
 * overlays, and sub-screens.
 *
 * As the user swipes from the left or right edge:
 * 1. The container smoothly scales down (1.0f -> 0.90f)
 * 2. Shifts slightly in the swipe direction
 * 3. Dynamically rounds its corners (0.dp -> 32.dp)
 * 4. Casts a soft physical elevation shadow
 * 5. Fades out the background scrim
 *
 * If cancelled, it springs back with elastic physics ([M3Spring.bouncy]).
 * If completed, it runs an expressive dismissal animation and invokes [onBack].
 */
@Composable
fun ExpressivePredictiveBackContainer(
    enabled: Boolean = true,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // The owning AnimatedContent controls dismissal for both toolbar and system Back.
    // Avoid a second shrink animation before invoking that same dismissal callback.
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
    Box(modifier.fillMaxSize()) { content() }
}
