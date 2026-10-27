package com.invatech.pockit.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.home.model.DaySpending
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.Lavender
import com.invatech.pockit.ui.theme.Mint

private val MaxBarHeight = 100.dp
private val BarWidth = 18.dp

@Composable
fun SpendingChartCard(
    data: List<DaySpending>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val maxAmount = remember(data) {
        (data.maxOfOrNull { it.amount } ?: 1f).coerceAtLeast(1f)
    }

    var selectedIndex by remember(data) {
        val highestIndex = data.indexOfFirst { it.amount == maxAmount }
        mutableStateOf(if (highestIndex >= 0) highestIndex else data.lastIndex)
    }

    val total = remember(data) {
        data.sumOf { it.amount.toDouble() }.toInt()
    }

    Surface(
        color = Mint,
        shape = RoundedCornerShape(32.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Spent this week",
                        style = MaterialTheme.typography.labelMedium,
                        color = Ink.copy(alpha = 0.65f)
                    )
                    Text(
                        text = "₹$total",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                }
                Surface(color = Lavender, shape = CircleShape) {
                    Text(
                        text = "Last 7 days",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sleek, fixed-proportional 7-day bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEachIndexed { index, day ->
                    val isSelected = index == selectedIndex
                    val hasSpending = day.amount > 0f

                    val targetHeight = if (!hasSpending) {
                        6.dp
                    } else if (appeared) {
                        (MaxBarHeight * (day.amount / maxAmount)).coerceIn(16.dp, MaxBarHeight)
                    } else {
                        6.dp
                    }

                    val barHeight by animateDpAsState(
                        targetValue = targetHeight,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "bar_height_$index"
                    )

                    val barColor by animateColorAsState(
                        targetValue = when {
                            isSelected && hasSpending -> Ink
                            hasSpending -> Color.White.copy(alpha = 0.85f)
                            else -> Color.White.copy(alpha = 0.35f)
                        },
                        label = "bar_color_$index"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                selectedIndex = index
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Value label above bar
                        Column(
                            modifier = Modifier.height(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AnimatedVisibility(
                                visible = isSelected && hasSpending,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Text(
                                    text = "₹${day.amount.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Sleek, elegant fixed-width pill capsule (NEVER bloated)
                        Box(
                            modifier = Modifier
                                .width(BarWidth)
                                .height(barHeight)
                                .clip(RoundedCornerShape(50))
                                .background(barColor)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = day.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Ink else Ink.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
