package com.invatech.pockit.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.invatech.pockit.PockitApplication
import com.invatech.pockit.data.local.dao.WalletDao
import com.invatech.pockit.data.local.entity.WalletTransactionEntity
import com.invatech.pockit.ui.home.model.WalletTransaction
import com.invatech.pockit.ui.home.model.WalletTransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.withLock

class WalletRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val owner = auth.currentUser?.uid
    companion object {
        private val locks = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.sync.Mutex>()
        private val balances = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.flow.MutableStateFlow<Long>>()
    }
    private fun balanceState(): kotlinx.coroutines.flow.MutableStateFlow<Long> = balances.getOrPut(owner.orEmpty()) {
        kotlinx.coroutines.flow.MutableStateFlow(PockitApplication.instance.getSharedPreferences("wallet_balance", 0).getLong(owner.orEmpty(), 0))
    }
    fun observeBalance(): kotlinx.coroutines.flow.StateFlow<Long> = balanceState()
    private fun publishBalance(value: Long) {
        balanceState().value = value
        PockitApplication.instance.getSharedPreferences("wallet_balance", 0).edit().putLong(owner.orEmpty(), value).apply()
    }
    private val localDao: WalletDao? = runCatching { PockitApplication.instance.database.walletDao() }.getOrNull()

    /**
     * Exposes a real-time Flow of transactions directly from the local Room database.
     * Ensures offline-first instant loading and zero latency on UI launches.
     */
    fun observeTransactions(): Flow<List<WalletTransaction>> {
        return localDao?.observeTransactions()?.map { list ->
            list.map { it.toWalletTransaction() }
        } ?: emptyFlow()
    }

    suspend fun getAllLocalTransactions(): List<WalletTransaction> {
        return localDao?.getAllTransactions()?.map { it.toWalletTransaction() } ?: emptyList()
    }

    private fun userId(): String =
        owner?.takeIf { it == auth.currentUser?.uid }
            ?: throw IllegalStateException("Please sign in again.")

    private fun walletRef(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("wallets")
            .document("main")

    fun watchCloud(onTransactionsChanged: () -> Unit, onError: (String) -> Unit): List<com.google.firebase.firestore.ListenerRegistration> {
        val uid = owner ?: return emptyList()
        val walletListener = walletRef(uid).addSnapshotListener { snapshot, failure ->
            if (auth.currentUser?.uid != uid) return@addSnapshotListener
            if (failure != null) onError(failure.message ?: "Wallet sync failed.")
            else snapshot?.getLong("balancePaise")?.let(::publishBalance)
        }
        val transactionsListener = firestore.collection("users").document(uid).collection("transactions").addSnapshotListener { _, failure ->
            if (auth.currentUser?.uid != uid) return@addSnapshotListener
            if (failure != null) onError(failure.message ?: "Activity sync failed.") else onTransactionsChanged()
        }
        return listOf(walletListener, transactionsListener)
    }

    suspend fun setRecordedBalance(value: Long, expected: Long) {
        require(value in -com.invatech.pockit.domain.Money.MAX_AMOUNT..com.invatech.pockit.domain.Money.MAX_AMOUNT)
        locks.getOrPut(owner.orEmpty()) { kotlinx.coroutines.sync.Mutex() }.withLock {
            val uid = userId()
            val ref = walletRef(uid)
            val audit = firestore.collection("users").document(uid).collection("balanceAdjustments").document()
            firestore.runTransaction { tx ->
                val snapshot = tx.get(ref)
                val previous = snapshot.getLong("balancePaise") ?: 0L
                check(previous == expected) { "Balance changed on another device. Refresh and try again." }
                tx.set(ref, mapOf("balancePaise" to value, "updatedAt" to FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge())
                tx.set(audit, mapOf("previousPaise" to previous, "balancePaise" to value, "createdAt" to FieldValue.serverTimestamp()))
            }.await()
            publishBalance(value)
        }
    }

    suspend fun getBalancePaise(): Long = locks.getOrPut(owner.orEmpty()) { kotlinx.coroutines.sync.Mutex() }.withLock {
        val uid = userId()
        val snapshot = walletRef(uid).get().await()
        (snapshot.getLong("balancePaise") ?: 0L).also(::publishBalance)
    }

    /**
     * Fetches user transactions with a limit to optimize bandwidth and billing,
     * while storing everything in Room for rich offline-first analytics.
     * Default limit is 100 to populate Room with comprehensive historical records.
     */
    suspend fun getTransactions(limit: Long = 100): List<com.invatech.pockit.ui.home.model.WalletTransaction> {
        return locks.getOrPut(owner.orEmpty()) { kotlinx.coroutines.sync.Mutex() }.withLock {

        val uid = runCatching { userId() }.getOrNull()
            ?: return localDao?.getAllTransactions()?.map { it.toWalletTransaction() }
            ?: localDao?.getTransactions(limit.toInt())?.map { it.toWalletTransaction() } ?: emptyList()

        return try {
            val query = firestore.collection("users").document(uid).collection("transactions")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            val documents = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
            var cursor: com.google.firebase.firestore.DocumentSnapshot? = null
            do {
                val page = (cursor?.let { query.startAfter(it) } ?: query).limit(500)
                    .get(com.google.firebase.firestore.Source.SERVER).await()
                documents.addAll(page.documents)
                cursor = page.documents.lastOrNull()
            } while (page.size() == 500)

            val list = documents.mapNotNull { document ->
                val amount = document.getLong("amountPaise") ?: return@mapNotNull null
                val typeName = document.getString("type") ?: return@mapNotNull null

                val type = runCatching {
                    WalletTransactionType.valueOf(typeName)
                }.getOrNull() ?: return@mapNotNull null

                com.invatech.pockit.ui.home.model.WalletTransaction(
                    id = document.id,
                    title = document.getString("title") ?: "",
                    amountPaise = amount,
                    type = type,
                    category = document.getString("category") ?: "Other",
                    createdAt = document.getTimestamp("createdAt")
                )
            }
            // Cache locally in Room
            runCatching {
                val entities = list.map { WalletTransactionEntity.fromWalletTransaction(it) }
                localDao?.replaceSnapshot(entities)
            }
            list
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            throw IllegalStateException("Could not refresh activity. Your saved records are still available; retry when connected.", e)
        }

        }
}

    /**
     * Atomically adds a transaction and updates the wallet balance.
     * Returns the updated balance and newly generated transaction ID so the caller
     * can update local UI state without triggering full-history re-fetches.
     */
    suspend fun addTransaction(
        title: String,
        amountPaise: Long,
        type: WalletTransactionType,
        category: String = "Other",
        requestId: String? = null,
        occurredAtMillis: Long = System.currentTimeMillis()
    ): Pair<Long, String> {
        return locks.getOrPut(owner.orEmpty()) { kotlinx.coroutines.sync.Mutex() }.withLock {

        require(title.isNotBlank()) {
            "Please enter a description."
        }
        require(amountPaise in 1..com.invatech.pockit.domain.Money.MAX_AMOUNT) {
            "Amount must be greater than zero."
        }

        val uid = userId()
        val wallet = walletRef(uid)
        val transactionRef = firestore
            .collection("users")
            .document(uid)
            .collection("transactions")
            .document(requestId ?: java.util.UUID.randomUUID().toString())

        var transactionTime = occurredAtMillis
        val newBalance = firestore.runTransaction { transaction ->
            val walletSnapshot = transaction.get(wallet)
            val currentBalance =
                walletSnapshot.getLong("balancePaise") ?: 0L

            val existing = transaction.get(transactionRef)
            if (existing.exists()) {
                require(existing.getLong("amountPaise") == amountPaise && existing.getString("type") == type.name && existing.getString("title") == title.trim() && existing.getString("category") == category) { "This payment was already saved with different details. Check your activity before trying again." }
                transactionTime = existing.getTimestamp("createdAt")?.toDate()?.time ?: occurredAtMillis
                return@runTransaction currentBalance
            }
            val calculatedBalance = when (type) {
                WalletTransactionType.INCOME ->
                    Math.addExact(currentBalance, amountPaise)

                WalletTransactionType.EXPENSE -> {
                    Math.subtractExact(currentBalance, amountPaise)
                }
            }

            val walletData = mapOf(
                "balancePaise" to calculatedBalance,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            transaction.set(
                wallet,
                walletData,
                com.google.firebase.firestore.SetOptions.merge()
            )

            val transactionData = mapOf(
                "title" to title.trim(),
                "amountPaise" to amountPaise,
                "type" to type.name,
                "category" to category,
                "createdAt" to com.google.firebase.Timestamp(java.util.Date(transactionTime)),
                "recordedAt" to FieldValue.serverTimestamp()
            )

            transaction.set(transactionRef, transactionData)
            calculatedBalance
        }.await()

        runCatching {
            localDao?.insertTransaction(
                WalletTransactionEntity(
                    id = transactionRef.id,
                    title = title.trim(),
                    amountPaise = amountPaise,
                    type = type,
                    category = category,
                    createdAtMillis = transactionTime
                )
            )
        }
        notifyWidgetUpdate()

        if (type == WalletTransactionType.EXPENSE) {
            runCatching {
                BudgetRepository().checkBudgetAlert(PockitApplication.instance, category, amountPaise)
            }
        }

        publishBalance(newBalance)
        return Pair(newBalance, transactionRef.id)

        }
}

    /**
     * Deletes a transaction and adjusts the wallet balance atomically in Firestore.
     * Returns the updated balance so local state can update without re-fetching all documents.
     */
    suspend fun deleteTransaction(transactionId: String): Long {
        return locks.getOrPut(owner.orEmpty()) { kotlinx.coroutines.sync.Mutex() }.withLock {

        val uid = userId()
        val wallet = walletRef(uid)
        val transactionRef = firestore
            .collection("users")
            .document(uid)
            .collection("transactions")
            .document(transactionId)

        val updatedBalance = firestore.runTransaction { transaction ->
            val txSnapshot = transaction.get(transactionRef)
            val walletSnapshot = transaction.get(wallet)
            val currentBalance = walletSnapshot.getLong("balancePaise") ?: 0L

            if (!txSnapshot.exists()) return@runTransaction currentBalance

            val amountPaise = txSnapshot.getLong("amountPaise") ?: 0L
            val typeName = txSnapshot.getString("type") ?: ""
            val type = runCatching { WalletTransactionType.valueOf(typeName) }.getOrNull()

            val newBalance = when (type) {
                WalletTransactionType.EXPENSE -> Math.addExact(currentBalance, amountPaise)
                WalletTransactionType.INCOME -> Math.subtractExact(currentBalance, amountPaise)
                null -> currentBalance
            }

            val walletData = mapOf(
                "balancePaise" to newBalance,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            transaction.set(wallet, walletData, com.google.firebase.firestore.SetOptions.merge())
            transaction.delete(transactionRef)
            newBalance
        }.await()
        notifyWidgetUpdate()
        localDao?.deleteTransaction(transactionId)
        publishBalance(updatedBalance)
        return updatedBalance

        }
}

    /**
     * Restores a deleted transaction and re-applies its balance change atomically.
     * Returns the updated balance for local state update.
     */
    suspend fun restoreTransaction(
        transactionId: String,
        title: String,
        amountPaise: Long,
        type: WalletTransactionType,
        category: String,
        createdAt: com.google.firebase.Timestamp?
    ): Long {
        return locks.getOrPut(owner.orEmpty()) { kotlinx.coroutines.sync.Mutex() }.withLock {

        val uid = userId()
        val wallet = walletRef(uid)
        val transactionRef = firestore
            .collection("users")
            .document(uid)
            .collection("transactions")
            .document(transactionId)

        val newBalance = firestore.runTransaction { transaction ->
            val walletSnapshot = transaction.get(wallet)
            val currentBalance = walletSnapshot.getLong("balancePaise") ?: 0L

            if (transaction.get(transactionRef).exists()) return@runTransaction currentBalance
            val newBalance = when (type) {
                WalletTransactionType.INCOME -> Math.addExact(currentBalance, amountPaise)
                WalletTransactionType.EXPENSE -> {

                    Math.subtractExact(currentBalance, amountPaise)
                }
            }

            val walletData = mapOf(
                "balancePaise" to newBalance,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            transaction.set(wallet, walletData, com.google.firebase.firestore.SetOptions.merge())

            val transactionData = mapOf(
                "title" to title.trim(),
                "amountPaise" to amountPaise,
                "type" to type.name,
                "category" to category,
                "createdAt" to (createdAt ?: FieldValue.serverTimestamp())
            )
            transaction.set(transactionRef, transactionData)
            newBalance
        }.await()

        runCatching {
            localDao?.insertTransaction(
                WalletTransactionEntity(
                    id = transactionId,
                    title = title.trim(),
                    amountPaise = amountPaise,
                    type = type,
                    category = category,
                    createdAtMillis = createdAt?.toDate()?.time ?: System.currentTimeMillis()
                )
            )
        }
        notifyWidgetUpdate()

        if (type == WalletTransactionType.EXPENSE) {
            runCatching {
                BudgetRepository().checkBudgetAlert(PockitApplication.instance, category, amountPaise)
            }
        }

        publishBalance(newBalance)
        return newBalance

        }
}

    /**
     * Updates an existing transaction and atomically recalculates the wallet balance in Firestore.
     * Reverts the previous transaction impact, applies the new transaction impact,
     * and returns the updated balance.
     */
    suspend fun updateTransaction(
        transactionId: String,
        newTitle: String,
        newAmountPaise: Long,
        newType: WalletTransactionType,
        newCategory: String
    ): Long {
        return locks.getOrPut(owner.orEmpty()) { kotlinx.coroutines.sync.Mutex() }.withLock {

        require(newTitle.isNotBlank()) { "Please enter a description." }
        require(newAmountPaise in 1..com.invatech.pockit.domain.Money.MAX_AMOUNT) { "Amount must be greater than zero." }
        val uid = userId()
        val wallet = walletRef(uid)
        val transactionRef = firestore
            .collection("users")
            .document(uid)
            .collection("transactions")
            .document(transactionId)

        val newBalance = firestore.runTransaction { transaction ->
            val txSnapshot = transaction.get(transactionRef)
            val walletSnapshot = transaction.get(wallet)
            val currentBalance = walletSnapshot.getLong("balancePaise") ?: 0L

            check(txSnapshot.exists()) { "Transaction no longer exists. Refresh and try again." }

            val oldAmountPaise = txSnapshot.getLong("amountPaise") ?: 0L
            val oldTypeName = txSnapshot.getString("type") ?: ""
            val oldType = runCatching { WalletTransactionType.valueOf(oldTypeName) }.getOrNull()
                ?: WalletTransactionType.EXPENSE

            // Revert old transaction effect on balance
            val balanceAfterRevert = when (oldType) {
                WalletTransactionType.EXPENSE -> Math.addExact(currentBalance, oldAmountPaise)
                WalletTransactionType.INCOME -> Math.subtractExact(currentBalance, oldAmountPaise)
            }

            // Apply new transaction effect on balance
            val finalBalance = when (newType) {
                WalletTransactionType.INCOME -> Math.addExact(balanceAfterRevert, newAmountPaise)
                WalletTransactionType.EXPENSE -> {

                    Math.subtractExact(balanceAfterRevert, newAmountPaise)
                }
            }

            val walletData = mapOf(
                "balancePaise" to finalBalance,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            transaction.set(wallet, walletData, com.google.firebase.firestore.SetOptions.merge())

            val txData = mapOf(
                "title" to newTitle.trim(),
                "amountPaise" to newAmountPaise,
                "type" to newType.name,
                "category" to newCategory
            )
            transaction.set(transactionRef, txData, com.google.firebase.firestore.SetOptions.merge())

            finalBalance
        }.await()

        runCatching {
            localDao?.insertTransaction(
                WalletTransactionEntity(
                    id = transactionId,
                    title = newTitle.trim(),
                    amountPaise = newAmountPaise,
                    type = newType,
                    category = newCategory,
                    createdAtMillis = localDao?.getTransactionById(transactionId)?.createdAtMillis ?: System.currentTimeMillis()
                )
            )
        }
        notifyWidgetUpdate()

        publishBalance(newBalance)
        return newBalance

        }
}

    private fun notifyWidgetUpdate() {
        runCatching {
            com.invatech.pockit.glance.PockitWidgetManager.updateWidget(PockitApplication.instance)
        }
    }
}
