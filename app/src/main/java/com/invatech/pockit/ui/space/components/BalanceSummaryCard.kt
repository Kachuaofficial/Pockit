package com.invatech.pockit.ui.space.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.space.util.formatMoney
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.Mint
import com.invatech.pockit.ui.theme.Rose
import kotlin.math.abs

/**
 * Premium dark card summarizing what the current user owes / is owed with rolling counter animations.
 */
@Composable
fun BalanceSummaryCard(
    myBalance: Double,
    modifier: Modifier = Modifier
) {
    val isOwed = myBalance > 0.5
    val owes = myBalance < -0.5

    // Fluid rolling number animation
    val animatedBalance by animateFloatAsState(
        targetValue = abs(myBalance).toFloat(),
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "animated_balance_counter"
    )

    // Dynamic accent color morphing
    val accent by animateColorAsState(
        targetValue = when {
            isOwed -> Mint
            owes -> Rose
            else -> Color.White
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "balance_accent_color"
    )

    val iconScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "icon_bounce"
    )

    val caption = when {
        isOwed -> "You're owed overall"
        owes -> "You owe overall"
        else -> "You're all settled"
    }

    Surface(
        color = Ink,
        shape = RoundedCornerShape(30.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted
                )
                Text(
                    text = formatMoney(animatedBalance.toDouble()),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted
                )
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .scale(iconScale)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isOwed -> Icons.Rounded.ArrowDownward
                        owes -> Icons.Rounded.ArrowUpward
                        else -> Icons.Rounded.Check
                    },
                    contentDescription = null,
                    tint = Ink
                )
            }
        }
    }
}
