package com.example.ui.screens.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CategoryRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.SalesRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.BusinessSettings
import com.example.domain.model.Category
import com.example.domain.model.PaymentMethod
import com.example.domain.model.Product
import com.example.domain.model.Sale
import com.example.domain.model.SaleItem
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartItem(
    val product: Product,
    val quantity: Double = 1.0,
    val unitSalePrice: Double = product.salePrice,
    val unitCostPrice: Double = product.costPrice
) {
    val unitPrice: Double
        get() = unitSalePrice

    val unitCost: Double
        get() = unitCostPrice

    val subtotal: Double
        get() = quantity * unitSalePrice

    val totalCost: Double
        get() = quantity * unitCostPrice

    val profit: Double
        get() = subtotal - totalCost
}

data class SalesUiState(
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val cartItems: List<CartItem> = emptyList(),
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val customerNote: String = "",
    val isCartOpen: Boolean = false,
    val completedSale: Sale? = null,
    val isProcessingSale: Boolean = false,
    val userFeedbackMessage: String? = null,
    val errorMessage: String? = null,
    val businessSettings: BusinessSettings = BusinessSettings(),
    val allSales: List<Sale> = emptyList(),
    val filteredSales: List<Sale> = emptyList(),
    val historySearchQuery: String = "",
    val selectedHistoryPeriod: SalesPeriodFilter = SalesPeriodFilter.TODAY
) {
    val filteredSalesHistory: List<Sale>
        get() = filteredSales

    val cartTotalAmount: Double
        get() = cartItems.sumOf { it.subtotal }

    val cartTotalProfit: Double
        get() = cartItems.sumOf { it.profit }

    val cartTotalItemsCount: Int
        get() = cartItems.size
}

enum class SalesPeriodFilter(val label: String) {
    TODAY("Hoy"),
    LAST_7_DAYS("7 días"),
    THIS_MONTH("Este mes"),
    ALL("Todas")
}

private data class PosState(
    val query: String = "",
    val catId: Long? = null,
    val cart: List<CartItem> = emptyList(),
    val payment: PaymentMethod = PaymentMethod.EFECTIVO,
    val note: String = "",
    val isCartOpen: Boolean = false,
    val completed: Sale? = null,
    val processing: Boolean = false,
    val feedback: String? = null,
    val error: String? = null,
    val historyQuery: String = "",
    val historyPeriod: SalesPeriodFilter = SalesPeriodFilter.TODAY
)

class SalesViewModel(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val salesRepository: SalesRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    private val _selectedPaymentMethod = MutableStateFlow(PaymentMethod.EFECTIVO)
    private val _customerNote = MutableStateFlow("")
    private val _isCartOpen = MutableStateFlow(false)
    private val _completedSale = MutableStateFlow<Sale?>(null)
    private val _isProcessingSale = MutableStateFlow(false)
    private val _userFeedback = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _historySearchQuery = MutableStateFlow("")
    private val _selectedHistoryPeriod = MutableStateFlow(SalesPeriodFilter.TODAY)

    private val _posStateFlow = combine(
        combine(_searchQuery, _selectedCategoryId, _cartItems, _selectedPaymentMethod) { q, cat, cart, pay ->
            PosPart1(q, cat, cart, pay)
        },
        combine(_customerNote, _isCartOpen, _completedSale, _isProcessingSale) { note, open, comp, proc ->
            PosPart2(note, open, comp, proc)
        },
        combine(_userFeedback, _errorMessage, _historySearchQuery, _selectedHistoryPeriod) { fb, err, hq, hp ->
            PosPart3(fb, err, hq, hp)
        }
    ) { p1, p2, p3 ->
        PosState(
            query = p1.query,
            catId = p1.catId,
            cart = p1.cart,
            payment = p1.payment,
            note = p2.note,
            isCartOpen = p2.isOpen,
            completed = p2.completed,
            processing = p2.processing,
            feedback = p3.feedback,
            error = p3.error,
            historyQuery = p3.hq,
            historyPeriod = p3.hp
        )
    }

    val uiState: StateFlow<SalesUiState> = combine(
        productRepository.allActiveProducts,
        categoryRepository.allCategories,
        settingsRepository.settingsFlow,
        salesRepository.allSales,
        _posStateFlow
    ) { products, categories, settings, sales, pos ->
        val filteredProducts = products.filter { p ->
            val matchesQuery = pos.query.isBlank() ||
                    p.name.contains(pos.query, ignoreCase = true) ||
                    p.sku.contains(pos.query, ignoreCase = true)
            val matchesCategory = pos.catId == null || p.categoryId == pos.catId
            matchesQuery && matchesCategory
        }

        val startOfToday = DateUtils.getStartOfToday()
        val startOf7d = DateUtils.getStartOfDaysAgo(7)
        val startOfMonth = DateUtils.getStartOfCurrentMonth()

        val filteredSales = sales.filter { s ->
            val matchesPeriod = when (pos.historyPeriod) {
                SalesPeriodFilter.TODAY -> s.timestamp >= startOfToday
                SalesPeriodFilter.LAST_7_DAYS -> s.timestamp >= startOf7d
                SalesPeriodFilter.THIS_MONTH -> s.timestamp >= startOfMonth
                SalesPeriodFilter.ALL -> true
            }
            val matchesSearch = pos.historyQuery.isBlank() ||
                    s.customerNote.contains(pos.historyQuery, ignoreCase = true) ||
                    s.items.any { it.productName.contains(pos.historyQuery, ignoreCase = true) }
            matchesPeriod && matchesSearch
        }

        SalesUiState(
            products = products,
            filteredProducts = filteredProducts,
            categories = categories,
            selectedCategoryId = pos.catId,
            searchQuery = pos.query,
            cartItems = pos.cart,
            selectedPaymentMethod = pos.payment,
            customerNote = pos.note,
            isCartOpen = pos.isCartOpen,
            completedSale = pos.completed,
            isProcessingSale = pos.processing,
            userFeedbackMessage = pos.feedback,
            errorMessage = pos.error,
            businessSettings = settings,
            allSales = sales,
            filteredSales = filteredSales,
            historySearchQuery = pos.historyQuery,
            selectedHistoryPeriod = pos.historyPeriod
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SalesUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryFilterSelected(categoryId: Long?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
    }

    fun onSelectCategory(categoryId: Long?) = onCategoryFilterSelected(categoryId)

    fun addToCart(product: Product) {
        val current = _cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == product.id }
        if (existingIndex != -1) {
            val item = current[existingIndex]
            current[existingIndex] = item.copy(quantity = item.quantity + 1.0)
        } else {
            current.add(CartItem(product = product, quantity = 1.0))
        }
        _cartItems.value = current
    }

    fun updateCartItemQuantity(productId: Long, quantity: Double) {
        if (quantity <= 0.0) {
            removeFromCart(productId)
            return
        }
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            current[index] = current[index].copy(quantity = quantity)
            _cartItems.value = current
        }
    }

    fun removeFromCart(productId: Long) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _customerNote.value = ""
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
    }

    fun setCustomerNote(note: String) {
        _customerNote.value = note
    }

    fun openCart() {
        _isCartOpen.value = true
        _errorMessage.value = null
    }

    fun closeCart() {
        _isCartOpen.value = false
    }

    fun processCheckout() {
        val cart = _cartItems.value
        if (cart.isEmpty()) {
            _errorMessage.value = "El carrito está vacío"
            return
        }

        _isProcessingSale.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val totalAmount = cart.sumOf { it.subtotal }
                val totalCost = cart.sumOf { it.totalCost }
                val totalProfit = totalAmount - totalCost

                val saleItems = cart.map { c ->
                    SaleItem(
                        productId = c.product.id,
                        productName = c.product.name,
                        unitCost = c.unitCostPrice,
                        unitPrice = c.unitSalePrice,
                        quantity = c.quantity,
                        unit = c.product.unit,
                        subtotal = c.subtotal,
                        totalCost = c.totalCost
                    )
                }

                val sale = Sale(
                    totalAmount = totalAmount,
                    totalCost = totalCost,
                    profit = totalProfit,
                    paymentMethod = _selectedPaymentMethod.value,
                    customerNote = _customerNote.value.trim(),
                    items = saleItems,
                    timestamp = System.currentTimeMillis()
                )

                val saleId = salesRepository.registerSale(sale)
                val finished = sale.copy(id = saleId)

                _completedSale.value = finished
                _cartItems.value = emptyList()
                _customerNote.value = ""
                _isCartOpen.value = false
                _userFeedback.value = "¡Venta registrada con éxito!"
            } catch (e: Exception) {
                _errorMessage.value = "Error al registrar la venta: ${e.localizedMessage ?: "Error desconocido"}"
            } finally {
                _isProcessingSale.value = false
            }
        }
    }

    fun completeSale() = processCheckout()

    fun dismissCompletedSale() {
        _completedSale.value = null
    }

    fun onHistorySearchChange(query: String) {
        _historySearchQuery.value = query
    }

    fun onHistorySearchQueryChange(query: String) = onHistorySearchChange(query)

    fun onHistoryPeriodSelected(period: SalesPeriodFilter) {
        _selectedHistoryPeriod.value = period
    }

    fun onSelectHistoryPeriod(period: SalesPeriodFilter) = onHistoryPeriodSelected(period)

    fun clearFeedback() {
        _userFeedback.value = null
        _errorMessage.value = null
    }
}

private data class PosPart1(val query: String, val catId: Long?, val cart: List<CartItem>, val payment: PaymentMethod)
private data class PosPart2(val note: String, val isOpen: Boolean, val completed: Sale?, val processing: Boolean)
private data class PosPart3(val feedback: String?, val error: String?, val hq: String, val hp: SalesPeriodFilter)
