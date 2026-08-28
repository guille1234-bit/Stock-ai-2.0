package com.example.domain.usecase

import com.example.domain.model.Expense
import com.example.domain.model.Product
import com.example.domain.model.ProductRanking
import com.example.domain.model.ProfitSummary
import com.example.domain.model.Sale

object BusinessCalculations {

    fun calculateProfitSummary(
        periodTitle: String,
        sales: List<Sale>,
        expenses: List<Expense>
    ): ProfitSummary {
        val totalRevenue = sales.sumOf { it.totalAmount }
        val totalCostOfGoodsSold = sales.sumOf { it.totalCost }
        val grossProfit = totalRevenue - totalCostOfGoodsSold
        val totalExpenses = expenses.sumOf { it.amount }
        val netProfit = grossProfit - totalExpenses
        val profitMarginPercent = if (totalRevenue > 0) (netProfit / totalRevenue) * 100.0 else 0.0
        val unitsSold = sales.flatMap { it.items }.sumOf { it.quantity }

        return ProfitSummary(
            periodTitle = periodTitle,
            totalRevenue = totalRevenue,
            totalCostOfGoodsSold = totalCostOfGoodsSold,
            grossProfit = grossProfit,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            profitMarginPercent = profitMarginPercent,
            salesCount = sales.size,
            unitsSold = unitsSold
        )
    }

    fun calculateProductRankings(
        sales: List<Sale>,
        productsMap: Map<Long, Product>
    ): List<ProductRanking> {
        val productSalesAggregates = mutableMapOf<Long, ProductSalesAgg>()

        for (sale in sales) {
            for (item in sale.items) {
                val current = productSalesAggregates.getOrPut(item.productId) {
                    ProductSalesAgg(
                        productId = item.productId,
                        productName = item.productName,
                        unit = item.unit
                    )
                }
                current.quantity += item.quantity
                current.revenue += item.subtotal
                current.cost += item.totalCost
            }
        }

        return productSalesAggregates.values.map { agg ->
            val product = productsMap[agg.productId]
            val category = product?.categoryName ?: "General"
            val totalProfit = agg.revenue - agg.cost
            val unitProfit = if (agg.quantity > 0) totalProfit / agg.quantity else (product?.unitProfit ?: 0.0)
            val margin = if (agg.revenue > 0) (totalProfit / agg.revenue) * 100.0 else 0.0

            ProductRanking(
                productId = agg.productId,
                productName = agg.productName,
                categoryName = category,
                unit = agg.unit,
                totalUnitsSold = agg.quantity,
                totalRevenue = agg.revenue,
                totalProfit = totalProfit,
                unitProfit = unitProfit,
                marginPercent = margin
            )
        }
    }

    /**
     * Price Calculator:
     * Mode A (Margen sobre Precio de Venta):
     * Price = Cost / (1 - (margin / 100))
     * E.g. Cost $6000, Margin 40% -> Price = $10,000, Profit = $4000 (40% of $10,000)
     *
     * Mode B (Recargo sobre Costo / Markup):
     * Price = Cost * (1 + (markup / 100))
     * E.g. Cost $6000, Markup 40% -> Price = $8,400, Profit = $2400 (40% of $6,000)
     */
    fun calculatePriceByMarginOnSale(cost: Double, desiredMarginPercent: Double): PriceCalculationResult {
        if (cost <= 0) return PriceCalculationResult(0.0, 0.0, 0.0, 0.0)
        if (desiredMarginPercent >= 100.0) return PriceCalculationResult(cost, 0.0, 0.0, 0.0)

        val marginFraction = desiredMarginPercent / 100.0
        val recommendedPrice = cost / (1.0 - marginFraction)
        val profit = recommendedPrice - cost
        val markupOnCost = if (cost > 0) (profit / cost) * 100.0 else 0.0

        return PriceCalculationResult(
            recommendedPrice = recommendedPrice,
            profit = profit,
            grossMarginPercent = desiredMarginPercent,
            markupOnCostPercent = markupOnCost
        )
    }

    fun calculatePriceByMarkupOnCost(cost: Double, desiredMarkupPercent: Double): PriceCalculationResult {
        if (cost <= 0) return PriceCalculationResult(0.0, 0.0, 0.0, 0.0)

        val markupFraction = desiredMarkupPercent / 100.0
        val recommendedPrice = cost * (1.0 + markupFraction)
        val profit = recommendedPrice - cost
        val grossMargin = if (recommendedPrice > 0) (profit / recommendedPrice) * 100.0 else 0.0

        return PriceCalculationResult(
            recommendedPrice = recommendedPrice,
            profit = profit,
            grossMarginPercent = grossMargin,
            markupOnCostPercent = desiredMarkupPercent
        )
    }

    private data class ProductSalesAgg(
        val productId: Long,
        var productName: String,
        var unit: String,
        var quantity: Double = 0.0,
        var revenue: Double = 0.0,
        var cost: Double = 0.0
    )
}

data class PriceCalculationResult(
    val recommendedPrice: Double,
    val profit: Double,
    val grossMarginPercent: Double,
    val markupOnCostPercent: Double
)
