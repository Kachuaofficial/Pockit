package com.invatech.pockit.data.repository

import com.invatech.pockit.PockitApplication
import com.invatech.pockit.data.local.dao.NotificationDao
import com.invatech.pockit.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class NotificationRepository {

    private val localDao: NotificationDao? by lazy {
        runCatching { PockitApplication.instance.database.notificationDao() }.getOrNull()
    }

    fun observeNotifications(): Flow<List<NotificationEntity>> =
        localDao?.observeNotifications() ?: emptyFlow()

    fun observeUnreadCount(): Flow<Int> =
        localDao?.observeUnreadCount() ?: emptyFlow()

    suspend fun insertNotification(
        title: String,
        body: String,
        type: String = "general",
        targetId: String? = null
    ): Long {
        return localDao?.insertNotification(
            NotificationEntity(
                title = title,
                body = body,
                type = type,
                targetId = targetId,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
        ) ?: -1L
    }

    suspend fun markAsRead(id: String) {
        localDao?.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        localDao?.markAllAsRead()
    }

    suspend fun deleteNotification(id: String) {
        localDao?.deleteNotification(id)
    }

    suspend fun clearAll() {
        localDao?.clearAllNotifications()
    }

    suspend fun ensureInitialWelcomeNotification() {
        val count = localDao?.getNotificationCount() ?: 0
        if (count == 0) {
            localDao?.insertNotification(
                NotificationEntity(
                    title = "Welcome to Pockit! \uD83D\uDE80",
                    body = "Track daily spends, split bills with friends in Spaces, and get instant notifications here.",
                    type = "general",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
            )
        }
    }
}
