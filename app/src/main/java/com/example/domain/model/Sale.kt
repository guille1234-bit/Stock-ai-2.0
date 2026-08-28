package com.example.domain.model

data class Sale(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val totalCost: Double,
    val profit: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val customerNote: String = "",
    val items: List<SaleItem> = emptyList()
) {
    val totalProfit: Double
        get() = profit
}

data class SaleItem(
    val id: Long = 0,
    val saleId: Long = 0,
    val productId: Long,
    val productName: String,
    val unit: String = "unidad",
    val quantity: Double,
    val unitPrice: Double,
    val unitCost: Double,
    val subtotal: Double = quantity * unitPrice,
    val totalCost: Double = quantity * unitCost
) {
    val unitSalePrice: Double
        get() = unitPrice

    val unitCostPrice: Double
        get() = unitCost

    val profit: Double
        get() = subtotal - totalCost
}

enum class PaymentMethod(val label: String, val emoji: String) {
    EFECTIVO("Efectivo", "💵"),
    TRANSFERENCIA("Transferencia", "📲"),
    TARJETA("Tarjeta", "💳"),
    OTRO("Otro", "🔄")
}
