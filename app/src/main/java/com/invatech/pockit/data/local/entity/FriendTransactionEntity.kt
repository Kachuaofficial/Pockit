package com.invatech.pockit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.invatech.pockit.ui.friends.model.ExpenseDirection
import com.invatech.pockit.ui.friends.model.FriendTransaction
import com.invatech.pockit.ui.friends.model.TransactionStatus

@Entity(tableName = "friend_transactions")
data class FriendTransactionEntity(
    @PrimaryKey val id: String,
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
    fun toFriendTransaction(): FriendTransaction = FriendTransaction(
        id = id,
        friendId = friendId,
        amount = amount,
        direction = direction,
        description = description,
        timestamp = timestamp,
        note = note,
        payerId = payerId,
        creatorId = creatorId,
        status = status
    )

    companion object {
        fun fromFriendTransaction(tx: FriendTransaction): FriendTransactionEntity = FriendTransactionEntity(
            id = tx.id,
            friendId = tx.friendId,
            amount = tx.amount,
            direction = tx.direction,
            description = tx.description,
            timestamp = tx.timestamp,
            note = tx.note,
            payerId = tx.payerId,
            creatorId = tx.creatorId,
            status = tx.status
        )
    }
}
