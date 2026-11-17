package com.invatech.pockit.ui.space.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.space.model.CurrentUserId
import com.invatech.pockit.ui.space.model.Member
import com.invatech.pockit.ui.space.model.OwedColor
import com.invatech.pockit.ui.space.model.SpaceExpense
import com.invatech.pockit.ui.space.util.formatMoney
import com.invatech.pockit.ui.theme.CocoaEspresso
import com.invatech.pockit.ui.theme.CocoaMuted
import com.invatech.pockit.ui.theme.ErrorRed

/**
 * Colorful kawaii pastel card for space expenses, consistent with Home TransactionItem cards.
 */
@Composable
fun SpaceExpenseItem(
    expense: SpaceExpense,
    members: Map<String, Member>,
    modifier: Modifier = Modifier
) {
    val payer = members[expense.paidById]
    val payerName = payer?.displayName ?: "Someone"
    val myShare = expense.shares[CurrentUserId]
    val isPendingSettlement = expense.isSettlement && !expense.isConfirmed

    val (statusText, statusColor) = when {
        isPendingSettlement -> "Pending confirmation" to ErrorRed
        expense.isSettlement -> "Settled up ✓" to Color(0xFF2E7D42)
        expense.paidById == CurrentUserId -> {
            val lent = expense.amount - (myShare ?: 0.0)
            if (lent > 0.005) "you lent ${formatMoney(lent)}" to Color(0xFF2E7D42)
            else "just you" to CocoaMuted
        }
        myShare != null -> "you owe ${formatMoney(myShare)}" to Color(0xFFB33A24)
        else -> "not involved" to CocoaMuted
    }

    val subtitle = if (expense.isSettlement) {
        val receiver = expense.shares.keys.firstOrNull()?.let { members[it] }
        if (isPendingSettlement) {
            "$payerName sent payment to ${receiver?.sentenceName ?: "someone"} • ${expense.dateLabel}"
        } else {
            "$payerName paid ${receiver?.sentenceName ?: "someone"} • ${expense.dateLabel}"
        }
    } else {
        "Paid by $payerName • ${expense.dateLabel}"
    }

    // Cheerful pastel color schemes consistent with Home TransactionItem & RecordDoodleUi
    val (cardBg, avatarBg, iconTint) = remember(expense.isSettlement, expense.paidById, myShare, isPendingSettlement) {
        when {
            isPendingSettlement -> Triple(
                Color(0xFFFFDDD3), // Soft Coral
                Color(0xFFFFA690),
                Color(0xFF6B2D1E)
            )
            expense.isSettlement -> Triple(
                Color(0xFFEDE6FB), // Soft Lavender
                Color(0xFFC9B8F3),
                Color(0xFF43306B)
            )
            expense.paidById == CurrentUserId -> Triple(
                Color(0xFFDFF5E0), // Soft Mint (You lent)
                Color(0xFF9EDDA5),
                Color(0xFF1E562B)
            )
            myShare != null -> Triple(
                Color(0xFFFFE7D6), // Soft Peach/Apricot (You owe)
                Color(0xFFFFB27A),
                Color(0xFF7A3614)
            )
            else -> Triple(
                Color(0xFFDBEFFB), // Soft Sky
                Color(0xFF93CBF2),
                Color(0xFF1B4E75)
            )
        }
    }

    val iconVector = when {
        isPendingSettlement -> Icons.Rounded.Schedule
        expense.isSettlement -> Icons.Rounded.CheckCircle
        else -> Icons.AutoMirrored.Rounded.ReceiptLong
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = CocoaEspresso
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = CocoaMuted
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatMoney(expense.amount),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = CocoaEspresso
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isPendingSettlement) FontWeight.Bold else FontWeight.Medium
                ),
                color = statusColor
            )
        }
    }
}
