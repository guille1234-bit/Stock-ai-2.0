package com.example.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.BusinessSettings
import com.example.domain.model.Expense
import com.example.domain.model.ExpenseCategory
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExpensesUiState(
    val allExpenses: List<Expense> = emptyList(),
    val filteredExpenses: List<Expense> = emptyList(),
    val selectedCategory: ExpenseCategory? = null,
    val selectedMonthFilter: Int = 0, // 0 = Current month, 1 = Previous month, 2 = All
    val businessSettings: BusinessSettings = BusinessSettings(),
    val isFormOpen: Boolean = false,
    val userFeedbackMessage: String? = null,
    val errorMessage: String? = null
) {
    val currentMonthTotal: Double
        get() = filteredExpenses.sumOf { it.amount }

    val totalFilteredExpenses: Double
        get() = filteredExpenses.sumOf { it.amount }

    val expensesCount: Int
        get() = filteredExpenses.size
}

private data class ExpenseUiFilterState(
    val category: ExpenseCategory? = null,
    val monthFilter: Int = 0,
    val isFormOpen: Boolean = false,
    val feedback: String? = null,
    val error: String? = null
)

class ExpensesViewModel(
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<ExpenseCategory?>(null)
    private val _selectedMonthFilter = MutableStateFlow(0)
    private val _isFormOpen = MutableStateFlow(false)
    private val _userFeedback = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _filterState = combine(
        _selectedCategory,
        _selectedMonthFilter,
        _isFormOpen,
        combine(_userFeedback, _errorMessage) { fb, err -> Pair(fb, err) }
    ) { cat, monthFilter, formOpen, (fb, err) ->
        ExpenseUiFilterState(
            category = cat,
            monthFilter = monthFilter,
            isFormOpen = formOpen,
            feedback = fb,
            error = err
        )
    }

    val uiState: StateFlow<ExpensesUiState> = combine(
        expenseRepository.allExpenses,
        settingsRepository.settingsFlow,
        _filterState
    ) { expenses, settings, filter ->
        val startOfCurrentMonth = DateUtils.getStartOfCurrentMonth()
        val startOfPrevMonth = DateUtils.getStartOfPreviousMonth()
        val endOfPrevMonth = DateUtils.getEndOfPreviousMonth()

        val filtered = expenses.filter { e ->
            val matchesMonth = when (filter.monthFilter) {
                0 -> e.timestamp >= startOfCurrentMonth
                1 -> e.timestamp in startOfPrevMonth..endOfPrevMonth
                else -> true
            }
            val matchesCategory = filter.category == null || e.category == filter.category
            matchesMonth && matchesCategory
        }

        ExpensesUiState(
            allExpenses = expenses,
            filteredExpenses = filtered,
            selectedCategory = filter.category,
            selectedMonthFilter = filter.monthFilter,
            businessSettings = settings,
            isFormOpen = filter.isFormOpen,
            userFeedbackMessage = filter.feedback,
            errorMessage = filter.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpensesUiState()
    )

    fun openForm() {
        _isFormOpen.value = true
        _errorMessage.value = null
    }

    fun closeForm() {
        _isFormOpen.value = false
        _errorMessage.value = null
    }

    fun selectMonthFilter(index: Int) {
        _selectedMonthFilter.value = index
    }

    fun setMonthFilter(index: Int) = selectMonthFilter(index)

    fun selectCategoryFilter(category: ExpenseCategory?) {
        _selectedCategory.value = category
    }

    fun setCategoryFilter(category: ExpenseCategory?) = selectCategoryFilter(category)

    fun saveExpense(category: ExpenseCategory, amount: Double, description: String) {
        if (amount <= 0.0) {
            _errorMessage.value = "El monto debe ser mayor a 0"
            return
        }

        viewModelScope.launch {
            try {
                val expense = Expense(
                    category = category,
                    amount = amount,
                    description = description.trim(),
                    timestamp = System.currentTimeMillis()
                )
                expenseRepository.insertExpense(expense)
                _userFeedback.value = "Gasto registrado con éxito"
                closeForm()
            } catch (e: Exception) {
                _errorMessage.value = "Error al guardar gasto: ${e.localizedMessage}"
            }
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            try {
                expenseRepository.deleteExpense(id)
                _userFeedback.value = "Gasto eliminado"
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar: ${e.localizedMessage}"
            }
        }
    }

    fun clearFeedback() {
        _userFeedback.value = null
        _errorMessage.value = null
    }
}
