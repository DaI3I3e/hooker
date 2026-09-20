package com.example.data.local.relation

import androidx.room.Embedded
import com.example.data.local.entity.TransactionEntity

data class TransactionWithDetails(
    @Embedded val transaction: TransactionEntity,
    val accountName: String?,
    val toAccountName: String?,
    val categoryName: String?,
    val categoryColor: Int?,
    val categoryIcon: String?,
    val accountLogo: String? = null,
    val accountCardNumber: String? = null
)
