package com.invatech.pockit.ui.space.addexpense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.space.SpaceViewModel
import com.invatech.pockit.ui.space.components.AmountCard
import com.invatech.pockit.ui.space.components.PaidBySelector
import com.invatech.pockit.ui.space.components.PockitPrimaryButton
import com.invatech.pockit.ui.space.components.PockitTopBar
import com.invatech.pockit.ui.space.components.SectionTitle
import com.invatech.pockit.ui.space.components.SoftTextField
import com.invatech.pockit.ui.space.components.SplitMemberRow
import com.invatech.pockit.ui.space.components.SplitModeToggle
import com.invatech.pockit.ui.space.model.CurrentUserId
import com.invatech.pockit.ui.space.model.OwedColor
import com.invatech.pockit.ui.space.model.SplitMode
import com.invatech.pockit.ui.space.util.BalanceCalculator
import com.invatech.pockit.ui.space.util.formatMoney
import com.invatech.pockit.ui.theme.ErrorRed
import kotlin.math.abs

private val MoneyInput = Regex("""\d{0,7}(\.\d{0,2})?""")

@Composable
fun AddExpenseScreen(
    spaceId: String,
    viewModel: SpaceViewModel,
    onBack: () -> Unit
) {
    val spaces by viewModel.spaces.collectAsState()
    val space = spaces.firstOrNull { it.id == spaceId } ?: return
    val members = space.members

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var paidById by remember { mutableStateOf(CurrentUserId) }
    var mode by remember { mutableStateOf(SplitMode.Equal) }
    var selectedIds by remember { mutableStateOf(members.map { it.id }.toSet()) }
    val customAmounts = remember { mutableStateMapOf<String, String>() }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val shares: Map<String, Double> = when (mode) {
        SplitMode.Equal -> BalanceCalculator.splitEqually(
            amount = amount,
            ids = members.map { it.id }.filter { it in selectedIds }
        )
        SplitMode.Custom -> members.mapNotNull { member ->
            customAmounts[member.id]
                ?.toDoubleOrNull()
                ?.takeIf { it > 0 }
                ?.let { member.id to it }
        }.toMap()
    }
    val remaining = amount - shares.values.sum()
    val isBalanced = abs(remaining) < 0.01
    val isValid = title.isNotBlank() && amount > 0 && shares.isNotEmpty() && isBalanced

    val (statusText, statusColor) = when (mode) {
        SplitMode.Equal -> {
            val count = selectedIds.size
            if (count == 0) "Select at least one person" to ErrorRed
            else "Split equally between $count ${if (count == 1) "person" else "people"}" to
                MaterialTheme.colorScheme.onSurfaceVariant
        }
        SplitMode.Custom -> when {
            amount <= 0 -> "Enter the total amount first" to MaterialTheme.colorScheme.onSurfaceVariant
            isBalanced -> "All assigned ✓" to OwedColor
            remaining > 0 -> "${formatMoney(remaining)} left to assign" to MaterialTheme.colorScheme.onSurfaceVariant
            else -> "${formatMoney(-remaining)} over the total" to ErrorRed
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        PockitTopBar(title = "Add expense", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AmountCard(
                amountText = amountText,
                onAmountChange = { if (MoneyInput.matches(it)) amountText = it }
            )

            SoftTextField(
                value = title,
                onValueChange = { if (it.length <= 40) title = it },
                placeholder = "What was it for? e.g. Dinner",
                capitalization = KeyboardCapitalization.Sentences
            )

            SectionTitle("Paid by")
            PaidBySelector(
                members = members,
                selectedId = paidById,
                onSelect = { paidById = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("Split", modifier = Modifier.weight(1f))
                SplitModeToggle(
                    mode = mode,
                    onModeChange = { mode = it },
                    modifier = Modifier.width(190.dp)
                )
            }

            members.forEach { member ->
                SplitMemberRow(
                    member = member,
                    mode = mode,
                    selected = member.id in selectedIds,
                    equalShare = shares[member.id] ?: 0.0,
                    customText = customAmounts[member.id] ?: "",
                    onToggle = {
                        selectedIds = if (member.id in selectedIds) {
                            selectedIds - member.id
                        } else {
                            selectedIds + member.id
                        }
                    },
                    onCustomChange = { value ->
                        if (MoneyInput.matches(value)) customAmounts[member.id] = value
                    }
                )
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor
            )
        }

        Box(modifier = Modifier.padding(20.dp)) {
            PockitPrimaryButton(
                text = "Save expense",
                enabled = isValid,
                onClick = {
                    viewModel.addExpense(
                        spaceId = space.id,
                        title = title,
                        amount = amount,
                        paidById = paidById,
                        shares = shares
                    )
                    onBack()
                }
            )
        }
    }
}
