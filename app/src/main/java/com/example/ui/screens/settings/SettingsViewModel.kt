package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.BackupRepository
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.PurchaseRepository
import com.example.data.repository.SalesRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.StockMovementRepository
import com.example.domain.model.BusinessSettings
import com.example.domain.model.Expense
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.PaymentMethod
import com.example.domain.model.Product
import com.example.domain.model.Sale
import com.example.domain.model.SaleItem
import com.example.domain.usecase.BusinessCalculations
import com.example.domain.usecase.PriceCalculationResult
import com.example.utils.CsvExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PriceCalcMode {
    MARGIN_ON_SALE, // Margen sobre precio de venta (Recomendado)
    MARKUP_ON_COST // Recargo sobre costo
}

data class SettingsUiState(
    val businessSettings: BusinessSettings = BusinessSettings(),
    val calcCostInput: String = "1000",
    val calcPercentInput: String = "40",
    val calcMode: PriceCalcMode = PriceCalcMode.MARGIN_ON_SALE,
    val calcResult: PriceCalculationResult = PriceCalculationResult(0.0, 0.0, 0.0, 0.0),
    val exportedJsonText: String? = null,
    val exportedCsvContent: String? = null,
    val exportedCsvTitle: String? = null,
    val isResetConfirmOpen: Boolean = false,
    val isDemoConfirmOpen: Boolean = false,
    val isImportDialogOpen: Boolean = false,
    val isProcessing: Boolean = false,
    val userFeedbackMessage: String? = null,
    val errorMessage: String? = null
)

private data class SettingsCalcState(
    val costInput: String = "1000",
    val percentInput: String = "40",
    val mode: PriceCalcMode = PriceCalcMode.MARGIN_ON_SALE
)

private data class SettingsDialogsState(
    val jsonText: String? = null,
    val csvContent: String? = null,
    val csvTitle: String? = null,
    val resetOpen: Boolean = false,
    val demoOpen: Boolean = false,
    val importOpen: Boolean = false,
    val processing: Boolean = false,
    val feedback: String? = null,
    val error: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val productRepository: ProductRepository,
    private val salesRepository: SalesRepository,
    private val purchaseRepository: PurchaseRepository,
    private val expenseRepository: ExpenseRepository,
    private val stockMovementRepository: StockMovementRepository
) : ViewModel() {

    private val _calcCost = MutableStateFlow("1000")
    private val _calcPercent = MutableStateFlow("40")
    private val _calcMode = MutableStateFlow(PriceCalcMode.MARGIN_ON_SALE)

    private val _exportedJson = MutableStateFlow<String?>(null)
    private val _exportedCsv = MutableStateFlow<String?>(null)
    private val _exportedCsvTitle = MutableStateFlow<String?>(null)
    private val _isResetOpen = MutableStateFlow(false)
    private val _isDemoOpen = MutableStateFlow(false)
    private val _isImportDialogOpen = MutableStateFlow(false)
    private val _isProcessing = MutableStateFlow(false)
    private val _feedback = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)

    private val _calcStateFlow = combine(_calcCost, _calcPercent, _calcMode) { cost, pct, mode ->
        SettingsCalcState(cost, pct, mode)
    }

    private val _dialogsStateFlow = combine(
        combine(_exportedJson, _exportedCsv, _exportedCsvTitle) { j, c, ct -> Triple(j, c, ct) },
        combine(_isResetOpen, _isDemoOpen, _isImportDialogOpen) { r, d, i -> Triple(r, d, i) },
        combine(_isProcessing, _feedback, _error) { p, f, e -> Triple(p, f, e) }
    ) { (j, c, ct), (r, d, i), (p, f, e) ->
        SettingsDialogsState(
            jsonText = j,
            csvContent = c,
            csvTitle = ct,
            resetOpen = r,
            demoOpen = d,
            importOpen = i,
            processing = p,
            feedback = f,
            error = e
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settingsFlow,
        _calcStateFlow,
        _dialogsStateFlow
    ) { settings, calc, dialogs ->
        val cost = calc.costInput.toDoubleOrNull() ?: 0.0
        val pct = calc.percentInput.toDoubleOrNull() ?: 0.0

        val calcResult = if (calc.mode == PriceCalcMode.MARGIN_ON_SALE) {
            BusinessCalculations.calculatePriceByMarginOnSale(cost, pct)
        } else {
            BusinessCalculations.calculatePriceByMarkupOnCost(cost, pct)
        }

        SettingsUiState(
            businessSettings = settings,
            calcCostInput = calc.costInput,
            calcPercentInput = calc.percentInput,
            calcMode = calc.mode,
            calcResult = calcResult,
            exportedJsonText = dialogs.jsonText,
            exportedCsvContent = dialogs.csvContent,
            exportedCsvTitle = dialogs.csvTitle,
            isResetConfirmOpen = dialogs.resetOpen,
            isDemoConfirmOpen = dialogs.demoOpen,
            isImportDialogOpen = dialogs.importOpen,
            isProcessing = dialogs.processing,
            userFeedbackMessage = dialogs.feedback,
            errorMessage = dialogs.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun onCalcCostChange(cost: String) {
        _calcCost.value = cost
    }

    fun onCalcPercentChange(percent: String) {
        _calcPercent.value = percent
    }

    fun onCalcModeChange(mode: PriceCalcMode) {
        _calcMode.value = mode
    }

    fun updateBusinessProfile(name: String, type: String, currencySymbol: String, currencyCode: String) {
        viewModelScope.launch {
            try {
                val current = settingsRepository.getSettings()
                val updated = current.copy(
                    businessName = name.trim().ifBlank { "Mi Negocio" },
                    businessType = type.trim().ifBlank { "Comercio" },
                    currencySymbol = currencySymbol.trim().ifBlank { "$" },
                    currencyCode = currencyCode.trim().ifBlank { "ARS" }
                )
                settingsRepository.updateSettings(updated)
                _feedback.value = "Datos del negocio actualizados correctamente"
            } catch (e: Exception) {
                _error.value = "Error al actualizar perfil: ${e.localizedMessage}"
            }
        }
    }

    fun generateBackupJson() {
        viewModelScope.launch {
            try {
                val json = backupRepository.exportBackupJson()
                _exportedJson.value = json
                _feedback.value = "Copia de seguridad JSON generada"
            } catch (e: Exception) {
                _error.value = "Error al generar backup: ${e.localizedMessage}"
            }
        }
    }

    fun dismissExportedJson() {
        _exportedJson.value = null
    }

    fun openImportDialog() {
        _isImportDialogOpen.value = true
    }

    fun closeImportDialog() {
        _isImportDialogOpen.value = false
    }

    fun importBackupJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val success = backupRepository.importBackupJson(jsonString)
                if (success) {
                    _feedback.value = "Copia de seguridad restaurada correctamente"
                    closeImportDialog()
                } else {
                    _error.value = "El formato del archivo de respaldo JSON no es válido"
                }
            } catch (e: Exception) {
                _error.value = "Error al restaurar: ${e.localizedMessage}"
            }
        }
    }

    fun exportProductsCsv() {
        viewModelScope.launch {
            val products = productRepository.allActiveProducts.first()
            _exportedCsv.value = CsvExporter.exportProductsToCsv(products)
            _exportedCsvTitle.value = "Catálogo de Productos (CSV)"
        }
    }

    fun exportSalesCsv() {
        viewModelScope.launch {
            val sales = salesRepository.allSales.first()
            _exportedCsv.value = CsvExporter.exportSalesToCsv(sales)
            _exportedCsvTitle.value = "Historial de Ventas (CSV)"
        }
    }

    fun exportPurchasesCsv() {
        viewModelScope.launch {
            val purchases = purchaseRepository.allPurchases.first()
            _exportedCsv.value = CsvExporter.exportPurchasesToCsv(purchases)
            _exportedCsvTitle.value = "Compras a Proveedores (CSV)"
        }
    }

    fun exportExpensesCsv() {
        viewModelScope.launch {
            val expenses = expenseRepository.allExpenses.first()
            _exportedCsv.value = CsvExporter.exportExpensesToCsv(expenses)
            _exportedCsvTitle.value = "Gastos Operativos (CSV)"
        }
    }

    fun exportMovementsCsv() {
        viewModelScope.launch {
            val movements = stockMovementRepository.allMovements.first()
            _exportedCsv.value = CsvExporter.exportMovementsToCsv(movements)
            _exportedCsvTitle.value = "Auditoría de Stock (CSV)"
        }
    }

    fun dismissExportedCsv() {
        _exportedCsv.value = null
        _exportedCsvTitle.value = null
    }

    fun openResetConfirm() {
        _isResetOpen.value = true
    }

    fun closeResetConfirm() {
        _isResetOpen.value = false
    }

    fun wipeDatabase() {
        viewModelScope.launch {
            try {
                backupRepository.clearAllDatabase()
                _feedback.value = "Base de datos restablecida a cero"
                closeResetConfirm()
            } catch (e: Exception) {
                _error.value = "Error al restablecer: ${e.localizedMessage}"
            }
        }
    }

    fun openDemoDataConfirm() {
        _isDemoOpen.value = true
    }

    fun closeDemoDataConfirm() {
        _isDemoOpen.value = false
    }

    fun loadDemoData() {
        viewModelScope.launch {
            try {
                loadPanaderiaDemoData()
                _feedback.value = "¡Datos de demostración cargados con éxito!"
                closeDemoDataConfirm()
            } catch (e: Exception) {
                _error.value = "Error al cargar demo: ${e.localizedMessage}"
            }
        }
    }

    private suspend fun loadPanaderiaDemoData() {
        settingsRepository.updateSettings(
            BusinessSettings(
                businessName = "Panadería & Rotisería La Espiga",
                businessType = "Panadería y Almacén",
                currencyCode = "ARS",
                currencySymbol = "$"
            )
        )

        val demoProducts = listOf(
            Product(name = "Pan Francés (Kg)", categoryId = 1, costPrice = 1200.0, salePrice = 2400.0, currentStock = 45.0, minStock = 10.0, unit = "kg"),
            Product(name = "Medialunas de Manteca (Docena)", categoryId = 1, costPrice = 2500.0, salePrice = 6000.0, currentStock = 18.0, minStock = 5.0, unit = "docena"),
            Product(name = "Facturas Surtidas (Docena)", categoryId = 1, costPrice = 2200.0, salePrice = 5500.0, currentStock = 14.0, minStock = 5.0, unit = "docena"),
            Product(name = "Bizcochitos de Grasa (Kg)", categoryId = 1, costPrice = 1800.0, salePrice = 4200.0, currentStock = 8.0, minStock = 4.0, unit = "kg"),
            Product(name = "Pizza Muzzarella Lista", categoryId = 2, costPrice = 3000.0, salePrice = 7500.0, currentStock = 12.0, minStock = 4.0, unit = "unidad"),
            Product(name = "Empanadas de Carne (Docena)", categoryId = 2, costPrice = 5000.0, salePrice = 12000.0, currentStock = 10.0, minStock = 3.0, unit = "docena"),
            Product(name = "Coca-Cola 1.5L", categoryId = 3, costPrice = 1500.0, salePrice = 2800.0, currentStock = 3.0, minStock = 6.0, unit = "unidad"),
            Product(name = "Agua Mineral 500ml", categoryId = 3, costPrice = 600.0, salePrice = 1200.0, currentStock = 24.0, minStock = 10.0, unit = "unidad"),
            Product(name = "Sándwich de Miga Jamón y Queso (Unidad)", categoryId = 4, costPrice = 800.0, salePrice = 2200.0, currentStock = 20.0, minStock = 6.0, unit = "unidad"),
            Product(name = "Alfajor de Maicena Artesanal", categoryId = 5, costPrice = 500.0, salePrice = 1500.0, currentStock = 1.0, minStock = 8.0, unit = "unidad")
        )

        val insertedIds = demoProducts.map { prod ->
            productRepository.insertProduct(prod)
        }

        val now = System.currentTimeMillis()
        val oneHour = 3600000L
        val oneDay = 86400000L

        // Today sale 1
        salesRepository.registerSale(
            Sale(
                totalAmount = 8400.0,
                totalCost = 3700.0,
                profit = 4700.0,
                paymentMethod = PaymentMethod.EFECTIVO,
                customerNote = "Cliente habitual",
                timestamp = now - (2 * oneHour),
                items = listOf(
                    SaleItem(productId = insertedIds[0], productName = "Pan Francés (Kg)", unitCost = 1200.0, unitPrice = 2400.0, quantity = 1.0, subtotal = 2400.0, totalCost = 1200.0),
                    SaleItem(productId = insertedIds[1], productName = "Medialunas de Manteca (Docena)", unitCost = 2500.0, unitPrice = 6000.0, quantity = 1.0, subtotal = 6000.0, totalCost = 2500.0)
                )
            )
        )

        // Today sale 2
        salesRepository.registerSale(
            Sale(
                totalAmount = 14800.0,
                totalCost = 6500.0,
                profit = 8300.0,
                paymentMethod = PaymentMethod.TRANSFERENCIA,
                customerNote = "Pedido almuerzo",
                timestamp = now - (4 * oneHour),
                items = listOf(
                    SaleItem(productId = insertedIds[4], productName = "Pizza Muzzarella Lista", unitCost = 3000.0, unitPrice = 7500.0, quantity = 1.0, subtotal = 7500.0, totalCost = 3000.0),
                    SaleItem(productId = insertedIds[6], productName = "Coca-Cola 1.5L", unitCost = 1500.0, unitPrice = 2800.0, quantity = 1.0, subtotal = 2800.0, totalCost = 1500.0),
                    SaleItem(productId = insertedIds[8], productName = "Alfajor de Maicena Artesanal", unitCost = 500.0, unitPrice = 1500.0, quantity = 3.0, subtotal = 4500.0, totalCost = 1500.0)
                )
            )
        )

        // Yesterday sale
        salesRepository.registerSale(
            Sale(
                totalAmount = 24000.0,
                totalCost = 10000.0,
                profit = 14000.0,
                paymentMethod = PaymentMethod.TRANSFERENCIA,
                customerNote = "Catering cumpleaños",
                timestamp = now - oneDay,
                items = listOf(
                    SaleItem(productId = insertedIds[5], productName = "Empanadas de Carne (Docena)", unitCost = 5000.0, unitPrice = 12000.0, quantity = 2.0, subtotal = 24000.0, totalCost = 10000.0)
                )
            )
        )

        // Demo Expenses
        expenseRepository.insertExpense(
            Expense(category = ExpenseCategory.ELECTRICIDAD, amount = 25000.0, description = "Factura de Luz - Consumo hornos y heladeras")
        )
        expenseRepository.insertExpense(
            Expense(category = ExpenseCategory.ALQUILER, amount = 90000.0, description = "Alquiler del local - Mes corriente")
        )
        expenseRepository.insertExpense(
            Expense(category = ExpenseCategory.INSUMOS, amount = 12000.0, description = "Bolsas de papel y bandejas descartables")
        )
    }

    fun clearFeedback() {
        _feedback.value = null
        _error.value = null
    }
}
