package com.example.data.repository

import com.example.data.local.dao.SmsPatternDao
import com.example.data.local.entity.SmsPatternEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SmsPatternRepository(
    private val smsPatternDao: SmsPatternDao
) {
    val allPatterns: Flow<List<SmsPatternEntity>> = smsPatternDao.getAll()
    val activePatterns: Flow<List<SmsPatternEntity>> = smsPatternDao.getAllActive()

    suspend fun getActivePatternsList(): List<SmsPatternEntity> = withContext(Dispatchers.IO) {
        smsPatternDao.getAllActiveList()
    }

    fun getByAccountIdentifier(identifier: String): Flow<List<SmsPatternEntity>> {
        return smsPatternDao.getByAccountIdentifier(identifier)
    }

    fun getById(id: Long): Flow<SmsPatternEntity?> {
        return smsPatternDao.getById(id)
    }

    suspend fun insertPattern(pattern: SmsPatternEntity): Long = withContext(Dispatchers.IO) {
        smsPatternDao.insert(pattern)
    }

    suspend fun updatePattern(pattern: SmsPatternEntity) = withContext(Dispatchers.IO) {
        smsPatternDao.update(pattern)
    }

    suspend fun deletePattern(pattern: SmsPatternEntity) = withContext(Dispatchers.IO) {
        smsPatternDao.delete(pattern)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        smsPatternDao.deleteById(id)
    }
}
