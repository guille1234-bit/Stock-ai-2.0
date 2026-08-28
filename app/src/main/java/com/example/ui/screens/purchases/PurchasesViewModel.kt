package com.example.ui.screens.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ProductRepository
import com.example.data.repository.PurchaseRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SupplierRepository
import com.example.domain.model.BusinessSettings
import com.example.domain.model.Product
import com.example.domain.model.Purchase
import com.example.domain.model.PurchaseItem
import com.example.domain.model.Supplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PurchaseCartItem(
    val product: Product,
    val quantity: Double = 1.0,
    val unitCost: Double = product.costPrice,
    val updateCatalogCost: Boolean = true
) {
    val unitCostPrice: Double
        get() = unitCost

    val subtotal: Double
        get() = quantity * unitCost
}

data class PurchasesUiState(
    val products: List<Product> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val allPurchases: List<Purchase> = emptyList(),
    val businessSettings: BusinessSettings = BusinessSettings(),
    val isFormOpen: Boolean = false,
    val selectedSupplierId: Long? = null,
    val purchaseItems: List<PurchaseCartItem> = emptyList(),
    val purchaseNote: String = "",
    val isProcessing: Boolean = false,
    val userFeedbackMessage: String? = null,
    val errorMessage: String? = null
) {
    val totalAmount: Double
        get() = purchaseItems.sumOf { it.subtotal }

    val totalPurchaseAmount: Double
        get() = totalAmount
}

private data class PurchaseFormState(
    val isOpen: Boolean = false,
    val supplierId: Long? = null,
    val items: List<PurchaseCartItem> = emptyList(),
    val note: String = "",
    val processing: Boolean = false,
    val feedback: String? = null,
    val error: String? = null
)

class PurchasesViewModel(
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository,
    private val purchaseRepository: PurchaseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isFormOpen = MutableStateFlow(false)
    private val _selectedSupplierId = MutableStateFlow<Long?>(null)
    private val _purchaseItems = MutableStateFlow<List<PurchaseCartItem>>(emptyList())
    private val _purchaseNote = MutableStateFlow("")
    private val _isProcessing = MutableStateFlow(false)
    private val _userFeedback = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _formStateFlow = combine(
        combine(_isFormOpen, _selectedSupplierId, _purchaseItems) { open, sup, items ->
            Triple(open, sup, items)
        },
        combine(_purchaseNote, _isProcessing, _userFeedback, _errorMessage) { note, proc, fb, err ->
            QuadPurchase(note, proc, fb, err)
        }
    ) { (open, sup, items), (note, proc, fb, err) ->
        PurchaseFormState(
            isOpen = open,
            supplierId = sup,
            items = items,
            note = note,
            processing = proc,
            feedback = fb,
            error = err
        )
    }

    val uiState: StateFlow<PurchasesUiState> = combine(
        productRepository.allActiveProducts,
        supplierRepository.allSuppliers,
        purchaseRepository.allPurchases,
        settingsRepository.settingsFlow,
        _formStateFlow
    ) { products, suppliers, purchases, settings, form ->
        PurchasesUiState(
            products = products,
            suppliers = suppliers,
            allPurchases = purchases,
            businessSettings = settings,
            isFormOpen = form.isOpen,
            selectedSupplierId = form.supplierId,
            purchaseItems = form.items,
            purchaseNote = form.note,
            isProcessing = form.processing,
            userFeedbackMessage = form.feedback,
            errorMessage = form.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PurchasesUiState()
    )

    fun openNewPurchaseForm() {
        _isFormOpen.value = true
        _purchaseItems.value = emptyList()
        _purchaseNote.value = ""
        _selectedSupplierId.value = null
        _errorMessage.value = null
    }

    fun closeForm() {
        _isFormOpen.value = false
        _purchaseItems.value = emptyList()
        _errorMessage.value = null
    }

    fun selectSupplier(supplierId: Long?) {
        _selectedSupplierId.value = supplierId
    }

    fun setSupplier(supplierId: Long?) = selectSupplier(supplierId)

    fun setPurchaseNote(note: String) {
        _purchaseNote.value = note
    }

    fun setNote(note: String) = setPurchaseNote(note)

    fun addProductToPurchase(product: Product) {
        val current = _purchaseItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            val item = current[index]
            current[index] = item.copy(quantity = item.quantity + 1.0)
        } else {
            current.add(PurchaseCartItem(product = product, quantity = 1.0, unitCost = product.costPrice, updateCatalogCost = true))
        }
        _purchaseItems.value = current
    }

    fun addItemToPurchase(product: Product) = addProductToPurchase(product)

    fun updateItemQuantity(productId: Long, newQuantity: Double) {
        if (newQuantity <= 0.0) {
            removeItem(productId)
            return
        }
        val current = _purchaseItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            current[index] = current[index].copy(quantity = newQuantity)
            _purchaseItems.value = current
        }
    }

    fun updateItemCost(productId: Long, newCost: Double) {
        val current = _purchaseItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            current[index] = current[index].copy(unitCost = newCost)
            _purchaseItems.value = current
        }
    }

    fun updateToggleUpdateCatalog(productId: Long, update: Boolean) {
        val current = _purchaseItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            current[index] = current[index].copy(updateCatalogCost = update)
            _purchaseItems.value = current
        }
    }

    fun updatePurchaseItem(productId: Long, quantity: Double, unitCost: Double) {
        if (quantity <= 0.0) {
            removePurchaseItem(productId)
            return
        }
        val current = _purchaseItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            current[index] = current[index].copy(quantity = quantity, unitCost = unitCost)
            _purchaseItems.value = current
        }
    }

    fun removePurchaseItem(productId: Long) {
        _purchaseItems.value = _purchaseItems.value.filter { it.product.id != productId }
    }

    fun removeItem(productId: Long) = removePurchaseItem(productId)

    fun savePurchase() {
        val items = _purchaseItems.value
        if (items.isEmpty()) {
            _errorMessage.value = "Agrega al menos un producto a la compra"
            return
        }

        _isProcessing.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val totalAmount = items.sumOf { it.subtotal }
                val purchaseModelItems = items.map { item ->
                    PurchaseItem(
                        productId = item.product.id,
                        productName = item.product.name,
                        quantity = item.quantity,
                        unitCost = item.unitCost,
                        unit = item.product.unit,
                        subtotal = item.subtotal,
                        updateProductCost = item.updateCatalogCost
                    )
                }

                val supId = _selectedSupplierId.value
                val supName = uiState.value.suppliers.firstOrNull { it.id == supId }?.name

                val purchase = Purchase(
                    supplierId = supId,
                    supplierName = supName,
                    totalAmount = totalAmount,
                    note = _purchaseNote.value.trim(),
                    items = purchaseModelItems,
                    timestamp = System.currentTimeMillis()
                )

                purchaseRepository.registerPurchase(purchase)
                _userFeedback.value = "Compra registrada y stock actualizado automáticamente"
                closeForm()
            } catch (e: Exception) {
                _errorMessage.value = "Error al guardar compra: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearFeedback() {
        _userFeedback.value = null
        _errorMessage.value = null
    }
}

private data class QuadPurchase<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
