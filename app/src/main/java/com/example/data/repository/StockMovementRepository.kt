package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.database.entity.StockMovementEntity
import com.example.domain.model.MovementType
import com.example.domain.model.StockMovement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StockMovementRepository(
    private val database: AppDatabase
) {
    private val movementDao = database.stockMovementDao()
    private val productDao = database.productDao()

    val allMovements: Flow<List<StockMovement>> = movementDao.getAllMovements()
        .map { list -> list.map { it.toDomain() } }

    fun getMovementsByProduct(productId: Long): Flow<List<StockMovement>> =
        movementDao.getMovementsByProduct(productId).map { list -> list.map { it.toDomain() } }

    fun getMovementsByType(type: MovementType): Flow<List<StockMovement>> =
        movementDao.getMovementsByType(type.name).map { list -> list.map { it.toDomain() } }

    fun getMovementsByDateRange(startTime: Long, endTime: Long): Flow<List<StockMovement>> =
        movementDao.getMovementsByDateRange(startTime, endTime).map { list -> list.map { it.toDomain() } }

    fun searchMovements(query: String): Flow<List<StockMovement>> =
        movementDao.searchMovements(query).map { list -> list.map { it.toDomain() } }

    suspend fun adjustStock(
        productId: Long,
        newStock: Double,
        reason: String
    ): Result<Unit> {
        if (newStock < 0.0) {
            return Result.failure(IllegalArgumentException("El stock no puede ser negativo."))
        }

        return try {
            database.withTransaction {
                val product = productDao.getProductById(productId)
                    ?: throw IllegalStateException("Producto no encontrado.")

                val prevStock = product.currentStock
                val delta = newStock - prevStock

                productDao.updateStock(productId, newStock)

                val movement = StockMovementEntity(
                    productId = productId,
                    productName = product.name,
                    type = MovementType.AJUSTE.name,
                    quantityChange = delta,
                    previousStock = prevStock,
                    newStock = newStock,
                    unit = product.unit,
                    reason = reason.ifBlank { "Ajuste manual de inventario" }
                )
                movementDao.insertMovement(movement)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordLoss(
        productId: Long,
        lostQuantity: Double,
        reason: String
    ): Result<Unit> {
        if (lostQuantity <= 0.0) {
            return Result.failure(IllegalArgumentException("La cantidad perdida debe ser mayor a 0."))
        }

        return try {
            database.withTransaction {
                val product = productDao.getProductById(productId)
                    ?: throw IllegalStateException("Producto no encontrado.")

                val prevStock = product.currentStock
                val newStock = (prevStock - lostQuantity).coerceAtLeast(0.0)

                productDao.updateStock(productId, newStock)

                val movement = StockMovementEntity(
                    productId = productId,
                    productName = product.name,
                    type = MovementType.PERDIDA.name,
                    quantityChange = -lostQuantity,
                    previousStock = prevStock,
                    newStock = newStock,
                    unit = product.unit,
                    reason = reason.ifBlank { "Pérdida / Merma / Vencimiento" }
                )
                movementDao.insertMovement(movement)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerLoss(productId: Long, quantityLost: Double, reason: String) {
        val res = recordLoss(productId, quantityLost, reason)
        res.getOrThrow()
    }
}
