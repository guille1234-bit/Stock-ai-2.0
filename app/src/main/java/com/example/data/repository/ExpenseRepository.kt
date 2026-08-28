package com.example.data.repository

import com.example.data.database.dao.ExpenseDao
import com.example.data.database.entity.ExpenseEntity
import com.example.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepository(
    private val expenseDao: ExpenseDao
) {
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
        .map { list -> list.map { it.toDomain() } }

    fun getExpensesByDateRange(startTime: Long, endTime: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRange(startTime, endTime).map { list -> list.map { it.toDomain() } }

    suspend fun getExpenseById(id: Long): Expense? =
        expenseDao.getExpenseById(id)?.toDomain()

    suspend fun createExpense(expense: Expense): Long =
        expenseDao.insertExpense(ExpenseEntity.fromDomain(expense))

    suspend fun insertExpense(expense: Expense): Long = createExpense(expense)

    suspend fun updateExpense(expense: Expense) =
        expenseDao.updateExpense(ExpenseEntity.fromDomain(expense))

    suspend fun deleteExpense(id: Long) =
        expenseDao.deleteExpense(id)
}
