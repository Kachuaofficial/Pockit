package com.invatech.pockit.ui.home.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.home.model.Transaction
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.InkSoft
import com.invatech.pockit.ui.theme.MintDeep
import java.text.NumberFormat
import java.util.Locale

/** One row inside the dark "Recent" card. */
@Composable
fun TransactionItem(
    transaction: Transaction,
    modifier: Modifier = Modifier
) {
    val amountText = NumberFormat
        .getIntegerInstance(Locale.forLanguageTag("en-IN"))
        .format(transaction.amount.toLong())

    val isLight = MaterialTheme.colorScheme.background == com.invatech.pockit.ui.theme.Cloud
    val itemBg = if (isLight) InkSoft else MaterialTheme.colorScheme.surfaceVariant
    val titleColor = if (isLight) Color.White else MaterialTheme.colorScheme.onSurface
    val expenseColor = if (isLight) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(itemBg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(transaction.category.tint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = transaction.category.icon,
                contentDescription = transaction.category.label,
                tint = Ink,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor
            )
            Text(
                text = transaction.category.label,
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (transaction.isExpense) "-₹" else "+₹") + amountText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.isExpense) expenseColor else MintDeep
            )
            Text(
                text = transaction.dateLabel,
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted
            )
        }
    }
}
