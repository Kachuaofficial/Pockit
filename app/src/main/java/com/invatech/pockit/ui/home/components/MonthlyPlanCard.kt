package com.invatech.pockit.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.common.motion.MotionSettings
import com.invatech.pockit.ui.finance.FinanceCard
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun MonthlyPlanCard(
    color: Color,
    hasBudget: Boolean,
    spentText: String,
    limitText: String,
    usedFraction: Float,
    percentUsed: Int,
    amountLeftText: String,
    isOver: Boolean,
    onNavigateToBudgets: () -> Unit,
    onPlanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = if (color.luminance() > .5f) Color(0xFF153D35) else Color.White
    val muted = ink.copy(alpha = .76f)
    val status = when {
        isOver -> Color(0xFFA52D32)
        usedFraction >= .8f -> Color(0xFF805000)
        else -> ink
    }
    val locale = LocalConfiguration.current.locales[0]
    val month = SimpleDateFormat("MMMM yyyy", locale).format(Date())
    val motion by MotionSettings.enabled.collectAsState()
    val progress by animateFloatAsState(usedFraction.coerceIn(0f, 1f),
        animationSpec = if (motion) tween(450) else snap(), label = "monthly_budget_progress")

    FinanceCard(modifier, color) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Monthly plan", style = MaterialTheme.typography.titleLarge, color = ink)
            Text(month, style = MaterialTheme.typography.bodySmall, color = muted)
        }
        if (hasBudget) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (isOver) "Over your budget" else "Left to spend", style = MaterialTheme.typography.labelLarge, color = status)
                Text(amountLeftText, style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold, color = status)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = status, trackColor = ink.copy(alpha = .12f))
                // Separate lines keep large balances and enlarged text readable.
                Text("$spentText spent · $limitText budget", style = MaterialTheme.typography.bodyMedium, color = ink)
                Text(when {
                    isOver -> "${percentUsed.coerceAtLeast(0)}% used. Review your limit or recent expenses."
                    usedFraction >= 1f -> "You've reached this month's limit."
                    usedFraction >= .8f -> "${percentUsed.coerceAtLeast(0)}% used. You're getting close to your limit."
                    else -> "${percentUsed.coerceAtLeast(0)}% of your monthly budget used"
                }, style = MaterialTheme.typography.bodySmall, color = muted)
            }
        } else {
            Text("Make your money last the month.", style = MaterialTheme.typography.headlineSmall, color = ink)
            Text("Set a monthly spending limit. See what's left as you add expenses.",
                style = MaterialTheme.typography.bodyMedium, color = muted)
        }
        Button(onClick = onNavigateToBudgets, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ink, contentColor = color),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) {
            Text(if (hasBudget) "View budget" else "Set monthly budget", modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp))
        }
        TextButton(onClick = onPlanner, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = ink)) {
            Text("Plan your pocket money", style = MaterialTheme.typography.labelLarge)
        }
    }
}
