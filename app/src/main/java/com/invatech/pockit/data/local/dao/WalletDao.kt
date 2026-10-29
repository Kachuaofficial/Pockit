package com.invatech.pockit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.invatech.pockit.data.local.entity.WalletTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    @Query("SELECT * FROM wallet_transactions ORDER BY createdAtMillis DESC")
    fun observeTransactions(): Flow<List<WalletTransactionEntity>>

    @Query("SELECT * FROM wallet_transactions ORDER BY createdAtMillis DESC LIMIT :limit")
    suspend fun getTransactions(limit: Int): List<WalletTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<WalletTransactionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: WalletTransactionEntity): Long

    @Query("DELETE FROM wallet_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String): Int

    @Query("DELETE FROM wallet_transactions")
    suspend fun clearTransactions(): Int
}
