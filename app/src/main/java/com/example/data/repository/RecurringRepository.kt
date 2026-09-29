package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.FinTrackDatabase
import com.example.data.local.dao.RecurringDao
import com.example.data.local.entity.RecurrencePeriod
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.relation.RecurringWithDetails
import com.example.util.JalaliDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.ZoneId

class RecurringRepository(
    private val recurringDao: RecurringDao,
    private val database: FinTrackDatabase
) {
    fun getAll(): Flow<List<RecurringWithDetails>> =
        recurringDao.getAll().flowOn(Dispatchers.IO)

    fun getAllActive(): Flow<List<RecurringWithDetails>> =
        recurringDao.getAllActive().flowOn(Dispatchers.IO)

    fun getDueBefore(timestamp: Long): Flow<List<RecurringWithDetails>> =
        recurringDao.getDueBefore(timestamp).flowOn(Dispatchers.IO)

    fun getById(id: Long): Flow<RecurringTransactionEntity?> =
        recurringDao.getById(id).flowOn(Dispatchers.IO)

    suspend fun insert(recurring: RecurringTransactionEntity): Long =
        withContext(Dispatchers.IO) {
            recurringDao.insert(recurring)
        }

    suspend fun update(recurring: RecurringTransactionEntity) =
        withContext(Dispatchers.IO) {
            recurringDao.update(recurring)
        }

    suspend fun delete(recurring: RecurringTransactionEntity) =
        withContext(Dispatchers.IO) {
            recurringDao.delete(recurring)
        }

    suspend fun deleteById(id: Long) =
        withContext(Dispatchers.IO) {
            recurringDao.deleteById(id)
        }

    suspend fun executeRecurring(
        recurring: RecurringTransactionEntity,
        transactionRepository: TransactionRepository
    ): Long = withContext(Dispatchers.IO) {
        try {
            // Prevent duplicate execution for the same recurring transaction and due date
            val existingCount = transactionRepository.countByRecurringAndDate(recurring.id, recurring.nextDueDate)
            if (existingCount > 0) {
                return@withContext -1L
            }

            database.withTransaction {
                val now = System.currentTimeMillis()
                val installmentPrefix = if (recurring.isInstallment) {
                    val totalStr = recurring.totalInstallments?.toString() ?: "نامشخص"
                    "قسط ${recurring.paidInstallments + 1} از $totalStr: "
                } else {
                    "تراکنش دوره‌ای: "
                }
                val noteText = (recurring.note ?: "").trim()
                val finalNote = if (noteText.isNotEmpty()) "$installmentPrefix${recurring.title} - $noteText" else "$installmentPrefix${recurring.title}"

                // Validate account existence to prevent SQLite foreign key constraint violations
                val accountDao = database.accountDao()
                val primaryAccount = accountDao.getAccountById(recurring.accountId)
                val safeAccountId = if (primaryAccount != null) {
                    recurring.accountId
                } else {
                    accountDao.getFirstAccount()?.id
                }

                val safeToAccountId = if (recurring.type == com.example.data.local.entity.TransactionType.TRANSFER && recurring.toAccountId != null) {
                    val toAccount = accountDao.getAccountById(recurring.toAccountId)
                    if (toAccount != null && toAccount.id != safeAccountId) {
                        recurring.toAccountId
                    } else {
                        null
                    }
                } else {
                    null
                }

                val safeCategoryId = if (recurring.categoryId != null) {
                    val cat = database.categoryDao().getCategoryById(recurring.categoryId)
                    cat?.id
                } else {
                    null
                }

                // Create transaction with recurringId and executedForDate
                val tx = TransactionEntity(
                    type = recurring.type,
                    amount = recurring.amount,
                    accountId = safeAccountId,
                    toAccountId = safeToAccountId,
                    categoryId = safeCategoryId,
                    date = now,
                    note = finalNote,
                    recurringId = recurring.id,
                    executedForDate = recurring.nextDueDate
                )
                val txId = transactionRepository.insert(tx)

                // Calculate next state
                val newPaidCount = recurring.paidInstallments + 1
                val shouldDeactivate = recurring.isInstallment && 
                        recurring.totalInstallments != null && 
                        newPaidCount >= recurring.totalInstallments

                val nextDue = calculateNextDueDate(recurring.nextDueDate, recurring.period)

                recurringDao.updateExecution(
                    id = recurring.id,
                    nextDueDate = nextDue,
                    lastExecutedDate = now,
                    paidInstallments = newPaidCount,
                    isActive = !shouldDeactivate
                )

                txId
            }
        } catch (e: Exception) {
            android.util.Log.e("RecurringRepo", "Error executing recurring transaction ${recurring.id}: ${recurring.title}", e)
            throw e
        }
    }

    companion object {
        fun calculateNextDueDate(currentDueDate: Long, period: RecurrencePeriod): Long {
            return try {
                val baseDate = if (currentDueDate > 0) currentDueDate else System.currentTimeMillis()
                val jalali = JalaliDate.fromTimestamp(baseDate)
                when (period) {
                    RecurrencePeriod.DAILY -> {
                        val localDate = jalali.toLocalDate().plusDays(1)
                        localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    RecurrencePeriod.WEEKLY -> {
                        val localDate = jalali.toLocalDate().plusWeeks(1)
                        localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    RecurrencePeriod.MONTHLY -> {
                        var nextYear = jalali.year
                        var nextMonth = jalali.month + 1
                        if (nextMonth > 12) {
                            nextYear += 1
                            nextMonth = 1
                        }
                        val maxDays = JalaliDate.getJalaliMonthLength(nextYear, nextMonth)
                        val nextDay = jalali.day.coerceAtMost(maxDays)
                        JalaliDate(nextYear, nextMonth, nextDay).toStartOfDayTimestamp()
                    }
                    RecurrencePeriod.YEARLY -> {
                        val nextYear = jalali.year + 1
                        val maxDays = JalaliDate.getJalaliMonthLength(nextYear, jalali.month)
                        val nextDay = jalali.day.coerceAtMost(maxDays)
                        JalaliDate(nextYear, jalali.month, nextDay).toStartOfDayTimestamp()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("RecurringRepo", "Error calculating next due date for $currentDueDate, period: $period", e)
                val fallbackMillis = when (period) {
                    RecurrencePeriod.DAILY -> 86400000L
                    RecurrencePeriod.WEEKLY -> 7 * 86400000L
                    RecurrencePeriod.MONTHLY -> 30 * 86400000L
                    RecurrencePeriod.YEARLY -> 365 * 86400000L
                }
                (if (currentDueDate > 0) currentDueDate else System.currentTimeMillis()) + fallbackMillis
            }
        }
    }
}
