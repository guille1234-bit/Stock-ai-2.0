package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Product

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId"),
        Index("supplierId"),
        Index("name"),
        Index("isActive")
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val categoryId: Long? = null,
    val categoryName: String = "Otros",
    val sku: String = "",
    val salePrice: Double,
    val costPrice: Double,
    val currentStock: Double,
    val minStock: Double = 5.0,
    val unit: String = "unidad",
    val supplierId: Long? = null,
    val supplierName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    fun toDomain(): Product = Product(
        id = id,
        name = name,
        description = description,
        categoryId = categoryId,
        categoryName = categoryName,
        sku = sku,
        salePrice = salePrice,
        costPrice = costPrice,
        currentStock = currentStock,
        minStock = minStock,
        unit = unit,
        supplierId = supplierId,
        supplierName = supplierName,
        createdAt = createdAt,
        isActive = isActive
    )

    companion object {
        fun fromDomain(product: Product): ProductEntity = ProductEntity(
            id = product.id,
            name = product.name,
            description = product.description,
            categoryId = product.categoryId,
            categoryName = product.categoryName,
            sku = product.sku,
            salePrice = product.salePrice,
            costPrice = product.costPrice,
            currentStock = product.currentStock,
            minStock = product.minStock,
            unit = product.unit,
            supplierId = product.supplierId,
            supplierName = product.supplierName,
            createdAt = product.createdAt,
            isActive = product.isActive
        )
    }
}
