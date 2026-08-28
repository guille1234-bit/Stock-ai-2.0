package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Supplier

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Supplier = Supplier(
        id = id,
        name = name,
        phone = phone,
        description = description,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(supplier: Supplier): SupplierEntity = SupplierEntity(
            id = supplier.id,
            name = supplier.name,
            phone = supplier.phone,
            description = supplier.description,
            createdAt = supplier.createdAt
        )
    }
}
