package com.invatech.pockit

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.invatech.pockit.data.local.database.PockitDatabase
import com.invatech.pockit.data.local.entity.PendingWalletEntry
import com.invatech.pockit.data.offline.OfflineSyncState
import com.invatech.pockit.ui.common.OfflineStatusCard
import com.invatech.pockit.ui.home.components.DashboardGreeting
import com.invatech.pockit.ui.home.components.SpendingSummary
import com.invatech.pockit.ui.home.model.WalletTransactionType
import com.invatech.pockit.ui.theme.PockitTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class OfflineFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun offlineCardAndBrandStayVisibleWithPendingEntries() {
        compose.setContent { PockitTheme { Surface { Column {
            OfflineStatusCard(false, OfflineSyncState(count=3)) { error("Offline sync must not run") }
            DashboardGreeting("Shivam",0,{}, {})
            SpendingSummary(24500,100000,listOf(0,5000,24500),"Nov 2026",false,{}, {})
        } } } }
        compose.onNodeWithText("You're offline").assertIsDisplayed()
        compose.onNodeWithText("3 entries saved on this phone only. Sync when you're online.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Pockit official logo").assertIsDisplayed()
        compose.onNodeWithText("Open sync options").assertDoesNotExist()
    }

    @Test fun pendingQueueSurvivesDatabaseReopenAndCloudSnapshotReplacement() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val name="offline_"+UUID.randomUUID(); val other="offline_other_"+UUID.randomUUID()
        fun open(n: String)=Room.databaseBuilder(context,PockitDatabase::class.java,n).build()
        var db=open(name)
        try {
            val entry=PendingWalletEntry(UUID.randomUUID().toString(),"Offline lunch",12000,WalletTransactionType.EXPENSE,"Food",System.currentTimeMillis())
            db.pendingWalletDao().insert(entry); db.walletDao().replaceSnapshot(emptyList())
            db.close(); db=open(name)
            assertEquals(listOf(entry),db.pendingWalletDao().all())
            val isolated=open(other)
            try { assertTrue(isolated.pendingWalletDao().all().isEmpty()) } finally { isolated.close() }
            db.walletDao().insertTransaction(entry.cached()); db.pendingWalletDao().delete(listOf(entry.id))
            assertTrue(db.pendingWalletDao().all().isEmpty())
            assertEquals(12000L,db.walletDao().getTransactionById(entry.id)?.amountPaise)
        } finally { db.close(); context.deleteDatabase(name); context.deleteDatabase(other) }
    }
}
