package com.example.domain.model

import com.example.data.local.entity.CategoryEntity

data class CategorySummary(
    val category: CategoryEntity,
    val totalAmount: Long,
    val transactionCount: Int,
    val percentage: Float
)
