package com.invatech.pockit.ui.space.components

/*
 * Floating shared-expense entry action with press feedback; navigation is delegated to the caller.
 */

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.common.motion.M3Spring
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.Mint

@Composable
fun FloatingAddExpense(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = M3Spring.touchFeedback(),
        label = "fab_scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 8.dp,
        animationSpec = M3Spring.fastEffects(),
        label = "fab_elevation"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
        shadowElevation = elevation,
        modifier = modifier.scale(scale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "Add expense",
                modifier = Modifier.padding(start = 10.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
