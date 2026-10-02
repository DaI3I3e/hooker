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
    val savings_goal_entries: List<SavingsGoalEntryEntity>? = null
)

sealed class RestoreExecutionResult {
    data class Success(val isLegacy: Boolean, val message: String) : RestoreExecutionResult()
    data class Error(val message: String) : RestoreExecutionResult()
}

class BackupManager(private val database: FinTrackDatabase) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    companion object {
        const val CURRENT_BACKUP_FORMAT_VERSION = 1
        const val CURRENT_DATABASE_SCHEMA_VERSION = 8
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
            savings_goal_entries = savingsGoalEntries
        )
        gson.toJson(backupData)
    }

    /**
     * Creates an AES-GCM encrypted binary backup package: [12-byte IV] + [Encrypted JSON].
     */
    suspend fun createEncryptedBackup(): ByteArray = withContext(Dispatchers.IO) {
        val json = createBackupJson()
        BackupCrypto.encryptBackup(json)
    }

    /**
     * Creates an encrypted backup encoded as Base64 text (for clipboard or text sharing).
     */
    suspend fun createEncryptedBackupBase64(): String = withContext(Dispatchers.IO) {
        val bytes = createEncryptedBackup()
        BackupCrypto.bytesToBase64(bytes)
    }

    /**
     * Decrypts, validates, and prepares backup content from binary data.
     */
    fun decryptAndValidateBytes(bytes: ByteArray): Pair<DecryptResult, Boolean> {
        val decryptResult = BackupCrypto.decryptBackup(bytes)
        val isValid = when (decryptResult) {
            is DecryptResult.Success -> validateBackupJson(decryptResult.jsonString)
            is DecryptResult.LegacyPlainJson -> validateBackupJson(decryptResult.jsonString)
            is DecryptResult.Error -> false
        }
        return Pair(decryptResult, isValid)
    }

    /**
     * Decrypts, validates, and prepares backup content from string (Base64 or plain JSON).
     */
    fun decryptAndValidateString(rawText: String): Pair<DecryptResult, Boolean> {
        val decryptResult = BackupCrypto.decryptFromString(rawText)
        val isValid = when (decryptResult) {
            is DecryptResult.Success -> validateBackupJson(decryptResult.jsonString)
            is DecryptResult.LegacyPlainJson -> validateBackupJson(decryptResult.jsonString)
            is DecryptResult.Error -> false
        }
        return Pair(decryptResult, isValid)
    }

    suspend fun restoreDecryptedJson(jsonString: String, isLegacy: Boolean): RestoreExecutionResult = withContext(Dispatchers.IO) {
        try {
            val backupData = gson.fromJson(jsonString, BackupData::class.java)
                ?: return@withContext RestoreExecutionResult.Error("فایل پشتیبان ساختار نامعتبر دارد.")

            // Reject if backup comes from a newer database schema
            val fileDbVersion = backupData.databaseSchemaVersion
            if (fileDbVersion > CURRENT_DATABASE_SCHEMA_VERSION) {
                return@withContext RestoreExecutionResult.Error("نسخه پایگاه داده فایل پشتیبان ($fileDbVersion) از نسخه برنامه شما ($CURRENT_DATABASE_SCHEMA_VERSION) جدیدتر است. لطفاً برنامه را بروزرسانی کنید.")
            }

            if (!validateBackupJson(jsonString)) {
                return@withContext RestoreExecutionResult.Error("ساختار اطلاعات در فایل پشتیبان معتبر نیست.")
            }

            database.withTransaction {
                // Delete in reverse order of foreign key dependencies
                database.savingsGoalDao().deleteAllEntries()
                database.savingsGoalDao().deleteAll()
                database.recurringDao().deleteAll()
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
            }

            val msg = if (isLegacy) {
                "فایل پشتیبان قدیمی (رمزنگاری‌نشده) با موفقیت بازیابی شد."
            } else {
                "فایل پشتیبان امن و رمزنگاری‌شده (AES-256) با موفقیت بازیابی شد."
            }
            RestoreExecutionResult.Success(isLegacy, msg)
        } catch (e: Exception) {
            e.printStackTrace()
            RestoreExecutionResult.Error("خطا در بازیابی اطلاعات: ${e.message}")
        }
    }

    suspend fun restoreBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        val result = restoreDecryptedJson(jsonString, isLegacy = true)
        result is RestoreExecutionResult.Success
    }

    fun validateBackupJson(jsonString: String): Boolean {
        return try {
            val backupData = gson.fromJson(jsonString, BackupData::class.java) ?: return false

            if (backupData.accounts.isNullOrEmpty() || backupData.categories.isNullOrEmpty()) {
                return false
            }

            val validAccounts = backupData.accounts.all {
                it.name.isNotBlank()
            }
            if (!validAccounts) return false

            val validCategories = backupData.categories.all {
                it.name.isNotBlank()
            }
            if (!validCategories) return false

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
