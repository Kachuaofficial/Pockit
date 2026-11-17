package com.invatech.pockit.ui.common.loading

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.theme.Cloud
import com.invatech.pockit.ui.theme.CloudDeep
import com.invatech.pockit.ui.theme.InkSoft
import com.invatech.pockit.ui.theme.Mint
import com.invatech.pockit.ui.theme.ShimmerDarkHighlight

/**
 * Material 3 Expressive Shimmer effect modifier.
 * Creates an elegant, high-framerate diagonal light sweep with a subtle brand tint
 * to eliminate blank loading flashes and give a production-ready feel.
 */
fun Modifier.expressiveShimmer(
    baseColor: Color? = null,
    highlightColor: Color? = null,
    durationMillis: Int = 1200
): Modifier = composed {
    val defaultBase = baseColor ?: CloudDeep
    val defaultHighlight = highlightColor ?: Cloud.copy(alpha = 0.95f)
    val brandTint = Mint.copy(alpha = 0.25f)

    val enabled by com.invatech.pockit.ui.common.motion.MotionSettings.enabled.collectAsState()
    if (!enabled) return@composed this.background(defaultBase)
    val transition = rememberInfiniteTransition(label = "expressive_shimmer_transition")
    val progress by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "expressive_shimmer_progress"
    )

    val shimmerColors = listOf(
        defaultBase,
        defaultHighlight,
        brandTint,
        defaultHighlight,
        defaultBase
    )

    this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(x = progress * 1000f, y = progress * 500f),
            end = Offset(x = (progress + 0.6f) * 1000f, y = (progress + 0.6f) * 500f)
        )
    )
}

/**
 * Convenience skeleton box placeholder.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    baseColor: Color? = null,
    highlightColor: Color? = null
) {
    Box(
        modifier = modifier
            .clip(shape)
            .expressiveShimmer(
                baseColor = baseColor,
                highlightColor = highlightColor
            )
    )
}
