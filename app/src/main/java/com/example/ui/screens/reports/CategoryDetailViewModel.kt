package com.example.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionType
import com.example.data.local.relation.TransactionWithDetails
import com.example.data.repository.AccountRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CategoryDetailUiState(
    val category: CategoryEntity? = null,
    val transactions: List<TransactionWithDetails> = emptyList(),
    val totalAmount: Long = 0L,
    val transactionCount: Int = 0,
    val isLoading: Boolean = false
)

class CategoryDetailViewModel(
    private val categoryId: Long,
    private val startDate: Long,
    private val endDate: Long,
    private val accountId: Long?,
    private val reportType: TransactionType,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    val uiState: StateFlow<CategoryDetailUiState> = combine(
        categoryRepository.getCategoryById(categoryId),
        transactionRepository.allTransactionsWithDetails
    ) { category, allTx ->
        val matchingTx = allTx.filter { item ->
            val tx = item.transaction
            val matchesCategory = tx.categoryId == categoryId
            val matchesDate = tx.date in startDate..endDate
            val matchesAccount = (accountId == null || tx.accountId == accountId || tx.toAccountId == accountId)
            val matchesType = tx.type == reportType
            matchesCategory && matchesDate && matchesAccount && matchesType
        }.sortedByDescending { it.transaction.date }

        val total = matchingTx.sumOf { it.transaction.amount }

        CategoryDetailUiState(
            category = category,
            transactions = matchingTx,
            totalAmount = total,
            transactionCount = matchingTx.size,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryDetailUiState(isLoading = true)
    )

    class Factory(
        private val categoryId: Long,
        private val startDate: Long,
        private val endDate: Long,
        private val accountId: Long?,
        private val reportType: TransactionType,
        private val categoryRepository: CategoryRepository,
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CategoryDetailViewModel(
                categoryId,
                startDate,
                endDate,
                accountId,
                reportType,
                categoryRepository,
                transactionRepository,
                accountRepository
            ) as T
        }
    }
}
