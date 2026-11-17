package com.invatech.pockit.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.common.motion.rememberPressScale
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.Lavender
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import java.util.Calendar

@Composable
fun HomeHeader(
    userName: String,
    modifier: Modifier = Modifier,
    unreadNotificationCount: Int = 0,
    onNotificationClick: () -> Unit = {},
    onRewardsClick: () -> Unit = {}
) {
    val notifSource = remember { MutableInteractionSource() }
    val notifScale by rememberPressScale(notifSource, pressedScale = 0.92f)

    val coins by com.invatech.pockit.data.rewards.UserRewardManager.coinsFlow.collectAsState()
    val canClaimDaily = remember(coins) { com.invatech.pockit.data.rewards.UserRewardManager.canClaimDailyReward() }
    val coinSource = remember { MutableInteractionSource() }
    val coinScale by rememberPressScale(coinSource, pressedScale = 0.92f)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hey, $userName!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${greeting()} \uD83D\uDC4B",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Clickable Coin Balance Pill
        Surface(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = coinScale
                    scaleY = coinScale
                }
                .clip(CircleShape)
                .clickable(
                    interactionSource = coinSource,
                    indication = null,
                    onClick = onRewardsClick
                ),
            shape = CircleShape,
            color = Color(0xFFFFF7DD),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "🪙",
                    fontSize = 13.sp
                )
                Text(
                    text = "$coins",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE67E00)
                )
                if (canClaimDaily) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE64A19))
                    )
                }
            }
        }

        // Notification Bell Button with Unread Badge
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = notifScale
                    scaleY = notifScale
                }
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = notifSource,
                    indication = null,
                    onClick = onNotificationClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (unreadNotificationCount > 0) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
                contentDescription = "Notifications",
                tint = if (unreadNotificationCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )

            // Radiant Unread Count Badge
            if (unreadNotificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(if (unreadNotificationCount > 9) 18.dp else 12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (unreadNotificationCount > 9) {
                        Text(
                            text = "9+",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    } else if (unreadNotificationCount > 1) {
                        Text(
                            text = unreadNotificationCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userName.firstOrNull()?.uppercase() ?: "P",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
}
