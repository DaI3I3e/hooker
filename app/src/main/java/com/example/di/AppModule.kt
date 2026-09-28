package com.example.di

import android.content.Context
import com.example.data.backup.BackupManager
import com.example.data.local.FinTrackDatabase
import com.example.data.repository.AccountRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TransactionRepository
import com.example.domain.usecase.AddTransactionUseCase
import com.example.domain.usecase.GetDashboardDataUseCase
import com.example.util.Preferences

class AppModule(private val context: Context) {
    val database: FinTrackDatabase by lazy {
        FinTrackDatabase.getInstance(context)
    }

    val preferences: Preferences by lazy {
        Preferences(context)
    }

    val accountRepository: AccountRepository by lazy {
        AccountRepository(database.accountDao())
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(database.categoryDao())
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(preferences)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(database)
    }

    val debtRepository: com.example.data.repository.DebtRepository by lazy {
        com.example.data.repository.DebtRepository(database.debtDao())
    }

    val smsPatternRepository: com.example.data.repository.SmsPatternRepository by lazy {
        com.example.data.repository.SmsPatternRepository(database.smsPatternDao())
    }

    val chequeDao: com.example.data.local.dao.ChequeDao by lazy {
        database.chequeDao()
    }

    val chequeRepository: com.example.data.repository.ChequeRepository by lazy {
        com.example.data.repository.ChequeRepository(database.chequeDao())
    }

    val recurringDao: com.example.data.local.dao.RecurringDao by lazy {
        database.recurringDao()
    }

    val recurringRepository: com.example.data.repository.RecurringRepository by lazy {
        com.example.data.repository.RecurringRepository(database.recurringDao())
    }

    val savingsGoalDao: com.example.data.local.dao.SavingsGoalDao by lazy {
        database.savingsGoalDao()
    }

    val savingsGoalRepository: com.example.data.repository.SavingsGoalRepository by lazy {
        com.example.data.repository.SavingsGoalRepository(database.savingsGoalDao())
    }

    val addTransactionUseCase: AddTransactionUseCase by lazy {
        AddTransactionUseCase(transactionRepository)
    }

    val getDashboardDataUseCase: GetDashboardDataUseCase by lazy {
        GetDashboardDataUseCase(accountRepository, transactionRepository)
    }
}
