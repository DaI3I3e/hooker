package com.example.ui.screens.add_transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryType
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.repository.AccountRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.usecase.AddTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class AddTransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: Long = 0L,
    val selectedAccount: AccountEntity? = null,
    val selectedToAccount: AccountEntity? = null,
    val selectedCategory: CategoryEntity? = null,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val selectedHour: Int = java.time.LocalTime.now().hour,
    val selectedMinute: Int = java.time.LocalTime.now().minute,
    val note: String = "",
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class AddTransactionViewModel(
    initialTypeString: String,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddTransactionUiState(
            type = when (initialTypeString) {
                "INCOME" -> TransactionType.INCOME
                "TRANSFER" -> TransactionType.TRANSFER
                else -> TransactionType.EXPENSE
            }
        )
    )
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val accounts = accountRepository.allAccounts.firstOrNull() ?: emptyList()
            val categories = categoryRepository.allCategories.firstOrNull() ?: emptyList()

            val lastAccId = settingsRepository.lastAccountId
            val lastCatId = settingsRepository.lastCategoryId

            val defaultAccount = accounts.find { it.id == lastAccId } ?: accounts.firstOrNull()
            val defaultToAccount = accounts.find { it.id != defaultAccount?.id } ?: accounts.getOrNull(1)

            val currentType = _uiState.value.type
            val filteredCategories = filterCategories(categories, currentType)
            val defaultCategory = filteredCategories.find { it.id == lastCatId } ?: filteredCategories.firstOrNull()

            _uiState.value = _uiState.value.copy(
                accounts = accounts,
                categories = filteredCategories,
                selectedAccount = defaultAccount,
                selectedToAccount = defaultToAccount,
                selectedCategory = defaultCategory
            )
        }
    }

    fun setTransactionType(type: TransactionType) {
        viewModelScope.launch {
            val allCats = categoryRepository.allCategories.firstOrNull() ?: emptyList()
            val filtered = filterCategories(allCats, type)
            val selectedCat = filtered.find { it.id == _uiState.value.selectedCategory?.id } ?: filtered.firstOrNull()

            _uiState.value = _uiState.value.copy(
                type = type,
                categories = filtered,
                selectedCategory = selectedCat
            )
        }
    }

    fun setAmount(amount: Long) {
        _uiState.value = _uiState.value.copy(amount = amount, errorMessage = null)
    }

    fun setSelectedAccount(account: AccountEntity) {
        _uiState.value = _uiState.value.copy(selectedAccount = account)
    }

    fun setSelectedToAccount(account: AccountEntity) {
        _uiState.value = _uiState.value.copy(selectedToAccount = account)
    }

    fun setSelectedCategory(category: CategoryEntity) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun setDateTimestamp(timestamp: Long) {
        val hour = _uiState.value.selectedHour
        val minute = _uiState.value.selectedMinute
        val combined = com.example.util.DateFormatter.combineDateAndTime(timestamp, hour, minute)
        _uiState.value = _uiState.value.copy(dateTimestamp = combined)
    }

    fun setTime(hour: Int, minute: Int) {
        val combined = com.example.util.DateFormatter.combineDateAndTime(_uiState.value.dateTimestamp, hour, minute)
        _uiState.value = _uiState.value.copy(
            selectedHour = hour,
            selectedMinute = minute,
            dateTimestamp = combined
        )
    }

    fun setNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun saveTransaction() {
        val state = _uiState.value
        if (state.amount <= 0L) {
            _uiState.value = state.copy(errorMessage = "لطفاً مبلغ معتبری وارد کنید")
            return
        }
        if (state.selectedAccount == null) {
            _uiState.value = state.copy(errorMessage = "لطفاً حساب مبدا را انتخاب کنید")
            return
        }
        if (state.type == TransactionType.TRANSFER && state.selectedToAccount == null) {
            _uiState.value = state.copy(errorMessage = "لطفاً حساب مقصد را انتخاب کنید")
            return
        }
        if (state.type == TransactionType.TRANSFER && state.selectedAccount.id == state.selectedToAccount?.id) {
            _uiState.value = state.copy(errorMessage = "حساب مبدا و مقصد نمی‌توانند یکسان باشند")
            return
        }
        if (state.type != TransactionType.TRANSFER && state.selectedCategory == null) {
            _uiState.value = state.copy(errorMessage = "لطفاً دسته‌بندی را انتخاب کنید")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            val finalTimestamp = com.example.util.DateFormatter.combineDateAndTime(
                state.dateTimestamp,
                state.selectedHour,
                state.selectedMinute
            )

            val transaction = TransactionEntity(
                type = state.type,
                amount = state.amount,
                accountId = state.selectedAccount.id,
                toAccountId = if (state.type == TransactionType.TRANSFER) state.selectedToAccount?.id else null,
                categoryId = if (state.type != TransactionType.TRANSFER) state.selectedCategory?.id else null,
                date = finalTimestamp,
                note = state.note.ifBlank { null }
            )

            val result = addTransactionUseCase(transaction)
            result.onSuccess {
                settingsRepository.lastAccountId = state.selectedAccount.id
                state.selectedCategory?.let { cat ->
                    settingsRepository.lastCategoryId = cat.id
                }
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = ex.message ?: "خطا در ثبت تراکنش"
                )
            }
        }
    }

    private fun filterCategories(categories: List<CategoryEntity>, type: TransactionType): List<CategoryEntity> {
        return when (type) {
            TransactionType.EXPENSE -> categories.filter { it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH }
            TransactionType.INCOME -> categories.filter { it.type == CategoryType.INCOME || it.type == CategoryType.BOTH }
            TransactionType.TRANSFER -> emptyList()
        }
    }

    class Factory(
        private val initialTypeString: String,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val settingsRepository: SettingsRepository,
        private val addTransactionUseCase: AddTransactionUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddTransactionViewModel(
                initialTypeString,
                accountRepository,
                categoryRepository,
                settingsRepository,
                addTransactionUseCase
            ) as T
        }
    }
}
