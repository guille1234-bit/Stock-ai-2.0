package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.database.entity.ProductEntity
import com.example.data.database.entity.StockMovementEntity
import com.example.domain.model.MovementType
import com.example.domain.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val database: AppDatabase
) {
    private val productDao = database.productDao()
    private val movementDao = database.stockMovementDao()

    val allActiveProducts: Flow<List<Product>> = productDao.getAllActiveProducts()
        .map { list -> list.map { it.toDomain() } }

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
        .map { list -> list.map { it.toDomain() } }

    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
        .map { list -> list.map { it.toDomain() } }

    val activeProductCount: Flow<Int> = productDao.getProductCount()
    val lowStockCount: Flow<Int> = productDao.getLowStockCount()

    fun searchProducts(query: String): Flow<List<Product>> =
        productDao.searchProducts(query).map { list -> list.map { it.toDomain() } }

    fun getProductsByCategory(categoryId: Long): Flow<List<Product>> =
        productDao.getProductsByCategory(categoryId).map { list -> list.map { it.toDomain() } }

    suspend fun getProductById(id: Long): Product? =
        productDao.getProductById(id)?.toDomain()

    fun getProductByIdFlow(id: Long): Flow<Product?> =
        productDao.getProductByIdFlow(id).map { it?.toDomain() }

    suspend fun createProduct(product: Product): Long {
        return database.withTransaction {
            val entity = ProductEntity.fromDomain(product)
            val newId = productDao.insertProduct(entity)

            // Register initial stock movement if initial stock > 0
            if (product.currentStock > 0) {
                val movement = StockMovementEntity(
                    productId = newId,
                    productName = product.name,
                    type = MovementType.INICIAL.name,
                    quantityChange = product.currentStock,
                    previousStock = 0.0,
                    newStock = product.currentStock,
                    unit = product.unit,
                    reason = "Inventario inicial al crear producto"
                )
                movementDao.insertMovement(movement)
            }
            newId
        }
    }

    suspend fun insertProduct(product: Product): Long = createProduct(product)

    suspend fun updateProduct(product: Product, reason: String = "Edición de producto") {
        val entity = ProductEntity.fromDomain(product)
        productDao.updateProduct(entity)
    }

    suspend fun deleteProduct(productId: Long) {
        // Soft delete to protect historical sales & stock movements
        productDao.softDeleteProduct(productId)
    }
}
