package com.invatech.pockit.ui.space.addexpense

/*
 * Shared-expense form for selecting participants and allocating a bill.
 * The supported creation path uses the signed-in payer and requires shares to reconcile before submission.
 */

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.space.SpaceViewModel
import com.invatech.pockit.ui.space.components.AmountCard
import com.invatech.pockit.ui.space.components.MemberAvatar
import com.invatech.pockit.ui.space.components.PockitPrimaryButton
import com.invatech.pockit.ui.space.components.PockitTopBar
import com.invatech.pockit.ui.space.components.SectionTitle
import com.invatech.pockit.ui.space.components.SoftTextField
import com.invatech.pockit.ui.space.components.SplitMemberRow
import com.invatech.pockit.ui.space.components.SplitModeToggle
import com.invatech.pockit.ui.space.model.CurrentUserId
import com.invatech.pockit.ui.space.model.Member
import com.invatech.pockit.ui.theme.OwedColor
import com.invatech.pockit.ui.space.model.SplitMode
import com.invatech.pockit.ui.space.util.BalanceCalculator
import com.invatech.pockit.ui.space.util.formatMoney
import com.invatech.pockit.ui.theme.ErrorRed
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.Mint
import kotlin.math.abs

private val MoneyInput = Regex("""\d{0,7}(\.\d{0,2})?""")

/**
 * Collects a shared bill, allocation mode and participating members. Equal shares use the paise
 * splitter; custom shares must reconcile. This form and the repository currently record the
 * signed-in user as payer.
 */
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
    // Enforce that user can only add their own expense
    val paidById = CurrentUserId
    var mode by remember { mutableStateOf(SplitMode.Equal) }
    var selectedIds by remember { mutableStateOf(members.map { it.id }.toSet()) }
    val customAmounts = remember { mutableStateMapOf<String, String>() }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val shares: Map<String, Double> = if (space.isHousehold) {
        mapOf(paidById to amount)
    } else when (mode) {
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
    val isValid = if (space.isHousehold) {
        title.isNotBlank() && amount > 0
    } else {
        title.isNotBlank() && amount > 0 && shares.isNotEmpty() && isBalanced
    }

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
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val currentMember = members.firstOrNull { it.isYou } ?: Member(CurrentUserId, "You")
                    MemberAvatar(member = currentMember, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "You",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "You paid for this expense",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = Mint,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Payer",
                            color = Ink,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (space.isHousehold) {
                Surface(
                    color = Mint.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🏡", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Joint Household Expense",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "This expense will be added to your shared household total without creating debt splits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
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
