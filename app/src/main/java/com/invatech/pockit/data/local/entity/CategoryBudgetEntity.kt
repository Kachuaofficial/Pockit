package com.invatech.pockit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_budgets")
data class CategoryBudgetEntity(
    @PrimaryKey
    val category: String, // e.g. "Food & Drink", "Shopping", "Transport", "Groceries", "Entertainment", "Bills & Utilities", "Health", "Other"
    val monthlyLimitPaise: Long,
    val warningThresholdPercent: Int = 80,
    val updatedAtMillis: Long = System.currentTimeMillis()
)
