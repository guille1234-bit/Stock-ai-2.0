package com.example.domain.model

data class Purchase(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val supplierId: Long? = null,
    val supplierName: String? = null,
    val totalAmount: Double,
    val note: String = "",
    val items: List<PurchaseItem> = emptyList()
)

data class PurchaseItem(
    val id: Long = 0,
    val purchaseId: Long = 0,
    val productId: Long,
    val productName: String,
    val unit: String = "unidad",
    val quantity: Double,
    val unitCost: Double,
    val subtotal: Double = quantity * unitCost,
    val updateProductCost: Boolean = true
)
