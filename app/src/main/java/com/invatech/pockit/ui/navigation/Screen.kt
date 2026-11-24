package com.invatech.pockit.ui.navigation

/*
 * Route names, labels and icons used by navigation and the bottom bar.
 * Keeping route identifiers together prevents screens from inventing inconsistent destination strings.
 */

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String, val label: String, val icon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Rounded.Home)
    object Friends : Screen("friends", "Friends", Icons.Rounded.People)
    object Space : Screen("space", "Space", Icons.Rounded.Layers)
    object Transactions :
        Screen("all_transactions", "Transactions", Icons.AutoMirrored.Rounded.ReceiptLong)

    object Insights : Screen("insights", "Insights", Icons.Rounded.BarChart)
    object Budgets : Screen("budgets", "Budgets", Icons.Rounded.PieChart)
    object Profile : Screen("profile", "Profile", Icons.Rounded.Person)
}

val bottomBarScreens = listOf(
    Screen.Home,
    Screen.Transactions,
    Screen.Friends,
    Screen.Insights,
    Screen.Space
)
