package com.invatech.pockit.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.invatech.pockit.data.repository.AuthRepository
import com.invatech.pockit.ui.common.loading.ExpressiveLoadingIndicator
import com.invatech.pockit.ui.common.motion.EmphasizedDecelerate
import com.invatech.pockit.ui.common.motion.rememberPressScale
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
import com.invatech.pockit.ui.theme.Cloud
import com.invatech.pockit.ui.theme.ErrorRed
import com.invatech.pockit.ui.theme.ErrorSoft
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.MintDeep
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToLong

/** Gap between consecutive cards starting their entrance. */
private const val StaggerStepMillis = 70L

/** After this long the first-load entrance is considered finished (no replay on scroll). */
private const val EntranceSettleMillis = 1300L

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
    var txToDelete by remember { mutableStateOf<Transaction?>(null) }

    // The staggered entrance + balance count-up play once per Home visit, not every
    // time a card scrolls back into view. Saved so tab switches don't replay it either.
    var entranceDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.isInitialLoad) {
        if (!uiState.isInitialLoad && !entranceDone) {
            delay(EntranceSettleMillis)
            entranceDone = true
        }
    }

    // Spending Weekly Chart (hidden when empty)
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
                    } ?: "Just now",
                    stringId = item.id
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
            val listState = rememberLazyListState()
            val animateEntrance = !entranceDone

            // Balance rolls up from 0 on first open, and rolls between values on every change
            // (add income / delete expense), instead of snapping.
            val shownBalancePaise = rememberCountUpPaise(
                target = uiState.balancePaise.toLong(),
                animateFromZero = animateEntrance
            )

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 12.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(key = "header") {
                    StaggeredEntrance(
                        index = 0,
                        animate = animateEntrance,
                        modifier = Modifier.headerParallax(listState)
                    ) {
                        HomeHeader(userName = displayName)
                    }
                }

                item(key = "balance") {
                    StaggeredEntrance(
                        index = 1,
                        animate = animateEntrance,
                        modifier = Modifier.animateItem()
                    ) {
                        BalanceSection(
                            balance = shownBalancePaise.value / 100.0
                        )
                    }
                }

                item(key = "quick_actions") {
                    StaggeredEntrance(
                        index = 2,
                        animate = animateEntrance,
                        modifier = Modifier.animateItem()
                    ) {
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
                }

                // Sync badge: fades in/out and the cards below glide to make room
                // (animateItem) instead of jumping.
                if (uiState.isLoading) {
                    item(key = "sync_badge") {
                        Row(
                            modifier = Modifier
                                .animateItem()
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
                    item(key = "error") {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                // Spending Weekly Chart. Only present when there's data, so it no longer
                // leaves an empty 20dp gap; when the first expense is added it fades in
                // and the list below slides down smoothly.
                if (spendingData.isNotEmpty()) {
                    item(key = "spending_chart") {
                        StaggeredEntrance(
                            index = 3,
                            animate = animateEntrance,
                            modifier = Modifier.animateItem()
                        ) {
                            SpendingChartCard(data = spendingData)
                        }
                    }
                }

                // Recent Transactions Card (Shows strictly 5 items with "See all")
                item(key = "recent_transactions") {
                    StaggeredEntrance(
                        index = 4,
                        animate = animateEntrance,
                        modifier = Modifier.animateItem()
                    ) {
                        RecentTransactionsCard(
                            transactions = recentTransactions,
                            onViewAll = onViewAllTransactions,
                            onDeleteTransaction = { tx ->
                                txToDelete = tx
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Transaction Confirmation Alert Dialog
    txToDelete?.let { targetTx ->
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            icon = {
                // Bin badge pops in with an overshoot and a little wobble.
                val pop = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    pop.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                            rotationZ = (1f - pop.value) * -28f
                        }
                        .clip(CircleShape)
                        .background(ErrorSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Delete Transaction?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Are you sure you want to delete this transaction? Your wallet balance will be updated.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        color = Cloud,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = targetTx.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "₹${"%,.2f".format(targetTx.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (targetTx.isExpense) ErrorRed else MintDeep
                            )
                        }
                    }
                }
            },
            confirmButton = {
                val deleteSource = remember { MutableInteractionSource() }
                val deleteScale by rememberPressScale(deleteSource, pressedScale = 0.93f)
                Button(
                    onClick = {
                        val tx = targetTx
                        txToDelete = null
                        homeViewModel.deleteTransaction(tx.stringId)
                    },
                    modifier = Modifier.graphicsLayer {
                        scaleX = deleteScale
                        scaleY = deleteScale
                    },
                    interactionSource = deleteSource,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Delete",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                val cancelSource = remember { MutableInteractionSource() }
                val cancelScale by rememberPressScale(cancelSource, pressedScale = 0.93f)
                TextButton(
                    onClick = { txToDelete = null },
                    modifier = Modifier.graphicsLayer {
                        scaleX = cancelScale
                        scaleY = cancelScale
                    },
                    interactionSource = cancelSource
                ) {
                    Text(
                        text = "Cancel",
                        color = InkMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
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

/**
 * Each card rises, fades and un-squishes into place, one after another.
 * [animate] = false means "already played": the card starts fully visible, so items
 * that scroll back into view (or appear later) don't re-run the intro.
 */
@Composable
private fun StaggeredEntrance(
    index: Int,
    animate: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(if (animate) 0f else 1f) }

    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(index * StaggerStepMillis)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val p = progress.value
                alpha = p.coerceIn(0f, 1f)
                translationY = (1f - p) * 40.dp.toPx()
                val s = 0.94f + 0.06f * p
                scaleX = s
                scaleY = s
            }
    ) {
        content()
    }
}

/**
 * Header drifts down slower than the list and fades while you scroll away from the top.
 * Everything is read inside graphicsLayer, so scrolling never triggers recomposition.
 */
private fun Modifier.headerParallax(
    listState: LazyListState,
    fadeDistance: Dp = 120.dp
): Modifier = graphicsLayer {
    val distancePx = fadeDistance.toPx()
    val scrolled = if (listState.firstVisibleItemIndex == 0) {
        listState.firstVisibleItemScrollOffset.toFloat().coerceIn(0f, distancePx)
    } else {
        distancePx
    }
    alpha = 1f - scrolled / distancePx
    translationY = scrolled * 0.35f
}

/**
 * Animated paise value for count-up text. Uses Long math at the end of the animation,
 * so the final number is always exactly [target] (no float rounding on big balances).
 */
@Composable
private fun rememberCountUpPaise(target: Long, animateFromZero: Boolean): State<Long> {
    val progress = remember { Animatable(1f) }
    var start by remember { mutableLongStateOf(if (animateFromZero) 0L else target) }
    var end by remember { mutableLongStateOf(if (animateFromZero) 0L else target) }
    var hasAnimated by remember { mutableStateOf(false) }

    val shown = remember {
        derivedStateOf {
            (start + (end - start) * progress.value.toDouble()).roundToLong()
        }
    }

    LaunchedEffect(target) {
        start = shown.value
        end = target
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = if (hasAnimated) 650 else 1100,
                delayMillis = if (hasAnimated) 0 else 280,
                easing = EmphasizedDecelerate
            )
        )
        hasAnimated = true
    }

    return shown
}