package com.invatech.pockit.ui.common.swipe

/*
 * Reusable swipe-to-delete presentation with optional confirmation and collapse animation.
 * Calls the parent action after the relevant gesture/animation; the parent owns persistence and undo.
 */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.common.motion.M3Spring
import com.invatech.pockit.ui.theme.ErrorRed
import kotlinx.coroutines.delay

/**
 * A buttery-smooth swipe-to-delete container with physics-based spring animations,
 * dynamic scaling trash indicator, haptic feedback when crossing threshold,
 * and seamless vertical collapse animation.
 *
 * If [requireConfirmation] is true, sliding past threshold invokes [onDelete] to trigger
 * a confirmation alert/dialog and springs the item back to settled state instead of auto-deleting.
 */
@Composable
fun SwipeToDeleteContainer(
    itemKey: Any,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    enabled: Boolean = true,
    requireConfirmation: Boolean = false,
    content: @Composable () -> Unit
) {
    var isDismissed by remember(itemKey) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
        confirmValueChange = { targetValue ->
            if (targetValue == SwipeToDismissBoxValue.EndToStart) {
                if (requireConfirmation) {
                    onDelete()
                    false // Prevent direct dismissal, snap back so user can confirm via dialog
                } else {
                    true
                }
            } else {
                false
            }
        }
    )

    // Trigger tactile haptic feedback as soon as threshold is crossed
    val willDismiss = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
    LaunchedEffect(willDismiss) {
        if (willDismiss && !isDismissed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Detect when item has been released past threshold (when requireConfirmation is false)
    LaunchedEffect(dismissState.currentValue) {
        if (!requireConfirmation && dismissState.currentValue == SwipeToDismissBoxValue.EndToStart && !isDismissed) {
            isDismissed = true
        }
    }

    // Smoothly wait for the vertical collapse animation before invoking onDelete
    LaunchedEffect(isDismissed) {
        if (isDismissed) {
            delay(280)
            onDelete()
        }
    }

    AnimatedVisibility(
        visible = !isDismissed,
        modifier = modifier,
        exit = shrinkVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeOut(
            animationSpec = tween(durationMillis = 180)
        )
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = enabled,
            backgroundContent = {
                val backgroundColor by animateColorAsState(
                    targetValue = if (willDismiss) com.invatech.pockit.ui.theme.ErrorSoft else com.invatech.pockit.ui.theme.ErrorSoft.copy(alpha = 0.82f),
                    animationSpec = M3Spring.fastEffects(),
                    label = "swipe_delete_bg"
                )

                val iconScale by animateFloatAsState(
                    targetValue = if (willDismiss) 1.25f else 1.0f,
                    animationSpec = M3Spring.bouncy(),
                    label = "swipe_delete_icon_scale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(backgroundColor)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.scale(iconScale)
                    ) {
                        Text(
                            text = "Delete",
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ErrorRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (willDismiss) Icons.Rounded.Delete else Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            content = {
                content()
            }
        )
    }
}
