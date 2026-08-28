package com.example.ui.screens.earnings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ProductRanking
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.theme.Blue600
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(
    viewModel: EarningsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val currencySymbol = state.businessSettings.currencySymbol
    val currencyCode = state.businessSettings.currencyCode
    val summary = state.profitSummary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ganancias & Rentabilidad",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Period Filters
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(EarningsPeriod.entries) { period ->
                        FilterChip(
                            selected = state.selectedPeriod == period,
                            onClick = { viewModel.setPeriod(period) },
                            label = { Text(period.label) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Real Net Profit Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (summary.netProfit >= 0) Emerald50 else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GANANCIA NETA REAL (${summary.periodTitle.uppercase()})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = if (summary.netProfit >= 0) Emerald700 else MaterialTheme.colorScheme.error
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = (if (summary.netProfit >= 0) Emerald600 else MaterialTheme.colorScheme.error).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Margen Neto: ${CurrencyFormatter.formatPercent(summary.netMarginPercent)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (summary.netProfit >= 0) Emerald700 else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = CurrencyFormatter.format(summary.netProfit, currencySymbol, currencyCode),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (summary.netProfit >= 0) Emerald700 else MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Ventas (${CurrencyFormatter.format(summary.totalRevenue, currencySymbol, currencyCode)}) − Costo de mercadería (${CurrencyFormatter.format(summary.costOfGoodsSold, currencySymbol, currencyCode)}) − Gastos operativos (${CurrencyFormatter.format(summary.totalExpenses, currencySymbol, currencyCode)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Stat Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Ventas Totales",
                        value = CurrencyFormatter.format(summary.totalRevenue, currencySymbol, currencyCode),
                        subtitle = "${summary.salesCount} tickets cobrados",
                        icon = Icons.Default.MonetizationOn,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Ganancia Bruta",
                        value = CurrencyFormatter.format(summary.grossProfit, currencySymbol, currencyCode),
                        subtitle = "Margen: ${CurrencyFormatter.formatPercent(summary.grossMarginPercent)}",
                        icon = Icons.Default.TrendingUp,
                        valueColor = Emerald600,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Gastos Totales",
                        value = CurrencyFormatter.format(summary.totalExpenses, currencySymbol, currencyCode),
                        subtitle = "Costos operativos",
                        icon = Icons.Default.AttachMoney,
                        valueColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Ticket Promedio",
                        value = CurrencyFormatter.format(summary.averageTicket, currencySymbol, currencyCode),
                        subtitle = "Promedio por cliente",
                        icon = Icons.Default.Receipt,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Detailed P&L Breakdown Card
            item {
                SectionHeader(title = "Desglose contable")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PlBreakdownRow(
                            label = "(+) Total Ingresos por Ventas",
                            amount = CurrencyFormatter.format(summary.totalRevenue, currencySymbol, currencyCode),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        PlBreakdownRow(
                            label = "(-) Costo de Mercadería Vendida (COGS)",
                            amount = "-${CurrencyFormatter.format(summary.costOfGoodsSold, currencySymbol, currencyCode)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        PlBreakdownRow(
                            label = "(=) Ganancia Bruta",
                            amount = CurrencyFormatter.format(summary.grossProfit, currencySymbol, currencyCode),
                            color = Emerald600,
                            isBold = true
                        )
                        PlBreakdownRow(
                            label = "(-) Gastos Operativos (Luz, Alquiler, etc.)",
                            amount = "-${CurrencyFormatter.format(summary.totalExpenses, currencySymbol, currencyCode)}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        PlBreakdownRow(
                            label = "(=) Ganancia Neta en Bolsillo",
                            amount = CurrencyFormatter.format(summary.netProfit, currencySymbol, currencyCode),
                            color = if (summary.netProfit >= 0) Emerald600 else MaterialTheme.colorScheme.error,
                            isBold = true
                        )
                    }
                }
            }

            // Top Products by Profit
            if (state.topProductsByProfit.isNotEmpty()) {
                item {
                    SectionHeader(title = "Productos más rentables (Ganancia)")
                }

                items(state.topProductsByProfit.take(5)) { ranking ->
                    ProductRankingCard(
                        ranking = ranking,
                        currencySymbol = currencySymbol,
                        currencyCode = currencyCode,
                        showByProfit = true
                    )
                }
            }

            // Top Products by Units
            if (state.topProductsByUnits.isNotEmpty()) {
                item {
                    SectionHeader(title = "Productos más vendidos (Unidades)")
                }

                items(state.topProductsByUnits.take(5)) { ranking ->
                    ProductRankingCard(
                        ranking = ranking,
                        currencySymbol = currencySymbol,
                        currencyCode = currencyCode,
                        showByProfit = false
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun PlBreakdownRow(
    label: String,
    amount: String,
    color: Color,
    isBold: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = amount,
            style = if (isBold) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold) else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

@Composable
fun ProductRankingCard(
    ranking: ProductRanking,
    currencySymbol: String,
    currencyCode: String,
    showByProfit: Boolean,
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
                Text(
                    text = ranking.productName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${CurrencyFormatter.formatQuantity(ranking.totalUnitsSold, ranking.unit)} vendidos • Ingresos: ${CurrencyFormatter.format(ranking.totalRevenue, currencySymbol, currencyCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (showByProfit) {
                    Text(
                        text = "+${CurrencyFormatter.format(ranking.totalProfit, currencySymbol, currencyCode)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Emerald600
                    )
                    Text(
                        text = "${CurrencyFormatter.formatPercent(ranking.grossMarginPercent)} margen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = CurrencyFormatter.formatQuantity(ranking.totalUnitsSold, ranking.unit),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "+${CurrencyFormatter.format(ranking.totalProfit, currencySymbol, currencyCode)} gan.",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Emerald600
                    )
                }
            }
        }
    }
}
