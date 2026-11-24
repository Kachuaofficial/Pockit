package com.invatech.pockit.data.local.dao

/*
 * Local personal-wallet history queries and mutations. Snapshot replacement is transactional so stale rows are removed together.
 */

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.invatech.pockit.data.local.entity.WalletTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    /**
     * Replaces the cached wallet history inside a Room transaction. Clearing and inserting as one
     * local operation prevents observers from treating a partial snapshot as the final refresh
     * result.
     */
    @androidx.room.Transaction
    suspend fun replaceSnapshot(rows: List<WalletTransactionEntity>) {
        clearTransactions()
        insertTransactions(rows)
    }


    @Query("SELECT * FROM wallet_transactions ORDER BY createdAtMillis DESC")
    fun observeTransactions(): Flow<List<WalletTransactionEntity>>

    @Query("SELECT * FROM wallet_transactions ORDER BY createdAtMillis DESC LIMIT :limit")
    suspend fun getTransactions(limit: Int): List<WalletTransactionEntity>

    @Query("SELECT * FROM wallet_transactions ORDER BY createdAtMillis DESC")
    suspend fun getAllTransactions(): List<WalletTransactionEntity>

    @Query("SELECT * FROM wallet_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): WalletTransactionEntity?

    @Query("SELECT * FROM wallet_transactions WHERE amountPaise = :amountPaise AND type = :type AND createdAtMillis >= :sinceMillis LIMIT 1")
    suspend fun findRecentSimilarTransaction(
        amountPaise: Long,
        type: com.invatech.pockit.ui.home.model.WalletTransactionType,
        sinceMillis: Long
    ): WalletTransactionEntity?

    @Query("UPDATE wallet_transactions SET title = :title, amountPaise = :amountPaise, type = :type, category = :category WHERE id = :id")
    suspend fun updateTransaction(
        id: String,
        title: String,
        amountPaise: Long,
        type: com.invatech.pockit.ui.home.model.WalletTransactionType,
        category: String
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<WalletTransactionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: WalletTransactionEntity): Long

    @Query("DELETE FROM wallet_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String): Int

    @Query("DELETE FROM wallet_transactions")
    suspend fun clearTransactions(): Int
}
