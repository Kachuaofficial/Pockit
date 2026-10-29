package com.invatech.pockit.ui.space.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.space.model.Member
import com.invatech.pockit.ui.space.util.Settlement
import com.invatech.pockit.ui.space.util.formatMoney
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.Mint
import com.invatech.pockit.ui.theme.Peach

@Composable
fun SettleUpCard(
    settlements: List<Settlement>,
    members: Map<String, Member>,
    onSettle: (Settlement) -> Unit,
    pendingSettlementKeys: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Settle up",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            settlements.forEach { settlement ->
                val from = members[settlement.fromId]
                val to = members[settlement.toId]
                if (from != null && to != null) {
                    val isPending = pendingSettlementKeys.contains("${settlement.fromId}_${settlement.toId}")

                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.92f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "settle_btn_scale"
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MemberAvatar(member = from, size = 40.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${from.displayName} ${if (from.isYou) "pay" else "pays"} ${to.sentenceName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatMoney(settlement.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        when {
                            isPending -> {
                                Surface(
                                    color = Peach,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "Pending approval",
                                        color = Ink,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            from.isYou -> {
                                Surface(
                                    color = Mint,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .scale(buttonScale)
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            onClick = { onSettle(settlement) }
                                        )
                                ) {
                                    Text(
                                        text = "I have Paid",
                                        color = Ink,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            to.isYou -> {
                                Surface(
                                    color = Mint,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .scale(buttonScale)
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            onClick = { onSettle(settlement) }
                                        )
                                ) {
                                    Text(
                                        text = "Settle",
                                        color = Ink,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
