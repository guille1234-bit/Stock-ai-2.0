package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.StockAiEngine
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.SalesRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.AiInsight
import com.example.domain.model.BusinessSettings
import com.example.domain.model.Expense
import com.example.domain.model.Product
import com.example.domain.model.Sale
import com.example.domain.usecase.BusinessCalculations
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val businessSettings: BusinessSettings = BusinessSettings(),
    val todayRevenue: Double = 0.0,
    val todayProfit: Double = 0.0,
    val todaySalesCount: Int = 0,
    val monthRevenue: Double = 0.0,
    val monthNetProfit: Double = 0.0,
    val totalActiveProducts: Int = 0,
    val lowStockProducts: List<Product> = emptyList(),
    val outOfStockCount: Int = 0,
    val recentSales: List<Sale> = emptyList(),
    val aiInsights: List<AiInsight> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val productRepository: ProductRepository,
    private val salesRepository: SalesRepository,
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val stockAiEngine: StockAiEngine
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.settingsFlow,
        productRepository.allActiveProducts,
        salesRepository.allSales,
        expenseRepository.allExpenses
    ) { settings, products, allSales, allExpenses ->
        val startOfToday = DateUtils.getStartOfToday()
        val startOf7d = DateUtils.getStartOfDaysAgo(7)
        val startOfMonth = DateUtils.getStartOfCurrentMonth()

        val salesToday = allSales.filter { it.timestamp >= startOfToday }
        val sales7d = allSales.filter { it.timestamp >= startOf7d }
        val salesMonth = allSales.filter { it.timestamp >= startOfMonth }
        val expensesMonth = allExpenses.filter { it.timestamp >= startOfMonth }

        val todayRevenue = salesToday.sumOf { it.totalAmount }
        val todayProfit = salesToday.sumOf { it.profit }

        val monthSummary = BusinessCalculations.calculateProfitSummary("Mes actual", salesMonth, expensesMonth)

        val lowStockList = products.filter { it.currentStock <= it.minStock }
        val outOfStockCount = products.count { it.currentStock <= 0.0 }

        val insights = stockAiEngine.generateLocalInsights(
            products = products,
            salesLast7Days = sales7d,
            salesLast30Days = allSales.filter { it.timestamp >= DateUtils.getStartOfDaysAgo(30) },
            expensesCurrentMonth = expensesMonth,
            currencySymbol = settings.currencySymbol,
            currencyCode = settings.currencyCode
        )

        HomeUiState(
            businessSettings = settings,
            todayRevenue = todayRevenue,
            todayProfit = todayProfit,
            todaySalesCount = salesToday.size,
            monthRevenue = monthSummary.totalRevenue,
            monthNetProfit = monthSummary.netProfit,
            totalActiveProducts = products.size,
            lowStockProducts = lowStockList,
            outOfStockCount = outOfStockCount,
            recentSales = allSales.take(5),
            aiInsights = insights,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )
}
