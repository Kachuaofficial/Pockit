package com.invatech.pockit.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.data.rewards.UserRewardManager
import com.invatech.pockit.ui.common.mascot.InteractivePockitMascot
import com.invatech.pockit.ui.common.mascot.MascotPose
import com.invatech.pockit.ui.common.motion.kawaiiFloating
import com.invatech.pockit.ui.common.motion.rememberPressScale
import com.invatech.pockit.ui.theme.CocoaEspresso
import com.invatech.pockit.ui.theme.CocoaMuted
import com.invatech.pockit.ui.theme.CocoaSoft
import com.invatech.pockit.ui.theme.PastelPeach
import com.invatech.pockit.ui.theme.PastelPink
import com.invatech.pockit.ui.theme.WarmBorder
import com.invatech.pockit.ui.theme.WarmCreamMuted
import java.text.NumberFormat
import java.util.Locale

/**
 * Aesthetic Header matching the "Beautiful Design / Make recording enjoyable" mockup:
 * - "Record" playful title with thought bubble
 * - Dual metric counters: Expenses ▼ and Income ▲
 * - Cute sleeping cat doodle illustration
 * - Action button on the top right
 */
@Composable
fun RecordHeaderSection(
    totalExpenses: Double,
    totalIncome: Double,
    balance: Double,
    unreadNotificationCount: Int = 0,
    onNotificationClick: () -> Unit = {},
    onRewardsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val notifSource = remember { MutableInteractionSource() }
    val notifScale by rememberPressScale(notifSource, pressedScale = 0.92f)

    val coins by UserRewardManager.coinsFlow.collectAsState()
    val canClaimDaily = remember(coins) { UserRewardManager.canClaimDailyReward() }
    val coinSource = remember { MutableInteractionSource() }
    val coinScale by rememberPressScale(coinSource, pressedScale = 0.92f)

    // Formatted amounts without jittery counting animations
    val indianFormat = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN")) }
    val expenseText = remember(totalExpenses) { indianFormat.format(totalExpenses.toLong()) }
    val incomeText = remember(totalIncome) { indianFormat.format(totalIncome.toLong()) }
    val balanceText = remember(balance) { indianFormat.format(balance.toLong()) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Row: "Record" + Thought bubble + Grid / Notif Action Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Record",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = CocoaEspresso
                )

                // Cute tiny thought doodle with gentle floating motion
                DoodleThoughtBubble(
                    modifier = Modifier
                        .size(24.dp)
                        .kawaiiFloating(durationMillis = 2600, floatDistance = 2.5f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Clickable Golden Coin Badge
                Surface(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = coinScale
                            scaleY = coinScale
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = coinSource,
                            indication = null,
                            onClick = onRewardsClick
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF7DD),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🪙", fontSize = 13.sp)
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

                // Top right rounded squircle action button
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = notifScale
                            scaleY = notifScale
                        }
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CocoaEspresso)
                        .clickable(
                            interactionSource = notifSource,
                            indication = null,
                            onClick = onNotificationClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (unreadNotificationCount > 0) Icons.Rounded.NotificationsActive else Icons.Rounded.GridView,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )

                    if (unreadNotificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PastelPink)
                        )
                    }
                }
            }
        }

        // Hero Current Available Balance (Big, Bold & Prominent)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "TOTAL BALANCE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = CocoaMuted
                )
                Text(
                    text = "🐾",
                    fontSize = 12.sp
                )
            }

            Text(
                text = "₹ $balanceText",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                ),
                color = CocoaEspresso
            )
        }

        // Dual Flow Metrics & Mascot Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Expenses Pill Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = PastelPeach.copy(alpha = 0.45f),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = CocoaMuted
                        )
                        Text(
                            text = "▼",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color(0xFFD32F2F)
                        )
                    }
                    Text(
                        text = "₹$expenseText",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CocoaEspresso
                    )
                }
            }

            // Center: Interactive sleeping mascot with petting reactions, quotes, and heart bursts
            InteractivePockitMascot(
                pose = MascotPose.SLEEPING,
                size = 64.dp,
                modifier = Modifier
                    .width(64.dp)
                    .padding(horizontal = 4.dp)
            )

            // Right: Income Pill Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFE8F5E9).copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = CocoaMuted
                        )
                        Text(
                            text = "▲",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Text(
                        text = "₹$incomeText",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@Composable
private fun DoodleThoughtBubble(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 1.8.dp.toPx()
        val color = CocoaMuted
        drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(size.width * 0.25f, size.height * 0.75f))
        drawCircle(color = color, radius = 3.5.dp.toPx(), center = Offset(size.width * 0.45f, size.height * 0.50f))
        drawCircle(
            color = color,
            radius = 6.dp.toPx(),
            center = Offset(size.width * 0.72f, size.height * 0.32f),
            style = Stroke(width = stroke)
        )
    }
}
