package com.invatech.pockit.ui.home.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.invatech.pockit.ui.theme.Cloud
import com.invatech.pockit.ui.theme.CloudDeep
import com.invatech.pockit.ui.theme.Lavender
import com.invatech.pockit.ui.theme.Mint
import com.invatech.pockit.ui.theme.Peach
import com.invatech.pockit.ui.theme.Rose

enum class Category(
    val label: String,
    val icon: ImageVector,
    val tint: Color
) {
    Food("Food", Icons.Rounded.Restaurant, Peach),
    Shopping("Shopping", Icons.Rounded.ShoppingBag, Lavender),
    Travel("Travel", Icons.Rounded.DirectionsCar, Mint),
    Entertainment("Entertainment", Icons.Rounded.Movie, Rose),
    Salary("Salary", Icons.Rounded.AccountBalanceWallet, Mint),
    Bills("Bills", Icons.Rounded.ReceiptLong, CloudDeep),
    Other("Other", Icons.Rounded.Category, Cloud)
}

data class Transaction(
    val id: Long,
    val title: String,
    val category: Category,
    val amount: Double,
    val isExpense: Boolean,
    val dateLabel: String,
    val stringId: String = ""
)

data class DaySpending(
    val label: String,
    val amount: Float
)
