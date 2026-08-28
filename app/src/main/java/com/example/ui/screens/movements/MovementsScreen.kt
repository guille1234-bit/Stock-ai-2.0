package com.example.ui.screens.movements

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MovementType
import com.example.domain.model.Product
import com.example.domain.model.StockMovement
import com.example.ui.components.AppSearchBar
import com.example.ui.components.EmptyState
import com.example.ui.components.MovementBadge
import com.example.ui.theme.Emerald600
import com.example.utils.CurrencyFormatter
import com.example.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsScreen(
    viewModel: MovementsViewModel,
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
                        text = "Auditoría de Stock",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Quick action buttons: Adjust stock & Record loss
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { viewModel.openAdjustDialog() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ajustar Stock")
                }

                Button(
                    onClick = { viewModel.openLossDialog() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Registrar Pérdida")
                }
            }

            // Search
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                AppSearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    placeholder = "Buscar por producto o motivo..."
                )
            }

            // Movement Types Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedTypeFilter == null,
                        onClick = { viewModel.onSelectTypeFilter(null) },
                        label = { Text("Todos (${state.allMovements.size})") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                items(MovementType.entries) { type ->
                    FilterChip(
                        selected = state.selectedTypeFilter == type,
                        onClick = {
                            viewModel.onSelectTypeFilter(if (state.selectedTypeFilter == type) null else type)
                        },
                        label = { Text(type.label) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Movements List
            if (state.filteredMovements.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Assessment,
                    title = "Sin movimientos registrados",
                    description = "Cada venta, compra, ajuste o pérdida que realices quedará registrada aquí como historial transparente.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.filteredMovements, key = { it.id }) { movement ->
                        StockMovementCard(movement = movement)
                    }
                }
            }
        }
    }

    // Adjust Dialog
    if (state.isAdjustDialogOpen) {
        StockAdjustDialog(
            products = state.products,
            initialProduct = state.selectedProductForAction,
            onDismiss = { viewModel.closeDialogs() },
            onSubmit = { productId, newStock, reason ->
                viewModel.submitStockAdjustment(productId, newStock, reason)
            }
        )
    }

    // Loss Dialog
    if (state.isLossDialogOpen) {
        StockLossDialog(
            products = state.products,
            initialProduct = state.selectedProductForAction,
            onDismiss = { viewModel.closeDialogs() },
            onSubmit = { productId, lostQty, reason ->
                viewModel.submitStockLoss(productId, lostQty, reason)
            }
        )
    }
}

@Composable
fun StockMovementCard(
    movement: StockMovement,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MovementBadge(type = movement.type)
                    Text(
                        text = movement.productName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${movement.reason} • ${DateUtils.getDayLabel(movement.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val isPositive = movement.quantityChange > 0
                Text(
                    text = "${if (isPositive) "+" else ""}${movement.quantityChange}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPositive) Emerald600 else MaterialTheme.colorScheme.error
                    )
                )
                Text(
                    text = "Quedan: ${movement.newStock}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustDialog(
    products: List<Product>,
    initialProduct: Product?,
    onDismiss: () -> Unit,
    onSubmit: (Long, Double, String) -> Unit
) {
    var selectedProductId by remember { mutableStateOf(initialProduct?.id ?: products.firstOrNull()?.id) }
    var newStockStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Conteo de inventario") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val selectedProduct = products.firstOrNull { it.id == selectedProductId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar Stock Real", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Indica la cantidad real que tienes actualmente en el local.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Product Dropdown
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.name ?: "Seleccionar Producto",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Producto") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        products.forEach { prod ->
                            DropdownMenuItem(
                                text = { Text("${prod.name} (Actual: ${prod.currentStock} ${prod.unit})") },
                                onClick = {
                                    selectedProductId = prod.id
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = newStockStr,
                    onValueChange = { newStockStr = it },
                    label = { Text("Nuevo Stock Real (${selectedProduct?.unit ?: "unidades"})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo del ajuste") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedProductId != null) {
                        val stock = newStockStr.toDoubleOrNull() ?: 0.0
                        onSubmit(selectedProductId!!, stock, reason.ifBlank { "Ajuste manual de stock" })
                    }
                },
                enabled = selectedProductId != null && newStockStr.isNotBlank()
            ) {
                Text("Guardar Ajuste")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockLossDialog(
    products: List<Product>,
    initialProduct: Product?,
    onDismiss: () -> Unit,
    onSubmit: (Long, Double, String) -> Unit
) {
    var selectedProductId by remember { mutableStateOf(initialProduct?.id ?: products.firstOrNull()?.id) }
    var lostQtyStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Producto vencido o dañado") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val selectedProduct = products.firstOrNull { it.id == selectedProductId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Pérdida / Merma", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Registra unidades rotas, vencidas o perdidas para descontarlas del stock con su debido motivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.name ?: "Seleccionar Producto",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Producto") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        products.forEach { prod ->
                            DropdownMenuItem(
                                text = { Text("${prod.name} (Stock: ${prod.currentStock} ${prod.unit})") },
                                onClick = {
                                    selectedProductId = prod.id
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = lostQtyStr,
                    onValueChange = { lostQtyStr = it },
                    label = { Text("Cantidad perdida (${selectedProduct?.unit ?: "unidades"})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo de la pérdida") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedProductId != null) {
                        val qty = lostQtyStr.toDoubleOrNull() ?: 0.0
                        onSubmit(selectedProductId!!, qty, reason.ifBlank { "Pérdida/Merma" })
                    }
                },
                enabled = selectedProductId != null && lostQtyStr.isNotBlank() && (lostQtyStr.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Registrar Pérdida")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
