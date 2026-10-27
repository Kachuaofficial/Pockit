package com.invatech.pockit.ui.space.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.space.SpaceViewModel
import com.invatech.pockit.ui.space.components.BalanceSummaryCard
import com.invatech.pockit.ui.space.components.CircleIconButton
import com.invatech.pockit.ui.space.components.FloatingAddExpense
import com.invatech.pockit.ui.space.components.InviteSheet
import com.invatech.pockit.ui.space.components.MembersCard
import com.invatech.pockit.ui.space.components.PockitTopBar
import com.invatech.pockit.ui.space.components.SectionTitle
import com.invatech.pockit.ui.space.components.SettleUpCard
import com.invatech.pockit.ui.space.components.SpaceExpenseItem
import com.invatech.pockit.ui.space.components.SpaceHeaderCard
import com.invatech.pockit.ui.space.model.CurrentUserId
import com.invatech.pockit.ui.space.util.BalanceCalculator

@Composable
fun SpaceDetailScreen(
    spaceId: String,
    viewModel: SpaceViewModel,
    onBack: () -> Unit,
    onAddExpense: () -> Unit
) {
    val spaces by viewModel.spaces.collectAsState()
    val space = spaces.firstOrNull { it.id == spaceId } ?: return
    var showInvite by remember { mutableStateOf(false) }

    val balances = remember(space) { BalanceCalculator.netBalances(space) }
    val settlements = remember(balances) { BalanceCalculator.settlements(balances) }
    val membersById = remember(space) { space.members.associateBy { it.id } }

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
                }
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { SpaceHeaderCard(space = space, onInvite = { showInvite = true }) }

                item { BalanceSummaryCard(myBalance = balances[CurrentUserId] ?: 0.0) }

                if (settlements.isNotEmpty()) {
                    item {
                        SettleUpCard(
                            settlements = settlements,
                            members = membersById,
                            onSettle = { viewModel.settle(space.id, it) }
                        )
                    }
                }

                item { MembersCard(members = space.members, balances = balances) }

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
                    items(space.expenses, key = { it.id }) { expense ->
                        SpaceExpenseItem(
                            expense = expense,
                            members = membersById,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
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
}
