package com.invatech.pockit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.invatech.pockit.ui.home.model.WalletTransaction
import com.invatech.pockit.ui.home.model.WalletTransactionType
import java.util.Date

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amountPaise: Long,
    val type: WalletTransactionType,
    val category: String,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun toWalletTransaction(): WalletTransaction = WalletTransaction(
        id = id,
        title = title,
        amountPaise = amountPaise,
        type = type,
        category = category,
        createdAt = Timestamp(Date(createdAtMillis))
    )

    companion object {
        fun fromWalletTransaction(tx: WalletTransaction): WalletTransactionEntity = WalletTransactionEntity(
            id = tx.id,
            title = tx.title,
            amountPaise = tx.amountPaise,
            type = tx.type,
            category = tx.category,
            createdAtMillis = tx.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        )
    }
}
