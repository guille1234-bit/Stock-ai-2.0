package com.example.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.ai.GeminiAdvisorService
import com.example.data.ai.StockAiEngine
import com.example.data.database.AppDatabase
import com.example.data.repository.BackupRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.PurchaseRepository
import com.example.data.repository.SalesRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.StockMovementRepository
import com.example.data.repository.SupplierRepository
import com.example.ui.screens.ai.AiAssistantViewModel
import com.example.ui.screens.earnings.EarningsViewModel
import com.example.ui.screens.expenses.ExpensesViewModel
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.movements.MovementsViewModel
import com.example.ui.screens.products.ProductsViewModel
import com.example.ui.screens.purchases.PurchasesViewModel
import com.example.ui.screens.sales.SalesViewModel
import com.example.ui.screens.settings.SettingsViewModel

class AppViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    private val database by lazy { AppDatabase.getDatabase(application) }

    private val productRepository by lazy { ProductRepository(database) }
    private val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    private val salesRepository by lazy { SalesRepository(database) }
    private val purchaseRepository by lazy { PurchaseRepository(database) }
    private val expenseRepository by lazy { ExpenseRepository(database.expenseDao()) }
    private val supplierRepository by lazy { SupplierRepository(database.supplierDao()) }
    private val stockMovementRepository by lazy { StockMovementRepository(database) }
    private val settingsRepository by lazy { SettingsRepository(database.settingsDao()) }
    private val backupRepository by lazy { BackupRepository(database) }

    private val stockAiEngine by lazy { StockAiEngine() }
    private val geminiAdvisorService by lazy { GeminiAdvisorService() }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    productRepository = productRepository,
                    salesRepository = salesRepository,
                    expenseRepository = expenseRepository,
                    settingsRepository = settingsRepository,
                    stockAiEngine = stockAiEngine
                ) as T
            }
            modelClass.isAssignableFrom(ProductsViewModel::class.java) -> {
                ProductsViewModel(
                    productRepository = productRepository,
                    categoryRepository = categoryRepository,
                    supplierRepository = supplierRepository,
                    settingsRepository = settingsRepository
                ) as T
            }
            modelClass.isAssignableFrom(SalesViewModel::class.java) -> {
                SalesViewModel(
                    productRepository = productRepository,
                    categoryRepository = categoryRepository,
                    salesRepository = salesRepository,
                    settingsRepository = settingsRepository
                ) as T
            }
            modelClass.isAssignableFrom(PurchasesViewModel::class.java) -> {
                PurchasesViewModel(
                    productRepository = productRepository,
                    supplierRepository = supplierRepository,
                    purchaseRepository = purchaseRepository,
                    settingsRepository = settingsRepository
                ) as T
            }
            modelClass.isAssignableFrom(ExpensesViewModel::class.java) -> {
                ExpensesViewModel(
                    expenseRepository = expenseRepository,
                    settingsRepository = settingsRepository
                ) as T
            }
            modelClass.isAssignableFrom(MovementsViewModel::class.java) -> {
                MovementsViewModel(
                    stockMovementRepository = stockMovementRepository,
                    productRepository = productRepository,
                    settingsRepository = settingsRepository
                ) as T
            }
            modelClass.isAssignableFrom(EarningsViewModel::class.java) -> {
                EarningsViewModel(
                    salesRepository = salesRepository,
                    expenseRepository = expenseRepository,
                    productRepository = productRepository,
                    settingsRepository = settingsRepository
                ) as T
            }
            modelClass.isAssignableFrom(AiAssistantViewModel::class.java) -> {
                AiAssistantViewModel(
                    productRepository = productRepository,
                    salesRepository = salesRepository,
                    expenseRepository = expenseRepository,
                    settingsRepository = settingsRepository,
                    stockAiEngine = stockAiEngine,
                    geminiAdvisorService = geminiAdvisorService
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    settingsRepository = settingsRepository,
                    backupRepository = backupRepository,
                    productRepository = productRepository,
                    salesRepository = salesRepository,
                    purchaseRepository = purchaseRepository,
                    expenseRepository = expenseRepository,
                    stockMovementRepository = stockMovementRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
