package com.example.ui.screens.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Category
import com.example.domain.model.Product
import com.example.domain.model.Supplier
import com.example.ui.components.AppSearchBar
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.StockStatusBadge
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald50
import com.example.ui.theme.PureWhite
import com.example.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: ProductsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.userFeedbackMessage, state.errorMessage) {
        state.userFeedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Catálogo de Productos",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddProductForm() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = PureWhite,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Nuevo Producto")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                AppSearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    placeholder = "Buscar por nombre, código SKU o categoría..."
                )
            }

            // Categories Filter Bar
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategoryId == null,
                        onClick = { viewModel.onSelectCategory(null) },
                        label = { Text("Todos (${state.products.size})") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                items(state.categories) { category ->
                    val isSelected = state.selectedCategoryId == category.id
                    val count = state.products.count { it.categoryId == category.id }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onSelectCategory(if (isSelected) null else category.id) },
                        label = { Text("${category.iconEmoji} ${category.name} ($count)") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Products List
            if (state.filteredProducts.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Inventory2,
                    title = if (state.searchQuery.isNotBlank() || state.selectedCategoryId != null) "No se encontraron productos" else "Sin productos aún",
                    description = if (state.searchQuery.isNotBlank() || state.selectedCategoryId != null) "Prueba con otra búsqueda o limpia los filtros." else "Empieza agregando tus productos con su precio y costo.",
                    actionLabel = if (state.searchQuery.isBlank() && state.selectedCategoryId == null) "Crear Primer Producto" else "Limpiar Filtros",
                    onActionClick = {
                        if (state.searchQuery.isBlank() && state.selectedCategoryId == null) {
                            viewModel.openAddProductForm()
                        } else {
                            viewModel.onSearchQueryChange("")
                            viewModel.onSelectCategory(null)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.filteredProducts, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            currencySymbol = state.businessSettings.currencySymbol,
                            currencyCode = state.businessSettings.currencyCode,
                            onEdit = { viewModel.openEditProductForm(product) },
                            onDelete = { viewModel.promptDeleteProduct(product.id) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Product Bottom Sheet
    if (state.isFormOpen) {
        ProductFormBottomSheet(
            product = state.editingProduct,
            categories = state.categories,
            suppliers = state.suppliers,
            currencySymbol = state.businessSettings.currencySymbol,
            currencyCode = state.businessSettings.currencyCode,
            onDismiss = { viewModel.closeForm() },
            onSave = { id, name, desc, catId, catName, sku, cost, sale, stock, minStock, unit, supId, supName ->
                viewModel.saveProduct(id, name, desc, catId, catName, sku, cost, sale, stock, minStock, unit, supId, supName)
            }
        )
    }

    // Delete Confirmation Dialog
    if (state.isDeletingProductId != null) {
        val prodToDelete = state.products.firstOrNull { it.id == state.isDeletingProductId }
        ConfirmDialog(
            title = "Eliminar Producto",
            message = "¿Estás seguro de que deseas eliminar '${prodToDelete?.name ?: "este producto"}'? Su historial de ventas y compras se mantendrá para la contabilidad.",
            confirmText = "Eliminar",
            isDestructive = true,
            onConfirm = { viewModel.confirmDeleteProduct(state.isDeletingProductId!!) },
            onDismiss = { viewModel.cancelDeleteProduct() }
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    currencySymbol: String,
    currencyCode: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = product.categoryName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (product.sku.isNotBlank()) {
                            Text(
                                text = "SKU: ${product.sku}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                StockStatusBadge(status = product.stockStatus)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stock & Price Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Stock disponible",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatQuantity(product.currentStock, product.unit),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Precio de Venta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(product.salePrice, currencySymbol, currencyCode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
            Spacer(modifier = Modifier.height(8.dp))

            // Profit & Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Costo: ${CurrencyFormatter.format(product.costPrice, currencySymbol, currencyCode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = "•", color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "+${CurrencyFormatter.format(product.unitProfit, currencySymbol, currencyCode)} (${CurrencyFormatter.formatPercent(product.grossMarginPercent)})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Emerald600
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormBottomSheet(
    product: Product?,
    categories: List<Category>,
    suppliers: List<Supplier>,
    currencySymbol: String,
    currencyCode: String,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        description: String,
        categoryId: Long?,
        categoryName: String,
        sku: String,
        costPrice: Double,
        salePrice: Double,
        currentStock: Double,
        minStock: Double,
        unit: String,
        supplierId: Long?,
        supplierName: String?
    ) -> Unit
) {
    val isEdit = product != null
    var name by remember { mutableStateOf(product?.name ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var sku by remember { mutableStateOf(product?.sku ?: "") }
    var costPriceStr by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var salePriceStr by remember { mutableStateOf(product?.salePrice?.toString() ?: "") }
    var currentStockStr by remember { mutableStateOf(product?.currentStock?.toString() ?: "0") }
    var minStockStr by remember { mutableStateOf(product?.minStock?.toString() ?: "5") }

    val units = listOf("unidad", "kg", "g", "litro", "ml", "paquete", "docena")
    var selectedUnit by remember { mutableStateOf(product?.unit ?: "unidad") }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    var selectedCategoryId by remember { mutableStateOf(product?.categoryId ?: categories.firstOrNull()?.id) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var selectedSupplierId by remember { mutableStateOf(product?.supplierId) }
    var supplierDropdownExpanded by remember { mutableStateOf(false) }

    val cost = costPriceStr.toDoubleOrNull() ?: 0.0
    val sale = salePriceStr.toDoubleOrNull() ?: 0.0
    val unitProfit = sale - cost
    val grossMargin = if (sale > 0.0) ((sale - cost) / sale) * 100.0 else 0.0

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isEdit) "Editar Producto" else "Nuevo Producto",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del Producto *") },
                placeholder = { Text("Ej: Pan Francés, Pizza Muzzarella...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category & Unit Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.weight(1.2f)
                ) {
                    val currentCatName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Categoría"
                    OutlinedTextField(
                        value = currentCatName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.iconEmoji} ${cat.name}") },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Unit Selector
                ExposedDropdownMenuBox(
                    expanded = unitDropdownExpanded,
                    onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded },
                    modifier = Modifier.weight(0.8f)
                ) {
                    OutlinedTextField(
                        value = selectedUnit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unidad") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = unitDropdownExpanded,
                        onDismissRequest = { unitDropdownExpanded = false }
                    ) {
                        units.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u) },
                                onClick = {
                                    selectedUnit = u
                                    unitDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Prices: Cost & Sale
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = costPriceStr,
                    onValueChange = { costPriceStr = it },
                    label = { Text("Costo ($currencySymbol) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = salePriceStr,
                    onValueChange = { salePriceStr = it },
                    label = { Text("Precio Venta ($currencySymbol) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Live Profit & Margin Preview
            if (sale > 0 || cost > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (grossMargin >= 25.0) Emerald50 else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (grossMargin >= 25.0) Emerald600.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Margen de ganancia bruto",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatPercent(grossMargin),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (grossMargin >= 25.0) Emerald700 else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Ganancia por unidad",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.format(unitProfit, currencySymbol, currencyCode),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (unitProfit >= 0) Emerald700 else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Stock & Minimum Alert Threshold
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = currentStockStr,
                    onValueChange = { currentStockStr = it },
                    label = { Text("Stock Actual ($selectedUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = minStockStr,
                    onValueChange = { minStockStr = it },
                    label = { Text("Alerta Stock Mínimo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // SKU & Description
            OutlinedTextField(
                value = sku,
                onValueChange = { sku = it },
                label = { Text("Código SKU / Barras (Opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        val chosenCat = categories.firstOrNull { it.id == selectedCategoryId }
                        val chosenSup = suppliers.firstOrNull { it.id == selectedSupplierId }
                        onSave(
                            product?.id ?: 0L,
                            name,
                            description,
                            selectedCategoryId,
                            chosenCat?.name ?: "Otros",
                            sku,
                            costPriceStr.toDoubleOrNull() ?: 0.0,
                            salePriceStr.toDoubleOrNull() ?: 0.0,
                            currentStockStr.toDoubleOrNull() ?: 0.0,
                            minStockStr.toDoubleOrNull() ?: 5.0,
                            selectedUnit,
                            selectedSupplierId,
                            chosenSup?.name
                        )
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
