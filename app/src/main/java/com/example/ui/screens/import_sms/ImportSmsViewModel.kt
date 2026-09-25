package com.example.ui.screens.import_sms

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
import com.example.data.repository.SmsPatternRepository
import com.example.domain.usecase.AddTransactionUseCase
import com.example.util.ParsedSms
import com.example.util.SmsParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportSmsUiState(
    val smsText: String = "",
    val parsedResult: ParsedSms? = null,
    val isAnalyzed: Boolean = false,
    val isRecognized: Boolean = false,
    val amount: Long = 0L,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val selectedAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val note: String = "",
    val dateTimestamp: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val errorMessage: String? = null,
    val isSavedSuccess: Boolean = false
)

class ImportSmsViewModel(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val smsPatternRepository: SmsPatternRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportSmsUiState())
    val uiState: StateFlow<ImportSmsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            launch {
                accountRepository.allAccounts.collect { accs ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            accounts = accs,
                            selectedAccountId = currentState.selectedAccountId ?: accs.firstOrNull()?.id
                        )
                    }
                }
            }
            launch {
                categoryRepository.allCategories.collect { cats ->
                    updateFilteredCategories(cats, _uiState.value.transactionType)
                }
            }
        }
    }

    private fun updateFilteredCategories(allCategories: List<CategoryEntity>, type: TransactionType) {
        val filtered = allCategories.filter {
            when (type) {
                TransactionType.EXPENSE -> it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH
                TransactionType.INCOME -> it.type == CategoryType.INCOME || it.type == CategoryType.BOTH
                else -> true
            }
        }
        _uiState.update { currentState ->
            currentState.copy(
                categories = filtered,
                selectedCategoryId = if (filtered.any { it.id == currentState.selectedCategoryId }) {
                    currentState.selectedCategoryId
                } else {
                    filtered.firstOrNull()?.id
                }
            )
        }
    }

    fun onSmsTextChange(text: String) {
        _uiState.update {
            it.copy(
                smsText = text,
                isAnalyzed = if (it.isAnalyzed) false else it.isAnalyzed,
                parsedResult = if (it.isAnalyzed) null else it.parsedResult
            )
        }
    }

    fun setInitialSmsText(text: String, autoParse: Boolean = false) {
        if (text.isNotBlank()) {
            _uiState.update { it.copy(smsText = text) }
            if (autoParse) {
                parseSms()
            }
        }
    }

    fun parseSms() {
        val text = _uiState.value.smsText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            val patterns = smsPatternRepository.allPatterns.first()
            val result = SmsParser.parse(text, patterns)
            val recognized = result.bankName != null || (result.amount != null && result.amount > 0L) || result.transactionType != null

            val parsedAmt = result.amount ?: 0L
            val type = result.transactionType ?: TransactionType.EXPENSE
            val timestamp = result.date ?: System.currentTimeMillis()
            val rawNote = result.rawDescription ?: ""

            val allAccs = accountRepository.allAccounts.first()
            var matchedAccount: AccountEntity? = null

            val accIdentifier = result.accountIdentifier
            val bankName = result.bankName

            if (!accIdentifier.isNullOrBlank()) {
                matchedAccount = allAccs.find { acc ->
                    (acc.cardNumber != null && acc.cardNumber.contains(accIdentifier)) ||
                    (acc.shabaNumber != null && acc.shabaNumber.contains(accIdentifier)) ||
                    acc.name.contains(accIdentifier)
                }
            }

            if (matchedAccount == null && !bankName.isNullOrBlank()) {
                matchedAccount = allAccs.find { acc ->
                    acc.name.contains(bankName, ignoreCase = true) ||
                    (bankName.contains("ملی") && acc.name.contains("ملی")) ||
                    (bankName.contains("رسالت") && acc.name.contains("رسالت")) ||
                    (bankName.contains("تجارت") && acc.name.contains("تجارت")) ||
                    (bankName.contains("ملت") && acc.name.contains("ملت")) ||
                    (bankName.contains("دی") && acc.name.contains("دی"))
                }
            }

            val finalAccId = matchedAccount?.id ?: _uiState.value.selectedAccountId ?: allAccs.firstOrNull()?.id

            val allCats = categoryRepository.allCategories.first()
            val filteredCats = allCats.filter {
                when (type) {
                    TransactionType.EXPENSE -> it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH
                    TransactionType.INCOME -> it.type == CategoryType.INCOME || it.type == CategoryType.BOTH
                    else -> true
                }
            }

            _uiState.update { currentState ->
                currentState.copy(
                    parsedResult = result,
                    isAnalyzed = true,
                    isRecognized = recognized,
                    amount = parsedAmt,
                    transactionType = type,
                    dateTimestamp = timestamp,
                    note = rawNote,
                    selectedAccountId = finalAccId,
                    categories = filteredCats,
                    selectedCategoryId = filteredCats.firstOrNull()?.id
                )
            }
        }
    }

    fun setTransactionType(type: TransactionType) {
        viewModelScope.launch {
            val allCats = categoryRepository.allCategories.first()
            _uiState.update { currentState ->
                val filtered = allCats.filter {
                    when (type) {
                        TransactionType.EXPENSE -> it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH
                        TransactionType.INCOME -> it.type == CategoryType.INCOME || it.type == CategoryType.BOTH
                        else -> true
                    }
                }
                currentState.copy(
                    transactionType = type,
                    categories = filtered,
                    selectedCategoryId = if (filtered.any { it.id == currentState.selectedCategoryId }) {
                        currentState.selectedCategoryId
                    } else {
                        filtered.firstOrNull()?.id
                    }
                )
            }
        }
    }

    fun setAmount(amount: Long) {
        _uiState.update { it.copy(amount = amount) }
    }

    fun setSelectedAccount(accountId: Long) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun setSelectedCategory(categoryId: Long) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun setDateTimestamp(timestamp: Long) {
        _uiState.update { it.copy(dateTimestamp = timestamp) }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun saveTransaction() {
        val currentState = _uiState.value
        if (currentState.amount <= 0L) {
            _uiState.update { it.copy(errorMessage = "مبلغ تراکنش باید بیشتر از صفر باشد") }
            return
        }

        val accId = currentState.selectedAccountId
        if (accId == null || accId <= 0L) {
            _uiState.update { it.copy(errorMessage = "لطفاً یک حساب انتخاب کنید") }
            return
        }

        val catId = currentState.selectedCategoryId
        if (catId == null || catId <= 0L) {
            _uiState.update { it.copy(errorMessage = "لطفاً یک دسته‌بندی انتخاب کنید") }
            return
        }

        viewModelScope.launch {
            val transaction = TransactionEntity(
                type = currentState.transactionType,
                amount = currentState.amount,
                accountId = accId,
                toAccountId = null,
                categoryId = catId,
                date = currentState.dateTimestamp,
                note = currentState.note.ifBlank { null }
            )

            val res = addTransactionUseCase(transaction)
            if (res.isSuccess) {
                _uiState.update { it.copy(isSavedSuccess = true) }
            } else {
                _uiState.update { it.copy(errorMessage = res.exceptionOrNull()?.message ?: "خطا در ثبت تراکنش") }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val smsPatternRepository: SmsPatternRepository,
        private val addTransactionUseCase: AddTransactionUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ImportSmsViewModel(accountRepository, categoryRepository, smsPatternRepository, addTransactionUseCase) as T
        }
    }
}
