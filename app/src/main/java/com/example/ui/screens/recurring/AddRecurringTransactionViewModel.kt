package com.example.ui.screens.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurrencePeriod
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.repository.AccountRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.RecurringRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddRecurringUiState(
    val title: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: Long = 0L,
    val selectedAccountId: Long? = null,
    val selectedToAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val period: RecurrencePeriod = RecurrencePeriod.MONTHLY,
    val nextDueDate: Long = System.currentTimeMillis(),
    val isInstallment: Boolean = false,
    val totalInstallments: String = "12",
    val note: String = "",
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class AddRecurringTransactionViewModel(
    private val recurringRepository: RecurringRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddRecurringUiState())
    val uiState: StateFlow<AddRecurringUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val accountsList = accountRepository.getAll().firstOrNull() ?: emptyList()
            val categoriesList = categoryRepository.getAll().firstOrNull() ?: emptyList()
            _uiState.update {
                it.copy(
                    accounts = accountsList,
                    categories = categoriesList,
                    selectedAccountId = accountsList.firstOrNull()?.id,
                    selectedCategoryId = categoriesList.firstOrNull { c -> c.type.name == it.type.name }?.id
                )
            }
        }
    }

    fun setTitle(title: String) = _uiState.update { it.copy(title = title, errorMessage = null) }
    fun setAmount(amount: Long) = _uiState.update { it.copy(amount = amount, errorMessage = null) }
    fun setType(type: TransactionType) {
        _uiState.update { current ->
            val matchingCat = current.categories.firstOrNull { c -> c.type.name == type.name }?.id
            current.copy(type = type, selectedCategoryId = matchingCat)
        }
    }
    fun setAccount(accountId: Long) = _uiState.update { it.copy(selectedAccountId = accountId) }
    fun setToAccount(accountId: Long) = _uiState.update { it.copy(selectedToAccountId = accountId) }
    fun setCategory(categoryId: Long?) = _uiState.update { it.copy(selectedCategoryId = categoryId) }
    fun setPeriod(period: RecurrencePeriod) = _uiState.update { it.copy(period = period) }
    fun setDueDate(timestamp: Long) = _uiState.update { it.copy(nextDueDate = timestamp) }
    fun setIsInstallment(isInst: Boolean) = _uiState.update { it.copy(isInstallment = isInst) }
    fun setTotalInstallments(count: String) = _uiState.update { it.copy(totalInstallments = count) }
    fun setNote(note: String) = _uiState.update { it.copy(note = note) }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "لطفاً عنوان را وارد کنید") }
            return
        }
        if (state.amount <= 0) {
            _uiState.update { it.copy(errorMessage = "مبلغ باید بیشتر از صفر باشد") }
            return
        }
        if (state.selectedAccountId == null) {
            _uiState.update { it.copy(errorMessage = "لطفاً حساب مورد نظر را انتخاب کنید") }
            return
        }
        val totalInst = if (state.isInstallment) state.totalInstallments.toIntOrNull() ?: 12 else null

        viewModelScope.launch {
            val entity = RecurringTransactionEntity(
                title = state.title.trim(),
                type = state.type,
                amount = state.amount,
                accountId = state.selectedAccountId,
                toAccountId = if (state.type == TransactionType.TRANSFER) state.selectedToAccountId else null,
                categoryId = if (state.type != TransactionType.TRANSFER) state.selectedCategoryId else null,
                period = state.period,
                startDate = System.currentTimeMillis(),
                nextDueDate = state.nextDueDate,
                isInstallment = state.isInstallment,
                totalInstallments = totalInst,
                paidInstallments = 0,
                isActive = true,
                note = state.note.ifBlank { null }
            )
            recurringRepository.insert(entity)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    class Factory(
        private val recurringRepository: RecurringRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddRecurringTransactionViewModel(recurringRepository, accountRepository, categoryRepository) as T
        }
    }
}
