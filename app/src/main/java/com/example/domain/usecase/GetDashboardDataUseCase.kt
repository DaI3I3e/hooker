package com.example.domain.usecase

import com.example.data.repository.AccountRepository
import com.example.data.repository.TransactionRepository
import com.example.domain.model.DashboardSummary
import com.example.util.JalaliDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetDashboardDataUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<DashboardSummary> {
        val today = JalaliDate.today()
        val todayStart = today.toStartOfDayTimestamp()
        val todayEnd = today.toEndOfDayTimestamp()

        val accountsFlow = accountRepository.allAccountsWithBalance
        val recentTransactionsFlow = transactionRepository.getRecentTransactions(limit = 10)
        val todayIncomeFlow = transactionRepository.getTodayIncome(todayStart, todayEnd)
        val todayExpenseFlow = transactionRepository.getTodayExpense(todayStart, todayEnd)

        return combine(
            accountsFlow,
            recentTransactionsFlow,
            todayIncomeFlow,
            todayExpenseFlow
        ) { accounts, recentTx, todayIncome, todayExpense ->
            val totalBalance = accounts.sumOf { it.currentBalance }
            DashboardSummary(
                totalBalance = totalBalance,
                todayIncome = todayIncome,
                todayExpense = todayExpense,
                recentTransactions = recentTx,
                accounts = accounts
            )
        }
    }
}
