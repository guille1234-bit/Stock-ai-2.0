package com.example.ui.screens.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.SectionHeader
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

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
                        text = "Ajustes & Herramientas",
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Negocio", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Calculadora", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Datos & Copias", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Save, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> BusinessProfileTab(state = state, viewModel = viewModel)
                1 -> PriceCalculatorTab(state = state, viewModel = viewModel)
                2 -> DataManagementTab(state = state, viewModel = viewModel)
            }
        }
    }

    // Dialogs
    if (state.exportedJsonText != null) {
        ExportedDataDialog(
            title = "Copia de Seguridad (JSON)",
            content = state.exportedJsonText!!,
            onDismiss = { viewModel.dismissExportedJson() }
        )
    }

    if (state.exportedCsvContent != null) {
        ExportedDataDialog(
            title = state.exportedCsvTitle ?: "Exportación CSV",
            content = state.exportedCsvContent!!,
            onDismiss = { viewModel.dismissExportedCsv() }
        )
    }

    if (state.isImportDialogOpen) {
        ImportBackupDialog(
            onDismiss = { viewModel.closeImportDialog() },
            onImport = { json -> viewModel.importBackupJson(json) }
        )
    }

    if (state.isDemoConfirmOpen) {
        ConfirmDialog(
            title = "Cargar Datos de Ejemplo",
            message = "Se cargarán productos, ventas, compras y gastos de una panadería/almacén modelo para que puedas explorar todas las funciones de Stock AI con datos realistas.",
            confirmText = "Cargar Datos",
            onConfirm = { viewModel.loadDemoData() },
            onDismiss = { viewModel.closeDemoDataConfirm() }
        )
    }

    if (state.isResetConfirmOpen) {
        ConfirmDialog(
            title = "Restablecer Base de Datos",
            message = "⚠️ Esta acción eliminará todas las ventas, compras, productos y movimientos registrados en este dispositivo y reiniciará la base a cero. ¿Deseas continuar?",
            confirmText = "Restablecer a Cero",
            isDestructive = true,
            onConfirm = { viewModel.wipeDatabase() },
            onDismiss = { viewModel.closeResetConfirm() }
        )
    }
}

@Composable
fun BusinessProfileTab(
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    var name by remember(state.businessSettings.businessName) { mutableStateOf(state.businessSettings.businessName) }
    var type by remember(state.businessSettings.businessType) { mutableStateOf(state.businessSettings.businessType) }
    var symbol by remember(state.businessSettings.currencySymbol) { mutableStateOf(state.businessSettings.currencySymbol) }
    var code by remember(state.businessSettings.currencyCode) { mutableStateOf(state.businessSettings.currencyCode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Perfil del Negocio",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre del Negocio") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = type,
            onValueChange = { type = it },
            label = { Text("Rubro / Tipo de Comercio") },
            placeholder = { Text("Ej: Panadería, Kiosco, Almacén, Rotisería...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = symbol,
                onValueChange = { symbol = it },
                label = { Text("Símbolo Moneda") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Código Moneda") },
                placeholder = { Text("ARS, USD, MXN...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Button(
            onClick = { viewModel.updateBusinessProfile(name, type, symbol, code) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Guardar Cambios", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PriceCalculatorTab(
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val currencySymbol = state.businessSettings.currencySymbol
    val currencyCode = state.businessSettings.currencyCode
    val res = state.calcResult

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Calculadora Inteligente de Precios y Márgenes",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Text(
            text = "Aprende y calcula el precio exacto que debes cobrar para obtener el margen de ganancia que deseas sin perder dinero.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Mode selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.calcMode == PriceCalcMode.MARGIN_ON_SALE,
                onClick = { viewModel.onCalcModeChange(PriceCalcMode.MARGIN_ON_SALE) },
                label = { Text("Margen s/ Venta (Recomendado)") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = state.calcMode == PriceCalcMode.MARKUP_ON_COST,
                onClick = { viewModel.onCalcModeChange(PriceCalcMode.MARKUP_ON_COST) },
                label = { Text("Recargo s/ Costo (Markup)") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.calcCostInput,
                onValueChange = viewModel::onCalcCostChange,
                label = { Text("Costo del Producto ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.calcPercentInput,
                onValueChange = viewModel::onCalcPercentChange,
                label = { Text("% Deseado") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Result Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Emerald50)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "PRECIO DE VENTA SUGERIDO",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Emerald700
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = CurrencyFormatter.format(res.recommendedPrice, currencySymbol, currencyCode),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Emerald700
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Emerald600.copy(alpha = 0.2f)))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Ganancia neta por unidad:", style = MaterialTheme.typography.bodySmall, color = Emerald700)
                        Text(
                            text = "+${CurrencyFormatter.format(res.profit, currencySymbol, currencyCode)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Emerald700
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Margen Real sobre Venta:", style = MaterialTheme.typography.bodySmall, color = Emerald700)
                        Text(
                            text = CurrencyFormatter.formatPercent(res.grossMarginPercent),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Emerald700
                        )
                    }
                }
            }
        }

        // Educational Difference Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "¿Cuál es la diferencia?",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Margen sobre Venta: Si vendes a $1000 con un 40% de margen, te quedan $400 limpios en mano.\n• Recargo sobre Costo (Markup): Si te cuesta $1000 y le sumas 40%, cobras $1400, pero tu margen real sobre la venta final es del 28.5%.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DataManagementTab(
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Copias de seguridad")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.generateBackupJson() },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exportar JSON")
            }

            OutlinedButton(
                onClick = { viewModel.openImportDialog() },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Importar JSON")
            }
        }

        SectionHeader(title = "Exportar tablas a Excel / CSV")

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.exportProductsCsv() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar Catálogo de Productos (CSV)")
            }

            OutlinedButton(
                onClick = { viewModel.exportSalesCsv() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar Historial de Ventas (CSV)")
            }

            OutlinedButton(
                onClick = { viewModel.exportPurchasesCsv() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar Compras a Proveedores (CSV)")
            }

            OutlinedButton(
                onClick = { viewModel.exportExpensesCsv() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar Gastos Operativos (CSV)")
            }

            OutlinedButton(
                onClick = { viewModel.exportMovementsCsv() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar Auditoría de Movimientos (CSV)")
            }
        }

        SectionHeader(title = "Mantenimiento & Demo")

        OutlinedButton(
            onClick = { viewModel.openDemoDataConfirm() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cargar Datos de Demostración (Panadería/Kiosco)")
        }

        Button(
            onClick = { viewModel.openResetConfirm() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Restablecer Base de Datos a Cero")
        }
    }
}

@Composable
fun ExportedDataDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Puedes copiar y guardar este contenido en un archivo:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
fun ImportBackupDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restaurar Copia JSON", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pega a continuación el código JSON de tu copia de seguridad:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    placeholder = { Text("{\"version\":1, \"businessSettings\": ...}") },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(jsonText) },
                enabled = jsonText.isNotBlank()
            ) {
                Text("Restaurar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
