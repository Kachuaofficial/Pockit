package com.invatech.pockit.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.home.components.TransactionItem
import com.invatech.pockit.ui.home.model.Category
import com.invatech.pockit.ui.home.model.Transaction
import com.invatech.pockit.ui.home.model.WalletTransactionType
import com.invatech.pockit.ui.space.components.PockitTopBar
import com.invatech.pockit.ui.space.components.SoftTextField
import com.invatech.pockit.ui.theme.Cloud
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.Mint
import com.invatech.pockit.ui.theme.Peach
import java.util.Locale

enum class TransactionFilter {
    ALL, EXPENSE, INCOME
}

@Composable
fun AllTransactionsScreen(
    homeViewModel: HomeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by homeViewModel.uiState.collectAsState()

    var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val allTransactions = remember(uiState.transactions) {
        uiState.transactions.map { item ->
            Transaction(
                id = item.id.hashCode().toLong(),
                title = item.title,
                category = Category.entries.firstOrNull {
                    it.name.equals(item.category, ignoreCase = true)
                } ?: Category.Food,
                amount = item.amountPaise / 100.0,
                isExpense = item.type == WalletTransactionType.EXPENSE,
                dateLabel = item.createdAt?.toDate()?.let {
                    java.text.SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(it)
                } ?: "Recent"
            )
        }
    }

    val filteredTransactions = remember(allTransactions, selectedFilter, searchQuery) {
        allTransactions.filter { tx ->
            val matchesFilter = when (selectedFilter) {
                TransactionFilter.ALL -> true
                TransactionFilter.EXPENSE -> tx.isExpense
                TransactionFilter.INCOME -> !tx.isExpense
            }
            val matchesSearch = searchQuery.isBlank() ||
                    tx.title.contains(searchQuery, ignoreCase = true) ||
                    tx.category.label.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PockitTopBar(
            title = "All Transactions",
            onBack = onBack
        )

        // Filter Pills & Search Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            SoftTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search transactions...",
                modifier = Modifier.fillMaxWidth()
            )

            // Filter Tabs (All / Expenses / Income)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTabPill(
                    label = "All (${allTransactions.size})",
                    selected = selectedFilter == TransactionFilter.ALL,
                    activeColor = Ink,
                    activeTextColor = Color.White,
                    onClick = { selectedFilter = TransactionFilter.ALL },
                    modifier = Modifier.weight(1f)
                )
                FilterTabPill(
                    label = "Expenses",
                    selected = selectedFilter == TransactionFilter.EXPENSE,
                    activeColor = Peach,
                    activeTextColor = Ink,
                    onClick = { selectedFilter = TransactionFilter.EXPENSE },
                    modifier = Modifier.weight(1f)
                )
                FilterTabPill(
                    label = "Income",
                    selected = selectedFilter == TransactionFilter.INCOME,
                    activeColor = Mint,
                    activeTextColor = Ink,
                    onClick = { selectedFilter = TransactionFilter.INCOME },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Transactions List
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Cloud),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = InkMuted,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No transactions found matching '$searchQuery'" else "No transactions in this category yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 4.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionItem(
                        transaction = tx,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterTabPill(
    label: String,
    selected: Boolean,
    activeColor: Color,
    activeTextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pill_scale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (selected) activeColor else Cloud,
        label = "pill_bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) activeTextColor else InkMuted,
        label = "pill_text"
    )

    Surface(
        color = bgColor,
        shape = CircleShape,
        modifier = modifier
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
        )
    }
}
