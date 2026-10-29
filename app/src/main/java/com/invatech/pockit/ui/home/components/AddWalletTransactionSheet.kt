package com.invatech.pockit.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.common.loading.ExpressiveLoadingIndicator
import com.invatech.pockit.ui.home.model.Category
import com.invatech.pockit.ui.home.model.WalletTransactionType
import com.invatech.pockit.ui.space.components.AmountCard
import com.invatech.pockit.ui.space.components.PockitPrimaryButton
import com.invatech.pockit.ui.space.components.SoftTextField
import com.invatech.pockit.ui.theme.Cloud
import com.invatech.pockit.ui.theme.ErrorRed
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.Mint
import com.invatech.pockit.ui.theme.Peach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWalletTransactionSheet(
    initialType: WalletTransactionType = WalletTransactionType.EXPENSE,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, amountPaise: Long, type: WalletTransactionType, category: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember {
        mutableStateOf(if (initialType == WalletTransactionType.INCOME) Category.Salary else Category.Food)
    }

    val amountPaise = runCatching {
        amountText.toBigDecimal()
            .movePointRight(2)
            .longValueExact()
    }.getOrNull() ?: 0L

    val isValid = title.isNotBlank() && amountPaise > 0L && !isLoading

    ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Type Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (selectedType == WalletTransactionType.EXPENSE) "Add Expense" else "Add Income",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Type Toggle Pill (Expense / Income)
                Surface(
                    color = Cloud,
                    shape = CircleShape
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        TypeToggleOption(
                            label = "Expense",
                            selected = selectedType == WalletTransactionType.EXPENSE,
                            selectedColor = Peach,
                            onClick = {
                                selectedType = WalletTransactionType.EXPENSE
                                if (selectedCategory == Category.Salary) {
                                    selectedCategory = Category.Food
                                }
                            }
                        )
                        TypeToggleOption(
                            label = "Income",
                            selected = selectedType == WalletTransactionType.INCOME,
                            selectedColor = Mint,
                            onClick = {
                                selectedType = WalletTransactionType.INCOME
                                selectedCategory = Category.Salary
                            }
                        )
                    }
                }
            }

            // Amount Input Card
            AmountCard(
                amountText = amountText,
                onAmountChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                        amountText = input
                    }
                }
            )

            // Description Input
            SoftTextField(
                value = title,
                onValueChange = { if (it.length <= 40) title = it },
                placeholder = if (selectedType == WalletTransactionType.EXPENSE) "What was it for? e.g. Groceries" else "Source, e.g. Freelance / Salary"
            )

            // Category Chips Selector
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Category.entries.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "category_scale"
                    )

                    Surface(
                        color = if (isSelected) cat.tint else MaterialTheme.colorScheme.surface,
                        shape = CircleShape,
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        modifier = Modifier
                            .scale(scale)
                            .clip(CircleShape)
                            .clickable { selectedCategory = cat }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = cat.label,
                                tint = Ink,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = Ink
                            )
                        }
                    }
                }
            }

            // Error display
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + slideInVertically()
            ) {
                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelMedium,
                        color = ErrorRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Primary Save Button
            PockitPrimaryButton(
                text = if (selectedType == WalletTransactionType.EXPENSE) "Save expense" else "Save income",
                enabled = isValid,
                onClick = {
                    onSave(
                        title.trim(),
                        amountPaise,
                        selectedType,
                        selectedCategory.name
                    )
                }
            )
        }
    }
}

@Composable
private fun TypeToggleOption(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) selectedColor else Color.Transparent,
        label = "toggle_bg"
    )

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
