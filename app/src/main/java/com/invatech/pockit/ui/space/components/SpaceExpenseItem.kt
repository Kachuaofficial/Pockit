package com.invatech.pockit.ui.space.components

/*
 * Shared-bill/settlement row with payer, share and pending-state presentation.
 */

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        expense.isSettlement -> "Settled up ✓" to Color(0xFF40D99A)
        expense.paidById == CurrentUserId -> {
            val lent = expense.amount - (myShare ?: 0.0)
            if (lent > 0.005) "you lent ${formatMoney(lent)}" to Color(0xFF40D99A)
            else "just you" to CocoaMuted
        }
        myShare != null -> "you owe ${formatMoney(myShare)}" to Color(0xFFFF9A62)
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

    // Light category-aware surfaces match the transaction and friend-detail card system.
    val (cardBg, avatarBg, iconTint) = remember(expense.isSettlement, expense.paidById, myShare, isPendingSettlement) {
        when {
            isPendingSettlement -> Triple(
                Color(0xFFFFF8F4), Color(0xFFFFE6D1), Color(0xFFE57B24)
            )
            expense.isSettlement -> Triple(
                Color(0xFFF8FFFB), Color(0xFFDDF8EC), Color(0xFF078C62)
            )
            expense.paidById == CurrentUserId -> Triple(
                Color(0xFFF7FFFB), Color(0xFFDDF8EC), Color(0xFF078C62)
            )
            myShare != null -> Triple(
                Color(0xFFFFFAF7), Color(0xFFFFE5D4), Color(0xFFEA7B25)
            )
            else -> Triple(
                Color(0xFFF8FBFF), Color(0xFFE0ECFF), Color(0xFF477CC7)
            )
        }
    }

    val iconVector = when {
        isPendingSettlement -> Icons.Rounded.Schedule
        expense.isSettlement -> Icons.Rounded.CheckCircle
        else -> Icons.AutoMirrored.Rounded.ReceiptLong
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(Color.White, cardBg)))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val back = Path().apply {
                moveTo(size.width * .46f, size.height)
                cubicTo(size.width * .62f, size.height * .62f, size.width * .78f, size.height * .94f, size.width, size.height * .48f)
                lineTo(size.width, size.height)
                close()
            }
            val front = Path().apply {
                moveTo(size.width * .67f, size.height)
                cubicTo(size.width * .80f, size.height * .74f, size.width * .91f, size.height * .98f, size.width, size.height * .68f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(back, iconTint.copy(alpha = .08f))
            drawPath(front, iconTint.copy(alpha = .14f))
            drawCircle(iconTint.copy(alpha = .06f), size.minDimension * .30f, Offset(size.width * .93f, size.height * .14f))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(23.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = CocoaEspresso,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = CocoaMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatMoney(expense.amount),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                ),
                color = iconTint,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = .10f))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = statusColor,
                maxLines = 1
            )
        }
    }
}
}
