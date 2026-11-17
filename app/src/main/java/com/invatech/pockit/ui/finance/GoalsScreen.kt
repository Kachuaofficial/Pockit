package com.invatech.pockit.ui.finance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.invatech.pockit.data.local.database.FinanceRecord
import com.invatech.pockit.domain.Money

@Composable
fun GoalsScreen(onBack: () -> Unit, vm: FinanceViewModel = viewModel()) {
    val goals by vm.goals.collectAsState()
    val error by vm.error.collectAsState()
    val busy by vm.busy.collectAsState()
    var add by remember { mutableStateOf(false) }
    var contribute by remember { mutableStateOf<FinanceRecord?>(null) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeading("Savings goals", "A little progress, every month.", onBack) }
        item { Text("Track money you've set aside. Goal contributions do not move money or alter your wallet.", style = MaterialTheme.typography.bodyMedium) }
        item { Button(onClick = { add = true }) { Text("New goal") } }
        item { ErrorNotice(error) }
        if (goals.isEmpty()) item { FinanceCard(color = com.invatech.pockit.ui.theme.PockitLavender) { Text("What are you saving for?", style = MaterialTheme.typography.titleLarge); Text("A laptop, a trip, or your next course. Start with a target.") } }
        items(goals, key = { it.id }) { goal -> FinanceCard {
            val saved = goal.direction.toLongOrNull() ?: 0L
            Text(goal.name, style = MaterialTheme.typography.titleLarge)
            Text("${Money.format(saved)} of ${Money.format(goal.amountPaise)}")
            LinearProgressIndicator(progress = { (saved.toDouble() / goal.amountPaise).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            if (saved < goal.amountPaise) TextButton(onClick = { vm.clearError(); contribute = goal }) { Text("Add savings") } else Text("Goal reached")
        } }
    }
    if (add || contribute != null) {
        var name by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { if (!busy) { add = false; contribute = null } }, title = { Text(if (add) "Create a goal" else "Add savings") }, text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (add) OutlinedTextField(name, { name = it }, label = { Text("Goal name") })
                AmountField(amount, { amount = it }, if (add) "Target (₹)" else "Saved amount (₹)")
                ErrorNotice(error)
            }
        }, confirmButton = { TextButton(enabled = !busy && Money.parse(amount) != null && (!add || name.isNotBlank()), onClick = {
            if (add) vm.addGoal(name, Money.parse(amount)!!) { add = false }
            else contribute?.let { vm.contribute(it, Money.parse(amount)!!) { contribute = null } }
        }) { Text("Save") } }, dismissButton = { TextButton(onClick = { add = false; contribute = null }, enabled = !busy) { Text("Cancel") } })
    }
}
