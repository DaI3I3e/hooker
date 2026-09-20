package com.example.domain.usecase

import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.TransactionRepository

class AddTransactionUseCase(private val transactionRepository: TransactionRepository) {
    suspend operator fun invoke(transaction: TransactionEntity): Result<Long> {
        if (transaction.amount <= 0) {
            return Result.failure(IllegalArgumentException("مبلغ باید بزرگتر از صفر باشد"))
        }
        if (transaction.accountId == null) {
            return Result.failure(IllegalArgumentException("حساب انتخاب نشده است"))
        }
        return try {
            val id = transactionRepository.insertTransaction(transaction)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
