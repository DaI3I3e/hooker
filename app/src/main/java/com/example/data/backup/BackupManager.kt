package com.example.data.backup

import androidx.room.withTransaction
import com.example.data.local.FinTrackDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ChequeEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.SavingsGoalEntryEntity
import com.example.data.local.entity.SmsPatternEntity
import com.example.data.local.entity.TransactionEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class BackupData(
    val backupFormatVersion: Int = 1,
    val databaseSchemaVersion: Int = 8,
    val version: Int? = null, // Backward compatibility with legacy backups
    val timestamp: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val debts: List<DebtEntity>? = null,
    val sms_patterns: List<SmsPatternEntity>? = null,
    val cheques: List<ChequeEntity>? = null,
    val recurring_transactions: List<RecurringTransactionEntity>? = null,
    val savings_goals: List<SavingsGoalEntity>? = null,
    val savings_goal_entries: List<SavingsGoalEntryEntity>? = null,
    val group_expenses: List<com.example.data.local.entity.GroupExpenseEntity>? = null,
    val group_expense_shares: List<com.example.data.local.entity.GroupExpenseShareEntity>? = null
)

class BackupManager(private val database: FinTrackDatabase) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    companion object {
        const val CURRENT_BACKUP_FORMAT_VERSION = 1
        const val CURRENT_DATABASE_SCHEMA_VERSION = 9
    }

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val accounts = database.accountDao().getAll().first()
        val categories = database.categoryDao().getAll().first()
        val transactions = database.transactionDao().getAll().first()
        val debts = database.debtDao().getAll().first()
        val smsPatterns = database.smsPatternDao().getAll().first()
        val cheques = database.chequeDao().getAll().first()
        val recurringTransactions = database.recurringDao().getAllEntities().first()
        val savingsGoals = database.savingsGoalDao().getAllGoalsRaw().first()
        val savingsGoalEntries = database.savingsGoalDao().getAllEntries().first()
        val groupExpenses = database.groupExpenseDao().getAllEntities()
        val groupExpenseShares = database.groupExpenseDao().getAllShares()

        val backupData = BackupData(
            backupFormatVersion = CURRENT_BACKUP_FORMAT_VERSION,
            databaseSchemaVersion = CURRENT_DATABASE_SCHEMA_VERSION,
            version = 4,
            timestamp = System.currentTimeMillis(),
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            debts = debts,
            sms_patterns = smsPatterns,
            cheques = cheques,
            recurring_transactions = recurringTransactions,
            savings_goals = savingsGoals,
            savings_goal_entries = savingsGoalEntries,
            group_expenses = groupExpenses,
            group_expense_shares = groupExpenseShares
        )
        gson.toJson(backupData)
    }

    suspend fun restoreBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupData = gson.fromJson(jsonString, BackupData::class.java) ?: return@withContext false

            // Reject if backup comes from a newer database schema
            val fileDbVersion = backupData.databaseSchemaVersion
            if (fileDbVersion > CURRENT_DATABASE_SCHEMA_VERSION) {
                throw IllegalStateException("نسخه پایگاه داده فایل پشتیبان ($fileDbVersion) از نسخه برنامه شما ($CURRENT_DATABASE_SCHEMA_VERSION) جدیدتر است. لطفاً برنامه را بروزرسانی کنید.")
            }

            // Validate mandatory structures
            if (!validateBackupJson(jsonString)) {
                return@withContext false
            }

            database.withTransaction {
                // Delete in reverse order of foreign key dependencies
                database.savingsGoalDao().deleteAllEntries()
                database.savingsGoalDao().deleteAll()
                database.recurringDao().deleteAll()
                database.groupExpenseDao().deleteAllShares()
                database.groupExpenseDao().deleteAllGroupExpenses()
                database.transactionDao().deleteAll()
                database.chequeDao().deleteAll()
                database.debtDao().deleteAll()
                database.smsPatternDao().deleteAll()
                database.categoryDao().deleteAll()
                database.accountDao().deleteAll()

                // Insert core data
                backupData.accounts.forEach { database.accountDao().insert(it) }
                database.categoryDao().insertAll(backupData.categories)
                backupData.transactions.forEach { database.transactionDao().insert(it) }

                // Restore optional/newer tables gracefully if present
                backupData.debts?.let { debtList ->
                    debtList.forEach { database.debtDao().insert(it) }
                }

                backupData.sms_patterns?.let { patterns ->
                    patterns.forEach { database.smsPatternDao().insert(it) }
                }

                backupData.cheques?.let { chequeList ->
                    chequeList.forEach { database.chequeDao().insert(it) }
                }

                backupData.recurring_transactions?.let { recurringList ->
                    recurringList.forEach { database.recurringDao().insert(it) }
                }

                backupData.savings_goals?.let { goalList ->
                    goalList.forEach { database.savingsGoalDao().insert(it) }
                }

                backupData.savings_goal_entries?.let { entryList ->
                    entryList.forEach { database.savingsGoalDao().insertEntry(it) }
                }

                backupData.group_expenses?.let { groupList ->
                    groupList.forEach { database.groupExpenseDao().insertGroupExpense(it) }
                }

                backupData.group_expense_shares?.let { shareList ->
                    database.groupExpenseDao().insertShares(shareList)
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
            val backupData = gson.fromJson(jsonString, BackupData::class.java) ?: return false

            // 1. Mandatory lists cannot be null
            if (backupData.accounts.isNullOrEmpty() || backupData.categories.isNullOrEmpty()) {
                return false
            }

            // 2. Validate accounts
            val validAccounts = backupData.accounts.all {
                it.name.isNotBlank()
            }
            if (!validAccounts) return false

            // 3. Validate categories
            val validCategories = backupData.categories.all {
                it.name.isNotBlank()
            }
            if (!validCategories) return false

            // 4. Validate transactions: amount >= 0 and positive date
            val validTransactions = backupData.transactions.all {
                it.amount >= 0 && it.date > 0
            }
            if (!validTransactions) return false

            true
        } catch (e: Exception) {
            false
        }
    }
}
