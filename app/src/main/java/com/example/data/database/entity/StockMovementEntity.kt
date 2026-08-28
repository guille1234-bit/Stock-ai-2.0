package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.MovementType
import com.example.domain.model.StockMovement

@Entity(
    tableName = "stock_movements",
    indices = [
        Index("productId"),
        Index("timestamp"),
        Index("type")
    ]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val productId: Long,
    val productName: String,
    val type: String, // COMPRA, VENTA, AJUSTE, PERDIDA, INICIAL
    val quantityChange: Double,
    val previousStock: Double,
    val newStock: Double,
    val unit: String = "unidad",
    val reason: String = "",
    val referenceId: Long? = null
) {
    fun toDomain(): StockMovement = StockMovement(
        id = id,
        timestamp = timestamp,
        productId = productId,
        productName = productName,
        type = try { MovementType.valueOf(type) } catch (e: Exception) { MovementType.AJUSTE },
        quantityChange = quantityChange,
        previousStock = previousStock,
        newStock = newStock,
        unit = unit,
        reason = reason,
        referenceId = referenceId
    )

    companion object {
        fun fromDomain(movement: StockMovement): StockMovementEntity = StockMovementEntity(
            id = movement.id,
            timestamp = movement.timestamp,
            productId = movement.productId,
            productName = movement.productName,
            type = movement.type.name,
            quantityChange = movement.quantityChange,
            previousStock = movement.previousStock,
            newStock = movement.newStock,
            unit = movement.unit,
            reason = movement.reason,
            referenceId = movement.referenceId
        )
    }
}
