package com.example.ui.screens.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CategoryRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SupplierRepository
import com.example.domain.model.BusinessSettings
import com.example.domain.model.Category
import com.example.domain.model.Product
import com.example.domain.model.Supplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductsUiState(
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val businessSettings: BusinessSettings = BusinessSettings(),
    val isFormOpen: Boolean = false,
    val editingProduct: Product? = null,
    val userFeedbackMessage: String? = null,
    val errorMessage: String? = null,
    val isDeletingProductId: Long? = null
)

private data class ProductUiFilterState(
    val query: String = "",
    val categoryId: Long? = null,
    val isFormOpen: Boolean = false,
    val editingProduct: Product? = null,
    val feedback: String? = null,
    val error: String? = null,
    val deletingId: Long? = null
)

class ProductsViewModel(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val supplierRepository: SupplierRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _isFormOpen = MutableStateFlow(false)
    private val _editingProduct = MutableStateFlow<Product?>(null)
    private val _userFeedback = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isDeletingProductId = MutableStateFlow<Long?>(null)

    private val _filterState = combine(
        combine(_searchQuery, _selectedCategoryId, _isFormOpen) { q, cat, open ->
            Triple(q, cat, open)
        },
        combine(_editingProduct, _userFeedback, _errorMessage, _isDeletingProductId) { edit, fb, err, del ->
            Quad(edit, fb, err, del)
        }
    ) { (q, cat, open), (edit, fb, err, del) ->
        ProductUiFilterState(
            query = q,
            categoryId = cat,
            isFormOpen = open,
            editingProduct = edit,
            feedback = fb,
            error = err,
            deletingId = del
        )
    }

    val uiState: StateFlow<ProductsUiState> = combine(
        productRepository.allActiveProducts,
        categoryRepository.allCategories,
        supplierRepository.allSuppliers,
        settingsRepository.settingsFlow,
        _filterState
    ) { products, categories, suppliers, settings, filter ->
        val filtered = products.filter { p ->
            val matchesQuery = filter.query.isBlank() ||
                    p.name.contains(filter.query, ignoreCase = true) ||
                    p.sku.contains(filter.query, ignoreCase = true) ||
                    p.categoryName.contains(filter.query, ignoreCase = true)

            val matchesCategory = filter.categoryId == null || p.categoryId == filter.categoryId
            matchesQuery && matchesCategory
        }

        ProductsUiState(
            products = products,
            filteredProducts = filtered,
            categories = categories,
            suppliers = suppliers,
            selectedCategoryId = filter.categoryId,
            searchQuery = filter.query,
            businessSettings = settings,
            isFormOpen = filter.isFormOpen,
            editingProduct = filter.editingProduct,
            userFeedbackMessage = filter.feedback,
            errorMessage = filter.error,
            isDeletingProductId = filter.deletingId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductsUiState()
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategoryFilterSelected(categoryId: Long?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
    }

    fun onSelectCategory(categoryId: Long?) = onCategoryFilterSelected(categoryId)

    fun openCreateForm() {
        _editingProduct.value = null
        _isFormOpen.value = true
        _errorMessage.value = null
    }

    fun openAddProductForm() = openCreateForm()

    fun openEditForm(product: Product) {
        _editingProduct.value = product
        _isFormOpen.value = true
        _errorMessage.value = null
    }

    fun openEditProductForm(product: Product) = openEditForm(product)

    fun closeForm() {
        _isFormOpen.value = false
        _editingProduct.value = null
        _errorMessage.value = null
    }

    fun saveProduct(
        id: Long = 0,
        name: String,
        description: String = "",
        categoryId: Long? = null,
        categoryName: String = "Otros",
        sku: String = "",
        costPrice: Double,
        salePrice: Double,
        currentStock: Double,
        minStock: Double = 5.0,
        unit: String = "unidad",
        supplierId: Long? = null,
        supplierName: String? = null
    ) {
        if (name.isBlank()) {
            _errorMessage.value = "El nombre del producto es obligatorio"
            return
        }

        if (salePrice < costPrice) {
            _errorMessage.value = "El precio de venta no puede ser menor al costo"
            return
        }

        viewModelScope.launch {
            try {
                if (id > 0) {
                    val existing = _editingProduct.value ?: productRepository.getProductById(id)
                    val updated = Product(
                        id = id,
                        name = name.trim(),
                        description = description.trim(),
                        categoryId = categoryId,
                        categoryName = categoryName.trim().ifBlank { "Otros" },
                        sku = sku.trim(),
                        salePrice = salePrice,
                        costPrice = costPrice,
                        currentStock = currentStock,
                        minStock = minStock,
                        unit = unit.trim().ifBlank { "unidad" },
                        supplierId = supplierId,
                        supplierName = supplierName,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        isActive = true
                    )
                    productRepository.updateProduct(updated, "Edición de datos de producto")
                    _userFeedback.value = "Producto '${name.trim()}' actualizado correctamente"
                } else {
                    val newProd = Product(
                        name = name.trim(),
                        description = description.trim(),
                        categoryId = categoryId,
                        categoryName = categoryName.trim().ifBlank { "Otros" },
                        sku = sku.trim(),
                        salePrice = salePrice,
                        costPrice = costPrice,
                        currentStock = currentStock,
                        minStock = minStock,
                        unit = unit.trim().ifBlank { "unidad" },
                        supplierId = supplierId,
                        supplierName = supplierName,
                        createdAt = System.currentTimeMillis(),
                        isActive = true
                    )
                    productRepository.insertProduct(newProd)
                    _userFeedback.value = "Producto '${name.trim()}' creado exitosamente"
                }
                closeForm()
            } catch (e: Exception) {
                _errorMessage.value = "Error al guardar: ${e.localizedMessage ?: "Error desconocido"}"
            }
        }
    }

    fun promptDeleteProduct(productId: Long) {
        _isDeletingProductId.value = productId
    }

    fun dismissDeletePrompt() {
        _isDeletingProductId.value = null
    }

    fun cancelDeleteProduct() = dismissDeletePrompt()

    fun confirmDeleteProduct(productId: Long) {
        viewModelScope.launch {
            try {
                productRepository.deleteProduct(productId)
                _userFeedback.value = "Producto eliminado"
                _isDeletingProductId.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar: ${e.localizedMessage}"
            }
        }
    }

    fun confirmDeleteProduct() {
        val id = _isDeletingProductId.value ?: return
        confirmDeleteProduct(id)
    }

    fun clearFeedback() {
        _userFeedback.value = null
        _errorMessage.value = null
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
