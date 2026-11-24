package com.invatech.pockit.data.local.dao

/*
 * Local space and space-expense queries/inserts used by SpaceRepository caching.
 */

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.invatech.pockit.data.local.entity.SpaceEntity
import com.invatech.pockit.data.local.entity.SpaceExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceDao {

    @Query("SELECT * FROM spaces ORDER BY updatedAt DESC")
    fun observeSpaces(): Flow<List<SpaceEntity>>

    @Query("SELECT * FROM spaces ORDER BY updatedAt DESC")
    suspend fun getAllSpaces(): List<SpaceEntity>

    @Query("SELECT * FROM spaces WHERE id = :id")
    suspend fun getSpaceById(id: String): SpaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpaces(spaces: List<SpaceEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpace(space: SpaceEntity): Long

    @Query("DELETE FROM spaces WHERE id = :id")
    suspend fun deleteSpace(id: String): Int

    @Query("DELETE FROM spaces")
    suspend fun clearSpaces(): Int

    // Space Expenses
    @Query("SELECT * FROM space_expenses WHERE spaceId = :spaceId ORDER BY createdAt DESC")
    fun observeExpenses(spaceId: String): Flow<List<SpaceExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<SpaceExpenseEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: SpaceExpenseEntity): Long

    @Query("DELETE FROM space_expenses WHERE id = :id")
    suspend fun deleteExpense(id: String): Int

    @Query("DELETE FROM space_expenses WHERE spaceId = :spaceId")
    suspend fun deleteExpensesForSpace(spaceId: String): Int
}
