package com.invatech.pockit.ui.home.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.common.swipe.SwipeToDeleteContainer
import com.invatech.pockit.ui.home.model.Transaction
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.MintDeep

@Composable
fun RecentTransactionsCard(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier,
    onViewAll: () -> Unit = {},
    onDeleteTransaction: ((Transaction) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val viewAllScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "view_all_scale"
    )

    Surface(
        color = Ink,
        shape = RoundedCornerShape(32.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (transactions.isNotEmpty()) {
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MintDeep,
                        modifier = Modifier
                            .scale(viewAllScale)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = onViewAll
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions yet\nTap an action above to get started ✨",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Strictly take maximum 5 transactions on the home screen
                transactions.take(5).forEach { transaction ->
                    if (onDeleteTransaction != null && transaction.stringId.isNotBlank()) {
                        SwipeToDeleteContainer(
                            itemKey = transaction.stringId,
                            shape = RoundedCornerShape(22.dp),
                            requireConfirmation = true,
                            onDelete = { onDeleteTransaction(transaction) }
                        ) {
                            TransactionItem(transaction = transaction)
                        }
                    } else {
                        TransactionItem(transaction = transaction)
                    }
                }
            }
        }
    }
}
