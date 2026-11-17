package com.invatech.pockit.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.invatech.pockit.PockitApplication
import com.invatech.pockit.data.local.dao.SubscriptionDao
import com.invatech.pockit.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class SubscriptionRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val owner = auth.currentUser?.uid
    private val localDao: SubscriptionDao? = runCatching { PockitApplication.instance.database.subscriptionDao() }.getOrNull()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val syncError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private fun userId(): String? = owner?.takeIf { it == auth.currentUser?.uid }

    private fun subscriptionsCollection(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("subscriptions")

    fun observeSubscriptions(): Flow<List<SubscriptionEntity>> = channelFlow {
        val uid = userId()
        if (uid == null) { send(emptyList()); close(); return@channelFlow }
        val local = launch { localDao?.observeSubscriptions()?.collect { send(it) } }
        syncFromFirebase()
        val listener = subscriptionsCollection(uid).addSnapshotListener { snapshot, failure ->
            if (failure != null) syncError.value = "Cloud sync failed: ${failure.message}"
            else if (snapshot != null && !snapshot.metadata.isFromCache) launch { syncFromFirebase() }
        }
        awaitClose { local.cancel(); listener.remove() }
    }

    fun observeActiveSubscriptions(): Flow<List<SubscriptionEntity>> {
        return observeSubscriptions().map { rows -> rows.filter { it.isActive } }
    }

    suspend fun getAllSubscriptions(): List<SubscriptionEntity> {
        return localDao?.getAllSubscriptions() ?: emptyList()
    }

    suspend fun addOrUpdateSubscription(subscription: SubscriptionEntity) {
        // 1. Instant local Room persistence


        // 2. Firebase Cloud Firestore Sync
        val uid = userId() ?: error("Please sign in again.")
        run {
            val data = mapOf(
                "title" to subscription.title,
                "amountPaise" to subscription.amountPaise,
                "billingCycle" to subscription.billingCycle,
                "dueDayOfMonth" to subscription.dueDayOfMonth,
                "category" to subscription.category,
                "reminderDaysBefore" to subscription.reminderDaysBefore,
                "paymentMethod" to subscription.paymentMethod,
                "notes" to subscription.notes,
                "isActive" to subscription.isActive,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            subscriptionsCollection(uid).document(subscription.id)
                .set(data, SetOptions.merge())
                .await()
            localDao?.insertSubscription(subscription)
        }
    }

    suspend fun deleteSubscription(id: String) {
        val uid = userId() ?: error("Please sign in again.")
        subscriptionsCollection(uid).document(id).delete().await()
        localDao?.deleteSubscription(id)
    }

    suspend fun toggleActive(id: String, isActive: Boolean) {
        val existing = localDao?.getSubscriptionById(id) ?: return
        val updated = existing.copy(isActive = isActive)
        addOrUpdateSubscription(updated)
    }

    suspend fun syncFromFirebase() {
        val uid = userId() ?: return
        try {
            val migration = PockitApplication.instance.getSharedPreferences("cloud_imports_$uid", 0)
            if (!migration.getBoolean("subscriptionsCollection_v1", false)) {
                for (row in localDao?.getAllSubscriptions().orEmpty()) {
                    val ref = subscriptionsCollection(uid).document(row.id)
                    firestore.runTransaction { tx -> if (!tx.get(ref).exists()) tx.set(ref, row) }.await()
                }
                migration.edit().putBoolean("subscriptionsCollection_v1", true).apply()
            }
            val snapshot = subscriptionsCollection(uid).get(com.google.firebase.firestore.Source.SERVER).await()
            val remoteList = snapshot.documents.mapNotNull { doc ->
                val title = doc.getString("title") ?: return@mapNotNull null
                val amountPaise = doc.getLong("amountPaise") ?: return@mapNotNull null
                val billingCycle = doc.getString("billingCycle") ?: "MONTHLY"
                val dueDayOfMonth = doc.getLong("dueDayOfMonth")?.toInt() ?: 1
                val category = doc.getString("category") ?: "Entertainment"
                val reminderDays = doc.getLong("reminderDaysBefore")?.toInt() ?: 1
                val paymentMethod = doc.getString("paymentMethod") ?: "Auto-debit / UPI"
                val notes = doc.getString("notes") ?: ""
                val isActive = doc.getBoolean("isActive") ?: true

                SubscriptionEntity(
                    id = doc.id,
                    title = title,
                    amountPaise = amountPaise,
                    billingCycle = billingCycle,
                    dueDayOfMonth = dueDayOfMonth,
                    category = category,
                    reminderDaysBefore = reminderDays,
                    paymentMethod = paymentMethod,
                    notes = notes,
                    isActive = isActive
                )
            }
            localDao?.replaceCloudSnapshot(remoteList)
            syncError.value = null
        } catch (e: Exception) { syncError.value = "Cloud sync failed: ${e.message}" }
    }

    /**
     * Calculates the normalized monthly outflow for all active recurring commitments.
     */
    fun calculateMonthlyTotalPaise(subscriptions: List<SubscriptionEntity>): Long {
        return subscriptions.filter { it.isActive }.sumOf { sub ->
            when (sub.billingCycle.uppercase()) {
                "MONTHLY" -> sub.amountPaise
                "YEARLY" -> sub.amountPaise / 12
                "QUARTERLY" -> sub.amountPaise / 3
                "WEEKLY" -> sub.amountPaise * 4
                else -> sub.amountPaise
            }
        }
    }

    /**
     * Calculates days remaining until the next due day for a given day-of-month.
     */
    fun getDaysUntilNextDue(dueDayOfMonth: Int): Int {
        val today = Calendar.getInstance()
        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        val maxDaysInCurrentMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH)
        val targetDay = dueDayOfMonth.coerceIn(1, maxDaysInCurrentMonth)

        return if (targetDay >= currentDay) {
            targetDay - currentDay
        } else {
            // Next month
            val daysLeftInThisMonth = maxDaysInCurrentMonth - currentDay
            daysLeftInThisMonth + dueDayOfMonth
        }
    }
}
