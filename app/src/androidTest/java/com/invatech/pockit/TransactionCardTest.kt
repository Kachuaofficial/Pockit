package com.invatech.pockit

/*
 * Compose instrumentation checks for transaction-card rendering and actions.
 */

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.invatech.pockit.ui.home.HomeTransactionUi
import com.invatech.pockit.ui.home.components.ExpressiveWalletTransactionCard
import com.invatech.pockit.ui.home.model.WalletTransactionType
import com.invatech.pockit.ui.theme.PockitTheme
import com.invatech.pockit.domain.Money
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class TransactionCardTest {
    @get:Rule val compose = createComposeRule()
    @Test fun longEntryKeepsAmountAndSeparateActionsAccessible() {
        val row = HomeTransactionUi("sample", "Dinner with flatmates after a very long college day", Money.MAX_AMOUNT, WalletTransactionType.EXPENSE, "Food")
        var edit = false
        var deleted = false
        var opened = false
        compose.setContent { PockitTheme { Surface { Column(Modifier.fillMaxSize().padding(20.dp)) {
            ExpressiveWalletTransactionCard(row, { edit = true }, { deleted = true }, onClick = { opened = true })
        } } } }
        compose.onNodeWithText("− " + Money.format(row.amountPaise)).assertIsDisplayed()
        compose.onNodeWithText(row.title).performClick()
        compose.runOnIdle { assertTrue(opened); assertFalse(edit); assertFalse(deleted) }
        compose.onNodeWithContentDescription("Actions for ${row.title}").performClick()
        compose.onNodeWithText("Edit entry").performClick()
        compose.runOnIdle { assertTrue(edit); assertFalse(deleted) }
        compose.onNodeWithContentDescription("Actions for ${row.title}").performClick()
        compose.onNodeWithText("Delete entry").performClick()
        compose.runOnIdle { assertTrue(deleted) }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "transaction-card.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun activityOpensAndReversesSharedDetailWithMotionOnAndOff() {
        org.junit.Assume.assumeTrue(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null)
        val dao = PockitApplication.instance.database.walletDao()
        val row = com.invatech.pockit.data.local.entity.WalletTransactionEntity("transition-test", "Shared card test", 12550, WalletTransactionType.EXPENSE, "Food")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val previous = com.invatech.pockit.ui.common.motion.MotionSettings.enabled.value
        kotlinx.coroutines.runBlocking { dao.insertTransaction(row) }
        try {
            compose.setContent { PockitTheme {
                val vm: com.invatech.pockit.ui.home.HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                com.invatech.pockit.ui.home.AllTransactionsScreen(vm, {})
            } }
            listOf(true, false).forEach { motion ->
                com.invatech.pockit.ui.common.motion.MotionSettings.setEnabled(context, motion)
                compose.waitUntil(5000) { compose.onAllNodesWithText(row.title).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(row.title).performClick()
                compose.onNodeWithText("Transaction").assertIsDisplayed()
                compose.onNodeWithText("− " + Money.format(row.amountPaise)).assertIsDisplayed()
                compose.onNodeWithContentDescription("Back").performClick()
                compose.onNodeWithText("Transactions").assertIsDisplayed()
            }
        } finally {
            com.invatech.pockit.ui.common.motion.MotionSettings.setEnabled(context, previous)
            kotlinx.coroutines.runBlocking { dao.deleteTransaction(row.id) }
        }
    }

    @Test fun missingPhotoUsesInitials() {
        compose.setContent { PockitTheme { com.invatech.pockit.ui.common.UserAvatar(null, "Shiva", Modifier.size(48.dp)) } }
        compose.onNodeWithText("S").assertIsDisplayed()
    }
}
