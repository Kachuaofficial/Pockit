package com.invatech.pockit.ui.space.components

/*
 * Space-list item showing identity and the current-user position calculated for the supplied space.
 */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.invatech.pockit.ui.space.model.CurrentUserId
import com.invatech.pockit.ui.space.model.Space
import com.invatech.pockit.ui.space.util.BalanceCalculator
import com.invatech.pockit.ui.space.util.formatMoney
import com.invatech.pockit.ui.theme.Ink

@Composable
fun SpaceCard(
    space: Space,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth tactile spring animation on touch
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = M3Spring.touchFeedback(),
        label = "space_card_press_scale"
    )

    val myBalance = remember(space) {
        BalanceCalculator.netBalances(space)[CurrentUserId] ?: 0.0
    }
    val statusText = when {
        myBalance > 0.5 -> "You're owed ${formatMoney(myBalance)}"
        myBalance < -0.5 -> "You owe ${formatMoney(-myBalance)}"
        else -> "All settled"
    }

    val badgeBackground by animateColorAsState(
        targetValue = when {
            myBalance > 0.5 -> com.invatech.pockit.ui.theme.PockitElevated
            myBalance < -0.5 -> com.invatech.pockit.ui.theme.PockitElevated
            else -> com.invatech.pockit.ui.theme.PockitElevated
        },
        animationSpec = M3Spring.fastEffects(),
        label = "status_badge_color"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(30.dp))
            .background(space.accent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(com.invatech.pockit.ui.theme.PockitElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(text = space.emoji, style = MaterialTheme.typography.headlineMedium)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = space.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                Text(
                    text = "${space.members.size} members · ${formatMoney(space.totalSpent)} spent",
                    style = MaterialTheme.typography.labelMedium,
                    color = Ink.copy(alpha = 0.65f)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(com.invatech.pockit.ui.theme.PockitOrange),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = com.invatech.pockit.ui.theme.PockitBackground,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarStack(
                members = space.members,
                borderColor = space.accent,
                modifier = Modifier.weight(1f)
            )
            Surface(color = badgeBackground, shape = CircleShape) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
