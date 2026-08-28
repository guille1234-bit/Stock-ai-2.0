package com.example.data.ai

import com.example.domain.model.AiInsight
import com.example.domain.model.Expense
import com.example.domain.model.InsightType
import com.example.domain.model.Product
import com.example.domain.model.Sale
import com.example.domain.usecase.BusinessCalculations
import com.example.utils.CurrencyFormatter
import com.example.utils.DateUtils
import java.util.UUID

class StockAiEngine {

    fun generateLocalInsights(
        products: List<Product>,
        salesLast7Days: List<Sale>,
        salesLast30Days: List<Sale>,
        expensesCurrentMonth: List<Expense>,
        currencySymbol: String,
        currencyCode: String
    ): List<AiInsight> {
        val insights = mutableListOf<AiInsight>()

        if (products.isEmpty()) {
            insights.add(
                AiInsight(
                    id = "empty_catalog",
                    title = "Empieza creando tus productos",
                    message = "Agrega tus primeros productos con su costo y precio de venta para que Stock AI pueda calcular tus márgenes y proyectar tu inventario.",
                    type = InsightType.TIP,
                    actionText = "Crear producto"
                )
            )
            return insights
        }

        // 1. Stockout & Low Stock Warnings with estimated days remaining
        val productSalesMap7d = mutableMapOf<Long, Double>()
        for (sale in salesLast7Days) {
            for (item in sale.items) {
                productSalesMap7d[item.productId] = (productSalesMap7d[item.productId] ?: 0.0) + item.quantity
            }
        }

        val outOfStock = products.filter { it.currentStock <= 0.0 }
        val lowStock = products.filter { it.currentStock > 0 && it.currentStock <= it.minStock }

        if (outOfStock.isNotEmpty()) {
            val names = outOfStock.take(2).joinToString(", ") { it.name }
            val extra = if (outOfStock.size > 2) " y ${outOfStock.size - 2} más" else ""
            insights.add(
                AiInsight(
                    id = "out_of_stock_alert",
                    title = "Productos agotados ($ {outOfStock.size})".replace("$ ", "${outOfStock.size} "),
                    message = "Tienes productos sin stock: $names$extra. Podrías estar perdiendo ventas.",
                    type = InsightType.WARNING,
                    actionText = "Registrar compra"
                )
            )
        }

        // Velocity & Run-rate projection
        for (product in products) {
            val sold7d = productSalesMap7d[product.id] ?: 0.0
            if (sold7d > 0) {
                val dailyRunRate = sold7d / 7.0
                val daysToDepletion = (product.currentStock / dailyRunRate).toInt()

                if (product.currentStock > 0 && daysToDepletion <= 3) {
                    insights.add(
                        AiInsight(
                            id = "depletion_${product.id}",
                            title = "Stock por agotarse pronto",
                            message = "Vendiste ${CurrencyFormatter.formatQuantity(sold7d, product.unit)} de '${product.name}' en los últimos 7 días. A este ritmo, tu stock actual durará aprox. $daysToDepletion días.",
                            type = InsightType.PREDICTION,
                            actionText = "Comprar stock",
                            relatedProductId = product.id
                        )
                    )
                } else if (sold7d >= 10 && product.grossMarginPercent >= 35.0) {
                    insights.add(
                        AiInsight(
                            id = "star_product_${product.id}",
                            title = "Producto Estrella: ${product.name}",
                            message = "Tiene excelente rotación (${CurrencyFormatter.formatQuantity(sold7d, product.unit)} en 7 días) y un margen de ganancia del ${CurrencyFormatter.formatPercent(product.grossMarginPercent)}. ¡Mantén siempre stock disponible!",
                            type = InsightType.OPPORTUNITY,
                            relatedProductId = product.id
                        )
                    )
                }
            } else if (product.currentStock > (product.minStock * 3) && salesLast30Days.isNotEmpty()) {
                // Dead stock / overstocked
                val sold30d = salesLast30Days.flatMap { it.items }.filter { it.productId == product.id }.sumOf { it.quantity }
                if (sold30d == 0.0) {
                    insights.add(
                        AiInsight(
                            id = "dead_stock_${product.id}",
                            title = "Baja rotación en ${product.name}",
                            message = "Tienes ${CurrencyFormatter.formatQuantity(product.currentStock, product.unit)} acumulados sin ventas en los últimos 30 días. Considera una promoción o combo para liberar capital.",
                            type = InsightType.INFO,
                            relatedProductId = product.id
                        )
                    )
                }
            }
        }

        // 2. Low Margin Alert
        val lowMarginProducts = products.filter { it.grossMarginPercent < 20.0 && it.salePrice > 0 }
        if (lowMarginProducts.isNotEmpty()) {
            val names = lowMarginProducts.take(2).joinToString(", ") { "${it.name} (${CurrencyFormatter.formatPercent(it.grossMarginPercent)})" }
            insights.add(
                AiInsight(
                    id = "low_margin_alert",
                    title = "Márgenes reducidos detectados",
                    message = "Productos con margen menor al 20%: $names. Revisa los costos de compra o utiliza la Calculadora de Precios.",
                    type = InsightType.TIP,
                    actionText = "Ver calculadora"
                )
            )
        }

        // 3. General health overview if there are sales
        if (salesLast7Days.isNotEmpty()) {
            val totalSales7d = salesLast7Days.sumOf { it.totalAmount }
            val profit7d = salesLast7Days.sumOf { it.profit }
            val margin7d = if (totalSales7d > 0) (profit7d / totalSales7d) * 100.0 else 0.0
            insights.add(
                AiInsight(
                    id = "weekly_health",
                    title = "Rendimiento semanal",
                    message = "En los últimos 7 días generaste ${CurrencyFormatter.format(totalSales7d, currencySymbol, currencyCode)} en ventas con una ganancia bruta de ${CurrencyFormatter.format(profit7d, currencySymbol, currencyCode)} (margen promedio: ${CurrencyFormatter.formatPercent(margin7d)}).",
                    type = InsightType.INFO
                )
            )
        } else if (products.isNotEmpty()) {
            insights.add(
                AiInsight(
                    id = "no_recent_sales",
                    title = "Registra tu primera venta",
                    message = "No registraste ventas en los últimos 7 días. Presiona en 'Nueva Venta' cuando cobres a un cliente para ver tus estadísticas y ganancias en tiempo real.",
                    type = InsightType.TIP,
                    actionText = "Nueva venta"
                )
            )
        }

        return insights.distinctBy { it.id }.take(5)
    }

    /**
     * Answers specific questions directly from verified local Room data.
     * Guaranteed NO hallucination.
     */
    fun answerBusinessQuestionLocally(
        question: String,
        products: List<Product>,
        allSales: List<Sale>,
        allExpenses: List<Expense>,
        currencySymbol: String,
        currencyCode: String
    ): String {
        val q = question.lowercase().trim()
        val productsMap = products.associateBy { it.id }

        val start7d = DateUtils.getStartOfDaysAgo(7)
        val sales7d = allSales.filter { it.timestamp >= start7d }

        val startMonth = DateUtils.getStartOfCurrentMonth()
        val salesMonth = allSales.filter { it.timestamp >= startMonth }
        val expensesMonth = allExpenses.filter { it.timestamp >= startMonth }

        val rankings7d = BusinessCalculations.calculateProductRankings(sales7d, productsMap)
        val rankingsAll = BusinessCalculations.calculateProductRankings(allSales, productsMap)

        return when {
            // Question: ¿Qué producto me deja más ganancia?
            q.contains("más ganancia") || q.contains("mayor ganancia") || q.contains("mejor rentabilidad") -> {
                if (products.isEmpty()) {
                    return "No tienes productos cargados en el inventario todavía."
                }
                val sortedByUnitProfit = products.sortedByDescending { it.unitProfit }
                val bestUnit = sortedByUnitProfit.firstOrNull()

                val bestTotal = rankingsAll.maxByOrNull { it.totalProfit }

                val sb = StringBuilder()
                if (bestUnit != null) {
                    sb.append("🎯 **Mayor ganancia por unidad:**\n")
                    sb.append("• **${bestUnit.name}**: te deja **${CurrencyFormatter.format(bestUnit.unitProfit, currencySymbol, currencyCode)}** de ganancia por cada ${bestUnit.unit} (Margen: ${CurrencyFormatter.formatPercent(bestUnit.grossMarginPercent)}).\n\n")
                }
                if (bestTotal != null && bestTotal.totalProfit > 0) {
                    sb.append("💰 **Mayor ganancia acumulada en ventas:**\n")
                    sb.append("• **${bestTotal.productName}**: ha generado un total de **${CurrencyFormatter.format(bestTotal.totalProfit, currencySymbol, currencyCode)}** de ganancia histórica en ${CurrencyFormatter.formatQuantity(bestTotal.totalUnitsSold, bestTotal.unit)} vendidas.")
                }
                sb.toString()
            }

            // Question: ¿Qué vendí más esta semana?
            q.contains("vendí más") || q.contains("más vendido") || q.contains("top ventas") -> {
                if (sales7d.isEmpty()) {
                    return "No registraste ventas en los últimos 7 días. Cuando realices ventas, aquí verás el ranking exacto."
                }
                val topProducts = rankings7d.sortedByDescending { it.totalUnitsSold }.take(3)
                val sb = StringBuilder("📊 **Productos más vendidos de los últimos 7 días:**\n\n")
                topProducts.forEachIndexed { index, item ->
                    val medal = when (index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "•" }
                    sb.append("$medal **${item.productName}**: ${CurrencyFormatter.formatQuantity(item.totalUnitsSold, item.unit)} vendidas (${CurrencyFormatter.format(item.totalRevenue, currencySymbol, currencyCode)})\n")
                }
                sb.toString()
            }

            // Question: ¿Qué debería comprar hoy / qué falta?
            q.contains("qué comprar") || q.contains("que comprar") || q.contains("qué debo comprar") || q.contains("falta stock") || q.contains("reponer") -> {
                val needPurchase = products.filter { it.currentStock <= it.minStock }
                if (needPurchase.isEmpty()) {
                    return "✅ ¡Excelente! Tu inventario está saludable. No tienes productos con stock por debajo del mínimo configurado."
                }
                val sb = StringBuilder("⚠️ **Productos que necesitas reponer (${needPurchase.size}):**\n\n")
                needPurchase.take(5).forEach { p ->
                    val statusEmoji = if (p.currentStock <= 0) "🔴" else "🟡"
                    sb.append("$statusEmoji **${p.name}**: te quedan **${CurrencyFormatter.formatQuantity(p.currentStock, p.unit)}** (Mínimo recomendado: ${CurrencyFormatter.formatQuantity(p.minStock, p.unit)})\n")
                }
                if (needPurchase.size > 5) {
                    sb.append("\n... y ${needPurchase.size - 5} productos más.")
                }
                sb.toString()
            }

            // Question: ¿Cuánto gané este mes?
            q.contains("gané este mes") || q.contains("ganancia este mes") || q.contains("cuanto gane") || q.contains("cuánto gané") || q.contains("resultado del mes") -> {
                val summary = BusinessCalculations.calculateProfitSummary("Mes actual", salesMonth, expensesMonth)
                val sb = StringBuilder("📈 **Balance del mes actual:**\n\n")
                sb.append("• **Total Ventas (Ingresos):** ${CurrencyFormatter.format(summary.totalRevenue, currencySymbol, currencyCode)}\n")
                sb.append("• **Costo de Mercadería:** ${CurrencyFormatter.format(summary.totalCostOfGoodsSold, currencySymbol, currencyCode)}\n")
                sb.append("• **Ganancia Bruta:** ${CurrencyFormatter.format(summary.grossProfit, currencySymbol, currencyCode)}\n")
                sb.append("• **Gastos Operativos:** ${CurrencyFormatter.format(summary.totalExpenses, currencySymbol, currencyCode)}\n")
                sb.append("──────────────\n")
                sb.append("💵 **GANANCIA NETA ESTIMADA:** **${CurrencyFormatter.format(summary.netProfit, currencySymbol, currencyCode)}**\n")
                sb.append("Margen neto: ${CurrencyFormatter.formatPercent(summary.profitMarginPercent)}")
                sb.toString()
            }

            // Question: ¿Qué productos tienen poco margen?
            q.contains("poco margen") || q.contains("bajo margen") || q.contains("margen bajo") -> {
                val lowMargin = products.filter { it.grossMarginPercent < 25.0 && it.salePrice > 0 }.sortedBy { it.grossMarginPercent }
                if (lowMargin.isEmpty()) {
                    return "👏 ¡Muy bien! Todos tus productos tienen un margen bruto saludable superior al 25%."
                }
                val sb = StringBuilder("⚠️ **Productos con menor margen de ganancia:**\n\n")
                lowMargin.take(5).forEach { p ->
                    sb.append("• **${p.name}**: Margen **${CurrencyFormatter.formatPercent(p.grossMarginPercent)}** (Costo: ${CurrencyFormatter.format(p.costPrice, currencySymbol, currencyCode)} | Venta: ${CurrencyFormatter.format(p.salePrice, currencySymbol, currencyCode)})\n")
                }
                sb.append("\n💡 *Tip: Puedes usar la Calculadora de Precios para recalcular el precio de venta sugerido.*")
                sb.toString()
            }

            // Question: ¿Qué productos están vendiendo menos?
            q.contains("menos vendido") || q.contains("vendiendo menos") || q.contains("baja rotacion") || q.contains("estancado") -> {
                val soldIds = salesMonth.flatMap { it.items }.map { it.productId }.toSet()
                val unsold = products.filter { it.id !in soldIds }
                if (unsold.isEmpty()) {
                    return "Todos tus productos han tenido al menos una venta durante este mes."
                }
                val sb = StringBuilder("📦 **Productos sin ventas en el mes actual (${unsold.size}):**\n\n")
                unsold.take(5).forEach { p ->
                    sb.append("• **${p.name}**: ${CurrencyFormatter.formatQuantity(p.currentStock, p.unit)} en stock.\n")
                }
                sb.append("\n💡 *Recomendación: Evalúa ofrecerlos en combos o promociones para liberar capital estancado.*")
                sb.toString()
            }

            // General query fallback with summary
            else -> {
                val todayStart = DateUtils.getStartOfToday()
                val salesToday = allSales.filter { it.timestamp >= todayStart }
                val totalToday = salesToday.sumOf { it.totalAmount }
                val profitToday = salesToday.sumOf { it.profit }

                "Consulté tus datos locales:\n" +
                "• Productos en catálogo: ${products.size}\n" +
                "• Ventas hoy: ${CurrencyFormatter.format(totalToday, currencySymbol, currencyCode)} (Ganancia: ${CurrencyFormatter.format(profitToday, currencySymbol, currencyCode)})\n" +
                "• Productos con stock bajo: ${products.count { it.currentStock <= it.minStock }}\n\n" +
                "Puedes hacerme preguntas específicas como: '¿Qué producto me deja más ganancia?', '¿Qué debería comprar?', o '¿Cuánto gané este mes?'"
            }
        }
    }
}
