package com.example.data.repository

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
    private val recurringDao: RecurringDao
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
        val now = System.currentTimeMillis()
        val installmentPrefix = if (recurring.isInstallment) {
            val totalStr = recurring.totalInstallments?.toString() ?: "نامشخص"
            "قسط ${recurring.paidInstallments + 1} از $totalStr: "
        } else {
            "تراکنش دوره‌ای: "
        }
        val noteText = (recurring.note ?: "").trim()
        val finalNote = if (noteText.isNotEmpty()) "$installmentPrefix${recurring.title} - $noteText" else "$installmentPrefix${recurring.title}"

        // Create transaction
        val tx = TransactionEntity(
            type = recurring.type,
            amount = recurring.amount,
            accountId = recurring.accountId,
            toAccountId = recurring.toAccountId,
            categoryId = recurring.categoryId,
            date = now,
            note = finalNote
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

    companion object {
        fun calculateNextDueDate(currentDueDate: Long, period: RecurrencePeriod): Long {
            val jalali = JalaliDate.fromTimestamp(currentDueDate)
            return when (period) {
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
        }
    }
}
