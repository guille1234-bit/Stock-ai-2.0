package com.example.ui.screens.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ProductRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.StockMovementRepository
import com.example.domain.model.BusinessSettings
import com.example.domain.model.MovementType
import com.example.domain.model.Product
import com.example.domain.model.StockMovement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MovementsUiState(
    val allMovements: List<StockMovement> = emptyList(),
    val filteredMovements: List<StockMovement> = emptyList(),
    val products: List<Product> = emptyList(),
    val selectedTypeFilter: MovementType? = null,
    val searchQuery: String = "",
    val businessSettings: BusinessSettings = BusinessSettings(),
    val isAdjustDialogOpen: Boolean = false,
    val isLossDialogOpen: Boolean = false,
    val selectedProductForAction: Product? = null,
    val userFeedbackMessage: String? = null,
    val errorMessage: String? = null
)

private data class MovementsLocalState(
    val typeFilter: MovementType? = null,
    val query: String = "",
    val isAdjustOpen: Boolean = false,
    val isLossOpen: Boolean = false,
    val selProd: Product? = null,
    val feedback: String? = null,
    val error: String? = null
)

class MovementsViewModel(
    private val stockMovementRepository: StockMovementRepository,
    private val productRepository: ProductRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedTypeFilter = MutableStateFlow<MovementType?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isAdjustDialogOpen = MutableStateFlow(false)
    private val _isLossDialogOpen = MutableStateFlow(false)
    private val _selectedProductForAction = MutableStateFlow<Product?>(null)
    private val _userFeedback = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _localStateFlow = combine(
        combine(_selectedTypeFilter, _searchQuery, _isAdjustDialogOpen) { tf, q, adj ->
            Triple(tf, q, adj)
        },
        combine(_isLossDialogOpen, _selectedProductForAction, _userFeedback, _errorMessage) { loss, prod, fb, err ->
            QuadMove(loss, prod, fb, err)
        }
    ) { (tf, q, adj), (loss, prod, fb, err) ->
        MovementsLocalState(
            typeFilter = tf,
            query = q,
            isAdjustOpen = adj,
            isLossOpen = loss,
            selProd = prod,
            feedback = fb,
            error = err
        )
    }

    val uiState: StateFlow<MovementsUiState> = combine(
        stockMovementRepository.allMovements,
        productRepository.allActiveProducts,
        settingsRepository.settingsFlow,
        _localStateFlow
    ) { movements, products, settings, local ->
        val filtered = movements.filter { m ->
            val matchesType = local.typeFilter == null || m.type == local.typeFilter
            val matchesQuery = local.query.isBlank() ||
                    m.productName.contains(local.query, ignoreCase = true) ||
                    m.reason.contains(local.query, ignoreCase = true)
            matchesType && matchesQuery
        }

        MovementsUiState(
            allMovements = movements,
            filteredMovements = filtered,
            products = products,
            selectedTypeFilter = local.typeFilter,
            searchQuery = local.query,
            businessSettings = settings,
            isAdjustDialogOpen = local.isAdjustOpen,
            isLossDialogOpen = local.isLossOpen,
            selectedProductForAction = local.selProd,
            userFeedbackMessage = local.feedback,
            errorMessage = local.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MovementsUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTypeFilterSelected(type: MovementType?) {
        _selectedTypeFilter.value = if (_selectedTypeFilter.value == type) null else type
    }

    fun onSelectTypeFilter(type: MovementType?) = onTypeFilterSelected(type)

    fun openAdjustDialog(product: Product? = null) {
        _selectedProductForAction.value = product
        _isAdjustDialogOpen.value = true
        _errorMessage.value = null
    }

    fun closeAdjustDialog() {
        _isAdjustDialogOpen.value = false
        _selectedProductForAction.value = null
    }

    fun openLossDialog(product: Product? = null) {
        _selectedProductForAction.value = product
        _isLossDialogOpen.value = true
        _errorMessage.value = null
    }

    fun closeLossDialog() {
        _isLossDialogOpen.value = false
        _selectedProductForAction.value = null
    }

    fun closeDialogs() {
        closeAdjustDialog()
        closeLossDialog()
    }

    fun submitStockAdjustment(productId: Long, newStock: Double, reason: String) {
        viewModelScope.launch {
            try {
                stockMovementRepository.adjustStock(
                    productId = productId,
                    newStock = newStock,
                    reason = reason.ifBlank { "Ajuste manual de inventario" }
                )
                _userFeedback.value = "Stock ajustado correctamente"
                closeAdjustDialog()
            } catch (e: Exception) {
                _errorMessage.value = "Error al ajustar stock: ${e.localizedMessage}"
            }
        }
    }

    fun submitStockLoss(productId: Long, quantityLost: Double, reason: String) {
        if (quantityLost <= 0.0) {
            _errorMessage.value = "La cantidad debe ser mayor a 0"
            return
        }

        viewModelScope.launch {
            try {
                stockMovementRepository.registerLoss(
                    productId = productId,
                    quantityLost = quantityLost,
                    reason = reason.ifBlank { "Pérdida / Rotura / Vencimiento" }
                )
                _userFeedback.value = "Pérdida registrada y descontada del stock"
                closeLossDialog()
            } catch (e: Exception) {
                _errorMessage.value = "Error al registrar pérdida: ${e.localizedMessage}"
            }
        }
    }

    fun clearFeedback() {
        _userFeedback.value = null
        _errorMessage.value = null
    }
}

private data class QuadMove<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
