package com.invatech.pockit.ui.finance

/*
 * Review queue for payment notifications detected on the device.
 * The user can correct and approve an entry or reject it before it affects the personal wallet.
 */

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.invatech.pockit.domain.Money

@Composable
fun UpiReviewScreen(onBack: () -> Unit, vm: FinanceViewModel = viewModel()) {
    val pending by vm.pending.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeading("Review payments", "Only approved entries change your wallet.", onBack) }
        item { ErrorNotice(error) }
        if (pending.isEmpty()) item { FinanceCard(color = com.invatech.pockit.ui.theme.PockitYellow) { Text("You're all caught up", style = MaterialTheme.typography.titleLarge); Text("Supported UPI payments will appear here. Review their amount, direction and category before saving.") } }
        items(pending, key = { it.id }) { record ->
            var name by remember(record.id) { mutableStateOf(record.name) }
            var amount by remember(record.id) { mutableStateOf(Money.decimal(record.amountPaise)) }
            var category by remember(record.id) { mutableStateOf(record.category) }
            var direction by remember(record.id) { mutableStateOf(record.direction) }
            FinanceCard {
                Text(record.note, style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(name, { name = it }, label = { Text("Paid to / received from") }, modifier = Modifier.fillMaxWidth())
                AmountField(amount, { amount = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(direction == "EXPENSE", { direction = "EXPENSE" }, label = { Text("Money out") })
                    FilterChip(direction == "INCOME", { direction = "INCOME" }, label = { Text("Money in") })
                }
                OutlinedTextField(category, { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                Text("Transfers between your own accounts should be dismissed to avoid counting them as income or spending.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { vm.reject(record) }, enabled = !busy) { Text("Dismiss") }
                    Button(onClick = { vm.approve(record, name.trim(), Money.parse(amount)!!, direction, category.trim()) },
                        enabled = !busy && name.isNotBlank() && category.isNotBlank() && Money.parse(amount) != null) { Text("Approve & add") }
                }
            }
        }
    }
}
