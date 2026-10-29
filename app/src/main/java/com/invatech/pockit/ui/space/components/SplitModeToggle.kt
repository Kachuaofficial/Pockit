package com.invatech.pockit.ui.space.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.common.motion.M3Spring
import com.invatech.pockit.ui.space.model.SplitMode

@Composable
fun SplitModeToggle(
    mode: SplitMode,
    onModeChange: (SplitMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        SplitMode.values().forEach { option ->
            val selected = option == mode
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.94f else 1f,
                animationSpec = M3Spring.touchFeedback(),
                label = "toggle_press_scale"
            )

            val background by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = M3Spring.fastEffects(),
                label = "segmentBackground"
            )
            val foreground by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = M3Spring.fastEffects(),
                label = "segmentForeground"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(background)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onModeChange(option) }
                    )
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.name,
                    color = foreground,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                )
            }
        }
    }
}
