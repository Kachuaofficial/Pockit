package com.invatech.pockit.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.invatech.pockit.ui.common.motion.EmphasizedAccelerate
import com.invatech.pockit.ui.common.motion.EmphasizedDecelerate
import com.invatech.pockit.ui.friends.FriendsScreen
import com.invatech.pockit.ui.home.AllTransactionsScreen
import com.invatech.pockit.ui.home.HomeScreen
import com.invatech.pockit.ui.home.HomeViewModel
import com.invatech.pockit.ui.profile.ProfileScreen
import com.invatech.pockit.ui.space.SpaceScreen
import com.invatech.pockit.ui.space.SpaceViewModel
import com.invatech.pockit.ui.space.addexpense.AddExpenseScreen
import com.invatech.pockit.ui.space.detail.SpaceDetailScreen

// ---------------------------------------------------------------------------
// Navigation motion
//
// Instead of one fade+scale for everything, the motion now tells the user WHERE
// they are going:
//   - switching tabs      -> short horizontal slide in the direction of the tab + fade-through
//   - opening a detail    -> new screen slides in, old one drifts back (parallax)
//   - going back          -> detail slides away, old screen drifts back into place
//   - add expense (modal) -> slides up, the screen beneath scales down for depth
//
// Everything is derived from the routes, so the same logic serves enter / exit /
// popEnter / popExit.
// ---------------------------------------------------------------------------

private const val NavDuration = 400

private enum class NavMotion {
    TabForward, TabBackward, DrillIn, DrillOut, ModalIn, ModalOut
}

/** 0 = tab root, 1 = detail screen, 2 = modal-style screen on top of a detail. */
private fun routeDepth(route: String?): Int = when {
    route == null -> 0
    route.endsWith("/add_expense") -> 2
    route == "all_transactions" || route.startsWith("space_detail") -> 1
    else -> 0
}

private fun tabIndexOf(route: String?): Int =
    bottomBarScreens.toList().indexOfFirst { it.route == route }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.currentMotion(): NavMotion {
    val fromRoute = initialState.destination.route
    val toRoute = targetState.destination.route
    val from = routeDepth(fromRoute)
    val to = routeDepth(toRoute)
    return when {
        to == 2 && from < 2 -> NavMotion.ModalIn
        from == 2 && to < 2 -> NavMotion.ModalOut
        to > from -> NavMotion.DrillIn
        to < from -> NavMotion.DrillOut
        tabIndexOf(toRoute) >= tabIndexOf(fromRoute) -> NavMotion.TabForward
        else -> NavMotion.TabBackward
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pockitEnter(): EnterTransition =
    when (currentMotion()) {
        NavMotion.TabForward ->
            slideInHorizontally(tween(NavDuration, easing = EmphasizedDecelerate)) {
                (it * 0.08f).toInt()
            } + fadeIn(tween(NavDuration / 2, delayMillis = 90, easing = LinearOutSlowInEasing))

        NavMotion.TabBackward ->
            slideInHorizontally(tween(NavDuration, easing = EmphasizedDecelerate)) {
                -(it * 0.08f).toInt()
            } + fadeIn(tween(NavDuration / 2, delayMillis = 90, easing = LinearOutSlowInEasing))

        NavMotion.DrillIn ->
            slideInHorizontally(tween(NavDuration, easing = EmphasizedDecelerate)) {
                (it * 0.25f).toInt()
            } + fadeIn(tween(NavDuration / 2, easing = LinearOutSlowInEasing))

        NavMotion.DrillOut ->
            slideInHorizontally(tween(NavDuration, easing = EmphasizedDecelerate)) {
                -(it * 0.10f).toInt()
            } + fadeIn(tween(NavDuration / 2, easing = LinearOutSlowInEasing))

        NavMotion.ModalIn ->
            slideInVertically(tween(NavDuration + 40, easing = EmphasizedDecelerate)) {
                (it * 0.15f).toInt()
            } + fadeIn(tween(NavDuration / 2, easing = LinearOutSlowInEasing))

        NavMotion.ModalOut ->
            scaleIn(
                animationSpec = tween(NavDuration, easing = EmphasizedDecelerate),
                initialScale = 0.95f
            ) + fadeIn(tween(NavDuration / 2, easing = LinearOutSlowInEasing))
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pockitExit(): ExitTransition =
    when (currentMotion()) {
        NavMotion.TabForward ->
            slideOutHorizontally(tween(NavDuration / 2, easing = EmphasizedAccelerate)) {
                -(it * 0.08f).toInt()
            } + fadeOut(tween(NavDuration / 3))

        NavMotion.TabBackward ->
            slideOutHorizontally(tween(NavDuration / 2, easing = EmphasizedAccelerate)) {
                (it * 0.08f).toInt()
            } + fadeOut(tween(NavDuration / 3))

        // The old screen stays visible under the incoming one, then fades late.
        NavMotion.DrillIn ->
            slideOutHorizontally(tween(NavDuration, easing = EmphasizedDecelerate)) {
                -(it * 0.10f).toInt()
            } + fadeOut(tween(NavDuration / 2, delayMillis = NavDuration / 4))

        NavMotion.DrillOut ->
            slideOutHorizontally(tween(NavDuration - 40, easing = EmphasizedAccelerate)) {
                (it * 0.30f).toInt()
            } + fadeOut(tween(NavDuration / 2))

        NavMotion.ModalIn ->
            scaleOut(
                animationSpec = tween(NavDuration, easing = EmphasizedDecelerate),
                targetScale = 0.95f
            ) + fadeOut(tween(NavDuration / 2, delayMillis = NavDuration / 4))

        NavMotion.ModalOut ->
            slideOutVertically(tween(NavDuration - 40, easing = EmphasizedAccelerate)) {
                (it * 0.15f).toInt()
            } + fadeOut(tween(NavDuration / 2))
    }

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
            enterTransition = { pockitEnter() },
            exitTransition = { pockitExit() },
            popEnterTransition = { pockitEnter() },
            popExitTransition = { pockitExit() }
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
            composable(Screen.Friends.route) {
                FriendsScreen()
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