package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.FinTrackDatabase
import com.example.data.local.dao.SavingsGoalDao
import com.example.data.local.entity.SavingsEntryType
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.SavingsGoalEntryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.local.relation.SavingsGoalWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class SavingsGoalRepository(
    private val savingsGoalDao: SavingsGoalDao,
    private val database: FinTrackDatabase
) {
    fun getAll(): Flow<List<SavingsGoalWithDetails>> =
        savingsGoalDao.getAll().flowOn(Dispatchers.IO)

    fun getById(id: Long): Flow<SavingsGoalWithDetails?> =
        savingsGoalDao.getById(id).flowOn(Dispatchers.IO)

    fun getEntriesForGoal(goalId: Long): Flow<List<SavingsGoalEntryEntity>> =
        savingsGoalDao.getEntriesForGoal(goalId).flowOn(Dispatchers.IO)

    suspend fun insert(goal: SavingsGoalEntity): Long =
        withContext(Dispatchers.IO) {
            savingsGoalDao.insert(goal)
        }

    suspend fun insertGoal(goal: SavingsGoalEntity, initialAmount: Long = 0L): Long =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val goalId = savingsGoalDao.insert(goal)
                if (initialAmount > 0) {
                    val entry = SavingsGoalEntryEntity(
                        goalId = goalId,
                        transactionId = null,
                        amount = initialAmount,
                        type = SavingsEntryType.DEPOSIT,
                        date = System.currentTimeMillis()
                    )
                    savingsGoalDao.insertEntry(entry)
                    if (goal.targetAmount > 0 && initialAmount >= goal.targetAmount) {
                        savingsGoalDao.updateCompletedStatus(goalId, true)
                    }
                }
                goalId
            }
        }

    suspend fun update(goal: SavingsGoalEntity) =
        withContext(Dispatchers.IO) {
            savingsGoalDao.update(goal)
        }

    suspend fun delete(goal: SavingsGoalEntity) =
        withContext(Dispatchers.IO) {
            savingsGoalDao.delete(goal)
        }

    suspend fun deleteById(id: Long) =
        withContext(Dispatchers.IO) {
            savingsGoalDao.deleteById(id)
        }

    suspend fun deposit(
        goalId: Long,
        amount: Long,
        accountId: Long,
        transactionRepository: TransactionRepository
    ) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val goalWithDetails = savingsGoalDao.getById(goalId).firstOrNull() ?: return@withTransaction
            val now = System.currentTimeMillis()

            // 1. Register expense transaction from selected account
            val tx = TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = amount,
                accountId = accountId,
                toAccountId = null,
                categoryId = null,
                date = now,
                note = "واریز به پس‌انداز: ${goalWithDetails.goal.title}"
            )
            val txId = transactionRepository.insert(tx)

            // 2. Register entry linked to this transaction
            val entry = SavingsGoalEntryEntity(
                goalId = goalId,
                transactionId = txId,
                amount = amount,
                type = SavingsEntryType.DEPOSIT,
                date = now
            )
            savingsGoalDao.insertEntry(entry)

            // 3. Update completion state
            val newTotal = goalWithDetails.currentAmount + amount
            val isCompleted = goalWithDetails.goal.targetAmount > 0 && newTotal >= goalWithDetails.goal.targetAmount
            savingsGoalDao.updateCompletedStatus(goalId, isCompleted)
        }
    }

    suspend fun withdraw(
        goalId: Long,
        amount: Long,
        accountId: Long,
        transactionRepository: TransactionRepository
    ) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val goalWithDetails = savingsGoalDao.getById(goalId).firstOrNull() ?: return@withTransaction
            val now = System.currentTimeMillis()

            // 1. Register income transaction back to selected account
            val tx = TransactionEntity(
                type = TransactionType.INCOME,
                amount = amount,
                accountId = accountId,
                toAccountId = null,
                categoryId = null,
                date = now,
                note = "برداشت از پس‌انداز: ${goalWithDetails.goal.title}"
            )
            val txId = transactionRepository.insert(tx)

            // 2. Register entry linked to this transaction
            val entry = SavingsGoalEntryEntity(
                goalId = goalId,
                transactionId = txId,
                amount = amount,
                type = SavingsEntryType.WITHDRAW,
                date = now
            )
            savingsGoalDao.insertEntry(entry)

            // 3. Update completion state
            val newTotal = (goalWithDetails.currentAmount - amount).coerceAtLeast(0L)
            val isCompleted = goalWithDetails.goal.targetAmount > 0 && newTotal >= goalWithDetails.goal.targetAmount
            savingsGoalDao.updateCompletedStatus(goalId, isCompleted)
        }
    }
}
