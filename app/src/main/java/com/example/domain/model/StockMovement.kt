package com.example.domain.model

data class StockMovement(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val productId: Long,
    val productName: String,
    val type: MovementType,
    val quantityChange: Double, // +10, -3, etc.
    val previousStock: Double,
    val newStock: Double,
    val unit: String = "unidad",
    val reason: String = "",
    val referenceId: Long? = null // saleId or purchaseId
)

enum class MovementType(val label: String, val sign: String, val emoji: String) {
    COMPRA("Compra", "+", "🛒"),
    VENTA("Venta", "-", "💰"),
    AJUSTE("Ajuste", "±", "⚖️"),
    PERDIDA("Pérdida", "-", "🗑️"),
    INICIAL("Inventario inicial", "+", "📦")
}
