package com.example.data.local.relation

import androidx.room.Embedded
import com.example.data.local.entity.RecurringTransactionEntity

data class RecurringWithDetails(
    @Embedded val recurring: RecurringTransactionEntity,
    val accountName: String?,
    val toAccountName: String?,
    val categoryName: String?,
    val categoryColor: Int?,
    val categoryIcon: String?
)
