package com.example.data.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.domain.model.PaymentMethod
import com.example.domain.model.Sale
import com.example.domain.model.SaleItem

@Entity(tableName = "sales", indices = [Index("timestamp")])
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val totalCost: Double,
    val profit: Double,
    val paymentMethod: String = "EFECTIVO",
    val customerNote: String = ""
) {
    companion object {
        fun fromDomain(sale: Sale): SaleEntity = SaleEntity(
            id = sale.id,
            timestamp = sale.timestamp,
            totalAmount = sale.totalAmount,
            totalCost = sale.totalCost,
            profit = sale.profit,
            paymentMethod = sale.paymentMethod.name,
            customerNote = sale.customerNote
        )
    }
}

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId"), Index("productId")]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long = 0,
    val productId: Long,
    val productName: String,
    val unit: String = "unidad",
    val quantity: Double,
    val unitPrice: Double,
    val unitCost: Double,
    val subtotal: Double,
    val totalCost: Double
) {
    fun toDomain(): SaleItem = SaleItem(
        id = id,
        saleId = saleId,
        productId = productId,
        productName = productName,
        unit = unit,
        quantity = quantity,
        unitPrice = unitPrice,
        unitCost = unitCost,
        subtotal = subtotal,
        totalCost = totalCost
    )

    companion object {
        fun fromDomain(item: SaleItem, parentSaleId: Long): SaleItemEntity = SaleItemEntity(
            id = item.id,
            saleId = parentSaleId,
            productId = item.productId,
            productName = item.productName,
            unit = item.unit,
            quantity = item.quantity,
            unitPrice = item.unitPrice,
            unitCost = item.unitCost,
            subtotal = item.subtotal,
            totalCost = item.totalCost
        )
    }
}

data class SaleWithItems(
    @Embedded val sale: SaleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId"
    )
    val items: List<SaleItemEntity>
) {
    fun toDomain(): Sale = Sale(
        id = sale.id,
        timestamp = sale.timestamp,
        totalAmount = sale.totalAmount,
        totalCost = sale.totalCost,
        profit = sale.profit,
        paymentMethod = try { PaymentMethod.valueOf(sale.paymentMethod) } catch (e: Exception) { PaymentMethod.EFECTIVO },
        customerNote = sale.customerNote,
        items = items.map { it.toDomain() }
    )
}
