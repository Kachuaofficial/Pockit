package com.invatech.pockit.ui.friends

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.common.ads.PockitNativeAdCard
import com.invatech.pockit.ui.common.mascot.MascotPose
import com.invatech.pockit.ui.common.mascot.PockitMascot
import com.invatech.pockit.ui.common.motion.ExpressivePredictiveBackContainer
import com.invatech.pockit.ui.friends.model.ExpenseDirection
import com.invatech.pockit.ui.friends.model.FriendTransaction
import com.invatech.pockit.ui.friends.model.FriendWithBalance
import com.invatech.pockit.ui.friends.model.TransactionStatus
import com.invatech.pockit.ui.home.components.RecordDoodleBackdrop
import com.invatech.pockit.ui.space.util.formatMoney
import com.invatech.pockit.ui.theme.CocoaEspresso
import com.invatech.pockit.ui.theme.CocoaMuted
import com.invatech.pockit.ui.theme.ErrorRed
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.MintContainerDark
import com.invatech.pockit.ui.theme.MintDark
import com.invatech.pockit.ui.theme.MintDeep
import com.invatech.pockit.ui.theme.PeachContainerDark
import com.invatech.pockit.ui.theme.PeachDark
import kotlin.math.abs

@Composable
fun FriendDetailScreen(
    friendWithBalance: FriendWithBalance,
    currentUserId: String,
    onDismiss: () -> Unit,
    onAddExpense: (direction: ExpenseDirection) -> Unit,
    onSettleUp: () -> Unit,
    onConfirmTransaction: (String) -> Unit,
    onRejectTransaction: (String) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onRemoveFriend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val friend = friendWithBalance.friend
    val balance = friendWithBalance.netBalance
    val txs = friendWithBalance.transactions

    var showRemoveFriendDialog by remember { mutableStateOf(false) }

    if (showRemoveFriendDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveFriendDialog = false },
            title = {
                Text(
                    text = "Remove ${friend.name}?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove ${friend.name}? They will be removed from your friends list.",
                    color = InkMuted,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveFriendDialog = false
                        onRemoveFriend()
                    }
                ) {
                    Text("Remove", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveFriendDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    ExpressivePredictiveBackContainer(
        enabled = true,
        onBack = onDismiss,
        modifier = modifier
    ) {
        RecordDoodleBackdrop(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        com.invatech.pockit.ui.common.UserAvatar(friend.id, friend.name, Modifier.size(44.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = friend.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (friend.phone.isNotBlank()) {
                                Text(
                                    text = friend.phone,
                                    fontSize = 11.sp,
                                    color = InkMuted
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Cute mascot peeking from the top bar!
                        PockitMascot(
                            pose = MascotPose.COZY_PEEK,
                            size = 42.dp
                        )

                        // Share reminder button
                        IconButton(
                            onClick = {
                                val statusMsg = when {
                                    balance > 0 -> "outstanding balance is ${formatMoney(balance)} (owed to you)."
                                    balance < 0 -> "outstanding balance is ${formatMoney(abs(balance))} (you owe ${friend.name})."
                                    else -> "all dues are settled up!"
                                }
                                val textToShare = "Hi ${friend.name}, here is our Pockit statement:\n$statusMsg"
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, textToShare)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share with ${friend.name}")
                                context.startActivity(shareIntent)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Remove friend button
                        IconButton(
                            onClick = { showRemoveFriendDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonRemove,
                                contentDescription = "Remove Friend",
                                tint = ErrorRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val heroBg = when {
                    balance > 0.009 -> if (isDark) MintContainerDark else MaterialTheme.colorScheme.primaryContainer
                    balance < -0.009 -> if (isDark) PeachContainerDark else MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val heroTitleColor = when {
                    balance > 0.009 -> if (isDark) MintDark else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    balance < -0.009 -> if (isDark) PeachDark else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val heroAmountColor = when {
                    balance > 0.009 -> if (isDark) MintDark else MaterialTheme.colorScheme.onPrimaryContainer
                    balance < -0.009 -> if (isDark) PeachDark else MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }

                // Net Balance Hero Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(heroBg)
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = when {
                                balance > 0.009 -> "${friend.name.uppercase()} OWES YOU"
                                balance < -0.009 -> "YOU OWE ${friend.name.uppercase()}"
                                else -> "ALL DUES SETTLED"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = heroTitleColor,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = formatMoney(abs(balance)),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = heroAmountColor
                        )

                        if (abs(balance) >= 0.01) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable(onClick = onSettleUp)
                                    .padding(horizontal = 16.dp, vertical = 9.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Handshake,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Settle Up Full Balance",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Two-Way Truth: Pending Settlement Approvals
                val pendingSettlements = friendWithBalance.pendingSettlements
                if (pendingSettlements.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        pendingSettlements.forEach { pendingTx ->
                            val isIncoming = pendingTx.creatorId.isNotBlank() && pendingTx.creatorId != currentUserId

                            if (isIncoming) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isDark) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Handshake,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Settlement Confirmation Required",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "${friend.name} marked ${formatMoney(abs(pendingTx.amount))} as settled. Did you receive this payment?",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 18.sp
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { onRejectTransaction(pendingTx.id) },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                                                shape = CircleShape
                                            ) {
                                                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reject", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            Button(
                                                onClick = { onConfirmTransaction(pendingTx.id) },
                                                modifier = Modifier.weight(1.3f),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                                shape = CircleShape
                                            ) {
                                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Confirm & Settle", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("⏳", fontSize = 16.sp)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Settlement Pending Approval",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "You marked ${formatMoney(abs(pendingTx.amount))} as settled. Waiting for ${friend.name} to confirm receipt.",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Add Udhar Buttons (Maine Diya vs Maine Liya)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Lent
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onAddExpense(ExpenseDirection.GAVE_UDHAR) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) MintContainerDark else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = null,
                                tint = if (isDark) MintDark else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "Lent",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) MintDark else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Borrowed
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onAddExpense(ExpenseDirection.TOOK_UDHAR) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) PeachContainerDark else MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = null,
                                tint = if (isDark) PeachDark else MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "Borrowed",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) PeachDark else MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Transactions Ledger Section
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transaction History",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (txs.isNotEmpty()) {
                            Text(
                                text = "${txs.size} records",
                                fontSize = 12.sp,
                                color = InkMuted
                            )
                        }
                    }

                    if (txs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PockitMascot(
                                    pose = MascotPose.SLEEPING,
                                    size = 72.dp
                                )
                                Text(
                                    text = "No transactions with ${friend.name} yet.\nTap 'Lent' or 'Borrowed' to record.",
                                    fontSize = 13.sp,
                                    color = InkMuted,
                                    lineHeight = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        txs.forEachIndexed { index, tx ->
                            LedgerItemRow(
                                tx = tx,
                                currentUserId = currentUserId,
                                onConfirm = { onConfirmTransaction(tx.id) },
                                onReject = { onRejectTransaction(tx.id) },
                                onDelete = { onDeleteTransaction(tx.id) }
                            )
                            if (index == 7 && index < txs.lastIndex) {
                                PockitNativeAdCard(placementKey = "FriendDetailScreen.native.1", modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
}

@Composable
private fun LedgerItemRow(
    tx: FriendTransaction,
    currentUserId: String,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncomingPending = tx.status == TransactionStatus.PENDING &&
            tx.creatorId.isNotBlank() &&
            tx.creatorId != currentUserId

    val cleanTitle = remember(tx.description, tx.direction, tx.status) {
        if (tx.direction == ExpenseDirection.SETTLE_UP) {
            if (tx.description.isBlank() || tx.description.contains("settlement", ignoreCase = true)) {
                "Settlement"
            } else {
                tx.description
            }
        } else {
            tx.description.ifBlank {
                if (tx.direction == ExpenseDirection.GAVE_UDHAR) "Lent" else "Borrowed"
            }
        }
    }

    // Cheerful pastel color schemes consistent with Home TransactionItem & RecordDoodleUi
    val (cardBg, avatarBg, iconTint, amountColor) = remember(tx.direction, tx.status) {
        when {
            tx.status == TransactionStatus.REJECTED -> Tuple4(
                Color(0xFFFFF1F1),
                ErrorRed.copy(alpha = 0.15f),
                ErrorRed,
                InkMuted
            )
            tx.direction == ExpenseDirection.GAVE_UDHAR -> Tuple4(
                Color(0xFFDFF5E0), // Soft Mint
                Color(0xFF9EDDA5),
                Color(0xFF1E562B),
                Color(0xFF2E7D42)
            )
            tx.direction == ExpenseDirection.TOOK_UDHAR -> Tuple4(
                Color(0xFFFFDDD3), // Soft Coral/Blush
                Color(0xFFFFA690),
                Color(0xFF6B2D1E),
                Color(0xFFB33A24)
            )
            tx.direction == ExpenseDirection.SETTLE_UP -> Tuple4(
                Color(0xFFEDE6FB), // Soft Lavender
                Color(0xFFC9B8F3),
                Color(0xFF43306B),
                Color(0xFF5B458A)
            )
            else -> Tuple4(
                Color(0xFFEDE6FB),
                Color(0xFFC9B8F3),
                Color(0xFF43306B),
                Color(0xFF5B458A)
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                tx.status == TransactionStatus.REJECTED -> Icons.Rounded.Close
                                tx.direction == ExpenseDirection.SETTLE_UP -> Icons.Rounded.Handshake
                                tx.direction == ExpenseDirection.GAVE_UDHAR -> Icons.Rounded.ArrowDownward
                                tx.direction == ExpenseDirection.TOOK_UDHAR -> Icons.Rounded.ArrowUpward
                                else -> Icons.Rounded.CheckCircle
                            },
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = cleanTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = CocoaEspresso,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            // Compact status badge
                            when (tx.status) {
                                TransactionStatus.PENDING -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFFB27A).copy(alpha = 0.35f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "Pending",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8A3B14)
                                        )
                                    }
                                }
                                TransactionStatus.REJECTED -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ErrorRed.copy(alpha = 0.15f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "Rejected",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ErrorRed
                                        )
                                    }
                                }
                                TransactionStatus.CONFIRMED -> {
                                    if (tx.direction == ExpenseDirection.SETTLE_UP) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF9EDDA5).copy(alpha = 0.45f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "Settled ✓",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E562B)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = tx.formattedDate,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = CocoaMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when {
                            tx.status == TransactionStatus.REJECTED -> formatMoney(abs(tx.amount))
                            tx.direction == ExpenseDirection.GAVE_UDHAR -> "+${formatMoney(tx.amount)}"
                            tx.direction == ExpenseDirection.TOOK_UDHAR -> "-${formatMoney(tx.amount)}"
                            else -> formatMoney(abs(tx.amount))
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete",
                            tint = CocoaMuted.copy(alpha = 0.6f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Quick Inline Confirm / Reject Buttons for incoming pending transactions
            if (isIncomingPending) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Confirm payment?",
                        fontSize = 11.sp,
                        color = CocoaMuted,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    OutlinedButton(
                        onClick = onReject,
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Reject", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onConfirm,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D42), contentColor = Color.White),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Confirm", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
