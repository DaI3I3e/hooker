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
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class EditTransactionUiState(
    val transactionId: Long = 0L,
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

class EditTransactionViewModel(
    private val transactionId: Long,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditTransactionUiState(transactionId = transactionId))
    val uiState: StateFlow<EditTransactionUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val accounts = accountRepository.allAccounts.firstOrNull() ?: emptyList()
            val categories = categoryRepository.allCategories.firstOrNull() ?: emptyList()

            val txWithDetails = transactionRepository.getTransactionByIdWithDetails(transactionId).firstOrNull()
            if (txWithDetails != null) {
                val tx = txWithDetails.transaction
                val selectedAcc = accounts.find { it.id == tx.accountId }
                val selectedToAcc = accounts.find { it.id == tx.toAccountId }

                val filteredCats = filterCategories(categories, tx.type)
                val selectedCat = categories.find { it.id == tx.categoryId }
                val (h, m) = com.example.util.DateFormatter.extractHourAndMinute(tx.date)

                _uiState.value = EditTransactionUiState(
                    transactionId = tx.id,
                    type = tx.type,
                    amount = tx.amount,
                    selectedAccount = selectedAcc,
                    selectedToAccount = selectedToAcc,
                    selectedCategory = selectedCat,
                    dateTimestamp = tx.date,
                    selectedHour = h,
                    selectedMinute = m,
                    note = tx.note ?: "",
                    accounts = accounts,
                    categories = filteredCats,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "تراکنش یافت نشد"
                )
            }
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

            val updatedTransaction = TransactionEntity(
                id = state.transactionId,
                type = state.type,
                amount = state.amount,
                accountId = state.selectedAccount.id,
                toAccountId = if (state.type == TransactionType.TRANSFER) state.selectedToAccount?.id else null,
                categoryId = if (state.type != TransactionType.TRANSFER) state.selectedCategory?.id else null,
                date = finalTimestamp,
                note = state.note.ifBlank { null },
                createdAt = System.currentTimeMillis()
            )

            transactionRepository.updateTransaction(updatedTransaction)
            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
        }
    }

    fun duplicateAsNew() {
        val state = _uiState.value
        if (state.amount <= 0L) {
            _uiState.value = state.copy(errorMessage = "مبلغ باید بیشتر از صفر باشد")
            return
        }
        val acc = state.selectedAccount
        if (acc == null) {
            _uiState.value = state.copy(errorMessage = "لطفاً حساب را انتخاب کنید")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val now = System.currentTimeMillis()
            val newTx = TransactionEntity(
                type = state.type,
                amount = state.amount,
                accountId = acc.id,
                toAccountId = if (state.type == TransactionType.TRANSFER) state.selectedToAccount?.id else null,
                categoryId = if (state.type != TransactionType.TRANSFER) state.selectedCategory?.id else null,
                date = now,
                note = state.note.ifBlank { null },
                createdAt = now
            )
            transactionRepository.insertTransaction(newTx)
            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
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
        private val transactionId: Long,
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditTransactionViewModel(
                transactionId,
                transactionRepository,
                accountRepository,
                categoryRepository
            ) as T
        }
    }
}
