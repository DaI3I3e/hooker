package com.example.data.repository

import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.local.relation.TransactionWithDetails
import com.example.domain.model.CategorySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TransactionRepository(private val transactionDao: TransactionDao) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAll()
    val allTransactionsWithDetails: Flow<List<TransactionWithDetails>> = transactionDao.getAllWithDetails()

    fun getTransactionById(id: Long): Flow<TransactionEntity?> =
        transactionDao.getById(id)

    fun getTransactionByIdWithDetails(id: Long): Flow<TransactionWithDetails?> =
        transactionDao.getByIdWithDetails(id)

    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionWithDetails>> =
        transactionDao.getRecent(limit)

    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionWithDetails>> =
        transactionDao.getByAccount(accountId)

    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<TransactionWithDetails>> =
        transactionDao.getByDateRange(start, end)

    fun getTodayIncome(todayStart: Long, todayEnd: Long): Flow<Long> =
        transactionDao.getTodayIncome(todayStart, todayEnd).map { it ?: 0L }

    fun getTodayExpense(todayStart: Long, todayEnd: Long): Flow<Long> =
        transactionDao.getTodayExpense(todayStart, todayEnd).map { it ?: 0L }

    fun getTotalIncome(start: Long, end: Long): Flow<Long> =
        transactionDao.getTotalIncome(start, end).map { it ?: 0L }

    fun getTotalExpense(start: Long, end: Long): Flow<Long> =
        transactionDao.getTotalExpense(start, end).map { it ?: 0L }

    fun getCategorySummary(start: Long, end: Long, type: TransactionType): Flow<List<CategorySummary>> {
        val totalFlow = if (type == TransactionType.EXPENSE) {
            getTotalExpense(start, end)
        } else {
            getTotalIncome(start, end)
        }
        val rawFlow = transactionDao.getCategorySummaryRaw(start, end, type)

        return combine(rawFlow, totalFlow) { rawList, total ->
            rawList.map { raw ->
                val percentage = if (total > 0) {
                    (raw.totalAmount.toFloat() / total.toFloat()) * 100f
                } else {
                    0f
                }
                CategorySummary(
                    category = raw.category,
                    totalAmount = raw.totalAmount,
                    transactionCount = raw.transactionCount,
                    percentage = percentage
                )
            }
        }
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        transactionDao.insert(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.update(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.delete(transaction)
    }

    val allSmsHashes: Flow<List<String>> = transactionDao.getAllSmsHashes()

    suspend fun getAllSmsHashesList(): List<String> = withContext(Dispatchers.IO) {
        transactionDao.getAllSmsHashesList()
    }

    suspend fun getAllTransactionsWithDetailsList(): List<TransactionWithDetails> = withContext(Dispatchers.IO) {
        transactionDao.getAllWithDetailsList()
    }

    suspend fun findMatchingTransactions(amount: Long, dayStart: Long, dayEnd: Long): List<TransactionWithDetails> = withContext(Dispatchers.IO) {
        transactionDao.findMatchingTransactions(amount, dayStart, dayEnd)
    }
}
