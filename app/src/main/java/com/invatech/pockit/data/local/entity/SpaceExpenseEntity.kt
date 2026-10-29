package com.invatech.pockit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.invatech.pockit.ui.space.model.SpaceExpense

@Entity(tableName = "space_expenses")
data class SpaceExpenseEntity(
    @PrimaryKey val id: String,
    val spaceId: String,
    val title: String,
    val amount: Double,
    val paidById: String,
    val shares: Map<String, Double>,
    val dateLabel: String,
    val isSettlement: Boolean = false,
    val isConfirmed: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toSpaceExpense(): SpaceExpense = SpaceExpense(
        id = id,
        title = title,
        amount = amount,
        paidById = paidById,
        shares = shares,
        dateLabel = dateLabel,
        isSettlement = isSettlement,
        isConfirmed = isConfirmed
    )

    companion object {
        fun fromSpaceExpense(spaceId: String, expense: SpaceExpense): SpaceExpenseEntity = SpaceExpenseEntity(
            id = expense.id,
            spaceId = spaceId,
            title = expense.title,
            amount = expense.amount,
            paidById = expense.paidById,
            shares = expense.shares,
            dateLabel = expense.dateLabel,
            isSettlement = expense.isSettlement,
            isConfirmed = expense.isConfirmed
        )
    }
}
