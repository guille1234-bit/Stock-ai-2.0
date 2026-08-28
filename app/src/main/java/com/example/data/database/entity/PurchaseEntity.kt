package com.example.data.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.domain.model.Purchase
import com.example.domain.model.PurchaseItem

@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("supplierId"), Index("timestamp")]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val supplierId: Long? = null,
    val supplierName: String? = null,
    val totalAmount: Double,
    val note: String = ""
) {
    companion object {
        fun fromDomain(purchase: Purchase): PurchaseEntity = PurchaseEntity(
            id = purchase.id,
            timestamp = purchase.timestamp,
            supplierId = purchase.supplierId,
            supplierName = purchase.supplierName,
            totalAmount = purchase.totalAmount,
            note = purchase.note
        )
    }
}

@Entity(
    tableName = "purchase_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("purchaseId"), Index("productId")]
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long = 0,
    val productId: Long,
    val productName: String,
    val unit: String = "unidad",
    val quantity: Double,
    val unitCost: Double,
    val subtotal: Double,
    val updateProductCost: Boolean = true
) {
    fun toDomain(): PurchaseItem = PurchaseItem(
        id = id,
        purchaseId = purchaseId,
        productId = productId,
        productName = productName,
        unit = unit,
        quantity = quantity,
        unitCost = unitCost,
        subtotal = subtotal,
        updateProductCost = updateProductCost
    )

    companion object {
        fun fromDomain(item: PurchaseItem, parentPurchaseId: Long): PurchaseItemEntity = PurchaseItemEntity(
            id = item.id,
            purchaseId = parentPurchaseId,
            productId = item.productId,
            productName = item.productName,
            unit = item.unit,
            quantity = item.quantity,
            unitCost = item.unitCost,
            subtotal = item.subtotal,
            updateProductCost = item.updateProductCost
        )
    }
}

data class PurchaseWithItems(
    @Embedded val purchase: PurchaseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "purchaseId"
    )
    val items: List<PurchaseItemEntity>
) {
    fun toDomain(): Purchase = Purchase(
        id = purchase.id,
        timestamp = purchase.timestamp,
        supplierId = purchase.supplierId,
        supplierName = purchase.supplierName,
        totalAmount = purchase.totalAmount,
        note = purchase.note,
        items = items.map { it.toDomain() }
    )
}
