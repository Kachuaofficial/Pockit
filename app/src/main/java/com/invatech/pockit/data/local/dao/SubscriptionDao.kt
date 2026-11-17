package com.invatech.pockit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.invatech.pockit.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions ORDER BY dueDayOfMonth ASC")
    fun observeSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE isActive = 1 ORDER BY dueDayOfMonth ASC")
    fun observeActiveSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions")
    suspend fun getAllSubscriptions(): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions WHERE isActive = 1")
    suspend fun getActiveSubscriptions(): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    suspend fun getSubscriptionById(id: String): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptions(subscriptions: List<SubscriptionEntity>): List<Long>

    @Update
    suspend fun updateSubscription(subscription: SubscriptionEntity): Int

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscription(id: String): Int

    @Query("DELETE FROM subscriptions")
    suspend fun clearSubscriptions(): Int
    @androidx.room.Transaction
    suspend fun replaceCloudSnapshot(rows: List<SubscriptionEntity>) {
        clearSubscriptions()
        insertSubscriptions(rows)
    }
}
