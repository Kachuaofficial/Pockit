package com.invatech.pockit.ui.home.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Face
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.invatech.pockit.ui.theme.PastelApricot
import com.invatech.pockit.ui.theme.PastelCyan
import com.invatech.pockit.ui.theme.PastelLilac
import com.invatech.pockit.ui.theme.PastelMint
import com.invatech.pockit.ui.theme.PastelPeach
import com.invatech.pockit.ui.theme.PastelPink
import com.invatech.pockit.ui.theme.PastelSky
import com.invatech.pockit.ui.theme.PastelYellow

enum class Category(
    val label: String,
    val icon: ImageVector,
    val tint: Color
) {
    Food("Food", Icons.Rounded.Restaurant, PastelPeach),
    Groceries("Groceries", Icons.Rounded.ShoppingBag, PastelMint),
    Health("Health", Icons.Rounded.Spa, PastelMint),
    Income("Income", Icons.Rounded.AccountBalanceWallet, PastelMint),
    Greens("Greens", Icons.Rounded.Spa, PastelMint),
    Dessert("Dessert", Icons.Rounded.Cake, PastelPink),
    Car("Car", Icons.Rounded.DirectionsCar, PastelSky),
    Game("Game", Icons.Rounded.SportsEsports, PastelApricot),
    Cosmetic("Cosmetic", Icons.Rounded.Face, PastelLilac),
    Sport("Sport", Icons.Rounded.SportsSoccer, PastelCyan),
    Shopping("Shopping", Icons.Rounded.ShoppingBag, PastelLilac),
    Travel("Travel", Icons.Rounded.DirectionsCar, PastelSky),
    Entertainment("Entertainment", Icons.Rounded.Movie, PastelApricot),
    Salary("Salary", Icons.Rounded.AccountBalanceWallet, PastelMint),
    Bills("Bills", Icons.Rounded.ReceiptLong, PastelYellow),
    Other("Other", Icons.Rounded.Category, PastelPeach)
}

data class Transaction(
    val id: Long,
    val title: String,
    val category: Category,
    val amount: Double,
    val isExpense: Boolean,
    val dateLabel: String,
    val stringId: String = "",
    val timeLabel: String = "",
    val dateKey: String = "",
    val dateMonthDay: String = "",
    val dayOfWeek: String = ""
)

data class DaySpending(
    val label: String,
    val amount: Float
)

data class DateGroupedTransactions(
    val dateKey: String,
    val dateMonthDay: String,
    val dayOfWeek: String,
    val netIncome: Double,
    val netExpense: Double,
    val transactions: List<Transaction>
)
