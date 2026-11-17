package com.invatech.pockit.ui.space.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.common.ads.PockitNativeAdCard
import com.invatech.pockit.ui.common.swipe.SwipeToDeleteContainer
import com.invatech.pockit.ui.space.SpaceViewModel
import com.invatech.pockit.ui.space.components.BalanceSummaryCard
import com.invatech.pockit.ui.space.components.CircleIconButton
import com.invatech.pockit.ui.space.components.FloatingAddExpense
import com.invatech.pockit.ui.space.components.HouseholdSummaryCard
import com.invatech.pockit.ui.space.components.InviteSheet
import com.invatech.pockit.ui.space.components.MembersCard
import com.invatech.pockit.ui.space.components.PockitPrimaryButton
import com.invatech.pockit.ui.space.components.PockitTopBar
import com.invatech.pockit.ui.space.components.SectionTitle
import com.invatech.pockit.ui.space.components.SettleUpCard
import com.invatech.pockit.ui.space.components.SoftTextField
import com.invatech.pockit.ui.space.components.SpaceExpenseItem
import com.invatech.pockit.ui.space.components.SpaceHeaderCard
import com.invatech.pockit.ui.space.model.CurrentUserId
import com.invatech.pockit.ui.space.model.Member
import com.invatech.pockit.ui.theme.OwedColor
import com.invatech.pockit.ui.space.model.SpaceExpense
import com.invatech.pockit.ui.space.util.BalanceCalculator
import com.invatech.pockit.ui.space.util.formatMoney
import com.invatech.pockit.ui.theme.Cloud
import com.invatech.pockit.ui.theme.ErrorRed
import com.invatech.pockit.ui.theme.ErrorSoft
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.Mint
import com.invatech.pockit.ui.theme.MintDeep
import com.invatech.pockit.ui.theme.Peach
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceDetailScreen(
    spaceId: String,
    viewModel: SpaceViewModel,
    onBack: () -> Unit,
    onAddExpense: () -> Unit
) {
    val context = LocalContext.current
    val spaces by viewModel.spaces.collectAsState()
    val space = spaces.firstOrNull { it.id == spaceId } ?: return

    var showInvite by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showRenameSheet by remember { mutableStateOf(false) }
    var renameName by remember(space.name) { mutableStateOf(space.name) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<Member?>(null) }
    var expenseToDelete by remember { mutableStateOf<SpaceExpense?>(null) }

    val balanceResult = remember(space) { runCatching { BalanceCalculator.netBalances(space) } }
    val balances = balanceResult.getOrDefault(emptyMap())
    val settlementResult = remember(balances) { runCatching { BalanceCalculator.settlements(balances) } }
    val settlements = settlementResult.getOrDefault(emptyList())
    val reconciliationError = balanceResult.exceptionOrNull() ?: settlementResult.exceptionOrNull()
    val membersById = remember(space) { space.members.associateBy { it.id } }

    val pendingSettlements = remember(space.expenses) {
        space.expenses.filter { it.isSettlement && !it.isConfirmed }
    }
    val pendingSettlementKeys = remember(pendingSettlements) {
        pendingSettlements.map { "${it.paidById}_${it.shares.keys.firstOrNull() ?: ""}" }.toSet()
    }
    val pendingForMe = remember(pendingSettlements) {
        pendingSettlements.filter { it.shares.containsKey(CurrentUserId) }
    }
    val pendingByMe = remember(pendingSettlements) {
        pendingSettlements.filter { it.paidById == CurrentUserId }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PockitTopBar(
                title = space.name,
                onBack = onBack,
                actions = {
                    CircleIconButton(
                        icon = Icons.Rounded.PersonAdd,
                        contentDescription = "Invite",
                        onClick = { showInvite = true }
                    )
                    Box {
                        CircleIconButton(
                            icon = Icons.Rounded.MoreVert,
                            contentDescription = "Options",
                            onClick = { showOptionsMenu = true }
                        )
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            shape = RoundedCornerShape(18.dp),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Rename space",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    renameName = space.name
                                    showRenameSheet = true
                                }
                            )

                            if (space.isCreator) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Delete space",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ErrorRed
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = null,
                                            tint = ErrorRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (reconciliationError != null) item {
                    Text("These expense shares do not reconcile. Check the entries before settling up.", color = MaterialTheme.colorScheme.error)
                }
                item {
                    SpaceHeaderCard(
                        space = space,
                        onInvite = { showInvite = true },
                        onRename = {
                            renameName = space.name
                            showRenameSheet = true
                        }
                    )
                }

                if (space.isHousehold) {
                    item {
                        HouseholdSummaryCard(space = space)
                    }
                } else {
                    item { BalanceSummaryCard(myBalance = balances[CurrentUserId] ?: 0.0) }

                // Pending payments that need current user's confirmation
                if (pendingForMe.isNotEmpty()) {
                    item {
                        Surface(
                            color = Mint.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(26.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "🔔", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Payment Confirmations (${pendingForMe.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Confirm once you have verified the payment in your bank or wallet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkMuted
                                )

                                pendingForMe.forEach { pendingTx ->
                                    val sender = membersById[pendingTx.paidById]
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${sender?.displayName ?: "A member"} paid you",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Marked ${pendingTx.dateLabel}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = InkMuted
                                                    )
                                                }
                                                Text(
                                                    text = formatMoney(pendingTx.amount),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = OwedColor
                                                )
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Button(
                                                    onClick = {
                                                        viewModel.confirmSettlement(space.id, pendingTx.id) { err ->
                                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Mint,
                                                        contentColor = Ink
                                                    ),
                                                    shape = RoundedCornerShape(16.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = "Confirm Received",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }

                                                TextButton(
                                                    onClick = {
                                                        viewModel.rejectSettlement(space.id, pendingTx.id) { err ->
                                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = "Not Received",
                                                        color = ErrorRed,
                                                        fontWeight = FontWeight.SemiBold,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Notice if current user sent a payment waiting for the other person's approval
                if (pendingByMe.isNotEmpty()) {
                    item {
                        Surface(
                            color = Peach.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = "⏳", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "Payment Waiting for Approval",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Ink
                                    )
                                }
                                pendingByMe.forEach { myPending ->
                                    val receiver = myPending.shares.keys.firstOrNull()?.let { membersById[it] }
                                    Text(
                                        text = "You marked ${formatMoney(myPending.amount)} paid to ${receiver?.displayName ?: "member"}. Balance settles once they confirm.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Ink.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (settlements.isNotEmpty()) {
                    item {
                        SettleUpCard(
                            settlements = settlements,
                            members = membersById,
                            pendingSettlementKeys = pendingSettlementKeys,
                            onSettle = { settlement ->
                                viewModel.settle(space.id, settlement)
                                Toast.makeText(
                                    context,
                                    "Payment marked! Waiting for ${membersById[settlement.toId]?.displayName ?: "receiver"} to confirm.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
                }

                item {
                    MembersCard(
                        members = space.members,
                        balances = balances,
                        isCreator = space.isCreator,
                        creatorId = space.createdBy,
                        isHousehold = space.isHousehold,
                        onRemoveMember = { member ->
                            memberToRemove = member
                        }
                    )
                }

                item { SectionTitle("Expenses") }

                if (space.expenses.isEmpty()) {
                    item {
                        Text(
                            text = "No expenses yet. Tap \"Add expense\" to split your first bill.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    itemsIndexed(space.expenses, key = { _, expense -> expense.id }) { index, expense ->
                        // Only the transaction owner who paid for the expense can delete it
                        val isOwner = expense.paidById == CurrentUserId
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SwipeToDeleteContainer(
                                itemKey = expense.id,
                                shape = RoundedCornerShape(24.dp),
                                enabled = isOwner,
                                requireConfirmation = true,
                                onDelete = {
                                    if (isOwner) {
                                        expenseToDelete = expense
                                    }
                                },
                                modifier = Modifier.animateItem()
                            ) {
                                SpaceExpenseItem(
                                    expense = expense,
                                    members = membersById
                                )
                            }
                            if (index == 7 && index < space.expenses.lastIndex) {
                                PockitNativeAdCard(placementKey = "SpaceDetailScreen.native.1", modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 86.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                actionColor = MintDeep,
                shape = RoundedCornerShape(18.dp)
            )
        }

        FloatingAddExpense(
            onClick = onAddExpense,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }

    if (showInvite) {
        InviteSheet(
            space = space,
            onDismiss = { showInvite = false }
        )
    }

    // Rename Space Bottom Sheet
    if (showRenameSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRenameSheet = false },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(space.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "Rename Space",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                SoftTextField(
                    value = renameName,
                    onValueChange = { renameName = it },
                    placeholder = "Space name",
                    containerColor = MaterialTheme.colorScheme.surface
                )

                PockitPrimaryButton(
                    text = "Save changes",
                    enabled = renameName.isNotBlank(),
                    onClick = {
                        if (renameName.isNotBlank()) {
                            viewModel.renameSpace(space.id, renameName.trim())
                            showRenameSheet = false
                        }
                    }
                )

                TextButton(
                    onClick = { showRenameSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkMuted
                    )
                }
            }
        }
    }

    // Delete Expense Confirmation Alert Dialog
    expenseToDelete?.let { targetExpense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
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
                    text = "Delete Expense?",
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
                        text = "Are you sure you want to delete this expense? All balances will be recalculated.",
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
                                text = targetExpense.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatMoney(targetExpense.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = ErrorRed
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val exp = targetExpense
                        expenseToDelete = null
                        viewModel.deleteExpense(space.id, exp.id)
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = "\"${exp.title}\" deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreExpense(space.id, exp)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
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
                TextButton(
                    onClick = { expenseToDelete = null }
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

    // Remove Member Confirmation Sheet
    memberToRemove?.let { targetMember ->
        ModalBottomSheet(
            onDismissRequest = { memberToRemove = null },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ErrorSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PersonRemove,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = "Remove ${targetMember.displayName}?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Are you sure you want to remove ${targetMember.displayName} from ${space.name}? They will no longer be able to view or split expenses in this space.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = ErrorRed,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            viewModel.removeMember(space.id, targetMember.id) { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                            memberToRemove = null
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Remove member",
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(
                    onClick = { memberToRemove = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkMuted
                    )
                }
            }
        }
    }

    // Delete Space Confirmation Sheet
    if (showDeleteDialog) {
        ModalBottomSheet(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ErrorSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Delete \"${space.name}\"?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "This will permanently delete this space along with all expenses, settlement records, and history for all members. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = ErrorRed,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            showDeleteDialog = false
                            viewModel.deleteSpace(space.id)
                            onBack()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Delete space permanently",
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkMuted
                    )
                }
            }
        }
    }
}
