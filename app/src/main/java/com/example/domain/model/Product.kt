package com.example.domain.model

data class Product(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val categoryId: Long? = null,
    val categoryName: String = "Otros",
    val sku: String = "",
    val salePrice: Double,
    val costPrice: Double,
    val currentStock: Double,
    val minStock: Double = 5.0,
    val unit: String = "unidad", // unidad, kg, g, litro, ml, paquete, docena
    val supplierId: Long? = null,
    val supplierName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    val minStockAlert: Double
        get() = minStock

    val unitProfit: Double
        get() = salePrice - costPrice

    val grossMarginPercent: Double
        get() = if (salePrice > 0.0) ((salePrice - costPrice) / salePrice) * 100.0 else 0.0

    val profitMarginPercent: Double
        get() = grossMarginPercent

    val isLowStock: Boolean
        get() = currentStock > 0 && currentStock <= minStock

    val isOutOfStock: Boolean
        get() = currentStock <= 0

    val stockStatus: StockStatus
        get() = when {
            currentStock <= 0 -> StockStatus.OUT_OF_STOCK
            currentStock <= minStock -> StockStatus.LOW_STOCK
            else -> StockStatus.NORMAL
        }
}

enum class StockStatus(val label: String, val emoji: String) {
    NORMAL("Stock normal", "🟢"),
    LOW_STOCK("Stock bajo", "🟡"),
    OUT_OF_STOCK("Sin stock", "🔴")
}
