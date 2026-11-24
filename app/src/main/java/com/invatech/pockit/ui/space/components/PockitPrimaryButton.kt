package com.invatech.pockit.ui.space.components

/*
 * Shared primary-action appearance and interaction feedback for the space forms.
 */

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.common.motion.M3Spring
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted

/**
 * High-production Material 3 Expressive tactile button with bouncy spring physics.
 */
@Composable
fun PockitPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = M3Spring.touchFeedback(),
        label = "primary_button_spring_scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(scale)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}
