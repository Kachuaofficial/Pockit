package com.invatech.pockit.ui.home.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.data.walkthrough.WalkthroughManager
import com.invatech.pockit.ui.common.mascot.MascotPose
import com.invatech.pockit.ui.common.mascot.PockitMascot
import com.invatech.pockit.ui.home.HomeTransactionUi
import com.invatech.pockit.ui.home.model.Category
import com.invatech.pockit.ui.home.model.DaySpending
import com.invatech.pockit.ui.home.model.WalletTransactionType
import com.invatech.pockit.ui.theme.CocoaEspresso
import com.invatech.pockit.ui.theme.CocoaMuted
import com.invatech.pockit.ui.theme.PastelPeach
import com.invatech.pockit.ui.theme.PastelPink
import com.invatech.pockit.ui.theme.WarmBorder
import com.invatech.pockit.ui.theme.WarmCreamMuted
import com.invatech.pockit.ui.theme.WarmCreamSurface
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AnalyticsPeriod(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    ALL("All Time")
}

enum class AnalyticsViewTab(val label: String, val icon: String) {
    TRENDS("Trends", "📊"),
    CATEGORIES("Categories", "🏷️"),
    INSIGHTS("Insights", "💡")
}

data class CategorySpendItem(
    val category: Category,
    val totalAmount: Double,
    val count: Int,
    val percentage: Float
)

data class AnalyticsBarItem(
    val label: String,
    val amount: Float,
    val isPeak: Boolean = false
)

private val MaxBarHeight = 90.dp
private val BarWidth = 22.dp

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Dedicated Spending Trends & Deep Analytics Suite.
 * Placed in AllTransactionsScreen for comprehensive period-by-period financial analysis.
 */
@Composable
fun SpendingTrendsCard(
    transactions: List<HomeTransactionUi>,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(AnalyticsPeriod.WEEK) }
    var currentTab by remember { mutableStateOf(AnalyticsViewTab.TRENDS) }

    val now = remember { System.currentTimeMillis() }
    val dayMillis = 86_400_000L

    val periodTransactions = remember(transactions, selectedPeriod) {
        when (selectedPeriod) {
            AnalyticsPeriod.WEEK -> {
                val cutoff = now - (7 * dayMillis)
                transactions.filter { (it.createdAt?.toDate()?.time ?: now) >= cutoff }
            }
            AnalyticsPeriod.MONTH -> {
                val cutoff = now - (30 * dayMillis)
                transactions.filter { (it.createdAt?.toDate()?.time ?: now) >= cutoff }
            }
            AnalyticsPeriod.ALL -> transactions
        }
    }

    val periodExpenses = remember(periodTransactions) {
        periodTransactions.filter { it.type == WalletTransactionType.EXPENSE }
    }

    val periodIncome = remember(periodTransactions) {
        periodTransactions.filter { it.type == WalletTransactionType.INCOME }
            .sumOf { it.amountPaise / 100.0 }
    }

    val totalSpent = remember(periodExpenses) {
        periodExpenses.sumOf { it.amountPaise / 100.0 }
    }

    val periodDays = remember(selectedPeriod) {
        when (selectedPeriod) {
            AnalyticsPeriod.WEEK -> 7
            AnalyticsPeriod.MONTH -> 30
            AnalyticsPeriod.ALL -> 60
        }
    }

    val dailyAverage = remember(totalSpent, periodDays) {
        if (periodDays > 0) totalSpent / periodDays else 0.0
    }

    // Bar data generation
    val barItems = remember(periodExpenses, selectedPeriod) {
        when (selectedPeriod) {
            AnalyticsPeriod.WEEK -> {
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val expenseMap = mutableMapOf<String, Float>()
                periodExpenses.forEach { item ->
                    item.createdAt?.toDate()?.let { date ->
                        val key = keyFormat.format(date)
                        expenseMap[key] = (expenseMap[key] ?: 0f) + (item.amountPaise / 100f)
                    }
                }
                val days = (6 downTo 0).map { daysAgo ->
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
                    val key = keyFormat.format(cal.time)
                    val label = dayFormat.format(cal.time)
                    val amt = expenseMap[key] ?: 0f
                    AnalyticsBarItem(label = label, amount = amt)
                }
                val maxVal = days.maxOfOrNull { it.amount } ?: 0f
                days.map { it.copy(isPeak = maxVal > 0f && it.amount == maxVal) }
            }
            AnalyticsPeriod.MONTH -> {
                val weekBuckets = Array(4) { 0f }
                periodExpenses.forEach { tx ->
                    val txTime = tx.createdAt?.toDate()?.time ?: now
                    val daysAgo = ((now - txTime) / dayMillis).toInt().coerceIn(0, 29)
                    val weekIndex = 3 - (daysAgo / 7).coerceIn(0, 3)
                    weekBuckets[weekIndex] += (tx.amountPaise / 100f)
                }
                val maxVal = weekBuckets.maxOrNull() ?: 0f
                weekBuckets.mapIndexed { idx, amt ->
                    AnalyticsBarItem(
                        label = "W${idx + 1}",
                        amount = amt,
                        isPeak = maxVal > 0f && amt == maxVal
                    )
                }
            }
            AnalyticsPeriod.ALL -> {
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                val monthMap = mutableMapOf<String, Float>()
                val monthsOrder = (4 downTo 0).map { monthsAgo ->
                    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -monthsAgo) }
                    val label = monthFormat.format(cal.time)
                    monthMap[label] = 0f
                    label
                }
                periodExpenses.forEach { tx ->
                    tx.createdAt?.toDate()?.let { d ->
                        val mLabel = monthFormat.format(d)
                        if (monthMap.containsKey(mLabel)) {
                            monthMap[mLabel] = (monthMap[mLabel] ?: 0f) + (tx.amountPaise / 100f)
                        }
                    }
                }
                val maxVal = monthMap.values.maxOrNull() ?: 0f
                monthsOrder.map { mLabel ->
                    val amt = monthMap[mLabel] ?: 0f
                    AnalyticsBarItem(label = mLabel, amount = amt, isPeak = maxVal > 0f && amt == maxVal)
                }
            }
        }
    }

    val maxBarAmount = remember(barItems) {
        (barItems.maxOfOrNull { it.amount } ?: 1f).coerceAtLeast(1f)
    }

    var selectedBarIndex by remember(barItems) {
        val peakIdx = barItems.indexOfFirst { it.isPeak }
        mutableIntStateOf(if (peakIdx >= 0) peakIdx else barItems.lastIndex)
    }

    val formattedSpent = remember(totalSpent) {
        NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN")).format(totalSpent.toLong())
    }

    // Category breakdown sorted descending
    val categoryBreakdowns = remember(periodExpenses, totalSpent) {
        val catMap = mutableMapOf<Category, Pair<Double, Int>>()
        periodExpenses.forEach { tx ->
            val cat = Category.entries.firstOrNull {
                it.name.equals(tx.category, ignoreCase = true)
            } ?: Category.Food
            val amt = tx.amountPaise / 100.0
            val current = catMap[cat] ?: Pair(0.0, 0)
            catMap[cat] = Pair(current.first + amt, current.second + 1)
        }

        catMap.map { (cat, stats) ->
            val pct = if (totalSpent > 0.0) ((stats.first / totalSpent) * 100.0).toFloat() else 0f
            CategorySpendItem(
                category = cat,
                totalAmount = stats.first,
                count = stats.second,
                percentage = pct
            )
        }.sortedByDescending { it.totalAmount }
    }

    val topCategory = categoryBreakdowns.firstOrNull()

    val smartAdvice = when {
        totalSpent <= 0.0 -> "No spending recorded this period! Ready to track whenever you spend 🐾"
        topCategory != null && topCategory.percentage >= 40f ->
            "${topCategory.category.label} takes up ${topCategory.percentage.toInt()}% of your budget. Consider budgeting a little extra here! 🍱"
        periodIncome > 0.0 && totalSpent <= periodIncome * 0.5 ->
            "Superb discipline! You saved more than 50% of your earnings. Keep growing your wealth ✨"
        else -> "Consistent tracking is the #1 habit for financial freedom. You're doing great! 🐾"
    }

    Surface(
        color = WarmCreamSurface,
        border = BorderStroke(1.dp, WarmBorder),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Title & Period Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Analytics & Insights",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CocoaEspresso
                    )
                    Text(text = "✨", fontSize = 14.sp)
                }

                // Period Selector (Week / Month / All)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(WarmCreamMuted.copy(alpha = 0.5f))
                        .padding(3.dp)
                ) {
                    AnalyticsPeriod.entries.forEach { period ->
                        val isSelected = period == selectedPeriod
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) PastelPeach else Color.Transparent)
                                .clickable { selectedPeriod = period }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = period.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = CocoaEspresso
                            )
                        }
                    }
                }
            }

            // Sub-nav Tabs: Trends 📊 | Categories 🏷️ | Insights 💡
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(WarmCreamMuted.copy(alpha = 0.35f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AnalyticsViewTab.entries.forEach { tab ->
                    val isTabSelected = tab == currentTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isTabSelected) WarmCreamSurface else Color.Transparent)
                            .clickable { currentTab = tab }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = tab.icon, fontSize = 12.sp)
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isTabSelected) CocoaEspresso else CocoaMuted
                            )
                        }
                    }
                }
            }

            // Tab Content
            when (currentTab) {
                AnalyticsViewTab.TRENDS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Spent Headline & Average Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = when (selectedPeriod) {
                                        AnalyticsPeriod.WEEK -> "Spent this week"
                                        AnalyticsPeriod.MONTH -> "Spent this month"
                                        AnalyticsPeriod.ALL -> "Total spent"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CocoaMuted
                                )
                                Text(
                                    text = "₹$formattedSpent",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = CocoaEspresso
                                )
                            }

                            if (dailyAverage > 0.0) {
                                Surface(
                                    color = PastelPeach.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "₹${dailyAverage.toInt()}/day avg",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = CocoaEspresso,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Refined Bar Chart
                        AnalyticsTrendsChart(
                            barItems = barItems,
                            maxAmount = maxBarAmount,
                            selectedIndex = selectedBarIndex,
                            onSelectIndex = { selectedBarIndex = it }
                        )
                    }
                }

                AnalyticsViewTab.CATEGORIES -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Category Breakdown (${categoryBreakdowns.size})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = CocoaEspresso
                            )
                            Text(
                                text = "Total: ₹$formattedSpent",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = CocoaMuted
                            )
                        }

                        AnalyticsCategoriesBreakdown(items = categoryBreakdowns)
                    }
                }

                AnalyticsViewTab.INSIGHTS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Savings Rate Meter
                        if (periodIncome > 0.0) {
                            val savings = (periodIncome - totalSpent).coerceAtLeast(0.0)
                            val savingsRate = ((savings / periodIncome) * 100.0).toInt().coerceIn(0, 100)

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Savings Rate",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CocoaEspresso
                                    )
                                    Text(
                                        text = "$savingsRate% saved",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(WarmCreamMuted)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(savingsRate / 100f)
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
                                }
                            }
                        }

                        // Mascot Smart Advice
                        MascotInsightBubble(text = smartAdvice)
                    }
                }
            }
        }
    }
}

/**
 * Main Analytics Entry Point on Home Screen.
 * Renders Advanced Deep Analytics (Gated behind Rewarded Video Ad with 24-hr unlock):
 * - Category Distribution Breakdown
 * - Financial Health & Savings Meter
 * - Smart AI Mascot Advice
 */
@Composable
fun SpendingAnalyticsCard(
    transactions: List<HomeTransactionUi>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val expenses = remember(transactions) {
        transactions.filter { it.type == WalletTransactionType.EXPENSE }
    }

    val totalSpent = remember(expenses) {
        expenses.sumOf { it.amountPaise / 100.0 }
    }

    val totalIncome = remember(transactions) {
        transactions.filter { it.type == WalletTransactionType.INCOME }
            .sumOf { it.amountPaise / 100.0 }
    }

    // Category breakdown sorted descending
    val categoryBreakdowns = remember(expenses, totalSpent) {
        val catMap = mutableMapOf<Category, Pair<Double, Int>>()
        expenses.forEach { tx ->
            val cat = Category.entries.firstOrNull {
                it.name.equals(tx.category, ignoreCase = true)
            } ?: Category.Food
            val amt = tx.amountPaise / 100.0
            val current = catMap[cat] ?: Pair(0.0, 0)
            catMap[cat] = Pair(current.first + amt, current.second + 1)
        }

        catMap.map { (cat, stats) ->
            val pct = if (totalSpent > 0.0) ((stats.first / totalSpent) * 100.0).toFloat() else 0f
            CategorySpendItem(
                category = cat,
                totalAmount = stats.first,
                count = stats.second,
                percentage = pct
            )
        }.sortedByDescending { it.totalAmount }
    }

    val topCategory = categoryBreakdowns.firstOrNull()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ─────────────────────────────────────────────────────────────
        // CARD 2: Advanced Deep Analytics (Gated behind Rewarded Ad)
        // ─────────────────────────────────────────────────────────────
        run {
            // UNLOCKED STATE: Category Distribution Card


            Surface(
                color = WarmCreamSurface,
                border = BorderStroke(1.dp, WarmBorder),
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Category Distribution",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = CocoaEspresso
                            )
                            Text(text = "🏷️", fontSize = 14.sp)
                        }

                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Insights",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    AnalyticsCategoriesBreakdown(items = categoryBreakdowns)
                }
            }

            // UNLOCKED STATE: Financial Health & Smart Advice Card
            Surface(
                color = WarmCreamSurface,
                border = BorderStroke(1.dp, WarmBorder),
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Financial Health & Advice",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = CocoaEspresso
                        )
                        Text(text = "✨", fontSize = 14.sp)
                    }

                    // Savings Meter
                    if (totalIncome > 0.0) {
                        val savings = (totalIncome - totalSpent).coerceAtLeast(0.0)
                        val savingsRate = ((savings / totalIncome) * 100.0).toInt().coerceIn(0, 100)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Savings Rate",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CocoaEspresso
                                )
                                Text(
                                    text = "$savingsRate% saved",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(WarmCreamMuted)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(savingsRate / 100f)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                            }
                        }
                    }

                    val smartAdvice = when {
                        totalSpent <= 0.0 -> "No spending recorded this period! Ready to track whenever you spend 🐾"
                        topCategory != null && topCategory.percentage >= 40f ->
                            "${topCategory.category.label} takes up ${topCategory.percentage.toInt()}% of your budget. Consider budgeting a little extra here! 🍱"
                        totalIncome > 0.0 && totalSpent <= totalIncome * 0.5 ->
                            "Superb discipline! You saved more than 50% of your earnings. Keep growing your wealth ✨"
                        else -> "Consistent tracking is the #1 habit for financial freedom. You're doing great! 🐾"
                    }

                    MascotInsightBubble(text = smartAdvice)
                }
            }
        }
    }
}

/**
 * Backward compatibility wrapper for SpendingChartCard.
 */
@Composable
fun SpendingChartCard(
    data: List<DaySpending>,
    modifier: Modifier = Modifier
) {
    val barItems = remember(data) {
        val maxVal = data.maxOfOrNull { it.amount } ?: 0f
        data.map {
            AnalyticsBarItem(
                label = it.label,
                amount = it.amount,
                isPeak = maxVal > 0f && it.amount == maxVal
            )
        }
    }

    val maxAmount = remember(data) {
        (data.maxOfOrNull { it.amount } ?: 1f).coerceAtLeast(1f)
    }

    var selectedIndex by remember(data) {
        val peakIdx = barItems.indexOfFirst { it.isPeak }
        mutableIntStateOf(if (peakIdx >= 0) peakIdx else barItems.lastIndex)
    }

    Surface(
        color = WarmCreamSurface,
        border = BorderStroke(1.dp, WarmBorder),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent this week",
                        style = MaterialTheme.typography.labelMedium,
                        color = CocoaMuted
                    )
                    Text(
                        text = "₹${data.sumOf { it.amount.toDouble() }.toInt()}",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = CocoaEspresso
                    )
                }
                Surface(
                    color = PastelPeach,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Last 7 days",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CocoaEspresso,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            AnalyticsTrendsChart(
                barItems = barItems,
                maxAmount = maxAmount,
                selectedIndex = selectedIndex,
                onSelectIndex = { selectedIndex = it }
            )
        }
    }
}

@Composable
private fun AnalyticsTrendsChart(
    barItems: List<AnalyticsBarItem>,
    maxAmount: Float,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    if (barItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PockitMascot(
                    pose = MascotPose.HAPPY_WAVING,
                    size = 42.dp
                )
                Text(
                    text = "No spending recorded in this period 🐾",
                    style = MaterialTheme.typography.bodySmall,
                    color = CocoaMuted
                )
            }
        }
        return
    }

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(165.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        barItems.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val hasSpending = item.amount > 0f

            val targetHeight = if (!hasSpending) {
                8.dp
            } else if (appeared) {
                (MaxBarHeight * (item.amount / maxAmount)).coerceIn(16.dp, MaxBarHeight)
            } else {
                8.dp
            }

            val barHeight by animateDpAsState(
                targetValue = targetHeight,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "analytics_bar_h_$index"
            )

            val barColor by animateColorAsState(
                targetValue = when {
                    isSelected && hasSpending -> CocoaEspresso
                    item.isPeak && hasSpending -> Color(0xFFFFA726)
                    hasSpending -> PastelPeach
                    else -> WarmCreamMuted
                },
                label = "analytics_bar_c_$index"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onSelectIndex(index)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Tooltip badge above bar
                Column(
                    modifier = Modifier.height(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedVisibility(
                        visible = isSelected && hasSpending,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            color = CocoaEspresso,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "₹${item.amount.toInt()}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!isSelected && item.isPeak && hasSpending) {
                        Text(
                            text = "★ Peak",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFE65100)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bar pill
                Box(
                    modifier = Modifier
                        .width(BarWidth)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(barColor)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom label
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (isSelected || item.isPeak) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) CocoaEspresso else CocoaMuted
                )
            }
        }
    }
}

@Composable
private fun AnalyticsCategoriesBreakdown(
    items: List<CategorySpendItem>
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No expenses recorded in this period 🐾",
                style = MaterialTheme.typography.bodySmall,
                color = CocoaMuted
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.take(5).forEach { item ->
            val doodleCat = remember(item.category) { item.category.toDoodleCategory() }
            val progress = (item.percentage / 100f).coerceIn(0f, 1f)
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "cat_progress_${item.category.name}"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryDoodleAvatar(
                    category = doodleCat,
                    modifier = Modifier.size(38.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.category.label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = CocoaEspresso
                        )
                        Text(
                            text = "₹${item.totalAmount.toInt()} (${item.percentage.toInt()}%)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CocoaEspresso
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // Progress Track & Fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(WarmCreamMuted)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(item.category.tint)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kawaii mascot thought bubble presenting real-time smart financial insights.
 */
@Composable
private fun MascotInsightBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFFBF4E8),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, WarmBorder.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PockitMascot(
                pose = MascotPose.HAPPY_WAVING,
                size = 38.dp
            )

            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                ),
                color = CocoaEspresso,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
