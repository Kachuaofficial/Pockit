package com.invatech.pockit.ui.home

/*
 * Wallet dashboard and coordinator for transaction entry, edit, balance adjustment and deletion dialogs.
 * Transforms wallet state into monthly/daily summaries; repository operations remain outside the layout.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.invatech.pockit.data.repository.BudgetRepository
import com.invatech.pockit.domain.Money
import com.invatech.pockit.ui.common.ads.PockitBannerAd
import com.invatech.pockit.ui.finance.AmountField
import com.invatech.pockit.ui.finance.ErrorNotice
import com.invatech.pockit.ui.finance.FinanceCard
import com.invatech.pockit.ui.finance.FinanceViewModel
import com.invatech.pockit.ui.home.components.AddWalletTransactionSheet
import com.invatech.pockit.ui.home.components.ExpressiveWalletTransactionCard
import com.invatech.pockit.ui.home.components.MonthlyPlanCard
import com.invatech.pockit.ui.home.model.WalletTransactionType
import com.invatech.pockit.ui.theme.PockitCharcoal
import com.invatech.pockit.ui.theme.PockitCoral
import com.invatech.pockit.ui.theme.PockitLavender
import com.invatech.pockit.ui.theme.PockitTeal
import com.invatech.pockit.ui.theme.PockitYellow
import java.util.Calendar
import com.invatech.pockit.ui.home.components.*
import com.invatech.pockit.ui.theme.*
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Renders the shared HomeViewModel state and owns the open/closed state of entry and confirmation
 * dialogs. Computes display summaries from the supplied transaction history; callbacks delegate
 * monetary mutations to the ViewModel/repository.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    userId: String? = null,
    userName: String = "there",
    homeViewModel: HomeViewModel = viewModel(),
    unreadNotificationCount: Int = 0,
    openAddSheetOnLaunch: Boolean = false,
    onAddSheetLaunched: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onViewAllTransactions: () -> Unit = {},
    onCoinShopClick: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onSettings: () -> Unit = {},
    onSpaces: () -> Unit = {},
    onGoals: () -> Unit = {},
    onReview: () -> Unit = {},
    onPlanner: () -> Unit = {},
    onReports: () -> Unit = {},
    onFriends: () -> Unit = {}
) {
    var txnSaving by remember { mutableStateOf(false) }
    var editBalance by remember { mutableStateOf(false) }
    var balanceInput by remember { mutableStateOf("") }
    var editingTransaction by remember { mutableStateOf<HomeTransactionUi?>(null) }
    var deletingTransaction by remember { mutableStateOf<HomeTransactionUi?>(null) }
    val state by homeViewModel.uiState.collectAsState()
    val online = com.invatech.pockit.ui.common.rememberOnline()
    LaunchedEffect(txnSaving, state.isLoading) {
        if (txnSaving && !state.isLoading) {
            txnSaving = false; if (state.errorMessage == null) editingTransaction = null
        }
    }

    val finance: FinanceViewModel = viewModel()
    val pending by finance.pending.collectAsState()
    val financeError by finance.error.collectAsState()
    val budgetRepo = remember { BudgetRepository() }
    val budgetSyncError by budgetRepo.syncError.collectAsState()
    val budgets by remember { budgetRepo.observeBudgetProgress() }.collectAsState(emptyList())
    var add by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(WalletTransactionType.EXPENSE) }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(openAddSheetOnLaunch) {
        if (openAddSheetOnLaunch) {
            add = true; onAddSheetLaunched()
        }
    }
    LaunchedEffect(state.isLoading, saving) {
        if (saving && !state.isLoading) {
            saving = false; if (state.errorMessage == null) add = false
        }
    }
    var previousMonth by rememberSaveable { mutableStateOf(false) }
    // Bound both ends so a selected month never includes next-month/future rows.
    val monthCalendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        if (previousMonth) add(Calendar.MONTH, -1)
    }
    val monthStart = monthCalendar.timeInMillis
    val monthEnd = (monthCalendar.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.timeInMillis
    val monthRows = remember(state.transactions, monthStart, monthEnd) {
        state.transactions.filter { (it.createdAt?.toDate()?.time ?: 0L) in monthStart until monthEnd }
    }
    val days = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val trend = remember(monthRows, monthStart) {
        val daily = LongArray(days)
        monthRows.filter { it.type == WalletTransactionType.EXPENSE }.forEach { tx ->
            val day = Calendar.getInstance().apply { timeInMillis = tx.createdAt!!.toDate().time }.get(Calendar.DAY_OF_MONTH) - 1
            daily[day] += tx.amountPaise
        }
        var cumulative = 0L
        listOf(0L) + daily.map { cumulative += it; cumulative }
    }
    val income = Money.sum(monthRows.filter { it.type == WalletTransactionType.INCOME }
        .map { it.amountPaise })
    val expense = Money.sum(monthRows.filter { it.type == WalletTransactionType.EXPENSE }
        .map { it.amountPaise })
    val adListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val nativeSlotNearViewport by remember {
        derivedStateOf {
            val info = adListState.layoutInfo
            info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index
                ?: -1) >= info.totalItemsCount - 3
        }
    }
    if (nativeSlotNearViewport && state.transactions.size >= 3) com.invatech.pockit.ui.common.ads.PreloadPockitNativeAd("home.native")
    LazyColumn(
        modifier.fillMaxSize(),
        state = adListState,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val overall = budgets.firstOrNull { it.category == "Overall" }
            Column {
                DashboardGreeting(
                    userName = userName,
                    unread = unreadNotificationCount,
                    onNotifications = onNotificationClick,
                    onSettings = onSettings,
                    onRewards = onCoinShopClick,
                    userId = userId
                )
                Box(Modifier.fillMaxWidth().padding(top = 82.dp)) {
                    SpendingSummary(
                        expense,
                        if (previousMonth) null else overall?.limitPaise,
                        trend,
                        java.text.SimpleDateFormat("MMM yyyy", androidx.compose.ui.platform.LocalConfiguration.current.locales[0]).format(monthCalendar.time),
                        previousMonth,
                        { previousMonth = it },
                        onNavigateToBudgets
                    )
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(com.invatech.pockit.R.drawable.pockit_panda_mascot),
                        contentDescription = "Pockit panda mascot",
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-110).dp)
                            .fillMaxWidth(.72f)
                            .aspectRatio(1672f / 941f)
                            .zIndex(1f)
                    )
                }
            }
        }
        item {
            DashboardActions(
                onExpense = { homeViewModel.clearError(); type = WalletTransactionType.EXPENSE; add = true },
                onIncome = { homeViewModel.clearError(); type = WalletTransactionType.INCOME; add = true },
                onReports = onReports, onSettings = onSettings)
        }
        item {
            CinematicCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Recorded balance", color = PockitSecondaryText, style = MaterialTheme.typography.bodySmall)
                        Text(Money.format(state.balancePaise), style = MaterialTheme.typography.headlineMedium)
                    }
                    TextButton(onClick = {
                        homeViewModel.clearError(); balanceInput = Money.decimal(state.balancePaise); editBalance = true
                    }, enabled = !state.isLoading) { Text("Edit balance") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${if (previousMonth) "Last" else "This"} month's income", style = MaterialTheme.typography.bodySmall, color = PockitSecondaryText)
                    Text(Money.format(income), style = MaterialTheme.typography.titleSmall, color = PockitGreen)
                }
            }
        }
        item { CategorySpending(monthRows, onReports) }
        if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item {
            ErrorNotice(state.errorMessage); ErrorNotice(budgetSyncError); ErrorNotice(financeError)
            if (online && financeError != null) TextButton(onClick = finance::retrySync) { Text("Retry account sync") }
        }
        if (online && state.errorMessage != null) item {
            TextButton(onClick = homeViewModel::loadWallet) {
                Text(
                    "Retry sync"
                )
            }
        }
        if (pending.isNotEmpty()) item {
            FinanceCard {
                Text(
                    "${pending.size} payments to review",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("Check detected UPI entries before adding them.")
                Button(onClick = onReview) { Text("Review payments") }
            }
        }
        item {
            val overall = budgets.firstOrNull { it.category == "Overall" }
            MonthlyPlanCard(
                color = PockitTeal,
                hasBudget = overall != null,
                spentText = overall?.let { Money.format(it.spentPaise) }.orEmpty(),
                limitText = overall?.let { Money.format(it.limitPaise) }.orEmpty(),
                usedFraction = overall?.let { it.percentageUsed / 100f } ?: 0f,
                percentUsed = overall?.percentageUsed?.toInt() ?: 0,
                amountLeftText = overall?.let {
                    Money.format(if (it.remainingPaise >= 0) it.remainingPaise else -it.remainingPaise)
                }.orEmpty(),
                isOver = overall != null && overall.remainingPaise < 0,
                onNavigateToBudgets = onNavigateToBudgets,
                onPlanner = onPlanner
            )
        }
        item { Text("Your money toolkit", style = MaterialTheme.typography.titleLarge) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeTool(
                    "Savings goals",
                    "Small steps, big plans",
                    PockitLavender,
                    Modifier.weight(1f),
                    onGoals
                )
                HomeTool(
                    "Budgets",
                    "Set spending limits",
                    PockitYellow,
                    Modifier.weight(1f),
                    onNavigateToBudgets
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeTool(
                    "Friends & Udhar", "Keep balances clear", PockitTeal, Modifier.weight(1f), onFriends
                )
                HomeTool(
                    "Group expenses",
                    "Share the good times",
                    PockitCoral.copy(alpha = .5f),
                    Modifier.weight(1f),
                    onSpaces
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeTool("UPI inbox", "Review & add payments", PockitSurface, Modifier.weight(1f), onReview)
                HomeTool("Pocket planner", "Plan your allowance", PockitSurface, Modifier.weight(1f), onPlanner)
            }
        }
        item {
            com.invatech.pockit.ui.common.ads.PockitNativeAdCard(
                placementKey = "home.toolkit.native", modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        item {
            val now = System.currentTimeMillis()
            val week = state.transactions.filter {
                it.type == WalletTransactionType.EXPENSE && (it.createdAt?.toDate()?.time
                    ?: 0L) in (now - 7 * 86400000L)..now
            }
            val previous = state.transactions.filter {
                it.type == WalletTransactionType.EXPENSE && (it.createdAt?.toDate()?.time
                    ?: 0L) >= now - 14 * 86400000L && (it.createdAt?.toDate()?.time
                    ?: 0L) < now - 7 * 86400000L
            }
            val currentTotal = Money.sum(week.map { it.amountPaise })
            val difference = currentTotal - Money.sum(previous.map { it.amountPaise })

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = PockitSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, com.invatech.pockit.ui.theme.WarmBorder
                ),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your week in money",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold, fontSize = 16.sp
                            ),
                            color = com.invatech.pockit.ui.theme.CocoaEspresso
                        )

                        val isMore = difference >= 0
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isMore) ExpenseRedContainer else IncomeGreenContainer,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, if (isMore) ExpenseRed.copy(alpha = .25f) else IncomeGreen.copy(alpha = .25f)
                            )
                        ) {
                            Text(
                                text = if (difference == 0L) "On par" else "${
                                    Money.format(
                                        kotlin.math.abs(
                                            difference
                                        )
                                    )
                                } ${if (isMore) "more" else "less"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isMore) com.invatech.pockit.ui.theme.ExpenseRed else com.invatech.pockit.ui.theme.IncomeGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = Money.format(currentTotal),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold, fontSize = 24.sp
                            ),
                            color = com.invatech.pockit.ui.theme.CocoaEspresso
                        )
                        Text(
                            text = "spent in the last 7 days",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.invatech.pockit.ui.theme.CocoaMuted
                        )
                    }

                    val topCategories = week.groupBy { it.category }
                        .mapValues { (_, rows) -> Money.sum(rows.map { it.amountPaise }) }.toList()
                        .sortedByDescending { it.second }.take(3)

                    if (topCategories.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            topCategories.forEach { (category, amount) ->
                                val progress =
                                    (amount.toDouble() / currentTotal.coerceAtLeast(1)).toFloat()
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = com.invatech.pockit.ui.theme.CocoaSoft
                                        )
                                        Text(
                                            text = Money.format(amount),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = com.invatech.pockit.ui.theme.CocoaEspresso
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = PockitOrange,
                                        trackColor = com.invatech.pockit.ui.theme.WarmCreamMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent activity", style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ), color = com.invatech.pockit.ui.theme.CocoaEspresso
                )
                TextButton(onClick = onViewAllTransactions) {
                    Text(
                        "View all",
                        color = PockitOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        if (state.transactions.isEmpty() && !state.isLoading) item {
            FinanceCard {
                Text("Start with your first entry"); Text(
                "Add income or an expense to build your money picture.",
                style = MaterialTheme.typography.bodyMedium
            )
            }
        }
        items(state.transactions.take(5), key = { it.id }) { tx ->
            ExpressiveWalletTransactionCard(
                transaction = tx,
                onEdit = { homeViewModel.clearError(); editingTransaction = tx },
                onDelete = { deletingTransaction = tx },
                modifier = Modifier.animateItem()
            )
        }
        if (state.transactions.size >= 3) item { com.invatech.pockit.ui.common.ads.PockitNativeAdCard(placementKey = "home.native") }
    }
    com.invatech.pockit.ui.finance.AccountCompletionPrompt(onSaved = homeViewModel::loadWallet)
    if (editBalance) AlertDialog(
        onDismissRequest = { if (!state.isLoading) editBalance = false },
        title = { Text("Edit recorded balance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This corrects your recorded balance. It doesn't add income or spending.")
                AmountField(balanceInput, { balanceInput = it })
                ErrorNotice(state.errorMessage)
            }
        },
        confirmButton = {
            TextButton(enabled = !state.isLoading && parseBalance(balanceInput) != null, onClick = {
                val value = parseBalance(balanceInput)!!
                homeViewModel.editBalance(value) { editBalance = false }
            }) { Text(if (state.isLoading) "Saving…" else "Save balance") }
        },
        dismissButton = {
            TextButton(
                onClick = { editBalance = false }, enabled = !state.isLoading
            ) { Text("Cancel") }
        })
    editingTransaction?.let { tx ->
        AddWalletTransactionSheet(
            initialType = tx.type,
            initialTitle = tx.title,
            initialAmountPaise = tx.amountPaise,
            initialCategory = com.invatech.pockit.ui.home.model.Category.entries.firstOrNull {
                it.name.equals(
                    tx.category, true
                )
            } ?: com.invatech.pockit.ui.home.model.Category.Other,
            isEditing = true,
            isLoading = state.isLoading,
            errorMessage = state.errorMessage,
            onDismiss = { editingTransaction = null },
            onSave = { title, amount, selectedType, category ->
                txnSaving = true
                homeViewModel.updateTransaction(tx.id, title, amount, selectedType, category)
            })
    }
    deletingTransaction?.let { tx ->
        AlertDialog(
            onDismissRequest = { deletingTransaction = null },
            title = { Text("Delete transaction?") },
            text = { Text("${tx.title} · ${Money.format(tx.amountPaise)}. Your recorded balance will be recalculated.") },
            confirmButton = {
                TextButton(onClick = {
                    homeViewModel.deleteTransaction(tx.id); deletingTransaction = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = {
                    deletingTransaction = null
                }) { Text("Cancel") }
            })
    }
    if (add) AddWalletTransactionSheet(
        initialType = type,
        isLoading = saving,
        canSave = !state.isLoading,
        errorMessage = state.errorMessage,
        onDismiss = { if (!saving) add = false },
        onSave = { title, amount, selectedType, category ->
            saving = true
            if (selectedType == WalletTransactionType.INCOME) homeViewModel.addIncome(
                title, amount, category
            ) else homeViewModel.addExpense(title, amount, category)
        })
}

@Composable
private fun LegacyHomeTool(
    title: String, subtitle: String, color: Color, modifier: Modifier, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "↗",
                modifier = Modifier
                    .align(Alignment.End)
                    .background(PockitElevated, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.titleLarge
            )
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HomeTool(
    title: String, subtitle: String, color: Color, modifier: Modifier, onClick: () -> Unit
) {
    val style = when (title) {
        "Savings goals" -> HomeToolStyle(Icons.Rounded.Savings, Color(0xFF8051C3), Color(0xFFF2E9FF), Color(0xFFD7C0FA))
        "Budgets" -> HomeToolStyle(Icons.AutoMirrored.Rounded.EventNote, Color(0xFFE95073), Color(0xFFFFF5CD), Color(0xFFFFD7A8))
        "Friends & Udhar" -> HomeToolStyle(Icons.Rounded.People, Color(0xFF07956C), Color(0xFFE8FAF2), Color(0xFFBDECD9))
        "Group expenses" -> HomeToolStyle(Icons.Rounded.Groups, Color(0xFFF04478), Color(0xFFFFECF3), Color(0xFFFFC6DB))
        "UPI inbox" -> HomeToolStyle(Icons.Rounded.Inbox, Color(0xFF4779C4), Color(0xFFEBF4FF), Color(0xFFC9DDF8))
        "Pocket planner" -> HomeToolStyle(Icons.Rounded.CalendarMonth, Color(0xFF7950B5), Color(0xFFF4EDFF), Color(0xFFDCC8F4))
        else -> HomeToolStyle(Icons.Rounded.AccountBalanceWallet, Color(0xFF8051C3), color.copy(alpha = .55f), Color(0xFFD9C7E9))
    }
    Card(
        onClick = onClick,
        modifier = modifier.height(126.dp).shadow(3.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(Brush.linearGradient(listOf(Color.White.copy(alpha = .96f), style.background)))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val back = Path().apply {
                    moveTo(0f, size.height)
                    cubicTo(size.width * .27f, size.height * .84f, size.width * .55f, size.height * 1.02f, size.width, size.height * .73f)
                    lineTo(size.width, size.height)
                    close()
                }
                val front = Path().apply {
                    moveTo(size.width * .22f, size.height)
                    cubicTo(size.width * .50f, size.height * .89f, size.width * .72f, size.height * 1.04f, size.width, size.height * .84f)
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(back, style.wave.copy(alpha = .62f))
                drawPath(front, style.wave.copy(alpha = .82f))
                drawLine(style.accent.copy(alpha = .76f), Offset(size.width * .37f, 17.dp.toPx()), Offset(size.width * .42f, 8.dp.toPx()), 2.dp.toPx())
                drawLine(style.accent.copy(alpha = .76f), Offset(size.width * .40f, 24.dp.toPx()), Offset(size.width * .48f, 21.dp.toPx()), 2.dp.toPx())
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = .94f),
                shadowElevation = 2.dp,
                modifier = Modifier.align(Alignment.TopStart).padding(11.dp).size(40.dp)
            ) {
                Icon(style.icon, null, tint = style.accent, modifier = Modifier.padding(8.dp))
            }
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = .96f),
                shadowElevation = 2.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(11.dp).size(30.dp)
            ) {
                Icon(Icons.Rounded.ArrowOutward, null, tint = Color(0xFF4B3D52), modifier = Modifier.padding(7.dp))
            }
            Column(
                Modifier.align(Alignment.TopStart).padding(start = 13.dp, end = 8.dp, top = 60.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(title, fontSize = 15.sp, lineHeight = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D2035), maxLines = 2)
                Text(subtitle, fontSize = 10.5.sp, lineHeight = 13.sp, color = Color(0xFF655A6A), maxLines = 1)
            }
        }
    }
}

private data class HomeToolStyle(
    val icon: ImageVector,
    val accent: Color,
    val background: Color,
    val wave: Color
)

private fun parseBalance(text: String): Long? = Money.parseBalance(text)
