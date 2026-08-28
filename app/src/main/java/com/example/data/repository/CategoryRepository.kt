package com.example.data.repository

import com.example.data.database.dao.CategoryDao
import com.example.data.database.entity.CategoryEntity
import com.example.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(
    private val categoryDao: CategoryDao
) {
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
        .map { list -> list.map { it.toDomain() } }

    suspend fun getCategoryById(id: Long): Category? =
        categoryDao.getCategoryById(id)?.toDomain()

    suspend fun createCategory(category: Category): Long =
        categoryDao.insertCategory(CategoryEntity.fromDomain(category))

    suspend fun updateCategory(category: Category) =
        categoryDao.updateCategory(CategoryEntity.fromDomain(category))

    suspend fun deleteCategory(id: Long) =
        categoryDao.deleteCategory(id)
}
