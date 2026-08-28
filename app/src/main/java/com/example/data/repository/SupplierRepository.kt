package com.example.data.repository

import com.example.data.database.dao.SupplierDao
import com.example.data.database.entity.SupplierEntity
import com.example.domain.model.Supplier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SupplierRepository(
    private val supplierDao: SupplierDao
) {
    val allSuppliers: Flow<List<Supplier>> = supplierDao.getAllSuppliers()
        .map { list -> list.map { it.toDomain() } }

    fun searchSuppliers(query: String): Flow<List<Supplier>> =
        supplierDao.searchSuppliers(query).map { list -> list.map { it.toDomain() } }

    suspend fun getSupplierById(id: Long): Supplier? =
        supplierDao.getSupplierById(id)?.toDomain()

    suspend fun createSupplier(supplier: Supplier): Long =
        supplierDao.insertSupplier(SupplierEntity.fromDomain(supplier))

    suspend fun updateSupplier(supplier: Supplier) =
        supplierDao.updateSupplier(SupplierEntity.fromDomain(supplier))

    suspend fun deleteSupplier(id: Long) =
        supplierDao.deleteSupplier(id)
}
