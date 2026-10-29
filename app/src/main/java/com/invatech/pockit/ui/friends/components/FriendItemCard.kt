package com.invatech.pockit.ui.friends.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.invatech.pockit.ui.common.motion.M3Spring
import com.invatech.pockit.ui.friends.model.ExpenseDirection
import com.invatech.pockit.ui.friends.model.FriendWithBalance
import com.invatech.pockit.ui.space.util.formatMoney
import com.invatech.pockit.ui.theme.CloudDeep
import com.invatech.pockit.ui.theme.ErrorRed
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.Lavender
import com.invatech.pockit.ui.theme.Mint
import com.invatech.pockit.ui.theme.MintDeep
import com.invatech.pockit.ui.theme.Peach
import com.invatech.pockit.ui.theme.PeachDeep
import kotlin.math.abs

@Composable
fun FriendItemCard(
    friendWithBalance: FriendWithBalance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val friend = friendWithBalance.friend
    val balance = friendWithBalance.netBalance
    val lastTx = friendWithBalance.lastTransaction

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = M3Spring.touchFeedback(),
        label = "friend_card_scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Initial Circle
                val initial = friend.name.firstOrNull()?.uppercase() ?: "F"
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friend.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val subtitle = when {
                        lastTx != null -> {
                            val action = when (lastTx.direction) {
                                ExpenseDirection.GAVE_UDHAR -> "Gave ${formatMoney(lastTx.amount)}"
                                ExpenseDirection.TOOK_UDHAR -> "Took ${formatMoney(lastTx.amount)}"
                                ExpenseDirection.SETTLE_UP -> "Settled"
                            }
                            "$action â€¢ ${lastTx.description}"
                        }
                        friend.phone.isNotBlank() -> friend.phone
                        else -> "No transactions yet"
                    }

                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = InkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Balance Status & Chevron
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    if (friendWithBalance.pendingSettlements.isNotEmpty()) {
                        Text(
                            text = "⏳ Settle Pending",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = PeachDeep
                        )
                    }
                    when {
                        balance > 0 -> {
                            Text(
                                text = "YOU'LL GET",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintDeep,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = formatMoney(balance),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        balance < 0 -> {
                            Text(
                                text = "YOU'LL GIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = formatMoney(abs(balance)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        else -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = InkMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Settled",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = InkMuted
                                )
                            }
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = InkMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
