package com.invatech.pockit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.invatech.pockit.data.local.entity.FriendEntity
import com.invatech.pockit.data.local.entity.FriendTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Query("SELECT * FROM friends ORDER BY createdAt DESC")
    fun observeFriends(): Flow<List<FriendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<FriendEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity): Long

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteFriend(id: String): Int

    @Query("DELETE FROM friends")
    suspend fun clearFriends(): Int

    // Friend Transactions
    @Query("SELECT * FROM friend_transactions WHERE friendId = :friendId ORDER BY timestamp DESC")
    fun observeTransactions(friendId: String): Flow<List<FriendTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<FriendTransactionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FriendTransactionEntity): Long

    @Query("DELETE FROM friend_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String): Int

    @Query("DELETE FROM friend_transactions WHERE friendId = :friendId")
    suspend fun deleteTransactionsForFriend(friendId: String): Int
}
