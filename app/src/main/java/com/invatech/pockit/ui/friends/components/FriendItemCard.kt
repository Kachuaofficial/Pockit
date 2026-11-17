package com.invatech.pockit.ui.friends.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.common.motion.rememberPressScale
import com.invatech.pockit.ui.friends.model.ExpenseDirection
import com.invatech.pockit.ui.friends.model.FriendWithBalance
import com.invatech.pockit.ui.space.util.formatMoney
import com.invatech.pockit.ui.theme.CocoaEspresso
import com.invatech.pockit.ui.theme.CocoaMuted
import kotlin.math.abs

/**
 * Colorful kawaii pastel card for friends, consistent with Home TransactionItem cards.
 */
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
    val pressScale by rememberPressScale(interactionSource, pressedScale = 0.96f)

    // Cheerful pastel color schemes consistent with Home TransactionItem & RecordDoodleUi
    val (cardBg, avatarBg, initialColor, amountColor) = remember(balance) {
        when {
            balance > 0.01 -> Quadruple(
                Color(0xFFDFF5E0), // Soft Mint (Lent / You'll Get)
                Color(0xFF9EDDA5),
                Color(0xFF1E562B),
                Color(0xFF2E7D42)
            )
            balance < -0.01 -> Quadruple(
                Color(0xFFFFDDD3), // Soft Coral/Peach (Borrowed / You'll Give)
                Color(0xFFFFA690),
                Color(0xFF6B2D1E),
                Color(0xFFB33A24)
            )
            else -> Quadruple(
                Color(0xFFEDE6FB), // Soft Lavender (All Settled)
                Color(0xFFC9B8F3),
                Color(0xFF43306B),
                Color(0xFF5B458A)
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.invatech.pockit.ui.common.UserAvatar(friend.id, friend.name, Modifier.size(44.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = CocoaEspresso,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(1.dp))

                val subtitle = when {
                    lastTx != null -> {
                        val action = when (lastTx.direction) {
                            ExpenseDirection.GAVE_UDHAR -> "Gave ${formatMoney(lastTx.amount)}"
                            ExpenseDirection.TOOK_UDHAR -> "Took ${formatMoney(lastTx.amount)}"
                            ExpenseDirection.SETTLE_UP -> "Settled"
                        }
                        "$action • ${lastTx.description}"
                    }
                    friend.phone.isNotBlank() -> friend.phone
                    else -> "No transactions yet"
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = CocoaMuted,
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFB27A).copy(alpha = 0.3f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⏳ Pending",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8A3B14)
                        )
                    }
                }
                when {
                    balance > 0.01 -> {
                        Text(
                            text = "YOU'LL GET",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = amountColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = formatMoney(balance),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )
                    }
                    balance < -0.01 -> {
                        Text(
                            text = "YOU'LL GIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = amountColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = formatMoney(abs(balance)),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
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
                                tint = amountColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Settled",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = amountColor
                            )
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = CocoaMuted.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
