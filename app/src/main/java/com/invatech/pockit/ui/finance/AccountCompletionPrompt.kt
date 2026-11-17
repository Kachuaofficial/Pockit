package com.invatech.pockit.ui.finance

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.invatech.pockit.domain.Money
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Ask only for required user-provided fields, never invent account balances. */
@Composable
fun AccountCompletionPrompt(onSaved: () -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = remember { FirebaseFirestore.getInstance() }
    var missingName by remember(uid) { mutableStateOf(false) }
    var missingBalance by remember(uid) { mutableStateOf(false) }
    var name by remember(uid) { mutableStateOf(FirebaseAuth.getInstance().currentUser?.displayName.orEmpty()) }
    var balance by remember(uid) { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val user = remember(uid) { db.collection("users").document(uid) }
    LaunchedEffect(uid, retry) {
        try {
            val profile = user.get(com.google.firebase.firestore.Source.SERVER).await()
            val wallet = user.collection("wallets").document("main").get(com.google.firebase.firestore.Source.SERVER).await()
            missingName = profile.getString("name").isNullOrBlank()
            missingBalance = wallet.getLong("balancePaise") == null
            error = null
        } catch (e: Exception) { error = "Couldn't check account setup. Connect and try again." }
    }
    if (missingName || missingBalance) AlertDialog(onDismissRequest = {}, title = { Text("Finish your account setup") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("These details are missing from your account. Save them once to use them across devices.")
            if (missingName) OutlinedTextField(name, { name = it }, label = { Text("Your name") }, singleLine = true)
            if (missingBalance) {
                AmountField(balance, { balance = it }, "Starting recorded balance (₹)")
                Text("Enter 0 to start fresh. This is not a live bank balance.", style = MaterialTheme.typography.bodySmall)
            }
            ErrorNotice(error)
        }
    }, confirmButton = { TextButton(enabled = !busy && (!missingName || name.isNotBlank()) && (!missingBalance || Money.parseBalance(balance) != null), onClick = {
        busy = true
        scope.launch {
            try {
                check(FirebaseAuth.getInstance().currentUser?.uid == uid)
                val wallet = user.collection("wallets").document("main")
                db.runTransaction { tx ->
                    val profile = tx.get(user)
                    val current = tx.get(wallet)
                    if (missingName && profile.getString("name").isNullOrBlank()) tx.set(user, mapOf("uid" to uid, "name" to name.trim()), SetOptions.merge())
                    if (missingBalance && current.getLong("balancePaise") == null) tx.set(wallet, mapOf("balancePaise" to Money.parseBalance(balance)!!), SetOptions.merge())
                }.await()
                missingName = false; missingBalance = false; onSaved()
            } catch (e: Exception) { error = "Couldn't save setup: ${e.message}" }
            finally { busy = false }
        }
    }) { Text(if (busy) "Saving…" else "Save & continue") } })
    else if (error != null) AlertDialog(onDismissRequest = { error = null }, title = { Text("Account connection") }, text = { Text(error.orEmpty()) },
        confirmButton = { TextButton(onClick = { retry++ }) { Text("Retry") } }, dismissButton = { TextButton(onClick = { error = null }) { Text("Later") } })
}
