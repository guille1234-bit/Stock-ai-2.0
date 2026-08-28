package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.database.entity.PurchaseEntity
import com.example.data.database.entity.PurchaseItemEntity
import com.example.data.database.entity.StockMovementEntity
import com.example.domain.model.MovementType
import com.example.domain.model.Purchase
import com.example.domain.model.PurchaseItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PurchaseRepository(
    private val database: AppDatabase
) {
    private val purchaseDao = database.purchaseDao()
    private val productDao = database.productDao()
    private val movementDao = database.stockMovementDao()

    val allPurchases: Flow<List<Purchase>> = purchaseDao.getAllPurchases()
        .map { list -> list.map { it.toDomain() } }

    fun getPurchasesByDateRange(startTime: Long, endTime: Long): Flow<List<Purchase>> =
        purchaseDao.getPurchasesByDateRange(startTime, endTime).map { list -> list.map { it.toDomain() } }

    suspend fun getPurchaseById(id: Long): Purchase? =
        purchaseDao.getPurchaseById(id)?.toDomain()

    suspend fun registerPurchase(purchase: Purchase): Long {
        val result = recordPurchase(
            supplierId = purchase.supplierId,
            supplierName = purchase.supplierName,
            items = purchase.items,
            note = purchase.note
        )
        return result.getOrThrow()
    }

    suspend fun recordPurchase(
        supplierId: Long?,
        supplierName: String?,
        items: List<PurchaseItem>,
        note: String = ""
    ): Result<Long> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Debes agregar al menos un producto a la compra."))
        }

        return try {
            val purchaseId = database.withTransaction {
                var totalAmount = 0.0

                for (item in items) {
                    if (item.quantity <= 0.0) {
                        throw IllegalArgumentException("La cantidad debe ser mayor a 0.")
                    }
                    if (item.unitCost < 0.0) {
                        throw IllegalArgumentException("El costo unitario no puede ser negativo.")
                    }
                    totalAmount += item.quantity * item.unitCost
                }

                val purchaseEntity = PurchaseEntity(
                    timestamp = System.currentTimeMillis(),
                    supplierId = supplierId,
                    supplierName = supplierName,
                    totalAmount = totalAmount,
                    note = note
                )
                val newPurchaseId = purchaseDao.insertPurchase(purchaseEntity)

                val itemEntities = items.map { item ->
                    PurchaseItemEntity.fromDomain(item, newPurchaseId)
                }
                purchaseDao.insertPurchaseItems(itemEntities)

                for (item in items) {
                    val currentProduct = productDao.getProductById(item.productId)
                    if (currentProduct != null) {
                        val prevStock = currentProduct.currentStock
                        val newStock = prevStock + item.quantity

                        productDao.updateStock(item.productId, newStock)

                        if (item.updateProductCost && item.unitCost > 0) {
                            productDao.updateCostPrice(item.productId, item.unitCost)
                        }

                        val movement = StockMovementEntity(
                            productId = item.productId,
                            productName = item.productName,
                            type = MovementType.COMPRA.name,
                            quantityChange = item.quantity,
                            previousStock = prevStock,
                            newStock = newStock,
                            unit = item.unit,
                            reason = if (!supplierName.isNullOrBlank()) "Compra a $supplierName" else "Compra #$newPurchaseId",
                            referenceId = newPurchaseId
                        )
                        movementDao.insertMovement(movement)
                    }
                }

                newPurchaseId
            }
            Result.success(purchaseId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
