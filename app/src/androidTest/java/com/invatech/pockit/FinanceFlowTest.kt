package com.invatech.pockit

/*
 * Device/UI regression checks for finance form behaviour and monetary input handling.
 */

import androidx.compose.ui.test.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.invatech.pockit.ui.finance.*
import com.invatech.pockit.ui.theme.PockitTheme
import com.invatech.pockit.data.local.database.*
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class FinanceFlowTest {
    @get:Rule val compose = createComposeRule()
    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun onboardingReachesCompletion() {
        var complete = false
        compose.setContent { PockitTheme { OnboardingScreen { complete = true } } }
        compose.onNodeWithText("1 of 4").assertExists()
        screenshot("onboarding")
        repeat(3) { index -> compose.onNodeWithText("Continue").performClick(); screenshot("liquid-step-${index + 2}") }
        compose.onNodeWithText("4 of 4").assertExists()
        compose.onNodeWithText("Get started").performClick()
        compose.runOnIdle { assertTrue(complete) }
    }
    @Test fun onboardingWorksWithMotionDisabledAndSwipe() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val before = com.invatech.pockit.ui.common.motion.MotionSettings.enabled.value
        com.invatech.pockit.ui.common.motion.MotionSettings.setEnabled(context, false)
        try {
            compose.setContent { PockitTheme { OnboardingScreen {} } }
            compose.onNodeWithText("Continue").performClick()
            compose.onNodeWithText("2 of 4").assertExists()
            compose.onNodeWithTag("intro_pager").performTouchInput { swipeLeft() }
            compose.onNodeWithText("3 of 4").assertExists()
            compose.onNodeWithText("Continue").performClick()
            compose.onNodeWithText("Get started").assertIsDisplayed()
        } finally { com.invatech.pockit.ui.common.motion.MotionSettings.setEnabled(context, before) }
    }

    @Test fun fourPrimaryTabsRemainAccessible() {
        compose.setContent {
            PockitTheme {
                val route = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("home") }
                androidx.compose.material3.Scaffold(bottomBar = {
                    com.invatech.pockit.ui.navigation.PockitBottomBar(route.value, { route.value = it.route })
                }) { padding ->
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().padding(padding)) {
                        when (route.value) {
                            "home" -> com.invatech.pockit.ui.home.HomeScreen(userName = "Shiva")
                            "space" -> com.invatech.pockit.ui.space.SpaceScreen()
                            "all_transactions" -> com.invatech.pockit.ui.home.AllTransactionsScreen(androidx.lifecycle.viewmodel.compose.viewModel(), {})
                            "insights" -> com.invatech.pockit.ui.home.InsightsScreen(onBack = {}, onBudgets = {}, onTransactions = {})
                            "friends" -> com.invatech.pockit.ui.friends.FriendsScreen()
                            "profile" -> com.invatech.pockit.ui.profile.ProfileScreen()
                        }
                    }
                }
            }
        }
        listOf("Home", "Transactions", "Friends", "Insights", "Space").forEach { label ->
            compose.onNode(hasText(label) and hasClickAction()).assertIsDisplayed().performClick().assertIsSelected()
            screenshot("reference-${label.lowercase()}")
        }
        screenshot("four-tabs")
    }

    @Test fun plannerRejectsInvalidDayAndRequiresAccount() {
        compose.setContent { PockitTheme { PlannerScreen(500000) {} } }
        compose.onNodeWithText("Expected monthly allowance (₹)").performTextClearance()
        compose.onNodeWithText("Expected monthly allowance (₹)").performTextInput("5000")
        compose.onNodeWithText("Allowance day each month (1–31)").performTextClearance()
        compose.onNodeWithText("Allowance day each month (1–31)").performTextInput("32")
        compose.onNodeWithText("Save plan").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Allowance day each month (1–31)").performTextClearance()
        compose.onNodeWithText("Allowance day each month (1–31)").performTextInput("15")
        compose.onNodeWithText("Save plan").performScrollTo().assertIsNotEnabled()
        screenshot("planner")
    }
    @Test fun pendingPaymentsAreIdempotentAndOwnerScoped() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        try {
            val dao = db.records()
            val record = FinanceRecord("event-a", "user-a", "UPI", "Cafe", 29, "EXPENSE")
            assertTrue(dao.insert(record) != -1L)
            assertEquals(-1L, dao.insert(record))
            assertEquals(1, dao.list("user-a", "UPI").size)
            assertTrue(dao.list("user-b", "UPI").isEmpty())
            dao.status("event-a", "user-b", "APPROVED")
            assertEquals("PENDING", dao.list("user-a", "UPI").single().status)
            dao.status("event-a", "user-a", "REJECTED")
            assertEquals("REJECTED", dao.list("user-a", "UPI").single().status)
        } finally { db.close() }
    }
}
