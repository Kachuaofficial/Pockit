package com.invatech.pockit.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.invatech.pockit.data.repository.AuthRepository
import com.invatech.pockit.ui.common.loading.ExpressiveLoadingIndicator
import com.invatech.pockit.ui.home.components.AddWalletTransactionSheet
import com.invatech.pockit.ui.home.components.BalanceSection
import com.invatech.pockit.ui.home.components.HomeHeader
import com.invatech.pockit.ui.home.components.HomeScreenSkeleton
import com.invatech.pockit.ui.home.components.QuickActions
import com.invatech.pockit.ui.home.components.RecentTransactionsCard
import com.invatech.pockit.ui.home.components.SpendingChartCard
import com.invatech.pockit.ui.home.model.Category
import com.invatech.pockit.ui.home.model.DaySpending
import com.invatech.pockit.ui.home.model.Transaction
import com.invatech.pockit.ui.home.model.WalletTransactionType
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    userName: String = "there",
    homeViewModel: HomeViewModel = viewModel(),
    onViewAllTransactions: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()

    // Resolve current user's display name if signed in with Google
    val displayName = remember {
        AuthRepository().currentUser?.displayName?.split(" ")?.firstOrNull() ?: userName
    }

    var showAddSheet by remember { mutableStateOf(false) }
    var sheetInitialType by remember { mutableStateOf(WalletTransactionType.EXPENSE) }

    // Spending Weekly Chart (hidden with AnimatedVisibility if empty)
    val spendingData = remember(uiState.transactions) {
        val expenses = uiState.transactions.filter {
            it.type == WalletTransactionType.EXPENSE
        }

        if (expenses.isEmpty()) {
            emptyList<DaySpending>()
        } else {
            val dayFormat = java.text.SimpleDateFormat("EEE", Locale.getDefault())
            val keyFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val expenseMap = mutableMapOf<String, Float>()
            expenses.forEach { item ->
                item.createdAt?.toDate()?.let { date ->
                    val key = keyFormat.format(date)
                    expenseMap[key] = (expenseMap[key] ?: 0f) + (item.amountPaise / 100f)
                }
            }

            // Generate exactly 7 consecutive days ending today
            val days = (6 downTo 0).map { daysAgo ->
                val cal = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
                }
                val date = cal.time
                val key = keyFormat.format(date)
                val dayLabel = dayFormat.format(date)
                val amount = expenseMap[key] ?: 0f
                DaySpending(label = dayLabel, amount = amount)
            }

            if (days.sumOf { it.amount.toDouble() } <= 0.0) {
                emptyList()
            } else {
                days
            }
        }
    }

    // Recent Transactions list (Strictly latest 5)
    val recentTransactions = remember(uiState.transactions) {
        uiState.transactions
            .take(5)
            .map { item ->
                Transaction(
                    id = item.id.hashCode().toLong(),
                    title = item.title,
                    category = Category.entries.firstOrNull {
                        it.name.equals(item.category, ignoreCase = true)
                    } ?: Category.Food,
                    amount = item.amountPaise / 100.0,
                    isExpense = item.type == WalletTransactionType.EXPENSE,
                    dateLabel = item.createdAt?.toDate()?.let {
                        java.text.SimpleDateFormat("dd MMM", Locale.getDefault()).format(it)
                    } ?: "Just now"
                )
            }
    }

    // Smooth transition from Material 3 Expressive Skeleton to Content
    Crossfade(
        targetState = uiState.isInitialLoad,
        animationSpec = tween(durationMillis = 350),
        label = "home_skeleton_crossfade"
    ) { isInitialLoading ->
        if (isInitialLoading) {
            HomeScreenSkeleton(modifier = modifier)
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 12.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    HomeHeader(userName = displayName)
                }

                item {
                    BalanceSection(
                        balance = uiState.balancePaise / 100.0,
                        changePercent = 0.0
                    )
                }

                item {
                    QuickActions(
                        onAddExpense = {
                            sheetInitialType = WalletTransactionType.EXPENSE
                            showAddSheet = true
                        },
                        onAddIncome = {
                            sheetInitialType = WalletTransactionType.INCOME
                            showAddSheet = true
                        }
                    )
                }

                // Expressive sync badge when background refreshing occurs
                if (uiState.isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExpressiveLoadingIndicator(
                                size = 24.dp,
                                showHalo = false
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Updating wallet...",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.errorMessage?.let { message ->
                    item {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Spending Weekly Chart (hidden if empty)
                item {
                    AnimatedVisibility(
                        visible = spendingData.isNotEmpty(),
                        enter = fadeIn(tween(350)) + expandVertically(tween(400)),
                        exit = fadeOut(tween(250)) + shrinkVertically(tween(300))
                    ) {
                        SpendingChartCard(data = spendingData)
                    }
                }

                // Recent Transactions Card (Shows strictly 5 items with "See all")
                item {
                    RecentTransactionsCard(
                        transactions = recentTransactions,
                        onViewAll = onViewAllTransactions
                    )
                }
            }
        }
    }

    // Modern Material 3 Modal Bottom Sheet for Adding Transactions
    if (showAddSheet) {
        AddWalletTransactionSheet(
            initialType = sheetInitialType,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onDismiss = {
                showAddSheet = false
                homeViewModel.clearError()
            },
            onSave = { title, amountPaise, type, category ->
                if (type == WalletTransactionType.INCOME) {
                    homeViewModel.addIncome(
                        title = title,
                        amountPaise = amountPaise,
                        category = category
                    )
                } else {
                    homeViewModel.addExpense(
                        title = title,
                        amountPaise = amountPaise,
                        category = category
                    )
                }
                showAddSheet = false
            }
        )
    }
}
