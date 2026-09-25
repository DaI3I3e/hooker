package com.example.data.repository

import com.example.data.local.dao.CategoryDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CategoryRepository(private val categoryDao: CategoryDao) {
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAll()

    fun getCategoriesByType(type: CategoryType): Flow<List<CategoryEntity>> = categoryDao.getByType(type)

    fun getCategoryById(id: Long): Flow<CategoryEntity?> = categoryDao.getById(id)

    suspend fun insertCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
        categoryDao.insert(category)
    }

    suspend fun updateCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.update(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.delete(category)
    }

    suspend fun toggleFavorite(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.updateFavorite(category.id, !category.isFavorite)
    }

    suspend fun updateBudget(categoryId: Long, budget: Long) = withContext(Dispatchers.IO) {
        categoryDao.updateBudget(categoryId, budget)
    }
}
