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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.invatech.pockit.ui.common.motion.M3Duration
import com.invatech.pockit.ui.common.motion.M3Easing
import com.invatech.pockit.ui.friends.FriendsScreen
import com.invatech.pockit.ui.home.AllTransactionsScreen
import com.invatech.pockit.ui.home.HomeScreen
import com.invatech.pockit.ui.home.HomeViewModel
import com.invatech.pockit.ui.notification.NotificationViewModel
import com.invatech.pockit.ui.notification.NotificationsScreen
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
    route == "all_transactions" || route == "notifications" || route.startsWith("space_detail") -> 1
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
        // Pattern 1: Lateral Transition (Moving forward/right across peer tabs)
        NavMotion.TabForward ->
            slideInHorizontally(tween(M3Duration.Medium4, easing = M3Easing.EmphasizedDecelerate)) {
                (it * 0.12f).toInt()
            } + scaleIn(
                animationSpec = tween(M3Duration.Medium4, easing = M3Easing.EmphasizedDecelerate),
                initialScale = 0.96f
            ) + fadeIn(tween(M3Duration.Medium2, delayMillis = M3Duration.Short2, easing = LinearOutSlowInEasing))

        // Pattern 1: Lateral Transition (Moving backward/left across peer tabs)
        NavMotion.TabBackward ->
            slideInHorizontally(tween(M3Duration.Medium4, easing = M3Easing.EmphasizedDecelerate)) {
                -(it * 0.12f).toInt()
            } + scaleIn(
                animationSpec = tween(M3Duration.Medium4, easing = M3Easing.EmphasizedDecelerate),
                initialScale = 0.96f
            ) + fadeIn(tween(M3Duration.Medium2, delayMillis = M3Duration.Short2, easing = LinearOutSlowInEasing))

        // Pattern 2: Forward Hierarchical Drill-In (Entering a detail screen from parent)
        NavMotion.DrillIn ->
            slideInHorizontally(tween(M3Duration.Long2, easing = M3Easing.EmphasizedDecelerate)) {
                it
            } + scaleIn(
                animationSpec = tween(M3Duration.Long2, easing = M3Easing.EmphasizedDecelerate),
                initialScale = 0.94f
            ) + fadeIn(tween(M3Duration.Medium3, easing = LinearOutSlowInEasing))

        // Pattern 2: Returning from Pop / Back Navigation
        NavMotion.DrillOut ->
            slideInHorizontally(tween(M3Duration.Medium4, easing = M3Easing.EmphasizedDecelerate)) {
                -(it * 0.28f).toInt()
            } + scaleIn(
                animationSpec = tween(M3Duration.Medium4, easing = M3Easing.EmphasizedDecelerate),
                initialScale = 0.94f
            ) + fadeIn(tween(M3Duration.Medium2, easing = LinearOutSlowInEasing))

        // Pattern 3: Modal Container Transform (Entering Add Expense modal)
        NavMotion.ModalIn ->
            slideInVertically(tween(M3Duration.Long2, easing = M3Easing.EmphasizedDecelerate)) {
                (it * 0.35f).toInt()
            } + scaleIn(
                animationSpec = tween(M3Duration.Long2, easing = M3Easing.EmphasizedDecelerate),
                initialScale = 0.92f
            ) + fadeIn(tween(M3Duration.Medium3, easing = LinearOutSlowInEasing))

        // Pattern 3: Returning from Modal Dismissal
        NavMotion.ModalOut ->
            scaleIn(
                animationSpec = tween(M3Duration.Medium4, easing = M3Easing.EmphasizedDecelerate),
                initialScale = 0.94f
            ) + fadeIn(tween(M3Duration.Medium2, easing = LinearOutSlowInEasing))
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pockitExit(): ExitTransition =
    when (currentMotion()) {
        // Pattern 1: Lateral Transition Exit (Forward)
        NavMotion.TabForward ->
            slideOutHorizontally(tween(M3Duration.Medium1, easing = M3Easing.EmphasizedAccelerate)) {
                -(it * 0.12f).toInt()
            } + scaleOut(
                animationSpec = tween(M3Duration.Medium1, easing = M3Easing.EmphasizedAccelerate),
                targetScale = 0.96f
            ) + fadeOut(tween(M3Duration.Short4))

        // Pattern 1: Lateral Transition Exit (Backward)
        NavMotion.TabBackward ->
            slideOutHorizontally(tween(M3Duration.Medium1, easing = M3Easing.EmphasizedAccelerate)) {
                (it * 0.12f).toInt()
            } + scaleOut(
                animationSpec = tween(M3Duration.Medium1, easing = M3Easing.EmphasizedAccelerate),
                targetScale = 0.96f
            ) + fadeOut(tween(M3Duration.Short4))

        // Pattern 2: Parent view receding to background during drill-in
        NavMotion.DrillIn ->
            slideOutHorizontally(tween(M3Duration.Medium3, easing = M3Easing.EmphasizedAccelerate)) {
                -(it * 0.28f).toInt()
            } + scaleOut(
                animationSpec = tween(M3Duration.Medium3, easing = M3Easing.EmphasizedAccelerate),
                targetScale = 0.94f
            ) + fadeOut(tween(M3Duration.Medium1, delayMillis = M3Duration.Short2))

        // Pattern 2: Detail view exiting during pop / back (back swipe animation)
        NavMotion.DrillOut ->
            slideOutHorizontally(tween(M3Duration.Medium3, easing = M3Easing.EmphasizedAccelerate)) {
                it
            } + scaleOut(
                animationSpec = tween(M3Duration.Medium3, easing = M3Easing.EmphasizedAccelerate),
                targetScale = 0.94f
            ) + fadeOut(tween(M3Duration.Medium1))

        // Pattern 3: Background view receding beneath modal
        NavMotion.ModalIn ->
            scaleOut(
                animationSpec = tween(M3Duration.Medium3, easing = M3Easing.EmphasizedAccelerate),
                targetScale = 0.92f
            ) + fadeOut(tween(M3Duration.Medium2, delayMillis = M3Duration.Short2))

        // Pattern 3: Modal sliding down during dismissal
        NavMotion.ModalOut ->
            slideOutVertically(tween(M3Duration.Medium3, easing = M3Easing.EmphasizedAccelerate)) {
                it
            } + scaleOut(
                animationSpec = tween(M3Duration.Medium3, easing = M3Easing.EmphasizedAccelerate),
                targetScale = 0.92f
            ) + fadeOut(tween(M3Duration.Medium1))
    }

@Composable
fun PockitApp(
    initialRoute: String? = null,
    onRouteHandled: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val homeViewModel: HomeViewModel = viewModel()
    val spaceViewModel: SpaceViewModel = viewModel()
    val notificationViewModel: NotificationViewModel = viewModel()
    val unreadNotificationCount by notificationViewModel.unreadCount.collectAsState()

    // Handle deep link or notification intent route
    LaunchedEffect(initialRoute) {
        if (!initialRoute.isNullOrBlank()) {
            navController.navigate(initialRoute) {
                launchSingleTop = true
            }
            onRouteHandled()
        }
    }

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
                    unreadNotificationCount = unreadNotificationCount,
                    onNotificationClick = {
                        navController.navigate("notifications")
                    },
                    onViewAllTransactions = {
                        navController.navigate("all_transactions")
                    }
                )
            }
            composable("notifications") {
                NotificationsScreen(
                    viewModel = notificationViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToSpace = { spaceId ->
                        navController.navigate("space_detail/$spaceId")
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