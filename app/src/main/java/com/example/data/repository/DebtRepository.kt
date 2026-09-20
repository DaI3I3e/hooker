package com.example.data.repository

import com.example.data.local.dao.DebtDao
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DebtRepository(private val debtDao: DebtDao) {

    val allDebts: Flow<List<DebtEntity>> = debtDao.getAll()
    val totalOwedToMe: Flow<Long> = debtDao.getTotalOwedToMe()
    val totalIOwe: Flow<Long> = debtDao.getTotalIOwe()

    fun getDebtsByType(type: DebtType): Flow<List<DebtEntity>> = debtDao.getByType(type)

    fun getDebtById(id: Long): Flow<DebtEntity?> = debtDao.getById(id)

    suspend fun insertDebt(debt: DebtEntity): Long = withContext(Dispatchers.IO) {
        debtDao.insert(debt)
    }

    suspend fun updateDebt(debt: DebtEntity) = withContext(Dispatchers.IO) {
        debtDao.update(debt)
    }

    suspend fun deleteDebt(debt: DebtEntity) = withContext(Dispatchers.IO) {
        debtDao.delete(debt)
    }

    suspend fun markAsSettled(debt: DebtEntity, isSettled: Boolean) = withContext(Dispatchers.IO) {
        val updated = debt.copy(
            isSettled = isSettled,
            settledDate = if (isSettled) System.currentTimeMillis() else null
        )
        debtDao.update(updated)
    }
}
