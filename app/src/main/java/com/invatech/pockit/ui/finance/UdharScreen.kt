package com.invatech.pockit.ui.finance

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.invatech.pockit.ui.common.ads.PockitBannerAd
import com.invatech.pockit.ui.common.ads.PockitNativeAdCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.invatech.pockit.domain.Money
import com.invatech.pockit.ui.common.motion.MotionSettings
import kotlinx.coroutines.CancellationException
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PersonalLedgerContent(vm: FinanceViewModel = viewModel()) {
    val records by vm.ledger.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<com.invatech.pockit.data.local.database.FinanceRecord?>(null) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    val groups = remember(records) { records.groupBy { it.name } }
    val balances = remember(groups) { groups.mapValues { udharBalance(it.value) } }
    val animations by MotionSettings.enabled.collectAsState()
    PredictiveBackHandler(enabled = selected != null) { progress ->
        try {
            progress.collect { /* tracking */ }
            selected = null
        } catch (_: CancellationException) {
        }
    }
    SharedTransitionLayout {
        AnimatedContent(selected, transitionSpec = {
            if (animations) {
                (slideInHorizontally(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)) { if (targetState != null) (it * 0.3f).toInt() else (-it * 0.3f).toInt() } + fadeIn() + scaleIn(initialScale = 0.95f)) togetherWith
                        (slideOutHorizontally(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)) { if (targetState != null) (-it * 0.2f).toInt() else (it * 0.2f).toInt() } + fadeOut() + scaleOut(targetScale = 0.96f))
            } else EnterTransition.None togetherWith ExitTransition.None
        }, label = "udhar_detail") { person ->
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { PageHeading(person ?: "Friends", if (person == null) "Personal records. No invitation needed." else "Private ledger · synced to your account", if (person != null) ({ selected = null }) else null) }
                item { ErrorNotice(error) }
                if (person == null) {
                    item { FinanceCard(color = com.invatech.pockit.ui.theme.PockitCoral) {
                        Text("You'll receive", style = MaterialTheme.typography.labelLarge)
                        Text(Money.format(Money.sum(balances.values.filter { it > 0 })), style = MaterialTheme.typography.headlineMedium)
                        Text("You owe  ${Money.format(-Money.sum(balances.values.filter { it < 0 }))}")
                        Button(onClick = { vm.clearError(); showAdd = true }) { Text("Add udhar") }
                    } }

                    if (groups.isEmpty()) item { Text("Lent a friend money? Add their name and amount. They don't need Pockit.") }
                    val friendNames = groups.keys.toList()
                    itemsIndexed(friendNames, key = { _, it -> it }) { index, name ->
                        val balance = balances[name] ?: 0L
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FinanceCard(Modifier.clickable { selected = name }) {
                                Text(name, style = MaterialTheme.typography.titleLarge, modifier = if (animations) Modifier.sharedElement(rememberSharedContentState("person_$name"), this@AnimatedContent) else Modifier)
                                Text(if (balance > 0) "You'll receive ${Money.format(balance)}" else if (balance < 0) "You owe ${Money.format(-balance)}" else "Settled up")
                            }
                            if (index == 7 && index < friendNames.lastIndex) {
                                PockitNativeAdCard(placementKey = "UdharScreen.native.1", modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }

                } else {
                    item { FinanceCard {
                        Text(person, style = MaterialTheme.typography.titleLarge, modifier = if (animations) Modifier.sharedElement(rememberSharedContentState("person_$person"), this@AnimatedContent) else Modifier)
                        val balance = balances[person] ?: 0L
                        Text(if (balance >= 0) "You'll receive ${Money.format(balance)}" else "You owe ${Money.format(-balance)}", style = MaterialTheme.typography.headlineSmall)
                        Button(onClick = { vm.clearError(); showAdd = true }) { Text("Add entry / repayment") }
                    } }
                    val entries = groups[person].orEmpty()
                    items(entries, key = { it.id }) { row -> FinanceCard {
                        Text(when(row.direction) { "LENT" -> "You lent"; "BORROWED" -> "You borrowed"; "RECEIVED" -> "Repayment received"; else -> "You repaid" }, style = MaterialTheme.typography.labelLarge)
                        Text(Money.format(row.amountPaise), style = MaterialTheme.typography.titleLarge)
                        if (row.note.isNotBlank()) Text(row.note)
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(row.occurredAt)), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { deleting = row }) { Text("Delete entry") }
                    } }

                }
            }
        }
    }
    deleting?.let { row -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete entry?") }, text = { Text("This removes the entry and recalculates this person's outstanding balance.") }, confirmButton = { TextButton(onClick = { vm.deleteEntry(row); deleting = null }, enabled = !busy) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }) }
    if (showAdd) {
        var name by remember { mutableStateOf(selected.orEmpty()) }
        var amount by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var direction by remember { mutableStateOf("LENT") }
        AlertDialog(onDismissRequest = { if (!busy) showAdd = false }, title = { Text("Record udhar") }, text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Friend's name") }, singleLine = true, enabled = selected == null)
                AmountField(amount, { amount = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(direction == "LENT", { direction = "LENT" }, label = { Text("I lent") })
                    FilterChip(direction == "BORROWED", { direction = "BORROWED" }, label = { Text("I borrowed") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(direction == "RECEIVED", { direction = "RECEIVED" }, label = { Text("Received back") })
                    FilterChip(direction == "REPAID", { direction = "REPAID" }, label = { Text("I repaid") })
                }
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") })
                Text("Udhar is a separate ledger. This entry does not change wallet spending or income.", style = MaterialTheme.typography.bodySmall)
                ErrorNotice(error)
            }
        }, confirmButton = {
            TextButton(onClick = { vm.addUdhar(name, Money.parse(amount)!!, direction, note) { showAdd = false } }, enabled = !busy && name.isNotBlank() && Money.parse(amount) != null) { Text(if (busy) "Saving…" else "Save") }
        }, dismissButton = { TextButton(onClick = { showAdd = false }, enabled = !busy) { Text("Cancel") } })
    }
}
