package com.invatech.pockit.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.invatech.pockit.data.repository.BudgetRepository
import com.invatech.pockit.PockitApplication
import com.invatech.pockit.domain.Money
import com.invatech.pockit.ui.common.ads.PockitNativeAdCard
import com.invatech.pockit.ui.finance.*
import com.invatech.pockit.ui.home.model.WalletTransactionType
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BudgetScreen(onBack: () -> Unit) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val repo = remember { BudgetRepository() }
    val syncError by repo.syncError.collectAsState()
    val budgets by remember { repo.observeBudgets() }.collectAsState(emptyList())
    val transactions by remember { PockitApplication.instance.database.walletDao().observeTransactions() }.collectAsState(emptyList())
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val month = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); add(Calendar.MONTH, offset) }
    val start = month.timeInMillis
    val end = (month.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.timeInMillis
    val spent = remember(transactions, start, end) { transactions.filter { it.type == WalletTransactionType.EXPENSE && it.createdAtMillis >= start && it.createdAtMillis < end } }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeading("Budgets", "Plan your month with confidence.", onBack) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { offset-- }) { Text("Previous") }
            Text(SimpleDateFormat("MMM yyyy", locale).format(month.time), Modifier.padding(top = 12.dp))
            TextButton(onClick = { offset++ }, enabled = offset < 0) { Text("Next") }
        } }
        item { FinanceCard(color = com.invatech.pockit.ui.theme.PockitYellow) {
            Text("Monthly spending", style = MaterialTheme.typography.labelLarge)
            Text(Money.format(Money.sum(spent.map { it.amountPaise })), style = MaterialTheme.typography.headlineLarge)
            Text(if (offset == 0) "Limits repeat each month. Transfers and udhar are tracked separately." else "Historical spending compared with your current recurring limits.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { editing = "Overall" }) { Text(if (budgets.isEmpty()) "Set your first budget" else "Set a budget") }
        } }
        item { ErrorNotice(error); ErrorNotice(syncError) }
        items(budgets, key = { it.category }) { budget ->
            val used = Money.sum(spent.filter { budget.category == "Overall" || it.category.equals(budget.category, true) }.map { it.amountPaise })
            val remaining = budget.monthlyLimitPaise - used
            FinanceCard {
                Text(budget.category, style = MaterialTheme.typography.titleLarge)
                Text("${Money.format(used)} of ${Money.format(budget.monthlyLimitPaise)}")
                LinearProgressIndicator(progress = { (used.toDouble() / budget.monthlyLimitPaise.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text(if (remaining >= 0) "${Money.format(remaining)} remaining" else "${Money.format(-remaining)} over budget", color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = { editing = budget.category }) { Text("Edit limit") }
            }
        }
        if (budgets.isNotEmpty()) {
            item { PockitNativeAdCard(placementKey = "BudgetScreen.native.1", modifier = Modifier.padding(vertical = 4.dp)) }
        }
    }
    editing?.let { selected ->
        var category by remember(selected) { mutableStateOf(selected) }
        var amount by remember(selected) { mutableStateOf(budgets.firstOrNull { it.category == selected }?.let { Money.decimal(it.monthlyLimitPaise) }.orEmpty()) }
        AlertDialog(onDismissRequest = { if (!busy) editing = null }, title = { Text("Monthly budget") }, text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Overall", "Food", "Groceries", "Travel", "Shopping", "Bills", "Entertainment", "Other").forEach { item -> FilterChip(category == item, { category = item }, label = { Text(item) }) }
                }
                OutlinedTextField(category, { category = it }, label = { Text("Category (Overall for all expenses)") })
                AmountField(amount, { amount = it }, "Monthly limit (₹)")
                Text("You'll get a warning at 80% and when the limit is reached.", style = MaterialTheme.typography.bodySmall)
                ErrorNotice(error)
            }
        }, confirmButton = { TextButton(enabled = !busy && Money.parse(amount) != null && category.isNotBlank(), onClick = {
            busy = true
            scope.launch { try { repo.setBudget(category.trim(), Money.parse(amount)!!); editing = null } catch(e: Exception) { error = e.message } finally { busy = false } }
        }) { Text("Save budget") } }, dismissButton = { TextButton(onClick = { editing = null }, enabled = !busy) { Text("Cancel") } })
    }
}
