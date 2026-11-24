package com.invatech.pockit.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.data.local.database.FinanceRecord
import com.invatech.pockit.domain.Money
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FriendAddOptionsSheet(
    onDismiss: () -> Unit,
    onAddPersonal: () -> Unit,
    onScanFriend: () -> Unit,
    onShareCode: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFFFFFAFC)) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add a friend", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF27172F))
            Text("Choose how you want to track money together.", color = Color(0xFF756879), fontSize = 14.sp)
            FriendActionRow(
                title = "Add without connection",
                subtitle = "Create a private khata using only their name",
                icon = Icons.Rounded.PersonAdd,
                tint = Color(0xFFFF4A7E),
                background = Color(0xFFFFE3EC),
                onClick = onAddPersonal
            )
            FriendActionRow(
                title = "Scan friend's code",
                subtitle = "Connect both Pockit accounts instantly",
                icon = Icons.Rounded.QrCodeScanner,
                tint = Color(0xFF6F3DB2),
                background = Color(0xFFEFE3FF),
                onClick = onScanFriend
            )
            FriendActionRow(
                title = "Show my code",
                subtitle = "Let your friend scan and connect with you",
                icon = Icons.Rounded.QrCode2,
                tint = Color(0xFF078C62),
                background = Color(0xFFDDF8ED),
                onClick = onShareCode
            )
        }
    }
}

@Composable
private fun FriendActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    background: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(16.dp), color = background, modifier = Modifier.size(50.dp)) {
                Icon(icon, null, tint = tint, modifier = Modifier.padding(13.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2034))
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF776C7B), maxLines = 2)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PersonalFriendSheet(
    name: String,
    records: List<FinanceRecord>,
    balance: Long,
    busy: Boolean,
    onDismiss: () -> Unit,
    onAddEntry: () -> Unit,
    onDelete: (FinanceRecord) -> Unit
) {
    var deleting by remember { mutableStateOf<FinanceRecord?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFFFFFAFC)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color(0xFFEEDFFF), modifier = Modifier.size(54.dp)) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Text(name.firstOrNull()?.uppercase() ?: "?", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF66389A))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF27172F))
                    Text("Private khata · no connection required", fontSize = 12.sp, color = Color(0xFF776C7B))
                }
            }
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (balance >= 0) Color(0xFFE3F9EF) else Color(0xFFFFE4EC),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (balance >= 0) "You'll Get" else "You'll Give", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(Money.format(kotlin.math.abs(balance)), fontSize = 28.sp, fontWeight = FontWeight.Black, color = if (balance >= 0) Color(0xFF08A66F) else Color(0xFFFF3F78))
                }
            }
            Button(
                onClick = onAddEntry,
                enabled = !busy,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3F78)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp).height(52.dp)
            ) { Text("Add entry or repayment", fontWeight = FontWeight.Bold) }
            Text("Activity", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2D2034))
            if (records.isEmpty()) {
                Text("No entries yet.", color = Color(0xFF776C7B), modifier = Modifier.padding(vertical = 22.dp))
            } else {
                LazyColumn(Modifier.fillMaxWidth().height(280.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(records.sortedByDescending { it.occurredAt }, key = { it.id }) { row ->
                        Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 1.dp) {
                            Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(directionLabel(row.direction), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6E3DA5))
                                    Text(Money.format(row.amountPaise), fontSize = 18.sp, fontWeight = FontWeight.Black)
                                    if (row.note.isNotBlank()) Text(row.note, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(row.occurredAt)), fontSize = 10.sp, color = Color(0xFF817585))
                                }
                                Icon(Icons.Rounded.DeleteOutline, "Delete entry", tint = Color(0xFFC84E70), modifier = Modifier.size(23.dp).clickable { deleting = row })
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    deleting?.let { row ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete entry?") },
            text = { Text("This entry will be removed and the balance recalculated.") },
            confirmButton = { TextButton(enabled = !busy, onClick = { onDelete(row); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
internal fun RecordUdharDialog(
    initialName: String,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, Long, String, String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("LENT") }
    val parsedAmount = Money.parse(amount)
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (initialName.isBlank()) "Add without connection" else "Record with $initialName", fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, enabled = initialName.isBlank(), singleLine = true, label = { Text("Friend's name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    amount,
                    { amount = it.filter { char -> char.isDigit() || char == '.' } },
                    singleLine = true,
                    label = { Text("Amount") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(direction == "LENT", { direction = "LENT" }, label = { Text("I lent") })
                    FilterChip(direction == "BORROWED", { direction = "BORROWED" }, label = { Text("I borrowed") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(direction == "RECEIVED", { direction = "RECEIVED" }, label = { Text("Received back") })
                    FilterChip(direction == "REPAID", { direction = "REPAID" }, label = { Text("I repaid") })
                }
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
                if (!error.isNullOrBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && name.isNotBlank() && parsedAmount != null, onClick = { onSave(name, parsedAmount!!, direction, note) }) {
                Text(if (busy) "Saving…" else "Save")
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun directionLabel(direction: String): String = when (direction) {
    "LENT" -> "You lent"
    "BORROWED" -> "You borrowed"
    "RECEIVED" -> "Repayment received"
    else -> "You repaid"
}
