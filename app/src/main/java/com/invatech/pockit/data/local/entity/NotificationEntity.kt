package com.invatech.pockit.data.local.entity

/*
 * Local inbox message row with read state and an optional navigation target; it is not a financial receipt.
 */

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val type: String = "general", // "friend_expense", "space_expense", "settle_up", "general"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val targetId: String? = null
)
