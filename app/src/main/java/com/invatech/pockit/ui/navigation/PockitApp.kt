package com.invatech.pockit.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.invatech.pockit.ui.home.AllTransactionsScreen
import com.invatech.pockit.ui.home.HomeScreen
import com.invatech.pockit.ui.home.HomeViewModel
import com.invatech.pockit.ui.profile.ProfileScreen
import com.invatech.pockit.ui.space.SpaceScreen
import com.invatech.pockit.ui.space.SpaceViewModel
import com.invatech.pockit.ui.space.addexpense.AddExpenseScreen
import com.invatech.pockit.ui.space.detail.SpaceDetailScreen

@Composable
fun PockitApp(
    onSignOut: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val homeViewModel: HomeViewModel = viewModel()
    val spaceViewModel: SpaceViewModel = viewModel()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            PockitBottomBar(
                currentRoute = currentRoute,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(280)) + slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { it / 6 }
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { -it / 6 }
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(280)) + slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { -it / 6 }
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { it / 6 }
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    homeViewModel = homeViewModel,
                    onViewAllTransactions = {
                        navController.navigate("all_transactions")
                    }
                )
            }
            composable("all_transactions") {
                AllTransactionsScreen(
                    homeViewModel = homeViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Space.route) {
                SpaceScreen(
                    viewModel = spaceViewModel,
                    onOpenSpace = { spaceId ->
                        navController.navigate("space_detail/$spaceId")
                    }
                )
            }
            composable("space_detail/{spaceId}") { backStackEntry ->
                val spaceId = backStackEntry.arguments?.getString("spaceId") ?: return@composable
                SpaceDetailScreen(
                    spaceId = spaceId,
                    viewModel = spaceViewModel,
                    onBack = { navController.popBackStack() },
                    onAddExpense = {
                        navController.navigate("space_detail/$spaceId/add_expense")
                    }
                )
            }
            composable("space_detail/{spaceId}/add_expense") { backStackEntry ->
                val spaceId = backStackEntry.arguments?.getString("spaceId") ?: return@composable
                AddExpenseScreen(
                    spaceId = spaceId,
                    viewModel = spaceViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(onSignOut = onSignOut)
            }
        }
    }
}
