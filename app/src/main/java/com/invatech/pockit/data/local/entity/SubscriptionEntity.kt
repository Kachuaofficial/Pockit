package com.invatech.pockit.data.local.entity

/*
 * Local recurring-expense definition with paise amount, billing cycle, due date and reminder preferences.
 */

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val amountPaise: Long,
    val billingCycle: String, // MONTHLY, YEARLY, WEEKLY, QUARTERLY
    val dueDayOfMonth: Int, // 1..31
    val category: String, // Entertainment, Utilities, Housing / EMI, Fitness, Investment / SIP, Other
    val reminderDaysBefore: Int = 1,
    val paymentMethod: String = "Auto-debit / UPI",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val nextDueMillis: Long = 0L
)
