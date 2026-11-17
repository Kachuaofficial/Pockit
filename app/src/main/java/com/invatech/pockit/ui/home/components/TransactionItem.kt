package com.invatech.pockit.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.common.motion.rememberPressScale
import com.invatech.pockit.ui.home.model.Transaction
import com.invatech.pockit.ui.theme.CocoaEspresso
import com.invatech.pockit.ui.theme.CocoaMuted
import java.text.NumberFormat
import java.util.Locale

/**
 * Pastel pill transaction card with custom kawaii doodle avatar matching RecordDoodleUi.
 */
@Composable
fun TransactionItem(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val doodleCategory = remember(transaction.category) {
        transaction.category.toDoodleCategory()
    }

    val formattedAmount = remember(transaction.amount, transaction.isExpense) {
        val whole = transaction.amount.toLong()
        val formatted = NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN")).format(whole)
        if (transaction.isExpense) "−$formatted" else "+$formatted"
    }

    val displayTime = remember(transaction.timeLabel, transaction.dateLabel) {
        transaction.timeLabel.ifBlank { transaction.dateLabel }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressScale by rememberPressScale(interactionSource, pressedScale = 0.96f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = if (onClick != null) pressScale else 1f
                scaleY = if (onClick != null) pressScale else 1f
            }
            .clip(RoundedCornerShape(20.dp))
            .background(doodleCategory.cardColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryDoodleAvatar(
            category = doodleCategory,
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = transaction.title.ifBlank { transaction.category.label },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = CocoaEspresso
            )
            Text(
                text = displayTime,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = CocoaMuted
            )
        }

        Text(
            text = formattedAmount,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (transaction.isExpense) CocoaEspresso else Color(0xFF3E9B5A)
        )
    }
}
