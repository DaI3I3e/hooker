package com.example.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.TransactionType
import com.example.data.local.relation.AccountWithBalance
import com.example.data.local.relation.TransactionWithDetails
import com.example.data.repository.AccountRepository
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AccountDetailUiState(
    val accountWithBalance: AccountWithBalance? = null,
    val transactions: List<TransactionWithDetails> = emptyList(),
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val transactionCount: Int = 0
)

class AccountDetailViewModel(
    val accountId: Long,
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<AccountDetailUiState> = combine(
        accountRepository.getAccountWithBalance(accountId),
        transactionRepository.getTransactionsByAccount(accountId)
    ) { accountWithBalance, transactions ->
        var incomeSum = 0L
        var expenseSum = 0L

        transactions.forEach { tx ->
            val t = tx.transaction
            if (t.accountId == accountId) {
                if (t.type == TransactionType.EXPENSE) {
                    expenseSum += t.amount
                } else if (t.type == TransactionType.INCOME) {
                    incomeSum += t.amount
                } else if (t.type == TransactionType.TRANSFER) {
                    expenseSum += t.amount
                }
            } else if (t.toAccountId == accountId && t.type == TransactionType.TRANSFER) {
                incomeSum += t.amount
            }
        }

        AccountDetailUiState(
            accountWithBalance = accountWithBalance,
            transactions = transactions,
            totalIncome = incomeSum,
            totalExpense = expenseSum,
            transactionCount = transactions.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountDetailUiState()
    )

    class Factory(
        private val accountId: Long,
        private val accountRepository: AccountRepository,
        private val transactionRepository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AccountDetailViewModel(accountId, accountRepository, transactionRepository) as T
        }
    }
}
