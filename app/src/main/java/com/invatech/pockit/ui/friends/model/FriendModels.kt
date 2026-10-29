package com.invatech.pockit.ui.friends.model

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExpenseDirection {
    GAVE_UDHAR, // Maine Diya (You gave money, they owe you)
    TOOK_UDHAR, // Maine Liya (You took money, you owe them)
    SETTLE_UP   // Hisab Chukaya (Payment settled)
}

enum class TransactionStatus {
    PENDING,   // Awaiting verification from the other party
    CONFIRMED, // Verified and accepted by both sides
    REJECTED   // Rejected / disputed by counterparty
}

data class FriendConnectionPayload(
    val userId: String,
    val name: String,
    val email: String = "",
    val avatarEmoji: String = "👤"
) {
    fun toQrString(): String {
        return Uri.Builder()
            .scheme("pockit")
            .authority("friend")
            .appendQueryParameter("uid", userId)
            .appendQueryParameter("name", name)
            .appendQueryParameter("email", email)
            .appendQueryParameter("emoji", avatarEmoji)
            .build()
            .toString()
    }

    companion object {
        fun fromQrString(raw: String): FriendConnectionPayload? {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return null

            return try {
                if (trimmed.startsWith("pockit://friend")) {
                    val uri = Uri.parse(trimmed)
                    val uid = uri.getQueryParameter("uid") ?: return null
                    val name = uri.getQueryParameter("name") ?: "Friend"
                    val email = uri.getQueryParameter("email") ?: ""
                    val emoji = uri.getQueryParameter("emoji") ?: "👤"
                    FriendConnectionPayload(
                        userId = uid,
                        name = name,
                        email = email,
                        avatarEmoji = emoji
                    )
                } else if (trimmed.contains("@")) {
                    // Plain email format entered
                    FriendConnectionPayload(
                        userId = "user_${trimmed.hashCode()}",
                        name = trimmed.substringBefore("@").replaceFirstChar { it.uppercase() },
                        email = trimmed,
                        avatarEmoji = "👤"
                    )
                } else {
                    // Short code format or raw UID
                    FriendConnectionPayload(
                        userId = trimmed,
                        name = "Friend",
                        email = "",
                        avatarEmoji = "👤"
                    )
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}

data class FriendTransaction(
    val id: String,
    val friendId: String,
    val amount: Double,
    val direction: ExpenseDirection,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null,
    val payerId: String? = null,
    val creatorId: String = "",
    val status: TransactionStatus = TransactionStatus.CONFIRMED
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}

data class Friend(
    val id: String,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val avatarEmoji: String = "👤",
    val note: String = "",
    val friendshipId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class FriendWithBalance(
    val friend: Friend,
    val transactions: List<FriendTransaction>
) {
    /**
     * Net balance calculation:
     * Positive (+) = Friend owes you (You'll get)
     * Negative (-) = You owe friend (You'll give)
     * Zero (0) = Settled
     *
     * Rules for two-way verification truth:
     * 1. REJECTED transactions never alter the balance.
     * 2. PENDING settlements do NOT clear balance until verified by the receiving party.
     */
    val netBalance: Double
        get() {
            var balance = 0.0
            for (t in transactions) {
                if (t.status == TransactionStatus.REJECTED) continue
                if (t.direction == ExpenseDirection.SETTLE_UP && t.status != TransactionStatus.CONFIRMED) continue

                when (t.direction) {
                    ExpenseDirection.GAVE_UDHAR -> balance += t.amount
                    ExpenseDirection.TOOK_UDHAR -> balance -= t.amount
                    ExpenseDirection.SETTLE_UP -> balance += t.amount
                }
            }
            val rounded = kotlin.math.round(balance * 100.0) / 100.0
            return if (kotlin.math.abs(rounded) < 0.01) 0.0 else rounded
        }

    val totalGave: Double
        get() = transactions.filter { it.direction == ExpenseDirection.GAVE_UDHAR && it.status != TransactionStatus.REJECTED }.sumOf { it.amount }

    val totalTook: Double
        get() = transactions.filter { it.direction == ExpenseDirection.TOOK_UDHAR && it.status != TransactionStatus.REJECTED }.sumOf { it.amount }

    val lastTransaction: FriendTransaction?
        get() = transactions.maxByOrNull { it.timestamp }

    val pendingSettlements: List<FriendTransaction>
        get() = transactions.filter { it.status == TransactionStatus.PENDING }
}

enum class FriendsFilter(val label: String) {
    ALL("All Friends"),
    YOULL_GET("You'll Get"),
    YOULL_GIVE("You'll Give"),
    SETTLED("Settled")
}
