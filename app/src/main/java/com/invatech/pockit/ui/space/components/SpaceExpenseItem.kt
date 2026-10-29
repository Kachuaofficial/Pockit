package com.invatech.pockit.ui.space.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.space.model.CurrentUserId
import com.invatech.pockit.ui.space.model.Member
import com.invatech.pockit.ui.space.model.OwedColor
import com.invatech.pockit.ui.space.model.SpaceExpense
import com.invatech.pockit.ui.space.util.formatMoney
import com.invatech.pockit.ui.theme.ErrorRed
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.Mint
import com.invatech.pockit.ui.theme.Peach

@Composable
fun SpaceExpenseItem(
    expense: SpaceExpense,
    members: Map<String, Member>,
    modifier: Modifier = Modifier
) {
    val payer = members[expense.paidById]
    val payerName = payer?.displayName ?: "Someone"
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val myShare = expense.shares[CurrentUserId]
    val isPendingSettlement = expense.isSettlement && !expense.isConfirmed

    val (statusText, statusColor) = when {
        isPendingSettlement -> "Pending confirmation" to ErrorRed
        expense.isSettlement -> "Settled up ✓" to OwedColor
        expense.paidById == CurrentUserId -> {
            val lent = expense.amount - (myShare ?: 0.0)
            if (lent > 0.005) "you lent ${formatMoney(lent)}" to OwedColor
            else "just you" to mutedColor
        }
        myShare != null -> "you owe ${formatMoney(myShare)}" to ErrorRed
        else -> "not involved" to mutedColor
    }

    val subtitle = if (expense.isSettlement) {
        val receiver = expense.shares.keys.firstOrNull()?.let { members[it] }
        if (isPendingSettlement) {
            "$payerName sent payment to ${receiver?.sentenceName ?: "someone"} · ${expense.dateLabel}"
        } else {
            "$payerName paid ${receiver?.sentenceName ?: "someone"} · ${expense.dateLabel}"
        }
    } else {
        "Paid by $payerName · ${expense.dateLabel}"
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val iconBackground = when {
                isPendingSettlement -> Peach
                expense.isSettlement -> Mint
                else -> Peach
            }
            val iconVector = when {
                isPendingSettlement -> Icons.Rounded.Schedule
                expense.isSettlement -> Icons.Rounded.CheckCircle
                else -> Icons.AutoMirrored.Rounded.ReceiptLong
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = Ink
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = mutedColor
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMoney(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isPendingSettlement) FontWeight.Bold else FontWeight.Normal,
                    color = statusColor
                )
            }
        }
    }
}
