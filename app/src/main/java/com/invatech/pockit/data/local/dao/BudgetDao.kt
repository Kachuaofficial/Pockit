package com.invatech.pockit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.invatech.pockit.data.local.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM category_budgets ORDER BY category ASC")
    fun observeBudgets(): Flow<List<CategoryBudgetEntity>>

    @Query("SELECT * FROM category_budgets")
    suspend fun getAllBudgets(): List<CategoryBudgetEntity>

    @Query("SELECT * FROM category_budgets WHERE category = :category LIMIT 1")
    suspend fun getBudgetForCategory(category: String): CategoryBudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: CategoryBudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgets(budgets: List<CategoryBudgetEntity>): List<Long>

    @Query("DELETE FROM category_budgets WHERE category = :category")
    suspend fun deleteBudget(category: String): Int

    @Query("DELETE FROM category_budgets")
    suspend fun clearBudgets(): Int
    @androidx.room.Transaction
    suspend fun replaceCloudSnapshot(rows: List<CategoryBudgetEntity>) {
        clearBudgets()
        upsertBudgets(rows)
    }
}
