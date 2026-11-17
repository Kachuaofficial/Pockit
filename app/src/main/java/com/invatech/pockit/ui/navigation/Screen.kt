package com.invatech.pockit.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SupervisedUserCircle
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Rounded.Home)
    object Friends : Screen("friends", "Friends", Icons.Rounded.People)
    object Space : Screen("space", "Spaces", Icons.Rounded.RocketLaunch)
    object Transactions : Screen("all_transactions", "Activity", Icons.AutoMirrored.Rounded.ReceiptLong)
    object Budgets : Screen("budgets", "Budgets", Icons.Rounded.PieChart)
    object Profile : Screen("profile", "Profile", Icons.Rounded.Person)
}

val bottomBarScreens = listOf(Screen.Home, Screen.Space, Screen.Friends, Screen.Profile)
