package com.invatech.pockit.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.invatech.pockit.PockitApplication
import com.invatech.pockit.data.local.dao.WalletDao
import com.invatech.pockit.data.local.entity.WalletTransactionEntity
import com.invatech.pockit.ui.home.model.WalletTransactionType
import kotlinx.coroutines.tasks.await

class WalletRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val localDao: WalletDao? by lazy {
        runCatching { PockitApplication.instance.database.walletDao() }.getOrNull()
    }

    private fun userId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in first.")

    private fun walletRef(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("wallets")
            .document("main")

    suspend fun getBalancePaise(): Long {
        val uid = userId()
        val snapshot = walletRef(uid).get().await()
        return snapshot.getLong("balancePaise") ?: 0L
    }

    /**
     * Fetches user transactions with a strict limit to prevent unbounded Firestore reads.
     * Default limit is 30 to optimize bandwidth and billing.
     */
    suspend fun getTransactions(limit: Long = 30): List<com.invatech.pockit.ui.home.model.WalletTransaction> {
        val uid = runCatching { userId() }.getOrNull()
            ?: return localDao?.getTransactions(limit.toInt())?.map { it.toWalletTransaction() } ?: emptyList()

        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("transactions")
                .orderBy(
                    "createdAt",
                    com.google.firebase.firestore.Query.Direction.DESCENDING
                )
                .limit(limit)
                .get()
                .await()

            val list = snapshot.documents.mapNotNull { document ->
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
                localDao?.insertTransactions(entities)
            }
            list
        } catch (e: Exception) {
            localDao?.getTransactions(limit.toInt())?.map { it.toWalletTransaction() } ?: emptyList()
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
        category: String = "Other"
    ): Pair<Long, String> {
        require(title.isNotBlank()) {
            "Please enter a description."
        }
        require(amountPaise > 0L) {
            "Amount must be greater than zero."
        }

        val uid = userId()
        val wallet = walletRef(uid)
        val transactionRef = firestore
            .collection("users")
            .document(uid)
            .collection("transactions")
            .document()

        val newBalance = firestore.runTransaction { transaction ->
            val walletSnapshot = transaction.get(wallet)
            val currentBalance =
                walletSnapshot.getLong("balancePaise") ?: 0L

            val calculatedBalance = when (type) {
                WalletTransactionType.INCOME ->
                    Math.addExact(currentBalance, amountPaise)

                WalletTransactionType.EXPENSE -> {
                    require(currentBalance >= amountPaise) {
                        "Insufficient balance."
                    }
                    currentBalance - amountPaise
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
                "createdAt" to FieldValue.serverTimestamp()
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
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }

        return Pair(newBalance, transactionRef.id)
    }

    /**
     * Deletes a transaction and adjusts the wallet balance atomically in Firestore.
     * Returns the updated balance so local state can update without re-fetching all documents.
     */
    suspend fun deleteTransaction(transactionId: String): Long {
        runCatching { localDao?.deleteTransaction(transactionId) }
        val uid = userId()
        val wallet = walletRef(uid)
        val transactionRef = firestore
            .collection("users")
            .document(uid)
            .collection("transactions")
            .document(transactionId)

        return firestore.runTransaction { transaction ->
            val txSnapshot = transaction.get(transactionRef)
            val walletSnapshot = transaction.get(wallet)
            val currentBalance = walletSnapshot.getLong("balancePaise") ?: 0L

            if (!txSnapshot.exists()) return@runTransaction currentBalance

            val amountPaise = txSnapshot.getLong("amountPaise") ?: 0L
            val typeName = txSnapshot.getString("type") ?: ""
            val type = runCatching { WalletTransactionType.valueOf(typeName) }.getOrNull()

            val newBalance = when (type) {
                WalletTransactionType.EXPENSE -> Math.addExact(currentBalance, amountPaise)
                WalletTransactionType.INCOME -> (currentBalance - amountPaise).coerceAtLeast(0L)
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
        val uid = userId()
        val wallet = walletRef(uid)
        val transactionRef = firestore
            .collection("users")
            .document(uid)
            .collection("transactions")
            .document(transactionId)

        return firestore.runTransaction { transaction ->
            val walletSnapshot = transaction.get(wallet)
            val currentBalance = walletSnapshot.getLong("balancePaise") ?: 0L

            val newBalance = when (type) {
                WalletTransactionType.INCOME -> Math.addExact(currentBalance, amountPaise)
                WalletTransactionType.EXPENSE -> {
                    require(currentBalance >= amountPaise) { "Insufficient balance." }
                    currentBalance - amountPaise
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
    }
}
