package com.invatech.pockit.ui.home.model

/*
 * Personal ledger event and income/expense type shared by repositories and UI mapping.
 * amountPaise is a Long; formatted currency belongs to the display layer.
 */


import com.google.firebase.Timestamp

enum class WalletTransactionType {
    INCOME,
    EXPENSE
}

data class WalletTransaction(
    val id: String = "",
    val title: String = "",
    val amountPaise: Long = 0L,
    val type: WalletTransactionType = WalletTransactionType.EXPENSE,
    val category: String = "Other",
    val createdAt: Timestamp? = null
)