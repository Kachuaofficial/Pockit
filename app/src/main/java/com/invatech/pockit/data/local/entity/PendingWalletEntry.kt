package com.invatech.pockit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.invatech.pockit.ui.home.model.WalletTransactionType

/** Stored on this device until connectivity returns and automatic cloud sync succeeds. */
@Entity(tableName = "pending_wallet_entries")
data class PendingWalletEntry(
    @PrimaryKey val id: String,
    val title: String,
    val amountPaise: Long,
    val type: WalletTransactionType,
    val category: String,
    val createdAtMillis: Long
) {
    fun cached() = WalletTransactionEntity(id, title, amountPaise, type, category, createdAtMillis)
    fun payload(): Map<String, Any> = mapOf("id" to id, "title" to title, "amountPaise" to amountPaise,
        "type" to type.name, "category" to category, "createdAtMillis" to createdAtMillis)
}
