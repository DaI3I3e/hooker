package com.example.data.backup

import com.example.data.local.FinTrackDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>
)

class BackupManager(private val database: FinTrackDatabase) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val accounts = database.accountDao().getAll().first()
        val categories = database.categoryDao().getAll().first()
        val transactions = database.transactionDao().getAll().first()

        val backupData = BackupData(
            version = 1,
            timestamp = System.currentTimeMillis(),
            accounts = accounts,
            categories = categories,
            transactions = transactions
        )
        gson.toJson(backupData)
    }

    suspend fun restoreBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupData = gson.fromJson(jsonString, BackupData::class.java) ?: return@withContext false
            database.runInTransaction {
                // Perform restoration
                kotlinx.coroutines.runBlocking {
                    database.transactionDao().deleteAll()
                    database.accountDao().deleteAll()
                    database.categoryDao().deleteAll()

                    backupData.accounts.forEach { database.accountDao().insert(it) }
                    database.categoryDao().insertAll(backupData.categories)
                    backupData.transactions.forEach { database.transactionDao().insert(it) }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun validateBackupJson(jsonString: String): Boolean {
        return try {
            val backupData = gson.fromJson(jsonString, BackupData::class.java)
            backupData != null && backupData.accounts != null && backupData.categories != null && backupData.transactions != null
        } catch (e: Exception) {
            false
        }
    }
}
