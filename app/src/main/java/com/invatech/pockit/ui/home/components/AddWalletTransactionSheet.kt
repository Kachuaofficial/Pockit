package com.invatech.pockit.ui.home.components

/*
 * Income/expense entry form with category selection and optional receipt OCR suggestions. Converts reviewed text with Money before calling onSave.
 */

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invatech.pockit.data.ml.ReceiptScannerManager
import com.invatech.pockit.ui.common.loading.ExpressiveLoadingIndicator
import kotlinx.coroutines.launch
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

/**
 * Owns editable transaction text, type and category while the sheet is open. Receipt OCR can
 * pre-fill suggestions; Money parses the reviewed amount into paise before onSave. The parent
 * decides when a successful save dismisses the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWalletTransactionSheet(
    initialType: WalletTransactionType = WalletTransactionType.EXPENSE,
    initialTitle: String = "",
    initialAmountPaise: Long? = null,
    initialCategory: Category? = null,
    isEditing: Boolean = false,
    isLoading: Boolean = false,
    canSave: Boolean = true,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, amountPaise: Long, type: WalletTransactionType, category: String) -> Unit
) {
    var selectedType by remember(initialType) { mutableStateOf(initialType) }
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var amountText by remember(initialAmountPaise) {
        mutableStateOf(
            if (initialAmountPaise != null && initialAmountPaise > 0L) {
                com.invatech.pockit.domain.Money.decimal(initialAmountPaise)
            } else ""
        )
    }
    var selectedCategory by remember(initialCategory, initialType) {
        mutableStateOf(
            initialCategory ?: if (initialType == WalletTransactionType.INCOME) Category.Salary else Category.Food
        )
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    val rewardVersion by com.invatech.pockit.data.ads.RewardedFeatureAccess.updates.collectAsState()
    val receiptUnlocked = remember(rewardVersion) {
        com.invatech.pockit.data.ads.RewardedFeatureAccess.isUnlocked(com.invatech.pockit.data.ads.RewardedFeature.RECEIPT_SCAN)
    }
    var showScanUnlock by remember { mutableStateOf(false) }

    val receiptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && com.invatech.pockit.data.ads.RewardedFeatureAccess.isUnlocked(com.invatech.pockit.data.ads.RewardedFeature.RECEIPT_SCAN)) {
            coroutineScope.launch {
                isScanning = true
                val result = ReceiptScannerManager.scanReceipt(context, uri).getOrNull()
                isScanning = false
                if (result != null) {
                    result.amount?.let { amt ->
                        amountText = if (amt % 1.0 == 0.0) {
                            amt.toLong().toString()
                        } else {
                            String.format(java.util.Locale.ROOT, "%.2f", amt)
                        }
                    }
                    result.merchantName?.let { name ->
                        title = name
                    }
                    result.category?.let { cat ->
                        selectedCategory = cat
                    }
                }
            }
        }
    }

    val amountPaise = com.invatech.pockit.domain.Money.parse(amountText) ?: 0L

    val isValid = title.isNotBlank() && amountPaise > 0L && !isLoading && canSave

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
                    text = when {
                        isEditing && selectedType == WalletTransactionType.EXPENSE -> "Edit Expense"
                        isEditing -> "Edit Income"
                        selectedType == WalletTransactionType.EXPENSE -> "Add Expense"
                        else -> "Add Income"
                    },
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

            // Google ML Kit Smart Bill / Receipt Scanner Pill
            if (selectedType == WalletTransactionType.EXPENSE) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !isScanning) {
                            if(receiptUnlocked) receiptLauncher.launch("image/*")
                            else showScanUnlock=true
                        },
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isScanning) {
                                ExpressiveLoadingIndicator(modifier = Modifier.size(18.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.DocumentScanner,
                                    contentDescription = "Scan Receipt with ML Kit",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isScanning) "Reading receipt…" else "Scan Bill or Receipt",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Auto-extracts bill total, store name & category",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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

            // Primary Save / Update Button
            PockitPrimaryButton(
                text = when {
                    isEditing -> "Update Record"
                    selectedType == WalletTransactionType.EXPENSE -> "Save expense"
                    else -> "Save income"
                },
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
    if(showScanUnlock) com.invatech.pockit.ui.commerce.RewardedFeatureDialog(
        com.invatech.pockit.data.ads.RewardedFeature.RECEIPT_SCAN,{showScanUnlock=false})
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
