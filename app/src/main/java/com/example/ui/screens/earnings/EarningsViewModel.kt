package com.example.ui.screens.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.SalesRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.BusinessSettings
import com.example.domain.model.Expense
import com.example.domain.model.Product
import com.example.domain.model.ProductRanking
import com.example.domain.model.ProfitSummary
import com.example.domain.model.Sale
import com.example.domain.usecase.BusinessCalculations
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class EarningsPeriod(val label: String) {
    TODAY("Hoy"),
    LAST_7_DAYS("7 días"),
    THIS_MONTH("Este mes"),
    PREVIOUS_MONTH("Mes anterior"),
    ALL_TIME("Histórico")
}

data class EarningsUiState(
    val selectedPeriod: EarningsPeriod = EarningsPeriod.THIS_MONTH,
    val profitSummary: ProfitSummary = ProfitSummary("Este mes", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0.0),
    val topProductsByProfit: List<ProductRanking> = emptyList(),
    val topProductsByUnits: List<ProductRanking> = emptyList(),
    val businessSettings: BusinessSettings = BusinessSettings(),
    val salesInPeriod: List<Sale> = emptyList(),
    val expensesInPeriod: List<Expense> = emptyList()
)

class EarningsViewModel(
    private val salesRepository: SalesRepository,
    private val expenseRepository: ExpenseRepository,
    private val productRepository: ProductRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(EarningsPeriod.THIS_MONTH)

    val uiState: StateFlow<EarningsUiState> = combine(
        salesRepository.allSales,
        expenseRepository.allExpenses,
        productRepository.allActiveProducts,
        settingsRepository.settingsFlow,
        _selectedPeriod
    ) { sales, expenses, products, settings, period ->
        val startOfToday = DateUtils.getStartOfToday()
        val startOf7d = DateUtils.getStartOfDaysAgo(7)
        val startOfThisMonth = DateUtils.getStartOfCurrentMonth()
        val startOfPrevMonth = DateUtils.getStartOfPreviousMonth()
        val endOfPrevMonth = DateUtils.getEndOfPreviousMonth()

        val filteredSales = sales.filter { s ->
            when (period) {
                EarningsPeriod.TODAY -> s.timestamp >= startOfToday
                EarningsPeriod.LAST_7_DAYS -> s.timestamp >= startOf7d
                EarningsPeriod.THIS_MONTH -> s.timestamp >= startOfThisMonth
                EarningsPeriod.PREVIOUS_MONTH -> s.timestamp in startOfPrevMonth..endOfPrevMonth
                EarningsPeriod.ALL_TIME -> true
            }
        }

        val filteredExpenses = expenses.filter { e ->
            when (period) {
                EarningsPeriod.TODAY -> e.timestamp >= startOfToday
                EarningsPeriod.LAST_7_DAYS -> e.timestamp >= startOf7d
                EarningsPeriod.THIS_MONTH -> e.timestamp >= startOfThisMonth
                EarningsPeriod.PREVIOUS_MONTH -> e.timestamp in startOfPrevMonth..endOfPrevMonth
                EarningsPeriod.ALL_TIME -> true
            }
        }

        val summary = BusinessCalculations.calculateProfitSummary(
            periodTitle = period.label,
            sales = filteredSales,
            expenses = filteredExpenses
        )

        val productsMap = products.associateBy { it.id }
        val rankings = BusinessCalculations.calculateProductRankings(filteredSales, productsMap)

        EarningsUiState(
            selectedPeriod = period,
            profitSummary = summary,
            topProductsByProfit = rankings.sortedByDescending { it.totalProfit },
            topProductsByUnits = rankings.sortedByDescending { it.totalUnitsSold },
            businessSettings = settings,
            salesInPeriod = filteredSales,
            expensesInPeriod = filteredExpenses
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EarningsUiState()
    )

    fun setPeriod(period: EarningsPeriod) {
        _selectedPeriod.value = period
    }
}
