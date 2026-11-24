package com.invatech.pockit.data.repository

/*
 * Personal ledger persistence and recorded-balance mutations.
 * A per-account mutex serializes local callers, Firestore transactions coordinate shared writes, and Room caches history.
 */

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
import kotlinx.coroutines.flow.combine
import com.invatech.pockit.data.network.NetworkMonitor
import com.invatech.pockit.data.local.entity.PendingWalletEntry
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.withLock

class WalletRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val owner = auth.currentUser?.uid
    companion object {
        // These mutexes serialize callers within this app process only.
        // Firestore transactions, not this map, handle concurrent changes from other devices.
        private val locks = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.sync.Mutex>()
        fun acceptSyncedBalance(uid: String, balance: Long) {
            balances.getOrPut(uid) { kotlinx.coroutines.flow.MutableStateFlow(balance) }.value = balance
            PockitApplication.instance.getSharedPreferences("wallet_balance", 0).edit().putLong(uid, balance).apply()
        }
        private val balances = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.flow.MutableStateFlow<Long>>()
    }
    private fun balanceState(): kotlinx.coroutines.flow.MutableStateFlow<Long> = balances.getOrPut(owner.orEmpty()) {
        kotlinx.coroutines.flow.MutableStateFlow(PockitApplication.instance.getSharedPreferences("wallet_balance", 0).getLong(owner.orEmpty(), 0))
    }
    fun observeBalance(): Flow<Long> = combine(balanceState(), pendingDao.observe()) { balance, rows ->
        rows.fold(balance) { total, row -> Math.addExact(total, if (row.type == WalletTransactionType.INCOME) row.amountPaise else -row.amountPaise) }
    }
    suspend fun currentBalance(): Long = pendingDao.all().fold(balanceState().value) { total, row ->
        Math.addExact(total, if (row.type == WalletTransactionType.INCOME) row.amountPaise else -row.amountPaise)
    }
    /**
     * Publishes the accepted recorded balance to StateFlow and stores an owner-keyed local copy
     * for the next launch.
     */
    private fun publishBalance(value: Long) {
        balanceState().value = value
        PockitApplication.instance.getSharedPreferences("wallet_balance", 0).edit().putLong(owner.orEmpty(), value).apply()
    }
    private val pendingDao = PockitApplication.instance.database.pendingWalletDao()
    private val localDao: WalletDao? = runCatching { PockitApplication.instance.database.walletDao() }.getOrNull()

    /**
     * Observes cached Room rows and maps them to wallet events. This enables local history display
     * without a fresh network request; it does not promise zero latency or offline cloud writes.
     */
    fun observeTransactions(): Flow<List<WalletTransaction>> {
        return combine(localDao?.observeTransactions() ?: kotlinx.coroutines.flow.flowOf(emptyList()), pendingDao.observe()) { cached, pending ->
            (cached.map { it.toWalletTransaction() } + pending.map { it.cached().toWalletTransaction() })
                .associateBy { it.id }.values.sortedByDescending { it.createdAt?.toDate()?.time ?: 0 }
        }
    }

    suspend fun getAllLocalTransactions(): List<WalletTransaction> {
        return (localDao?.getAllTransactions().orEmpty() + pendingDao.all().map { it.cached() })
            .associateBy { it.id }.values.sortedByDescending { it.createdAtMillis }.map { it.toWalletTransaction() }
    }

    /**
     * Checks that this repository still belongs to the active Firebase account. A repository
     * retained across sign-out/account-switch must not write through the old owner path.
     */
    private fun userId(): String =
        owner?.takeIf { it == auth.currentUser?.uid }
            ?: throw IllegalStateException("Please sign in again.")

    private fun walletRef(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("wallets")
            .document("main")

    /**
     * Attaches wallet/history listeners for this owner and returns their registrations to the
     * ViewModel. Late callbacks for another active account are ignored; the caller removes
     * registrations when its lifecycle ends.
     */
    fun watchCloud(onTransactionsChanged: () -> Unit, onError: (String) -> Unit): List<com.google.firebase.firestore.ListenerRegistration> {
        val uid = owner ?: return emptyList()
        val walletListener = walletRef(uid).addSnapshotListener { snapshot, failure ->
            if (auth.currentUser?.uid != uid) return@addSnapshotListener
            if (failure != null) onError(failure.message ?: "Wallet sync failed.")
            else if (!com.invatech.pockit.data.offline.OfflineSyncManager.state.value.busy) snapshot?.getLong("balancePaise")?.let(::publishBalance)
        }
        val transactionsListener = firestore.collection("users").document(uid).collection("transactions").addSnapshotListener { _, failure ->
            if (auth.currentUser?.uid != uid) return@addSnapshotListener
            if (failure != null) onError(failure.message ?: "Activity sync failed.") else onTransactionsChanged()
        }
        return listOf(walletListener, transactionsListener)
    }

    /**
     * Sets an explicit recorded balance only if the cloud value still equals the value the user
     * edited. The server transaction also writes an adjustment record, preventing a stale form
     * from silently overwriting another device update.
     */
    suspend fun setRecordedBalance(value: Long, expected: Long) {
        require(value in -com.invatech.pockit.domain.Money.MAX_AMOUNT..com.invatech.pockit.domain.Money.MAX_AMOUNT)
        locks.getOrPut(owner.orEmpty()) { kotlinx.coroutines.sync.Mutex() }.withLock {
            NetworkMonitor.requireOnline()
        check(pendingDao.all().isEmpty()) { "Sync your pending entries before editing saved records or the balance." }
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

    /**
     * Refreshes the owner wallet value under the local per-account mutex and publishes it. This is
     * the app-recorded balance, not a query to a bank.
     */
    suspend fun getBalancePaise(): Long = locks.getOrPut(owner.orEmpty()) { kotlinx.coroutines.sync.Mutex() }.withLock {
        val uid = userId()
        if (!NetworkMonitor.online.value) return@withLock currentBalance()
        val snapshot = walletRef(uid).get().await()
        (snapshot.getLong("balancePaise") ?: 0L).also(::publishBalance)
    }

    /**
     * Loads the full server history in 500-document pages and replaces the local snapshot after
     * mapping valid rows. The limit parameter currently affects only a local fallback path; it is
     * not a cap on the normal server fetch. Refresh errors are surfaced while previously cached
     * rows remain available to observers.
     */
    suspend fun getTransactions(limit: Long = 100): List<com.invatech.pockit.ui.home.model.WalletTransaction> {
        return locks.getOrPut(owner.orEmpty()) { kotlinx.coroutines.sync.Mutex() }.withLock {

        val uid = runCatching { userId() }.getOrNull()
            ?: return localDao?.getAllTransactions()?.map { it.toWalletTransaction() }
            ?: localDao?.getTransactions(limit.toInt())?.map { it.toWalletTransaction() } ?: emptyList()

        if (!NetworkMonitor.online.value) return@withLock getAllLocalTransactions()
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
            getAllLocalTransactions()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            throw IllegalStateException("Could not refresh activity. Your saved records are still available; retry when connected.", e)
        }

        }
}

    /**
     * Validates a positive paise amount and commits the ledger event plus wallet balance in one
     * Firestore transaction. Returns the resulting balance and event ID. Reusing requestId with
     * identical details is idempotent; reusing it with different details is rejected. Local
     * cache/widget/budget work follows cloud acceptance. The transaction requires connectivity.
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
        if (!NetworkMonitor.online.value || pendingDao.all().isNotEmpty()) {
            check(!com.invatech.pockit.data.offline.OfflineSyncManager.state.value.busy) { "Your entries are syncing. Please try again in a moment." }
            val rows = pendingDao.all()
            val id = requestId ?: java.util.UUID.randomUUID().toString()
            val existing = rows.firstOrNull { it.id == id }
            if (existing == null) {
                check(rows.size < 100) { "You have 100 pending entries. Connect and sync before adding more." }
                val entry = PendingWalletEntry(id, title.trim(), amountPaise, type, category, occurredAtMillis)
                val projected = Math.addExact(currentBalance(), if (type == WalletTransactionType.INCOME) amountPaise else -amountPaise)
                check(projected in -com.invatech.pockit.domain.Money.MAX_AMOUNT..com.invatech.pockit.domain.Money.MAX_AMOUNT) { "The resulting balance is outside the supported range." }
                pendingDao.insert(entry)
            } else check(existing.title == title.trim() && existing.amountPaise == amountPaise && existing.type == type && existing.category == category)
            return@withLock currentBalance() to id
        }
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

            // Read the stable request ID before writing so an uncertain-result retry cannot add the same event twice.
            val existing = transaction.get(transactionRef)
            if (existing.exists()) {
                require(existing.getLong("amountPaise") == amountPaise && existing.getString("type") == type.name && existing.getString("title") == title.trim() && existing.getString("category") == category) { "This payment was already saved with different details. Check your activity before trying again." }
                transactionTime = existing.getTimestamp("createdAt")?.toDate()?.time ?: occurredAtMillis
                return@runTransaction currentBalance
            }
            // Store a signed effect through the transaction type while keeping the entered amount positive.
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
     * Reads the stored event inside a cloud transaction, reverses its original signed balance
     * effect and deletes it. The balance change and event removal are committed together before
     * the local cache is updated.
     */
    suspend fun deleteTransaction(transactionId: String): Long {
        return locks.getOrPut(owner.orEmpty()) { kotlinx.coroutines.sync.Mutex() }.withLock {

        NetworkMonitor.requireOnline()
        check(pendingDao.all().isEmpty()) { "Sync your pending entries before editing saved records or the balance." }
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
     * Recreates the original event ID and reapplies its signed balance effect if the event is
     * absent. An already restored ID must not be counted a second time; this supports the UI undo
     * action.
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

        NetworkMonitor.requireOnline()
        check(pendingDao.all().isEmpty()) { "Sync your pending entries before editing saved records or the balance." }
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
     * Reverses the existing event effect and applies the replacement amount/type inside the same
     * cloud transaction. This handles both amount edits and income/expense direction changes
     * without double-counting the old value.
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
        NetworkMonitor.requireOnline()
        check(pendingDao.all().isEmpty()) { "Sync your pending entries before editing saved records or the balance." }
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

    /**
     * Requests a fresh home-screen widget after a wallet mutation; widget rendering is not part of
     * the financial transaction.
     */
    private fun notifyWidgetUpdate() {
        runCatching {
            com.invatech.pockit.glance.PockitWidgetManager.updateWidget(PockitApplication.instance)
        }
    }
}
