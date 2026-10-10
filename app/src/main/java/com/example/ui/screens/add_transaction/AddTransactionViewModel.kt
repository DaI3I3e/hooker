package com.example.ui.screens.add_transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryType
import com.example.data.local.entity.GroupExpenseShareMode
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.repository.AccountRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.GroupExpenseRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.usecase.AddTransactionUseCase
import com.example.util.AmountFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class GroupShareInput(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Long = 0L
)

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
    val isSuccess: Boolean = false,
    // Group Expense (Dong)
    val isGroupExpenseEnabled: Boolean = false,
    val groupShareMode: GroupExpenseShareMode = GroupExpenseShareMode.EQUAL,
    val groupShares: List<GroupShareInput> = emptyList(),
    val recentPersonNames: List<String> = emptyList()
)

class AddTransactionViewModel(
    initialTypeString: String,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val groupExpenseRepository: GroupExpenseRepository
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
            val recentNames = groupExpenseRepository.recentPersonNames.firstOrNull() ?: emptyList()

            val lastAccId = settingsRepository.lastAccountId
            val lastCatId = settingsRepository.lastCategoryId

            val defaultAccount = accounts.find { it.id == lastAccId } ?: accounts.firstOrNull()
            val defaultToAccount = accounts.find { it.id != defaultAccount?.id } ?: accounts.getOrNull(1)

            val currentType = _uiState.value.type
            val filteredCategories = filterCategories(categories, currentType)
            val preferredCatId = when (currentType) {
                TransactionType.EXPENSE -> if (settingsRepository.lastExpenseCategoryId != 0L) settingsRepository.lastExpenseCategoryId else lastCatId
                TransactionType.INCOME -> if (settingsRepository.lastIncomeCategoryId != 0L) settingsRepository.lastIncomeCategoryId else lastCatId
                else -> lastCatId
            }
            val defaultCategory = filteredCategories.find { it.id == preferredCatId } ?: filteredCategories.firstOrNull()

            _uiState.update {
                it.copy(
                    accounts = accounts,
                    categories = filteredCategories,
                    selectedAccount = defaultAccount,
                    selectedToAccount = defaultToAccount,
                    selectedCategory = defaultCategory,
                    recentPersonNames = recentNames
                )
            }
        }
    }

    fun setTransactionType(type: TransactionType) {
        viewModelScope.launch {
            val allCats = categoryRepository.allCategories.firstOrNull() ?: emptyList()
            val filtered = filterCategories(allCats, type)
            val preferredCatId = when (type) {
                TransactionType.EXPENSE -> settingsRepository.lastExpenseCategoryId
                TransactionType.INCOME -> settingsRepository.lastIncomeCategoryId
                else -> 0L
            }
            val selectedCat = filtered.find { it.id == preferredCatId }
                ?: filtered.find { it.id == _uiState.value.selectedCategory?.id }
                ?: filtered.firstOrNull()

            _uiState.update {
                it.copy(
                    type = type,
                    categories = filtered,
                    selectedCategory = selectedCat,
                    // Disable group expense if switching away from EXPENSE
                    isGroupExpenseEnabled = if (type == TransactionType.EXPENSE) it.isGroupExpenseEnabled else false
                )
            }
        }
    }

    fun setAmount(amount: Long) {
        _uiState.update { current ->
            val updatedShares = if (current.isGroupExpenseEnabled && current.groupShareMode == GroupExpenseShareMode.EQUAL && current.groupShares.isNotEmpty()) {
                recalculateEqualShares(current.groupShares, amount)
            } else current.groupShares

            current.copy(
                amount = amount,
                groupShares = updatedShares,
                errorMessage = null
            )
        }
    }

    fun setSelectedAccount(account: AccountEntity) {
        _uiState.update { it.copy(selectedAccount = account) }
    }

    fun setSelectedToAccount(account: AccountEntity) {
        _uiState.update { it.copy(selectedToAccount = account) }
    }

    fun setSelectedCategory(category: CategoryEntity) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setDateTimestamp(timestamp: Long) {
        val hour = _uiState.value.selectedHour
        val minute = _uiState.value.selectedMinute
        val combined = com.example.util.DateFormatter.combineDateAndTime(timestamp, hour, minute)
        _uiState.update { it.copy(dateTimestamp = combined) }
    }

    fun setTime(hour: Int, minute: Int) {
        val combined = com.example.util.DateFormatter.combineDateAndTime(_uiState.value.dateTimestamp, hour, minute)
        _uiState.update {
            it.copy(
                selectedHour = hour,
                selectedMinute = minute,
                dateTimestamp = combined
            )
        }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    // --- Group Expense (Dong) Functions ---
    fun setGroupExpenseEnabled(enabled: Boolean) {
        _uiState.update { current ->
            val updatedShares = if (enabled && current.groupShareMode == GroupExpenseShareMode.EQUAL && current.groupShares.isNotEmpty() && current.amount > 0) {
                recalculateEqualShares(current.groupShares, current.amount)
            } else current.groupShares

            current.copy(
                isGroupExpenseEnabled = enabled,
                groupShares = updatedShares
            )
        }
    }

    fun setGroupShareMode(mode: GroupExpenseShareMode) {
        _uiState.update { current ->
            val updatedShares = if (mode == GroupExpenseShareMode.EQUAL && current.groupShares.isNotEmpty() && current.amount > 0) {
                recalculateEqualShares(current.groupShares, current.amount)
            } else current.groupShares

            current.copy(
                groupShareMode = mode,
                groupShares = updatedShares,
                errorMessage = null
            )
        }
    }

    fun addGroupPerson(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        _uiState.update { current ->
            if (current.groupShares.any { it.name.equals(trimmed, ignoreCase = true) }) {
                return@update current
            }
            val newList = current.groupShares + GroupShareInput(name = trimmed)
            val updated = if (current.groupShareMode == GroupExpenseShareMode.EQUAL && current.amount > 0) {
                recalculateEqualShares(newList, current.amount)
            } else newList

            current.copy(groupShares = updated, errorMessage = null)
        }
    }

    fun removeGroupPerson(id: String) {
        _uiState.update { current ->
            val newList = current.groupShares.filterNot { it.id == id }
            val updated = if (current.groupShareMode == GroupExpenseShareMode.EQUAL && current.amount > 0 && newList.isNotEmpty()) {
                recalculateEqualShares(newList, current.amount)
            } else newList

            current.copy(groupShares = updated, errorMessage = null)
        }
    }

    fun updateGroupPersonAmount(id: String, amount: Long) {
        _uiState.update { current ->
            val newList = current.groupShares.map {
                if (it.id == id) it.copy(amount = amount) else it
            }
            current.copy(groupShares = newList, errorMessage = null)
        }
    }

    private fun recalculateEqualShares(shares: List<GroupShareInput>, totalAmount: Long): List<GroupShareInput> {
        if (shares.isEmpty()) return shares
        val count = shares.size
        val baseShare = totalAmount / count
        val remainder = totalAmount % count

        return shares.mapIndexed { index, item ->
            // First person receives remainder to strictly ensure sum equals total amount (exact Rial)
            val amount = if (index == 0) baseShare + remainder else baseShare
            item.copy(amount = amount)
        }
    }

    fun saveTransaction() {
        val state = _uiState.value
        if (state.amount <= 0L) {
            _uiState.update { it.copy(errorMessage = "لطفاً مبلغ معتبری وارد کنید") }
            return
        }
        if (state.selectedAccount == null) {
            _uiState.update { it.copy(errorMessage = "لطفاً حساب مبدا را انتخاب کنید") }
            return
        }
        if (state.type == TransactionType.TRANSFER && state.selectedToAccount == null) {
            _uiState.update { it.copy(errorMessage = "لطفاً حساب مقصد را انتخاب کنید") }
            return
        }
        if (state.type == TransactionType.TRANSFER && state.selectedAccount.id == state.selectedToAccount?.id) {
            _uiState.update { it.copy(errorMessage = "حساب مبدا و مقصد نمی‌توانند یکسان باشند") }
            return
        }
        if (state.type != TransactionType.TRANSFER && state.selectedCategory == null) {
            _uiState.update { it.copy(errorMessage = "لطفاً دسته‌بندی را انتخاب کنید") }
            return
        }

        // Validate Group Expense
        if (state.type == TransactionType.EXPENSE && state.isGroupExpenseEnabled) {
            if (state.groupShares.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "لطفاً حداقل نام یک نفر را برای هزینه مشترک وارد کنید") }
                return
            }
            if (state.groupShares.any { it.name.isBlank() }) {
                _uiState.update { it.copy(errorMessage = "نام تمام افراد در هزینه مشترک باید مشخص باشد") }
                return
            }
            if (state.groupShareMode == GroupExpenseShareMode.CUSTOM) {
                val sumShares = state.groupShares.sumOf { it.amount }
                if (sumShares != state.amount) {
                    val diff = kotlin.math.abs(state.amount - sumShares)
                    val diffText = AmountFormatter.formatAmountWithCurrency(diff)
                    val msg = if (sumShares < state.amount) {
                        "مجموع سهم‌ها کمتر از کل مبلغ است ($diffText کسری)"
                    } else {
                        "مجموع سهم‌ها بیشتر از کل مبلغ است ($diffText مازاد)"
                    }
                    _uiState.update { it.copy(errorMessage = msg) }
                    return
                }
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

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
            result.onSuccess { txId ->
                // If group expense was enabled, save shares
                if (state.type == TransactionType.EXPENSE && state.isGroupExpenseEnabled && state.groupShares.isNotEmpty()) {
                    val finalShares = if (state.groupShareMode == GroupExpenseShareMode.EQUAL) {
                        recalculateEqualShares(state.groupShares, state.amount)
                    } else state.groupShares

                    groupExpenseRepository.saveGroupExpense(
                        transactionId = txId,
                        shareMode = state.groupShareMode,
                        shares = finalShares.map { it.name to it.amount }
                    )
                }

                settingsRepository.lastAccountId = state.selectedAccount.id
                state.selectedCategory?.let { cat ->
                    settingsRepository.lastCategoryId = cat.id
                    if (state.type == TransactionType.EXPENSE) {
                        settingsRepository.lastExpenseCategoryId = cat.id
                    } else if (state.type == TransactionType.INCOME) {
                        settingsRepository.lastIncomeCategoryId = cat.id
                    }
                }
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ex.message ?: "خطا در ثبت تراکنش"
                    )
                }
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
        private val addTransactionUseCase: AddTransactionUseCase,
        private val groupExpenseRepository: GroupExpenseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddTransactionViewModel(
                initialTypeString,
                accountRepository,
                categoryRepository,
                settingsRepository,
                addTransactionUseCase,
                groupExpenseRepository
            ) as T
        }
    }
}
