package com.invatech.pockit.ui.finance

/*
 * Pocket-money planning interface based on the supplied recorded balance and planning inputs.
 * It presents an estimate; it does not obtain a live bank balance.
 */

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.invatech.pockit.domain.Money
import java.util.Calendar
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun PlannerScreen(balancePaise: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val online = com.invatech.pockit.ui.common.rememberOnline()
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val prefs = remember(uid) { context.getSharedPreferences("planner_$uid", 0) }
    var allowance by remember { mutableStateOf(prefs.getString("allowance", "").orEmpty()) }
    var day by remember { mutableStateOf(prefs.getInt("day", 1).toString()) }
    var reserved by remember { mutableStateOf(prefs.getString("reserved", "").orEmpty()) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(uid.isNotBlank()) }
    var saving by remember { mutableStateOf(false) }
    val ref = remember(uid) { if (uid.isBlank()) null else com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid).collection("settings").document("planner") }
    LaunchedEffect(uid, online) {
        if (!online) { loading = false; return@LaunchedEffect }
        try {
            val snapshot = ref?.get()?.await()
            if (snapshot?.exists() == true) {
                allowance = snapshot.getString("allowance").orEmpty()
                day = (snapshot.getLong("day") ?: 1).toString()
                reserved = snapshot.getString("reserved").orEmpty()
            } else if (ref != null && allowance.isNotBlank()) {
                ref.set(mapOf("allowance" to allowance, "day" to (day.toIntOrNull() ?: 1), "reserved" to reserved)).await()
            }
        } catch (e: Exception) { message = "Could not sync your plan: ${e.message}" }
        finally { loading = false }
    }
    val nextDay = day.toIntOrNull()
    val now = Calendar.getInstance()
    val next = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        set(Calendar.DAY_OF_MONTH, (nextDay ?: 1).coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
        if (timeInMillis <= now.timeInMillis) { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, 1); set(Calendar.DAY_OF_MONTH, (nextDay ?: 1).coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH))) }
    }
    val days = kotlin.math.ceil((next.timeInMillis - now.timeInMillis) / 86400000.0).toLong().coerceAtLeast(1)
    val reserve = if (reserved.isBlank() || reserved == "0") 0L else Money.parse(reserved)
    val available = (balancePaise - (reserve ?: 0L)).coerceAtLeast(0)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeading("Pocket-money planner", "Give your allowance a plan.", onBack) }
        item { FinanceCard(color = com.invatech.pockit.ui.theme.PockitTeal) {
            Text("Estimated daily room", style = MaterialTheme.typography.labelLarge)
            Text(if (nextDay in 1..31 && reserve != null) Money.format(available / days) else "Complete your plan", style = MaterialTheme.typography.headlineLarge)
            Text("Based on your recorded balance, reserved money and $days days until your next allowance. This is an estimate, not your live bank balance.", style = MaterialTheme.typography.bodySmall)
        } }
        item { FinanceCard {
            AmountField(allowance, { allowance = it; message = null }, "Expected monthly allowance (₹)")
            OutlinedTextField(day, { day = it; message = null }, label = { Text("Allowance day each month (1–31)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            AmountField(reserved, { reserved = it; message = null }, "Reserve for bills / savings (₹)")
            Text("Expected allowance is a plan only. Add it as income when it actually arrives.", style = MaterialTheme.typography.bodySmall)
            Button(enabled = online && !loading && !saving && ref != null && Money.parse(allowance) != null && nextDay in 1..31 && reserve != null, onClick = {
                saving = true
                scope.launch {
                    try {
                        ref!!.set(mapOf("allowance" to allowance, "day" to nextDay!!, "reserved" to reserved)).await()
                        prefs.edit().putString("allowance", allowance).putInt("day", nextDay!!).putString("reserved", reserved).apply()
                        message = "Plan synced to your account"
                    } catch (e: Exception) { message = "Plan not saved: ${e.message}" }
                    finally { saving = false }
                }
            }) { Text(if (!online) "Connect to save plan" else if (saving) "Saving…" else if (loading) "Syncing…" else "Save plan") }
            if (!online) Text("The calculator works offline. Connect to save your plan to your account.", style = MaterialTheme.typography.bodySmall)
            message?.let { Text(it) }
        } }
    }
}
