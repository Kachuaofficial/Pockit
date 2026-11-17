package com.invatech.pockit.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import com.invatech.pockit.ui.finance.*
import com.invatech.pockit.ui.common.ads.LocalScreenAdSlots
import com.invatech.pockit.ui.common.ads.rememberScreenAdSlots
import com.invatech.pockit.ui.common.motion.LocalNavAnimatedVisibilityScope
import com.invatech.pockit.ui.common.motion.LocalSharedTransitionScope
import com.invatech.pockit.ui.common.motion.M3NavMotion
import com.invatech.pockit.ui.common.motion.MotionSettings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.invatech.pockit.ui.budget.BudgetScreen
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PockitApp(
    initialRoute: String? = null,
    openAddExpense: Boolean = false,
    onRouteHandled: () -> Unit = {},
    onAddExpenseHandled: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val animations by MotionSettings.enabled.collectAsState()
    val onboardingPrefs = remember { context.getSharedPreferences("pockit_onboarding", 0) }
    var onboarded by rememberSaveable { mutableStateOf(onboardingPrefs.getBoolean("liquid_v2", false)) }
    if (!onboarded) {
        OnboardingScreen { onboardingPrefs.edit().putBoolean("liquid_v2", true).apply(); onboarded = true }
        return
    }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    var accountName by remember(uid) { mutableStateOf(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName ?: "there") }
    DisposableEffect(uid) {
        val profileListener = if (uid.isNotBlank()) com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid)
            .addSnapshotListener { snapshot, _ -> snapshot?.getString("name")?.takeIf { it.isNotBlank() }?.let { accountName = it } } else null
        val listener = if (uid.isNotBlank()) com.invatech.pockit.data.repository.AccountPreferences.bind(context, uid) else null
        onDispose { listener?.remove(); profileListener?.remove() }
    }
    val homeViewModel: HomeViewModel = viewModel(key = "home_$uid")
    val spaceViewModel: SpaceViewModel = viewModel(key = "space_$uid")
    val notificationViewModel: NotificationViewModel = viewModel(key = "notifications_$uid")
    val unreadNotificationCount by notificationViewModel.unreadCount.collectAsState()

    // Handle deep link or notification intent route
    LaunchedEffect(initialRoute) {
        if (!initialRoute.isNullOrBlank()) {
            val safeRoute = when {
                initialRoute in setOf("home", "friends", "space", "profile", "notifications", "all_transactions", "budgets", "upi_review", "goals", "planner") -> initialRoute
                Regex("space_detail/[A-Za-z0-9_-]+").matches(initialRoute) -> initialRoute
                else -> "home"
            }
            navController.navigate(safeRoute) {
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
        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalSharedTransitionScope provides this
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.padding(innerPadding),
                    enterTransition = { M3NavMotion.enter(animations) },
                    exitTransition = { M3NavMotion.exit(animations) },
                    popEnterTransition = { M3NavMotion.popEnter(animations) },
                    popExitTransition = { M3NavMotion.popExit(animations) }
                ) {
                    composable(Screen.Home.route) {
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable, LocalScreenAdSlots provides rememberScreenAdSlots()) {
                            androidx.activity.compose.BackHandler(enabled = navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                            HomeScreen(
                                homeViewModel = homeViewModel,
                                userName = accountName,
                                unreadNotificationCount = unreadNotificationCount,
                                openAddSheetOnLaunch = openAddExpense,
                                onAddSheetLaunched = onAddExpenseHandled,
                                onNotificationClick = {
                                    navController.navigate("notifications")
                                },
                                onViewAllTransactions = {
                                    navController.navigate("all_transactions")
                                },
                                onSettings = { navController.navigate("profile") },
                                onSpaces = { navController.navigate("space") },
                                onGoals = { navController.navigate("goals") },
                                onReview = { navController.navigate("upi_review") },
                                onPlanner = { navController.navigate("planner") },
                                onNavigateToBudgets = {
                                    navController.navigate("budgets")
                                }
                            )
                        }
                    }
                    composable("notifications") {
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable, LocalScreenAdSlots provides rememberScreenAdSlots()) {
                            androidx.activity.compose.BackHandler(enabled = navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                            NotificationsScreen(
                                viewModel = notificationViewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateToSpace = { spaceId ->
                                    navController.navigate("space_detail/$spaceId")
                                }
                            )
                        }
                    }
                    composable("all_transactions") {
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable, LocalScreenAdSlots provides rememberScreenAdSlots()) {
                            androidx.activity.compose.BackHandler(enabled = navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                            AllTransactionsScreen(
                                homeViewModel = homeViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    composable("upi_review") {
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable, LocalScreenAdSlots provides rememberScreenAdSlots()) {
                            androidx.activity.compose.BackHandler(enabled = navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                            UpiReviewScreen(onBack = { navController.popBackStack() })
                        }
                    }
                    composable("goals") {
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable, LocalScreenAdSlots provides rememberScreenAdSlots()) {
                            androidx.activity.compose.BackHandler(enabled = navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                            GoalsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                    composable("planner") {
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable, LocalScreenAdSlots provides rememberScreenAdSlots()) {
                            androidx.activity.compose.BackHandler(enabled = navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                            val wallet by homeViewModel.uiState.collectAsState()
                            PlannerScreen(wallet.balancePaise, onBack = { navController.popBackStack() })
                        }
                    }
                    composable("connected_friends") {
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable, LocalScreenAdSlots provides rememberScreenAdSlots()) {
                            androidx.activity.compose.BackHandler(enabled = navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                            com.invatech.pockit.ui.friends.ConnectedFriendsContent()
                        }
                    }
                    composable("budgets") {
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable, LocalScreenAdSlots provides rememberScreenAdSlots()) {
                            androidx.activity.compose.BackHandler(enabled = navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                            BudgetScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    composable(Screen.Friends.route) {
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable, LocalScreenAdSlots provides rememberScreenAdSlots()) {
                            androidx.activity.compose.BackHandler(enabled = navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                            FriendsScreen()
                        }
                    }
                    composable(Screen.Space.route) {
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable, LocalScreenAdSlots provides rememberScreenAdSlots()) {
                            androidx.activity.compose.BackHandler(enabled = navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                            SpaceScreen(
                                viewModel = spaceViewModel,
                                onOpenSpace = { spaceId ->
                                    navController.navigate("space_detail/$spaceId")
                                }
                            )
                        }
                    }
                    composable("space_detail/{spaceId}") { backStackEntry ->
                        val spaceId = backStackEntry.arguments?.getString("spaceId") ?: return@composable
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable, LocalScreenAdSlots provides rememberScreenAdSlots()) {
                            androidx.activity.compose.BackHandler(enabled = navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                            SpaceDetailScreen(
                                spaceId = spaceId,
                                viewModel = spaceViewModel,
                                onBack = { navController.popBackStack() },
                                onAddExpense = {
                                    navController.navigate("space_detail/$spaceId/add_expense")
                                }
                            )
                        }
                    }
                    composable("space_detail/{spaceId}/add_expense") { backStackEntry ->
                        val spaceId = backStackEntry.arguments?.getString("spaceId") ?: return@composable
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable, LocalScreenAdSlots provides rememberScreenAdSlots()) {
                            androidx.activity.compose.BackHandler(enabled = navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                            AddExpenseScreen(
                                spaceId = spaceId,
                                viewModel = spaceViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    composable(Screen.Profile.route) {
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable, LocalScreenAdSlots provides rememberScreenAdSlots()) {
                            androidx.activity.compose.BackHandler(enabled = navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                            ProfileScreen(
                                onSignOut = onSignOut,
                                onNavigateToBudgets = {
                                    navController.navigate("budgets")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
