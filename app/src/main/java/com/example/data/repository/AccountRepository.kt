package com.example.data.repository

import com.example.data.local.dao.AccountDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.relation.AccountWithBalance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AccountRepository(private val accountDao: AccountDao) {
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAll()
    val allAccountsWithBalance: Flow<List<AccountWithBalance>> = accountDao.getAllWithBalance()

    fun getAccountById(id: Long): Flow<AccountEntity?> = accountDao.getById(id)

    fun getAccountWithBalance(id: Long): Flow<AccountWithBalance?> = accountDao.getAccountWithBalance(id)

    suspend fun insertAccount(account: AccountEntity): Long = withContext(Dispatchers.IO) {
        accountDao.insert(account)
    }

    suspend fun updateAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        accountDao.update(account)
    }

    suspend fun deleteAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        accountDao.delete(account)
    }
}
