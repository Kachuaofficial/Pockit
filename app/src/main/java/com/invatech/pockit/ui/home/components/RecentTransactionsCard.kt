package com.invatech.pockit.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.common.motion.M3Spring
import com.invatech.pockit.ui.common.swipe.SwipeToDeleteContainer
import com.invatech.pockit.ui.home.model.Transaction
import com.invatech.pockit.ui.theme.CocoaEspresso
import com.invatech.pockit.ui.theme.CocoaMuted
import com.invatech.pockit.ui.theme.WarmCreamSurface

@Composable
fun RecentTransactionsCard(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier,
    onViewAll: () -> Unit = {},
    onDeleteTransaction: ((Transaction) -> Unit)? = null,
    onEditTransaction: ((Transaction) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val viewAllScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = M3Spring.touchFeedback(),
        label = "view_all_scale"
    )

    // Synchronously group transactions into days & hours using RecordDoodleUi logic
    val dayGroups = remember(transactions) {
        groupTransactionsToRecordDays(transactions)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section title row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Records",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = CocoaEspresso,
                modifier = Modifier.weight(1f)
            )
            if (transactions.isNotEmpty()) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = CocoaEspresso,
                    modifier = Modifier
                        .scale(viewAllScale)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onViewAll
                        )
                        .background(WarmCreamSurface)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        if (transactions.isEmpty()) {
            Surface(
                color = WarmCreamSurface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No records yet 🐾\nTap + below to start recording!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CocoaMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Render each day group matching RecordDoodleUi
            dayGroups.forEach { day ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    RecordDayHeader(day = day)

                    day.hours.forEach { hour ->
                        RecordHourLabel(label = hour.label)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            hour.entries.forEach { entry ->
                                val tx = transactions.firstOrNull {
                                    it.stringId == entry.id || it.id.toString() == entry.id
                                }

                                if (tx != null) {
                                    val editAction: (() -> Unit)? = if (onEditTransaction != null) { { onEditTransaction(tx) } } else null
                                    if (onDeleteTransaction != null && tx.stringId.isNotBlank()) {
                                        SwipeToDeleteContainer(
                                            itemKey = tx.stringId,
                                            shape = RoundedCornerShape(20.dp),
                                            requireConfirmation = true,
                                            onDelete = { onDeleteTransaction(tx) }
                                        ) {
                                            TransactionItem(
                                                transaction = tx,
                                                onClick = editAction
                                            )
                                        }
                                    } else {
                                        TransactionItem(
                                            transaction = tx,
                                            onClick = editAction
                                        )
                                    }
                                } else {
                                    RecordEntryRow(entry = entry)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}
