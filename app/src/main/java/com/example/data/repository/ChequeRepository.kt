package com.example.data.repository

import com.example.data.local.dao.ChequeDao
import com.example.data.local.entity.ChequeEntity
import com.example.data.local.entity.ChequeStatus
import com.example.data.local.entity.ChequeType
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ChequeRepository(
    private val chequeDao: ChequeDao
) {
    fun getAll(): Flow<List<ChequeEntity>> =
        chequeDao.getAll().flowOn(Dispatchers.IO)

    fun getByType(type: ChequeType): Flow<List<ChequeEntity>> =
        chequeDao.getByType(type).flowOn(Dispatchers.IO)

    fun getPending(): Flow<List<ChequeEntity>> =
        chequeDao.getPending().flowOn(Dispatchers.IO)

    fun getDueBefore(timestamp: Long): Flow<List<ChequeEntity>> =
        chequeDao.getDueBefore(timestamp).flowOn(Dispatchers.IO)

    fun getById(id: Long): Flow<ChequeEntity?> =
        chequeDao.getById(id).flowOn(Dispatchers.IO)

    suspend fun insert(cheque: ChequeEntity): Long =
        withContext(Dispatchers.IO) {
            chequeDao.insert(cheque)
        }

    suspend fun update(cheque: ChequeEntity) =
        withContext(Dispatchers.IO) {
            chequeDao.update(cheque)
        }

    suspend fun delete(cheque: ChequeEntity) =
        withContext(Dispatchers.IO) {
            chequeDao.delete(cheque)
        }

    suspend fun deleteById(id: Long) =
        withContext(Dispatchers.IO) {
            chequeDao.deleteById(id)
        }

    suspend fun updateStatus(id: Long, status: ChequeStatus) =
        withContext(Dispatchers.IO) {
            val clearedDate = if (status == ChequeStatus.CLEARED) System.currentTimeMillis() else null
            chequeDao.updateStatus(id, status, clearedDate)
        }

    suspend fun clearChequeAndRegisterTransaction(
        chequeId: Long,
        accountId: Long,
        transactionRepository: TransactionRepository
    ) = withContext(Dispatchers.IO) {
        val cheque = chequeDao.getById(chequeId).firstOrNull() ?: return@withContext
        val now = System.currentTimeMillis()

        val txType = if (cheque.type == ChequeType.PAYABLE) TransactionType.EXPENSE else TransactionType.INCOME
        val noteText = if (cheque.type == ChequeType.PAYABLE) {
            "پاس شدن چک پرداختی: صیاد ${cheque.sayadNumber} به ${cheque.partyName} (${cheque.bankName})"
        } else {
            "وصول چک دریافتی: صیاد ${cheque.sayadNumber} از ${cheque.partyName} (${cheque.bankName})"
        }

        val tx = TransactionEntity(
            type = txType,
            amount = cheque.amount,
            accountId = accountId,
            toAccountId = null,
            categoryId = null,
            date = now,
            note = noteText
        )
        transactionRepository.insert(tx)

        val updatedCheque = cheque.copy(
            status = ChequeStatus.CLEARED,
            accountId = accountId,
            clearedDate = now
        )
        chequeDao.update(updatedCheque)
    }
}
