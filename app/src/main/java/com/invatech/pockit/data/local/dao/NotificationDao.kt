package com.invatech.pockit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.invatech.pockit.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun observeNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getNotificationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>): List<Long>

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String): Int

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead(): Int

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: String): Int

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications(): Int
}
