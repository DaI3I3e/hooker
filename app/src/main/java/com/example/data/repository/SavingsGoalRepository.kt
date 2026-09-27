package com.example.data.repository

import com.example.data.local.dao.SavingsGoalDao
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class SavingsGoalRepository(
    private val savingsGoalDao: SavingsGoalDao
) {
    fun getAll(): Flow<List<SavingsGoalEntity>> =
        savingsGoalDao.getAll().flowOn(Dispatchers.IO)

    fun getById(id: Long): Flow<SavingsGoalEntity?> =
        savingsGoalDao.getById(id).flowOn(Dispatchers.IO)

    suspend fun insert(goal: SavingsGoalEntity): Long =
        withContext(Dispatchers.IO) {
            savingsGoalDao.insert(goal)
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
        val goal = savingsGoalDao.getById(goalId).firstOrNull() ?: return@withContext
        val newAmount = goal.currentAmount + amount
        val isCompleted = newAmount >= goal.targetAmount
        savingsGoalDao.updateAmount(goalId, newAmount, isCompleted)

        // Register expense transaction from account
        val tx = TransactionEntity(
            type = TransactionType.EXPENSE,
            amount = amount,
            accountId = accountId,
            toAccountId = null,
            categoryId = null,
            date = System.currentTimeMillis(),
            note = "واریز به پس‌انداز: ${goal.title}"
        )
        transactionRepository.insert(tx)
    }

    suspend fun withdraw(
        goalId: Long,
        amount: Long,
        accountId: Long,
        transactionRepository: TransactionRepository
    ) = withContext(Dispatchers.IO) {
        val goal = savingsGoalDao.getById(goalId).firstOrNull() ?: return@withContext
        val newAmount = (goal.currentAmount - amount).coerceAtLeast(0L)
        val isCompleted = newAmount >= goal.targetAmount
        savingsGoalDao.updateAmount(goalId, newAmount, isCompleted)

        // Register income transaction back to account
        val tx = TransactionEntity(
            type = TransactionType.INCOME,
            amount = amount,
            accountId = accountId,
            toAccountId = null,
            categoryId = null,
            date = System.currentTimeMillis(),
            note = "برداشت از پس‌انداز: ${goal.title}"
        )
        transactionRepository.insert(tx)
    }
}
