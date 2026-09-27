package com.example.data.backup

import androidx.room.withTransaction
import com.example.data.local.FinTrackDatabase
import com.example.data.local.SeedData
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ChequeEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.SavingsGoalEntity
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
    val accounts: List<AccountEntity>?,
    val categories: List<CategoryEntity>?,
    val transactions: List<TransactionEntity>?,
    val debts: List<DebtEntity>? = null,
    val sms_patterns: List<SmsPatternEntity>? = null,
    val recurring_transactions: List<RecurringTransactionEntity>? = null,
    val savings_goals: List<SavingsGoalEntity>? = null,
    val cheques: List<ChequeEntity>? = null
)

class BackupManager(private val database: FinTrackDatabase) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val accounts = database.accountDao().getAll().first()
        val categories = database.categoryDao().getAll().first()
        val transactions = database.transactionDao().getAll().first()
        val debts = database.debtDao().getAll().first()
        val smsPatterns = database.smsPatternDao().getAll().first()
        val recurringTransactions = database.recurringDao().getAll().first().map { it.recurring }
        val savingsGoals = database.savingsGoalDao().getAll().first()
        val cheques = database.chequeDao().getAll().first()

        val backupData = BackupData(
            version = 4,
            timestamp = System.currentTimeMillis(),
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            debts = debts,
            sms_patterns = smsPatterns,
            recurring_transactions = recurringTransactions,
            savings_goals = savingsGoals,
            cheques = cheques
        )
        gson.toJson(backupData)
    }

    suspend fun restoreBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (jsonString.isBlank()) return@withContext false
            val backupData = gson.fromJson(jsonString, BackupData::class.java) ?: return@withContext false

            val accountsList = backupData.accounts?.filterNotNull() ?: emptyList()
            val categoriesList = backupData.categories?.filterNotNull() ?: emptyList()
            val transactionsList = backupData.transactions?.filterNotNull() ?: emptyList()

            // Run completely inside Room's withTransaction to avoid deadlocks and ensure full atomicity
            database.withTransaction {
                // 1. Delete in reverse dependency order to satisfy foreign key constraints
                database.recurringDao().deleteAll()
                database.chequeDao().deleteAll()
                database.savingsGoalDao().deleteAll()
                database.transactionDao().deleteAll()
                database.debtDao().deleteAll()
                database.smsPatternDao().deleteAll()
                database.categoryDao().deleteAll()
                database.accountDao().deleteAll()

                // 2. Restore Accounts first
                if (accountsList.isNotEmpty()) {
                    accountsList.forEach { account ->
                        database.accountDao().insert(account)
                    }
                }

                // 3. Restore Categories
                if (categoriesList.isNotEmpty()) {
                    database.categoryDao().insertAll(categoriesList)
                } else {
                    // Re-seed default categories if empty in backup
                    SeedData.insertDefaults(database.accountDao(), database.categoryDao())
                }

                // 4. Restore Transactions
                if (transactionsList.isNotEmpty()) {
                    transactionsList.forEach { transaction ->
                        database.transactionDao().insert(transaction)
                    }
                }

                // 5. Restore Debts
                backupData.debts?.filterNotNull()?.forEach { debt ->
                    database.debtDao().insert(debt)
                }

                // 6. Restore SMS Patterns
                backupData.sms_patterns?.filterNotNull()?.forEach { pattern ->
                    database.smsPatternDao().insert(pattern)
                }

                // 7. Restore Recurring Transactions
                backupData.recurring_transactions?.filterNotNull()?.forEach { recurring ->
                    database.recurringDao().insert(recurring)
                }

                // 8. Restore Savings Goals
                backupData.savings_goals?.filterNotNull()?.forEach { goal ->
                    database.savingsGoalDao().insert(goal)
                }

                // 9. Restore Cheques
                backupData.cheques?.filterNotNull()?.forEach { cheque ->
                    database.chequeDao().insert(cheque)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun validateBackupJson(jsonString: String): Boolean {
        if (jsonString.isBlank()) return false
        return try {
            val backupData = gson.fromJson(jsonString, BackupData::class.java) ?: return false
            // A valid backup must at least have non-null accounts or categories
            backupData.accounts != null && backupData.categories != null
        } catch (e: Exception) {
            false
        }
    }
}
