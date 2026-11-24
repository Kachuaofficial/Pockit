package com.invatech.pockit.ui.home

/*
 * Full personal transaction history with filtering and record actions.
 * Shares HomeViewModel with the dashboard so edits and deletion use the same wallet operations.
 */

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.domain.Money
import com.invatech.pockit.ui.common.ads.PockitBannerAd
import com.invatech.pockit.ui.common.ads.PockitNativeAdCard
import com.invatech.pockit.ui.common.motion.MotionSettings
import com.invatech.pockit.ui.finance.ErrorNotice
import com.invatech.pockit.ui.finance.FinanceCard
import com.invatech.pockit.ui.finance.PageHeading
import com.invatech.pockit.ui.home.components.AddWalletTransactionSheet
import com.invatech.pockit.ui.home.components.ExpressiveWalletTransactionCard
import com.invatech.pockit.ui.home.model.Category
import com.invatech.pockit.ui.home.model.WalletTransactionType
import kotlinx.coroutines.CancellationException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
private fun TransactionSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    val surface = MaterialTheme.colorScheme.surface
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = com.invatech.pockit.ui.theme.PastelPink.copy(alpha = .2f),
                spotColor = com.invatech.pockit.ui.theme.PockitOrange.copy(alpha = .14f)
            ),
        shape = shape,
        color = surface,
        border = BorderStroke(1.dp, com.invatech.pockit.ui.theme.PockitOrange.copy(alpha = .2f))
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(com.invatech.pockit.ui.theme.PockitOrange),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            surface,
                            com.invatech.pockit.ui.theme.PastelPink.copy(alpha = .22f),
                            com.invatech.pockit.ui.theme.PastelLilac.copy(alpha = .16f)
                        )
                    )
                )
                .semantics { contentDescription = "Search transactions" },
            decorationBox = { input ->
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = com.invatech.pockit.ui.theme.PockitOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isBlank()) {
                            Text(
                                "Search transactions",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        input()
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun TransactionsHeader(onBack: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().height(152.dp).shadow(
            4.dp,
            shape,
            ambientColor = com.invatech.pockit.ui.theme.PastelPink.copy(alpha = .14f),
            spotColor = com.invatech.pockit.ui.theme.PockitOrange.copy(alpha = .1f)
        ),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, com.invatech.pockit.ui.theme.PockitOrange.copy(alpha = .1f))
    ) {
        Box(
            Modifier.background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        com.invatech.pockit.ui.theme.PastelPink.copy(alpha = .25f),
                        com.invatech.pockit.ui.theme.PastelLilac.copy(alpha = .28f)
                    )
                )
            )
        ) {
            Box(
                Modifier.align(Alignment.CenterEnd).padding(end = 10.dp).size(128.dp)
                    .graphicsLayer { rotationZ = 9f }
                    .background(
                        com.invatech.pockit.ui.theme.PastelPink.copy(alpha = .58f),
                        RoundedCornerShape(44.dp)
                    )
            )
            Box(
                Modifier.align(Alignment.CenterEnd).padding(end = 30.dp, top = 8.dp)
                    .width(108.dp).height(92.dp)
                    .graphicsLayer { rotationZ = -9f }
                    .background(
                        com.invatech.pockit.ui.theme.PastelLilac.copy(alpha = .52f),
                        RoundedCornerShape(38.dp)
                    )
            )
            Box(
                Modifier.align(Alignment.BottomEnd).width(156.dp).height(44.dp)
                    .background(
                        com.invatech.pockit.ui.theme.PastelPeach.copy(alpha = .42f),
                        RoundedCornerShape(topStart = 48.dp)
                    )
            )
            Column(
                Modifier.align(Alignment.CenterStart).padding(start = 14.dp, end = 146.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp).background(
                        MaterialTheme.colorScheme.surface.copy(alpha = .72f),
                        RoundedCornerShape(14.dp)
                    )
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Transactions",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        lineHeight = 32.sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    "Track, search and manage your money",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 17.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Image(
                painter = painterResource(com.invatech.pockit.R.drawable.pockit_transactions_wallet),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .width(148.dp)
                    .height(124.dp)
            )
        }
    }
}

@Composable
private fun MonthlyTransactionSummary(
    incomePaise: Long,
    expensePaise: Long,
    monthLabel: String
) {
    val shape = RoundedCornerShape(26.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(
            5.dp,
            shape,
            ambientColor = com.invatech.pockit.ui.theme.PastelPink.copy(alpha = .18f),
            spotColor = com.invatech.pockit.ui.theme.PockitOrange.copy(alpha = .12f)
        ),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, com.invatech.pockit.ui.theme.PockitOrange.copy(alpha = .12f))
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        com.invatech.pockit.ui.theme.PastelPink.copy(alpha = .28f),
                        com.invatech.pockit.ui.theme.PastelLilac.copy(alpha = .18f)
                    )
                )
            )
        ) {
            Row(
                Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                listOf(15.dp, 25.dp, 37.dp, 50.dp).forEach { barHeight ->
                    Box(
                        Modifier.width(9.dp).height(barHeight).background(
                            com.invatech.pockit.ui.theme.PockitOrange.copy(alpha = .24f),
                            RoundedCornerShape(8.dp)
                        )
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("This Month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Box(Modifier.weight(1f))
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        monthLabel,
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ArrowDownward, null, Modifier.size(17.dp), tint = com.invatech.pockit.ui.theme.IncomeGreen)
                            Text("Income", Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            Money.format(incomePaise),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = com.invatech.pockit.ui.theme.IncomeGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        Modifier.padding(horizontal = 14.dp).width(1.dp).height(52.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ArrowUpward, null, Modifier.size(17.dp), tint = com.invatech.pockit.ui.theme.ExpenseRed)
                            Text("Expense", Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            Money.format(expensePaise),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = com.invatech.pockit.ui.theme.ExpenseRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private data class TransactionDateSection(
    val dayKey: Long,
    val label: String,
    val transactions: List<HomeTransactionUi>,
    val startIndex: Int
)

private fun normalizedDay(date: Date?): Long {
    return Calendar.getInstance().apply {
        if (date != null) time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun buildDateSections(
    transactions: List<HomeTransactionUi>,
    locale: Locale
): List<TransactionDateSection> {
    val today = normalizedDay(Date())
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = today
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis
    val formatter = SimpleDateFormat("d MMM yyyy", locale)
    var startIndex = 0
    return transactions
        .groupBy { normalizedDay(it.createdAt?.toDate()) }
        .toSortedMap(compareByDescending { it })
        .map { (dayKey, dayTransactions) ->
            val label = when (dayKey) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> formatter.format(Date(dayKey))
            }
            TransactionDateSection(dayKey, label, dayTransactions, startIndex).also {
                startIndex += dayTransactions.size
            }
        }
}

@Composable
private fun TransactionDateHeader(section: TransactionDateSection) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = com.invatech.pockit.ui.theme.PastelPink.copy(alpha = .42f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                section.label,
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Box(Modifier.weight(1f))
        Text(
            "${section.transactions.size} ${if (section.transactions.size == 1) "transaction" else "transactions"}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AllTransactionsScreen(
    homeViewModel: HomeViewModel, onBack: () -> Unit, modifier: Modifier = Modifier
) {
    val state by homeViewModel.uiState.collectAsState()
    val animations by MotionSettings.enabled.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<HomeTransactionUi?>(null) }
    var deleting by remember { mutableStateOf<HomeTransactionUi?>(null) }
    var saving by remember { mutableStateOf(false) }
    val currentMonthTransactions = remember(state.transactions) {
        val now = Calendar.getInstance()
        state.transactions.filter { transaction ->
            transaction.createdAt?.toDate()?.let { date ->
                val calendar = Calendar.getInstance().apply { time = date }
                calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == now.get(Calendar.MONTH)
            } ?: true
        }
    }
    val monthlyIncome = remember(currentMonthTransactions) {
        Money.sum(currentMonthTransactions.filter { it.type == WalletTransactionType.INCOME }.map { it.amountPaise })
    }
    val monthlyExpense = remember(currentMonthTransactions) {
        Money.sum(currentMonthTransactions.filter { it.type == WalletTransactionType.EXPENSE }.map { it.amountPaise })
    }
    val monthLabel = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date()) }
    val rows = remember(state.transactions, query, filter) {
        state.transactions.filter {
            (filter == "All" || it.type.name == filter) && (it.title.contains(
                query, true
            ) || it.category.contains(query, true))
        }
    }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val transactionSections = remember(rows, locale) { buildDateSections(rows, locale) }
    PredictiveBackHandler(enabled = selected != null) { progress ->
        try {
            progress.collect { /* tracking */ }
            selected = null
        } catch (_: CancellationException) {
        }
    }
    LaunchedEffect(saving, state.isLoading) {
        if (saving && !state.isLoading) {
            saving = false; if (state.errorMessage == null) editing = null
        }
    }
    SharedTransitionLayout(modifier) {
        AnimatedContent(selected, transitionSpec = {
            if (animations) {
                (slideInHorizontally(
                    spring(
                        dampingRatio = 0.8f, stiffness = Spring.StiffnessLow
                    )
                ) { if (targetState != null) (it * 0.3f).toInt() else (-it * 0.3f).toInt() } + fadeIn() + scaleIn(
                    initialScale = 0.95f
                )) togetherWith (slideOutHorizontally(
                    spring(
                        dampingRatio = 0.85f, stiffness = Spring.StiffnessLow
                    )
                ) { if (targetState != null) (-it * 0.2f).toInt() else (it * 0.2f).toInt() } + fadeOut() + scaleOut(
                    targetScale = 0.96f
                ))
            } else EnterTransition.None togetherWith ExitTransition.None
        }, label = "transaction_detail") { id ->
            val detail = state.transactions.firstOrNull { it.id == id }
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    if (detail == null) {
                        TransactionsHeader(onBack)
                    } else {
                        PageHeading("Transaction", onBack = { selected = null })
                    }
                }
                item { ErrorNotice(state.errorMessage) }
                if (detail == null) {
                    item {
                        TransactionSearchBar(
                            query = query,
                            onQueryChange = { query = it }
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "All", "EXPENSE", "INCOME"
                            ).forEach { item ->
                                FilterChip(filter == item, { filter = item }, label = {
                                    Text(
                                        item.lowercase().replaceFirstChar { it.uppercase() })
                                })
                            }
                        }
                    }
                    item {
                        MonthlyTransactionSummary(
                            incomePaise = monthlyIncome,
                            expensePaise = monthlyExpense,
                            monthLabel = monthLabel
                        )
                    }
                    if (rows.isNotEmpty()) item {
                        PockitBannerAd(
                            placementKey = "AllTransactionsScreen.banner.1",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (rows.isEmpty()) item {
                        FinanceCard {
                            Text(if (query.isBlank()) "No transactions yet" else "No matching transactions"); Text(
                            "Your saved expenses and income appear here."
                        )
                        }
                    }
                    transactionSections.forEach { section ->
                        item(key = "date_${section.dayKey}") {
                            TransactionDateHeader(section)
                        }
                        itemsIndexed(section.transactions, key = { _, tx -> tx.id }) { localIndex, tx ->
                            val absoluteIndex = section.startIndex + localIndex
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                ExpressiveWalletTransactionCard(
                                    transaction = tx,
                                    onEdit = { homeViewModel.clearError(); editing = tx },
                                    onDelete = { deleting = tx },
                                    onClick = { selected = tx.id },
                                    modifier = if (animations) Modifier
                                        .animateItem()
                                        .sharedBounds(
                                            rememberSharedContentState("card_${tx.id}"),
                                            this@AnimatedContent
                                        ) else Modifier,
                                    titleModifier = if (animations) Modifier.sharedElement(
                                        rememberSharedContentState("tx_${tx.id}"), this@AnimatedContent
                                    ) else Modifier,
                                    amountModifier = if (animations) Modifier.sharedElement(
                                        rememberSharedContentState("amount_${tx.id}"),
                                        this@AnimatedContent
                                    ) else Modifier
                                )
                                if (com.invatech.pockit.ui.common.ads.NativePlacementPolicy.afterRow(absoluteIndex, rows.size)) {
                                    PockitNativeAdCard(
                                        placementKey = "AllTransactionsScreen.native.$absoluteIndex",
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                } else {
                    item {
                        Surface(
                            modifier = if (animations) Modifier
                                .fillMaxWidth()
                                .sharedBounds(
                                    rememberSharedContentState("card_${detail.id}"),
                                    this@AnimatedContent
                                ) else Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, com.invatech.pockit.ui.theme.WarmBorder)
                        ) {
                            Column(
                                Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                Text(
                                    if (detail.type == WalletTransactionType.EXPENSE) "EXPENSE" else "INCOME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    detail.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = if (animations) Modifier.sharedElement(
                                        rememberSharedContentState("tx_${detail.id}"),
                                        this@AnimatedContent
                                    ) else Modifier
                                )
                                Text(
                                    (if (detail.type == WalletTransactionType.EXPENSE) "− " else "+ ") + Money.format(
                                        detail.amountPaise
                                    ),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (detail.type == WalletTransactionType.EXPENSE) com.invatech.pockit.ui.theme.CocoaEspresso else Color(
                                        0xFF21664F
                                    ),
                                    modifier = if (animations) Modifier.sharedElement(
                                        rememberSharedContentState("amount_${detail.id}"),
                                        this@AnimatedContent
                                    ) else Modifier
                                )
                                HorizontalDivider()
                                Text(detail.category, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    detail.createdAt?.toDate()
                                    ?.let { DateFormat.getDateTimeInstance().format(it) }
                                    ?: "Just now", style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    FilledTonalButton(onClick = {
                                        homeViewModel.clearError(); editing = detail
                                    }, modifier = Modifier.weight(1f)) { Text("Edit entry") }
                                    OutlinedButton(
                                        onClick = { deleting = detail },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Delete") }
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    editing?.let { tx ->
        AddWalletTransactionSheet(
            initialType = tx.type,
            initialTitle = tx.title,
            initialAmountPaise = tx.amountPaise,
            initialCategory = Category.entries.firstOrNull { it.name.equals(tx.category, true) }
                ?: Category.Other,
            isEditing = true,
            isLoading = saving,
            canSave = !state.isLoading,
            errorMessage = state.errorMessage,
            onDismiss = { if (!saving) editing = null },
            onSave = { title, amount, type, category ->
                saving = true
                homeViewModel.updateTransaction(tx.id, title, amount, type, category)
            })
    }
    deleting?.let { tx ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete transaction?") },
            text = { Text("${tx.title} · ${Money.format(tx.amountPaise)}. Your recorded balance will be recalculated.") },
            confirmButton = {
                TextButton(onClick = {
                    homeViewModel.deleteTransaction(tx.id); deleting = null; selected = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
}
