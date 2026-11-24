package com.invatech.pockit.data.repository

/*
 * Owner-scoped category limits, cloud/local synchronization and monthly spending progress.
 * Calculates spending from wallet records and can request warning notifications after an expense.
 */

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.invatech.pockit.PockitApplication
import com.invatech.pockit.data.local.dao.BudgetDao
import com.invatech.pockit.data.local.dao.WalletDao
import com.invatech.pockit.data.local.entity.CategoryBudgetEntity
import com.invatech.pockit.data.notification.NotificationHelper
import com.invatech.pockit.ui.home.model.WalletTransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.combine
import com.invatech.pockit.domain.Money
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

enum class BudgetHealth {
    HEALTHY,
    WARNING,
    EXCEEDED
}

data class CategoryBudgetProgress(
    val category: String,
    val limitPaise: Long,
    val spentPaise: Long,
    val remainingPaise: Long,
    val percentageUsed: Int,
    val health: BudgetHealth
)

class BudgetRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val owner = auth.currentUser?.uid
    private val localDao: BudgetDao? = runCatching { PockitApplication.instance.database.budgetDao() }.getOrNull()
    private val walletDao: WalletDao? = runCatching { PockitApplication.instance.database.walletDao() }.getOrNull()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val syncError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private fun userId(): String? = owner?.takeIf { it == auth.currentUser?.uid }

    private fun budgetsCollection(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("budgets")

    /**
     * Collects Room budget rows while listening for remote updates. Refresh errors are exposed
     * through syncError; awaitClose cancels local collection and removes the cloud listener.
     */
    fun observeBudgets(): Flow<List<CategoryBudgetEntity>> = channelFlow {
        val uid = userId()
        if (uid == null) { send(emptyList()); close(); return@channelFlow }
        val local = launch { localDao?.observeBudgets()?.collect { send(it) } }
        syncFromFirebase()
        val listener = budgetsCollection(uid).addSnapshotListener { snapshot, failure ->
            if (failure != null) syncError.value = "Cloud sync failed: ${failure.message}"
            else if (snapshot != null && !snapshot.metadata.isFromCache) launch { syncFromFirebase() }
        }
        awaitClose { local.cancel(); listener.remove() }
    }

    suspend fun getAllBudgets(): List<CategoryBudgetEntity> {
        return localDao?.getAllBudgets() ?: emptyList()
    }

    suspend fun setBudget(
        category: String,
        limitPaise: Long,
        warningThresholdPercent: Int = 80
    ) {
        com.invatech.pockit.data.network.NetworkMonitor.requireOnline()
        require(limitPaise in 1..Money.MAX_AMOUNT) { "Enter a valid budget." }
        require(warningThresholdPercent in 1..100)
        val entity = CategoryBudgetEntity(
            category = category,
            monthlyLimitPaise = limitPaise,
            warningThresholdPercent = warningThresholdPercent,
            updatedAtMillis = System.currentTimeMillis()
        )

        val uid = userId() ?: error("Please sign in again.")
        run {
            val data = mapOf(
                "category" to category,
                "monthlyLimitPaise" to limitPaise,
                "warningThresholdPercent" to warningThresholdPercent,
                "clientUpdatedAtMillis" to entity.updatedAtMillis,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            budgetsCollection(uid).document(category)
                .set(data, SetOptions.merge()).await()
            localDao?.upsertBudget(entity)
        }
    }

    suspend fun deleteBudget(category: String) {
        com.invatech.pockit.data.network.NetworkMonitor.requireOnline()
        val uid = userId() ?: error("Please sign in again.")
        budgetsCollection(uid).document(category).delete().await()
        localDao?.deleteBudget(category)
    }

    suspend fun syncFromFirebase() {
        val uid = userId() ?: return
        try {
            val migration = PockitApplication.instance.getSharedPreferences("cloud_imports_$uid", 0)
            if (!migration.getBoolean("budgetsCollection_v1", false)) {
                for (row in localDao?.getAllBudgets().orEmpty()) {
                    val ref = budgetsCollection(uid).document(row.category)
                    firestore.runTransaction { tx -> if (!tx.get(ref).exists()) tx.set(ref, row) }.await()
                }
                migration.edit().putBoolean("budgetsCollection_v1", true).apply()
            }
            val snapshot = budgetsCollection(uid).get(com.google.firebase.firestore.Source.SERVER).await()
            val remoteList = snapshot.documents.mapNotNull { doc ->
                val category = doc.getString("category") ?: doc.id
                val limit = doc.getLong("monthlyLimitPaise") ?: return@mapNotNull null
                val warning = doc.getLong("warningThresholdPercent")?.toInt() ?: 80

                CategoryBudgetEntity(
                    category = category,
                    monthlyLimitPaise = limit,
                    warningThresholdPercent = warning,
                    updatedAtMillis = doc.getLong("clientUpdatedAtMillis") ?: doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                )
            }
            localDao?.replaceCloudSnapshot(remoteList)
            syncError.value = null
        } catch (e: Exception) { syncError.value = "Cloud sync failed: ${e.message}" }
    }

    /**
     * Calculates the spending in the current calendar month for a given category.
     */
    suspend fun calculateCurrentMonthSpentForCategory(category: String): Long {
        val startOfMonth = getStartOfCurrentMonthMillis()
        val allTx = walletDao?.getAllTransactions() ?: return 0L
        return allTx.filter {
            it.type == WalletTransactionType.EXPENSE &&
            (category == "Overall" || it.category.equals(category, ignoreCase = true)) &&
            it.createdAtMillis >= startOfMonth && it.createdAtMillis <= System.currentTimeMillis()
        }.let { rows -> Money.sum(rows.map { it.amountPaise }) }
    }

    /**
     * Combines category limits with cached wallet history for the current calendar month. Only
     * expense records contribute; Overall includes all categories. Remaining money and integer
     * percentage are derived values, not additional ledger entries.
     */
    fun observeBudgetProgress(): Flow<List<CategoryBudgetProgress>> {
        val wDao = walletDao ?: return emptyFlow()
        val bDao = localDao ?: return emptyFlow()

        return combine(observeBudgets(), wDao.observeTransactions()) { budgets, allTx ->
            val startOfMonth = getStartOfCurrentMonthMillis()
            budgets.map { b ->
                val spent = allTx.filter {
                    it.type == WalletTransactionType.EXPENSE &&
                    (b.category == "Overall" || it.category.equals(b.category, ignoreCase = true)) &&
                    it.createdAtMillis >= startOfMonth && it.createdAtMillis <= System.currentTimeMillis()
                }.let { rows -> Money.sum(rows.map { it.amountPaise }) }

                val pct = if (b.monthlyLimitPaise > 0L) {
                    Money.percent(spent, b.monthlyLimitPaise)
                } else 0

                val health = when {
                    pct >= 100 -> BudgetHealth.EXCEEDED
                    pct >= b.warningThresholdPercent -> BudgetHealth.WARNING
                    else -> BudgetHealth.HEALTHY
                }

                CategoryBudgetProgress(
                    category = b.category,
                    limitPaise = b.monthlyLimitPaise,
                    spentPaise = spent,
                    remainingPaise = Math.subtractExact(b.monthlyLimitPaise, spent),
                    percentageUsed = pct,
                    health = health
                )
            }
        }
    }

    /**
     * Verifies if an incoming transaction pushes the category budget past its warning or limit,
     * and shows a mindful, cute Mascot notification if so.
     */
    suspend fun checkBudgetAlert(context: Context, category: String, newExpensePaise: Long) {
        val prefs = context.getSharedPreferences("budget_alerts_${owner.orEmpty()}", 0)
        for (key in listOf(category, "Overall").distinct()) {
            val budget = localDao?.getBudgetForCategory(key) ?: continue
            if (budget.monthlyLimitPaise <= 0) continue
            val spent = calculateCurrentMonthSpentForCategory(key)
            val percent = Money.percent(spent, budget.monthlyLimitPaise)
            val threshold = if (percent >= 100) 100 else if (percent >= budget.warningThresholdPercent) budget.warningThresholdPercent else continue
            val alertKey = "${getStartOfCurrentMonthMillis()}_${budget.category}_$threshold"
            if (prefs.getBoolean(alertKey, false)) continue
            NotificationHelper.showAndStoreNotification(
                context = context,
                title = "Budget update: ${budget.category}",
                body = "You've used $percent% of your monthly limit.",
                type = "budget",
                extraData = mapOf("navigate_to" to "budgets"),
                stableId = alertKey.hashCode()
            )
            prefs.edit().putBoolean(alertKey, true).apply()
        }
    }

    private fun getStartOfCurrentMonthMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
