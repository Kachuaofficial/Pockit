package com.invatech.pockit

import com.google.firebase.auth.FirebaseAuth
import com.invatech.pockit.data.network.NetworkMonitor
import com.invatech.pockit.data.repository.WalletRepository
import com.invatech.pockit.ui.home.model.WalletTransactionType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** Run on the isolated QA emulator with Auth emulator forwarded and internet disabled. */
class OfflineWalletIntegrationTest {
    @Test fun incomeExpenseRetryAndAccountSwitchUseOnlyTheLocalQueue() = runBlocking {
        assertFalse("This integration test requires an offline device", NetworkMonitor.online.value)
        val auth=FirebaseAuth.getInstance()
        auth.useEmulator("127.0.0.1",9099)
        try {
            auth.signOut();auth.signInAnonymously().await()
            val firstUid=auth.currentUser!!.uid
            val repo=WalletRepository()
            val request=UUID.randomUUID().toString()
            assertEquals(-12000L,repo.addTransaction("Offline lunch",12000,WalletTransactionType.EXPENSE,"Food",request).first)
            assertEquals(-12000L,repo.addTransaction("Offline lunch",12000,WalletTransactionType.EXPENSE,"Food",request).first)
            assertEquals(38000L,repo.addTransaction("Allowance",50000,WalletTransactionType.INCOME,"Income").first)
            assertEquals(2,repo.getTransactions().size)
            assertEquals(38000L,WalletRepository().currentBalance())
            assertEquals(2,PockitApplication.instance.database.pendingWalletDao().all().size)
            try { repo.setRecordedBalance(50000,38000);fail("Cloud edits must be blocked offline") } catch (_:IllegalStateException) { }
            auth.signOut();auth.signInAnonymously().await()
            assertNotEquals(firstUid,auth.currentUser!!.uid)
            assertTrue(WalletRepository().getAllLocalTransactions().isEmpty())
            assertEquals(0L,WalletRepository().currentBalance())
        } finally { auth.signOut() }
    }
}
