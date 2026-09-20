package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CategoryType {
    EXPENSE, INCOME, BOTH
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val color: Int,
    val icon: String,
    val isDefault: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
