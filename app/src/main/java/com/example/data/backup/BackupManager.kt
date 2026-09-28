package com.example.data.backup

import androidx.room.withTransaction
import com.example.data.local.FinTrackDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SmsPatternEntity
import com.example.data.local.entity.TransactionEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class BackupData(
    val version: Int = 4,
    val timestamp: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val debts: List<com.example.data.local.entity.DebtEntity>? = null,
    val sms_patterns: List<SmsPatternEntity>? = null,
    val cheques: List<com.example.data.local.entity.ChequeEntity>? = null,
    val recurring_transactions: List<com.example.data.local.entity.RecurringTransactionEntity>? = null,
    val savings_goals: List<com.example.data.local.entity.SavingsGoalEntity>? = null
)

class BackupManager(private val database: FinTrackDatabase) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val accounts = database.accountDao().getAll().first()
        val categories = database.categoryDao().getAll().first()
        val transactions = database.transactionDao().getAll().first()
        val debts = database.debtDao().getAll().first()
        val smsPatterns = database.smsPatternDao().getAll().first()
        val cheques = database.chequeDao().getAll().first()
        val recurringTransactions = database.recurringDao().getAllEntities().first()
        val savingsGoals = database.savingsGoalDao().getAll().first()

        val backupData = BackupData(
            version = 4,
            timestamp = System.currentTimeMillis(),
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            debts = debts,
            sms_patterns = smsPatterns,
            cheques = cheques,
            recurring_transactions = recurringTransactions,
            savings_goals = savingsGoals
        )
        gson.toJson(backupData)
    }

    suspend fun restoreBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupData = gson.fromJson(jsonString, BackupData::class.java) ?: return@withContext false
            database.withTransaction {
                // Delete in reverse order to satisfy foreign keys
                database.recurringDao().deleteAll()
                database.transactionDao().deleteAll()
                database.chequeDao().deleteAll()
                database.debtDao().deleteAll()
                database.smsPatternDao().deleteAll()
                database.savingsGoalDao().deleteAll()
                database.categoryDao().deleteAll()
                database.accountDao().deleteAll()

                backupData.accounts.forEach { database.accountDao().insert(it) }
                database.categoryDao().insertAll(backupData.categories)
                backupData.transactions.forEach { database.transactionDao().insert(it) }

                // Restore debts if present in backup
                backupData.debts?.let { debtList ->
                    debtList.forEach { database.debtDao().insert(it) }
                }

                // Restore SMS patterns if present in backup (graceful if absent)
                backupData.sms_patterns?.let { patterns ->
                    patterns.forEach { database.smsPatternDao().insert(it) }
                }

                // Restore cheques if present in backup
                backupData.cheques?.let { chequeList ->
                    chequeList.forEach { database.chequeDao().insert(it) }
                }

                // Restore recurring transactions if present in backup
                backupData.recurring_transactions?.let { recurringList ->
                    recurringList.forEach { database.recurringDao().insert(it) }
                }

                // Restore savings goals if present in backup
                backupData.savings_goals?.let { goalList ->
                    goalList.forEach { database.savingsGoalDao().insert(it) }
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
