package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Expense
import com.example.domain.model.ExpenseCategory

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String,
    val amount: Double,
    val description: String = ""
) {
    fun toDomain(): Expense = Expense(
        id = id,
        timestamp = timestamp,
        category = ExpenseCategory.fromString(category),
        amount = amount,
        description = description
    )

    companion object {
        fun fromDomain(expense: Expense): ExpenseEntity = ExpenseEntity(
            id = expense.id,
            timestamp = expense.timestamp,
            category = expense.category.name,
            amount = expense.amount,
            description = expense.description
        )
    }
}
