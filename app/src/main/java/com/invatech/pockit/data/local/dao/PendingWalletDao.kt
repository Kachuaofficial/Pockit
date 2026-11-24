package com.invatech.pockit.data.local.dao

import androidx.room.*
import com.invatech.pockit.data.local.entity.PendingWalletEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingWalletDao {
    @Query("SELECT * FROM pending_wallet_entries ORDER BY createdAtMillis, id")
    fun observe(): Flow<List<PendingWalletEntry>>
    @Query("SELECT * FROM pending_wallet_entries ORDER BY createdAtMillis, id")
    suspend fun all(): List<PendingWalletEntry>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: PendingWalletEntry)
    @Query("DELETE FROM pending_wallet_entries WHERE id IN (:ids)")
    suspend fun delete(ids: List<String>)
}
