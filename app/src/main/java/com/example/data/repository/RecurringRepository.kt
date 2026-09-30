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
            database.withTransaction {
                // Fetch fresh recurring state from DB inside transaction
                val currentRecurring = database.recurringDao().getByIdSync(recurring.id) ?: recurring
                if (!currentRecurring.isActive && currentRecurring.isInstallment) {
                    return@withTransaction -1L
                }

                // Prevent duplicate execution atomically inside the transaction
                val existingCount = database.transactionDao().countByRecurringAndDate(currentRecurring.id, currentRecurring.nextDueDate)
                if (existingCount > 0) {
                    return@withTransaction -1L
                }

                val now = System.currentTimeMillis()
                val installmentPrefix = if (currentRecurring.isInstallment) {
                    val totalStr = currentRecurring.totalInstallments?.toString() ?: "نامشخص"
                    "قسط ${currentRecurring.paidInstallments + 1} از $totalStr: "
                } else {
                    "تراکنش دوره‌ای: "
                }
                val noteText = (currentRecurring.note ?: "").trim()
                val finalNote = if (noteText.isNotEmpty()) "$installmentPrefix${currentRecurring.title} - $noteText" else "$installmentPrefix${currentRecurring.title}"

                // Validate account existence to prevent SQLite foreign key constraint violations
                val accountDao = database.accountDao()
                val primaryAccount = accountDao.getAccountById(currentRecurring.accountId)
                val safeAccountId = if (primaryAccount != null) {
                    currentRecurring.accountId
                } else {
                    accountDao.getFirstAccount()?.id
                }

                val safeToAccountId = if (currentRecurring.type == com.example.data.local.entity.TransactionType.TRANSFER && currentRecurring.toAccountId != null) {
                    val toAccount = accountDao.getAccountById(currentRecurring.toAccountId)
                    if (toAccount != null && toAccount.id != safeAccountId) {
                        currentRecurring.toAccountId
                    } else {
                        null
                    }
                } else {
                    null
                }

                val safeCategoryId = if (currentRecurring.categoryId != null) {
                    val cat = database.categoryDao().getCategoryById(currentRecurring.categoryId)
                    cat?.id
                } else {
                    null
                }

                // Create transaction with recurringId and executedForDate
                val tx = TransactionEntity(
                    type = currentRecurring.type,
                    amount = currentRecurring.amount,
                    accountId = safeAccountId,
                    toAccountId = safeToAccountId,
                    categoryId = safeCategoryId,
                    date = now,
                    note = finalNote,
                    recurringId = currentRecurring.id,
                    executedForDate = currentRecurring.nextDueDate
                )
                val txId = transactionRepository.insert(tx)

                // Calculate next state
                val newPaidCount = currentRecurring.paidInstallments + 1
                val shouldDeactivate = currentRecurring.isInstallment && 
                        currentRecurring.totalInstallments != null && 
                        newPaidCount >= currentRecurring.totalInstallments

                val nextDue = calculateNextDueDate(currentRecurring.nextDueDate, currentRecurring.period)

                database.recurringDao().updateExecution(
                    id = currentRecurring.id,
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
