package com.invatech.pockit.ui.home

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.domain.Money
import com.invatech.pockit.ui.common.ads.PockitBannerAd
import com.invatech.pockit.ui.common.ads.PockitNativeAdCard
import com.invatech.pockit.ui.finance.*
import com.invatech.pockit.ui.home.components.AddWalletTransactionSheet
import com.invatech.pockit.ui.home.components.ExpressiveWalletTransactionCard
import com.invatech.pockit.ui.home.model.*
import com.invatech.pockit.ui.common.motion.MotionSettings
import kotlinx.coroutines.CancellationException
import java.text.DateFormat

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AllTransactionsScreen(homeViewModel: HomeViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by homeViewModel.uiState.collectAsState()
    val animations by MotionSettings.enabled.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<HomeTransactionUi?>(null) }
    var deleting by remember { mutableStateOf<HomeTransactionUi?>(null) }
    var saving by remember { mutableStateOf(false) }
    val rows = remember(state.transactions, query, filter) {
        state.transactions.filter { (filter == "All" || it.type.name == filter) && (it.title.contains(query, true) || it.category.contains(query, true)) }
    }
    PredictiveBackHandler(enabled = selected != null) { progress ->
        try {
            progress.collect { /* tracking */ }
            selected = null
        } catch (_: CancellationException) {
        }
    }
    LaunchedEffect(saving, state.isLoading) { if (saving && !state.isLoading) { saving = false; if (state.errorMessage == null) editing = null } }
    SharedTransitionLayout(modifier) {
        AnimatedContent(selected, transitionSpec = {
            if (animations) {
                (slideInHorizontally(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)) { if (targetState != null) (it * 0.3f).toInt() else (-it * 0.3f).toInt() } + fadeIn() + scaleIn(initialScale = 0.95f)) togetherWith
                        (slideOutHorizontally(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)) { if (targetState != null) (-it * 0.2f).toInt() else (it * 0.2f).toInt() } + fadeOut() + scaleOut(targetScale = 0.96f))
            } else EnterTransition.None togetherWith ExitTransition.None
        }, label = "transaction_detail") { id ->
            val detail = state.transactions.firstOrNull { it.id == id }
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { PageHeading(if (detail == null) "Activity" else "Transaction", onBack = { if (id != null) selected = null else onBack() }) }
                item { ErrorNotice(state.errorMessage) }
                if (detail == null) {
                    item { OutlinedTextField(query, { query = it }, label = { Text("Search transactions") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "EXPENSE", "INCOME").forEach { item -> FilterChip(filter == item, { filter = item }, label = { Text(item.lowercase().replaceFirstChar { it.uppercase() }) }) }
                    } }
                    if (rows.isNotEmpty()) item { PockitBannerAd(placementKey = "AllTransactionsScreen.banner.1", modifier = Modifier.padding(vertical = 4.dp)) }
                    if (rows.isEmpty()) item { FinanceCard { Text(if (query.isBlank()) "No transactions yet" else "No matching transactions"); Text("Your saved expenses and income appear here.") } }
                    itemsIndexed(rows, key = { _, tx -> tx.id }) { index, tx ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ExpressiveWalletTransactionCard(
                                transaction = tx,
                                onEdit = { homeViewModel.clearError(); editing = tx },
                                onDelete = { deleting = tx },
                                onClick = { selected = tx.id },
                                modifier = if (animations) Modifier.animateItem().sharedBounds(rememberSharedContentState("card_${tx.id}"), this@AnimatedContent) else Modifier,
                                titleModifier = if (animations) Modifier.sharedElement(rememberSharedContentState("tx_${tx.id}"), this@AnimatedContent) else Modifier,
                                amountModifier = if (animations) Modifier.sharedElement(rememberSharedContentState("amount_${tx.id}"), this@AnimatedContent) else Modifier
                            )
                            if (index == 7 && index < rows.lastIndex) {
                                PockitNativeAdCard(placementKey = "AllTransactionsScreen.native.1", modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }

                } else {
                    item {
                        Surface(
                            modifier = if (animations) Modifier.fillMaxWidth().sharedBounds(rememberSharedContentState("card_${detail.id}"), this@AnimatedContent) else Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, com.invatech.pockit.ui.theme.WarmBorder)
                        ) {
                            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                Text(if (detail.type == WalletTransactionType.EXPENSE) "EXPENSE" else "INCOME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(detail.title, style = MaterialTheme.typography.headlineSmall,
                                    modifier = if (animations) Modifier.sharedElement(rememberSharedContentState("tx_${detail.id}"), this@AnimatedContent) else Modifier)
                                Text((if (detail.type == WalletTransactionType.EXPENSE) "− " else "+ ") + Money.format(detail.amountPaise),
                                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                                    color = if (detail.type == WalletTransactionType.EXPENSE) com.invatech.pockit.ui.theme.CocoaEspresso else Color(0xFF21664F),
                                    modifier = if (animations) Modifier.sharedElement(rememberSharedContentState("amount_${detail.id}"), this@AnimatedContent) else Modifier)
                                HorizontalDivider()
                                Text(detail.category, style = MaterialTheme.typography.titleMedium)
                                Text(detail.createdAt?.toDate()?.let { DateFormat.getDateTimeInstance().format(it) } ?: "Just now", style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    FilledTonalButton(onClick = { homeViewModel.clearError(); editing = detail }, modifier = Modifier.weight(1f)) { Text("Edit entry") }
                                    OutlinedButton(onClick = { deleting = detail }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    editing?.let { tx -> AddWalletTransactionSheet(initialType = tx.type, initialTitle = tx.title, initialAmountPaise = tx.amountPaise,
        initialCategory = Category.entries.firstOrNull { it.name.equals(tx.category, true) } ?: Category.Other,
        isEditing = true, isLoading = saving, canSave = !state.isLoading, errorMessage = state.errorMessage,
        onDismiss = { if (!saving) editing = null }, onSave = { title, amount, type, category ->
            saving = true
            homeViewModel.updateTransaction(tx.id, title, amount, type, category)
        }) }
    deleting?.let { tx -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete transaction?") }, text = { Text("${tx.title} · ${Money.format(tx.amountPaise)}. Your recorded balance will be recalculated.") },
        confirmButton = { TextButton(onClick = { homeViewModel.deleteTransaction(tx.id); deleting = null; selected = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }) }
}
