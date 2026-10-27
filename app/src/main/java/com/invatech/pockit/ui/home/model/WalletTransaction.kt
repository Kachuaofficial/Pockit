package com.invatech.pockit.ui.home.model


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