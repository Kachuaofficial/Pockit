package com.invatech.pockit.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.common.motion.rememberPressScale
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.Mint
import kotlin.math.abs

private val ItemHeight = 52.dp
private val BarHeight = ItemHeight + 12.dp // 6.dp vertical padding top + bottom
private val ItemSpacing = 4.dp
private val PillRadius = 18.dp
private const val ColorDuration = 220

@Composable
fun PockitBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember { bottomBarScreens.toList() }

    // Bar glides up from the bottom the first time it appears.
    val enterState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    // Which tab is highlighted. On detail screens (no tab matches) we keep the pill
    // parked on the last tab and just fade it out, instead of snapping it back to 0.
    val activeIndex = tabs.indexOfFirst { it.route == currentRoute }
    var lastActiveIndex by remember {
        mutableIntStateOf(if (activeIndex >= 0) activeIndex else 0)
    }
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) lastActiveIndex = activeIndex
    }
    val targetIndex = if (activeIndex >= 0) activeIndex else lastActiveIndex

    // One shared pill that slides between tabs with a slightly underdamped spring.
    val indicatorPosition by animateFloatAsState(
        targetValue = targetIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bottomBarIndicatorPosition"
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (activeIndex >= 0) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "bottomBarIndicatorAlpha"
    )

    AnimatedVisibility(
        visibleState = enterState,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetY = { it }
        ) + fadeIn(tween(300))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Ink,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .heightIn(min = BarHeight)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .drawBehind {
                        val count = tabs.size
                        if (count == 0 || indicatorAlpha <= 0.01f) return@drawBehind

                        val spacingPx = ItemSpacing.toPx()
                        val itemWidthPx = (size.width - spacingPx * (count - 1)) / count

                        // Squash & stretch: while the pill is travelling it gets a bit
                        // wider and flatter, then settles back. Reads like liquid.
                        val travel = abs(targetIndex - indicatorPosition).coerceIn(0f, 1f)
                        val width = itemWidthPx * (1f + 0.16f * travel)
                        val height = size.height * (1f - 0.07f * travel)
                        val left = indicatorPosition * (itemWidthPx + spacingPx) -
                                (width - itemWidthPx) / 2f
                        val top = (size.height - height) / 2f

                        drawRoundRect(
                            color = Mint,
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            cornerRadius = CornerRadius(PillRadius.toPx()),
                            alpha = indicatorAlpha
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { screen ->
                    androidx.compose.runtime.key(screen.route) {
                        BottomBarItem(
                            screen = screen,
                            selected = currentRoute == screen.route,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(screen) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    screen: Screen,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale by rememberPressScale(interactionSource, pressedScale = 0.90f)
    val haptics = LocalHapticFeedback.current

    val contentColor by animateColorAsState(
        targetValue = if (selected) Ink else Color.White.copy(alpha = 0.72f),
        animationSpec = tween(durationMillis = ColorDuration),
        label = "bottomBarContentColor"
    )

    // Icon pops (overshoots) when its tab gets selected.
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.16f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_scale"
    )
    // ...and lifts a tiny bit so the label gets breathing room.
    val iconLift by animateFloatAsState(
        targetValue = if (selected) -1.5f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_lift"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .height(ItemHeight)
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = {
                    if (!selected) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onClick()
                }
            )
            .padding(horizontal = 3.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = screen.label,
            tint = contentColor,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    translationY = iconLift.dp.toPx()
                }
        )

        Text(
            text = screen.label,
            color = contentColor,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}