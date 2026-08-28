package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.database.entity.SaleEntity
import com.example.data.database.entity.SaleItemEntity
import com.example.data.database.entity.StockMovementEntity
import com.example.domain.model.MovementType
import com.example.domain.model.PaymentMethod
import com.example.domain.model.Sale
import com.example.domain.model.SaleItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SalesRepository(
    private val database: AppDatabase
) {
    private val saleDao = database.saleDao()
    private val productDao = database.productDao()
    private val movementDao = database.stockMovementDao()

    val allSales: Flow<List<Sale>> = saleDao.getAllSales()
        .map { list -> list.map { it.toDomain() } }

    fun getSalesByDateRange(startTime: Long, endTime: Long): Flow<List<Sale>> =
        saleDao.getSalesByDateRange(startTime, endTime).map { list -> list.map { it.toDomain() } }

    suspend fun getSaleById(id: Long): Sale? =
        saleDao.getSaleById(id)?.toDomain()

    suspend fun registerSale(sale: Sale, allowNegativeStock: Boolean = true): Long {
        val result = recordSale(
            items = sale.items,
            paymentMethod = sale.paymentMethod,
            customerNote = sale.customerNote,
            allowNegativeStock = allowNegativeStock
        )
        return result.getOrThrow()
    }

    suspend fun recordSale(
        items: List<SaleItem>,
        paymentMethod: PaymentMethod,
        customerNote: String = "",
        allowNegativeStock: Boolean = false
    ): Result<Long> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("El carrito de venta no puede estar vacío."))
        }

        return try {
            val saleId = database.withTransaction {
                // 1. Validate stocks and calculate totals
                var calculatedTotal = 0.0
                var calculatedCost = 0.0

                val verifiedItems = mutableListOf<SaleItem>()

                for (item in items) {
                    if (item.quantity <= 0.0) {
                        throw IllegalArgumentException("La cantidad de ${item.productName} debe ser mayor a 0.")
                    }

                    val currentProduct = productDao.getProductById(item.productId)
                        ?: throw IllegalStateException("El producto '${item.productName}' ya no existe.")

                    if (!allowNegativeStock && currentProduct.currentStock < item.quantity) {
                        throw IllegalStateException(
                            "Stock insuficiente para '${item.productName}'. Disponible: ${currentProduct.currentStock} ${item.unit}, solicitada: ${item.quantity} ${item.unit}"
                        )
                    }

                    val linePrice = item.unitPrice
                    val lineCost = currentProduct.costPrice
                    val subtotal = item.quantity * linePrice
                    val totalCost = item.quantity * lineCost

                    calculatedTotal += subtotal
                    calculatedCost += totalCost

                    verifiedItems.add(
                        item.copy(
                            unitPrice = linePrice,
                            unitCost = lineCost,
                            subtotal = subtotal,
                            totalCost = totalCost
                        )
                    )
                }

                val profit = calculatedTotal - calculatedCost

                // 2. Insert Sale master record
                val saleEntity = SaleEntity(
                    timestamp = System.currentTimeMillis(),
                    totalAmount = calculatedTotal,
                    totalCost = calculatedCost,
                    profit = profit,
                    paymentMethod = paymentMethod.name,
                    customerNote = customerNote
                )
                val newSaleId = saleDao.insertSale(saleEntity)

                // 3. Insert Sale items & update stocks & record movements
                val itemEntities = verifiedItems.map { item ->
                    SaleItemEntity.fromDomain(item, newSaleId)
                }
                saleDao.insertSaleItems(itemEntities)

                for (item in verifiedItems) {
                    val currentProduct = productDao.getProductById(item.productId)!!
                    val prevStock = currentProduct.currentStock
                    val newStock = prevStock - item.quantity

                    productDao.updateStock(item.productId, newStock)

                    val movement = StockMovementEntity(
                        productId = item.productId,
                        productName = item.productName,
                        type = MovementType.VENTA.name,
                        quantityChange = -item.quantity,
                        previousStock = prevStock,
                        newStock = newStock,
                        unit = item.unit,
                        reason = "Venta #$newSaleId",
                        referenceId = newSaleId
                    )
                    movementDao.insertMovement(movement)
                }

                newSaleId
            }
            Result.success(saleId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
