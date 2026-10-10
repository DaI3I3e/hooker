package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.FinTrackDatabase
import com.example.data.local.entity.GroupExpenseEntity
import com.example.data.local.entity.GroupExpenseShareEntity
import com.example.data.local.entity.GroupExpenseShareMode
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.local.relation.GroupExpenseWithShares
import kotlinx.coroutines.flow.Flow

class GroupExpenseRepository(
    private val database: FinTrackDatabase
) {
    private val groupExpenseDao = database.groupExpenseDao()
    private val transactionDao = database.transactionDao()

    val allGroupExpenses: Flow<List<GroupExpenseWithShares>> = groupExpenseDao.getAllWithShares()
    val recentPersonNames: Flow<List<String>> = groupExpenseDao.getRecentPersonNames()

    suspend fun getByTransactionId(transactionId: Long): GroupExpenseWithShares? {
        return groupExpenseDao.getByTransactionId(transactionId)
    }

    fun observeByTransactionId(transactionId: Long): Flow<GroupExpenseWithShares?> {
        return groupExpenseDao.observeByTransactionId(transactionId)
    }

    suspend fun saveGroupExpense(
        transactionId: Long,
        shareMode: GroupExpenseShareMode,
        shares: List<Pair<String, Long>>
    ) {
        if (shares.isEmpty()) return

        database.withTransaction {
            // Remove existing if any (in case of edit)
            groupExpenseDao.deleteByTransactionId(transactionId)

            val groupExpenseId = groupExpenseDao.insertGroupExpense(
                GroupExpenseEntity(
                    transactionId = transactionId,
                    shareMode = shareMode,
                    createdAt = System.currentTimeMillis()
                )
            )

            val shareEntities = shares.map { (name, amount) ->
                GroupExpenseShareEntity(
                    groupExpenseId = groupExpenseId,
                    personName = name.trim(),
                    amount = amount,
                    isSettled = false,
                    settledTransactionId = null
                )
            }
            groupExpenseDao.insertShares(shareEntities)
        }
    }

    suspend fun settleShare(
        shareId: Long,
        incomeAccountId: Long? = null,
        categoryId: Long? = null,
        customNote: String? = null
    ) {
        database.withTransaction {
            val share = groupExpenseDao.getShareById(shareId) ?: return@withTransaction
            var createdIncomeId: Long? = null

            if (incomeAccountId != null) {
                val now = System.currentTimeMillis()
                val note = customNote ?: "تسویه دنگ ${share.personName}"
                val incomeTransaction = TransactionEntity(
                    amount = share.amount,
                    type = TransactionType.INCOME,
                    accountId = incomeAccountId,
                    categoryId = categoryId,
                    date = now,
                    note = note
                )
                createdIncomeId = transactionDao.insert(incomeTransaction)
            }

            groupExpenseDao.updateShare(
                share.copy(
                    isSettled = true,
                    settledTransactionId = createdIncomeId
                )
            )
        }
    }

    suspend fun unsettleShare(shareId: Long) {
        database.withTransaction {
            val share = groupExpenseDao.getShareById(shareId) ?: return@withTransaction
            val incomeTxId = share.settledTransactionId
            if (incomeTxId != null) {
                transactionDao.deleteById(incomeTxId)
            }

            groupExpenseDao.updateShare(
                share.copy(
                    isSettled = false,
                    settledTransactionId = null
                )
            )
        }
    }

    suspend fun deleteGroupExpenseByTransaction(transactionId: Long) {
        groupExpenseDao.deleteByTransactionId(transactionId)
    }
}
