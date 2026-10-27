package com.invatech.pockit.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.invatech.pockit.ui.home.model.WalletTransactionType
import kotlinx.coroutines.tasks.await


class WalletRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

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


    suspend fun getTransactions(): List<com.invatech.pockit.ui.home.model.WalletTransaction> {
        val uid = userId()

        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("transactions")
            .orderBy(
                "createdAt",
                com.google.firebase.firestore.Query.Direction.DESCENDING
            )
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
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
    }


    suspend fun addTransaction(
        title: String,
        amountPaise: Long,
        type: WalletTransactionType,
        category: String = "Other"
    ) {
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

        firestore.runTransaction { transaction ->
            val walletSnapshot = transaction.get(wallet)
            val currentBalance =
                walletSnapshot.getLong("balancePaise") ?: 0L

            val newBalance = when (type) {
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
                "balancePaise" to newBalance,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            transaction.set(wallet, walletData,
                com.google.firebase.firestore.SetOptions.merge())

            val transactionData = mapOf(
                "title" to title.trim(),
                "amountPaise" to amountPaise,
                "type" to type.name,
                "category" to category,
                "createdAt" to FieldValue.serverTimestamp()
            )

            transaction.set(transactionRef, transactionData)
            true
        }.await()
    }
}
