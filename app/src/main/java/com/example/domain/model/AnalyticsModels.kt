package com.example.domain.model

data class ProfitSummary(
    val periodTitle: String,
    val totalRevenue: Double,
    val totalCostOfGoodsSold: Double,
    val grossProfit: Double = totalRevenue - totalCostOfGoodsSold,
    val totalExpenses: Double,
    val netProfit: Double = totalRevenue - totalCostOfGoodsSold - totalExpenses,
    val profitMarginPercent: Double = if (totalRevenue > 0) (netProfit / totalRevenue) * 100.0 else 0.0,
    val salesCount: Int = 0,
    val unitsSold: Double = 0.0
) {
    val costOfGoodsSold: Double
        get() = totalCostOfGoodsSold

    val netMarginPercent: Double
        get() = profitMarginPercent

    val grossMarginPercent: Double
        get() = if (totalRevenue > 0) (grossProfit / totalRevenue) * 100.0 else 0.0

    val averageTicket: Double
        get() = if (salesCount > 0) totalRevenue / salesCount else 0.0
}

data class ProductRanking(
    val productId: Long,
    val productName: String,
    val categoryName: String,
    val unit: String,
    val totalUnitsSold: Double,
    val totalRevenue: Double,
    val totalProfit: Double,
    val unitProfit: Double,
    val marginPercent: Double
) {
    val grossMarginPercent: Double
        get() = marginPercent
}

data class AiInsight(
    val id: String,
    val title: String,
    val message: String,
    val type: InsightType,
    val actionText: String? = null,
    val relatedProductId: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class InsightType(val emoji: String, val label: String) {
    WARNING("⚠️", "Atención"),
    OPPORTUNITY("🔥", "Oportunidad"),
    TIP("💡", "Consejo"),
    PREDICTION("📊", "Proyección"),
    INFO("ℹ️", "Dato clave")
}
