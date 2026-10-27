package com.invatech.pockit.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.SupervisedUserCircle
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Rounded.Home)
    object Space : Screen("space", "Space", Icons.Rounded.Groups)
    object Profile : Screen("profile", "Profile", Icons.Rounded.SupervisedUserCircle)
}

val bottomBarScreens = listOf(Screen.Home, Screen.Space, Screen.Profile)
