package com.invatech.pockit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.Timestamp
import com.invatech.pockit.ui.home.HomeTransactionUi
import com.invatech.pockit.ui.home.components.*
import com.invatech.pockit.ui.home.model.WalletTransactionType
import com.invatech.pockit.ui.navigation.PockitBottomBar
import com.invatech.pockit.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.After
import java.io.File

class CinematicBrandTest {
    @get:Rule val compose=createComposeRule()
    private var motionBefore=true
    @Before fun prepareMotion() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        motionBefore=com.invatech.pockit.ui.common.motion.MotionSettings.enabled.value
        com.invatech.pockit.ui.common.motion.MotionSettings.setEnabled(context,false)
    }
    @After fun restoreMotion() {
        com.invatech.pockit.ui.common.motion.MotionSettings.setEnabled(InstrumentationRegistry.getInstrumentation().targetContext,motionBefore)
    }
    private fun screenshot(name:String) {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null),"cinematic-$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
    }
    @Test fun dashboardActionsAndMonthPickerWork() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        com.invatech.pockit.ui.common.motion.MotionSettings.setEnabled(context,false)
        var expense=0;var income=0;var reports=0;var settings=0;var budget=0
        val rows=listOf("Food & Dining" to 994000L,"Transport" to 447300L,"Shopping" to 397600L,"Bills & Utilities" to 298200L,"Other" to 347900L)
            .mapIndexed { i,(category,amount)->HomeTransactionUi(id="$i",title=category,category=category,amountPaise=amount,
                type=WalletTransactionType.EXPENSE,createdAt=Timestamp.now()) }
        compose.setContent { PockitTheme {
            var previous by remember { mutableStateOf(false) }
            Scaffold(bottomBar={PockitBottomBar("home",{})}) { padding ->
                LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    item { DashboardGreeting("Shivam",2,{}, {settings++}) }
                    item { SpendingSummary(2485000,3500000,listOf(0,300000,510000,480000,1100000,1300000,1690000,1740000,2485000).map{it.toLong()},
                        "Nov 2026",previous,{previous=it},{budget++}) }
                    item { DashboardActions({expense++},{income++},{reports++},{settings++}) }
                    item { CategorySpending(rows,{reports++}) }
                }
            }
        } }
        compose.onNodeWithText("Add\nExpense").performClick()
        compose.onNodeWithText("Add\nIncome").performClick()
        compose.onNodeWithText("Reports").performClick()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Budget").performClick()
        compose.runOnIdle { assertEquals(1,expense);assertEquals(1,income);assertEquals(1,reports);assertEquals(1,settings);assertEquals(1,budget) }
        screenshot("dashboard")
        compose.onNodeWithText("This Month").performClick()
        compose.onNodeWithText("Last Month").performClick()
        compose.onNodeWithText("Last Month").assertIsDisplayed()
        compose.onNodeWithText("See All →").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2,reports) }
        screenshot("categories")
    }
    @Test fun emptyChartHasHonestState() {
        compose.setContent { PockitTheme { Surface { CategorySpending(emptyList(),{}) } } }
        compose.onNodeWithText("Your picture starts here").assertIsDisplayed()
        compose.onNodeWithText("Add an expense to see where your money goes.").assertIsDisplayed()
        screenshot("empty-chart")
    }
    @Test fun secondaryScreensAndSheetsRender() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        com.invatech.pockit.ui.common.motion.MotionSettings.setEnabled(instrumentation.targetContext,false)
        var screen by mutableIntStateOf(0)
        compose.setContent { PockitTheme { Surface(Modifier.fillMaxSize()) {
            val vm:com.invatech.pockit.ui.home.HomeViewModel=androidx.lifecycle.viewmodel.compose.viewModel()
            when(screen) {
                0 -> com.invatech.pockit.ui.profile.ProfileScreen()
                1 -> com.invatech.pockit.ui.friends.FriendsScreen()
                2 -> com.invatech.pockit.ui.budget.BudgetScreen({})
                3 -> com.invatech.pockit.ui.finance.GoalsScreen(onBack={})
                4 -> com.invatech.pockit.ui.finance.PlannerScreen(500000,onBack={})
                5 -> com.invatech.pockit.ui.finance.UpiReviewScreen(onBack={})
                6 -> AddWalletTransactionSheet(onDismiss={},onSave={_,_,_,_->})
                7 -> com.invatech.pockit.ui.space.components.CreateSpaceSheet(onDismiss={},onCreate={_,_,_,_->})
                8 -> com.invatech.pockit.ui.space.components.JoinSpaceSheet(onDismiss={},onJoin={com.invatech.pockit.ui.space.model.JoinResult.NotFound},onJoined={})
                9 -> com.invatech.pockit.ui.notification.NotificationsScreen(androidx.lifecycle.viewmodel.compose.viewModel(),{})
            }
        } } }
        listOf("profile","friends","budgets","goals","planner","upi","expense-sheet","create-space","join-space","notifications").forEachIndexed { index,name ->
            compose.runOnIdle { screen=index }
            compose.waitForIdle()
            val heading=listOf("Profile","Friends","Budgets","Savings goals","Pocket-money planner","Review payments","Add Expense","Create a space","Join with code","Notifications")[index]
            compose.onAllNodesWithText(heading).onFirst().assertIsDisplayed()
            val context=instrumentation.targetContext
            File(context.getExternalFilesDir(null),"cinematic-$name.png").outputStream().use {
                compose.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
            }
        }
    }
    @Test fun largeTextKeepsActionsAndChartsAccessible() {
        var added=false
        compose.setContent {
            val density=androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density,1.5f)) {
                PockitTheme { Scaffold(bottomBar={PockitBottomBar("home",{})}) { padding ->
                    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        item { SpendingSummary(2485000,3500000,listOf(0,600000,1200000,2485000).map{it.toLong()},"Nov 2026",false,{}, {}) }
                        item { DashboardActions({added=true},{},{},{}) }
                        item { CategorySpending(emptyList(),{}) }
                    }
                } }
            }
        }
        compose.onNodeWithText("Add\nExpense").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(added) }
        compose.onNodeWithText("Your picture starts here").performScrollTo().assertIsDisplayed()
        screenshot("large-text")
    }
}
