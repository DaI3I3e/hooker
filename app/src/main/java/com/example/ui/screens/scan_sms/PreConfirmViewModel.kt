package com.example.ui.screens.scan_sms

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
import com.example.domain.usecase.AddTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PreConfirmUiState(
    val amount: Long = 0L,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val bankName: String? = null,
    val accountIdent: String? = null,
    val smsHash: String = "",
    val note: String = "",
    val selectedAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val errorMessage: String? = null,
    val isSavedSuccess: Boolean = false
)

class PreConfirmViewModel(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreConfirmUiState())
    val uiState: StateFlow<PreConfirmUiState> = _uiState.asStateFlow()

    fun initFromItem(item: ScannedSmsItem?) {
        if (item == null) return
        val finalAmount = item.parsedSms.amount ?: 0L
        val type = item.parsedSms.transactionType ?: TransactionType.EXPENSE
        val finalDate = item.parsedSms.date ?: item.date
        val finalBankName = item.parsedSms.bankName
        val finalAccountIdent = item.parsedSms.accountIdentifier
        val finalSmsHash = item.smsHash
        val finalNote = item.parsedSms.rawDescription ?: ""

        _uiState.update {
            it.copy(
                amount = finalAmount,
                transactionType = type,
                dateTimestamp = finalDate,
                bankName = finalBankName?.ifBlank { null },
                accountIdent = finalAccountIdent?.ifBlank { null },
                smsHash = finalSmsHash,
                note = finalNote
            )
        }

        viewModelScope.launch {
            val allAccs = accountRepository.allAccounts.first()
            var matchedAccount: AccountEntity? = null

            if (!finalAccountIdent.isNullOrBlank()) {
                matchedAccount = allAccs.find { acc ->
                    (acc.cardNumber != null && acc.cardNumber.contains(finalAccountIdent)) ||
                    (acc.shabaNumber != null && acc.shabaNumber.contains(finalAccountIdent)) ||
                    acc.name.contains(finalAccountIdent)
                }
            }

            if (matchedAccount == null && !finalBankName.isNullOrBlank()) {
                matchedAccount = allAccs.find { acc ->
                    acc.name.contains(finalBankName, ignoreCase = true) ||
                    (finalBankName.contains("ملی") && acc.name.contains("ملی")) ||
                    (finalBankName.contains("رسالت") && acc.name.contains("رسالت")) ||
                    (finalBankName.contains("تجارت") && acc.name.contains("تجارت")) ||
                    (finalBankName.contains("ملت") && acc.name.contains("ملت")) ||
                    (finalBankName.contains("دی") && acc.name.contains("دی"))
                }
            }

            val defaultAccId = matchedAccount?.id ?: allAccs.firstOrNull()?.id

            val allCats = categoryRepository.allCategories.first()
            val filteredCats = allCats.filter {
                when (type) {
                    TransactionType.EXPENSE -> it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH
                    TransactionType.INCOME -> it.type == CategoryType.INCOME || it.type == CategoryType.BOTH
                    else -> true
                }
            }

            _uiState.update {
                it.copy(
                    accounts = allAccs,
                    selectedAccountId = defaultAccId,
                    categories = filteredCats,
                    selectedCategoryId = filteredCats.firstOrNull()?.id
                )
            }
        }
    }

    fun initData(
        amount: Long = 0L,
        typeStr: String = "EXPENSE",
        date: Long = System.currentTimeMillis(),
        bankName: String? = null,
        accountIdent: String? = null,
        smsHash: String = "",
        note: String? = null
    ) {
        val holderItem = ScanSmsDataHolder.selectedItem
        val finalAmount = if (holderItem != null && holderItem.parsedSms.amount != null && holderItem.parsedSms.amount > 0L) {
            holderItem.parsedSms.amount
        } else amount

        val finalTypeStr = if (holderItem != null) {
            (holderItem.parsedSms.transactionType ?: TransactionType.EXPENSE).name
        } else typeStr

        val finalDate = if (holderItem != null && holderItem.parsedSms.date != null) {
            holderItem.parsedSms.date
        } else date

        val finalBankName = if (holderItem != null) {
            holderItem.parsedSms.bankName ?: bankName
        } else bankName

        val finalAccountIdent = if (holderItem != null) {
            holderItem.parsedSms.accountIdentifier ?: accountIdent
        } else accountIdent

        val finalSmsHash = if (holderItem != null && holderItem.smsHash.isNotBlank()) {
            holderItem.smsHash
        } else smsHash

        val finalNote = if (holderItem != null) {
            holderItem.parsedSms.rawDescription ?: note
        } else note

        val type = try {
            TransactionType.valueOf(finalTypeStr)
        } catch (e: Exception) {
            TransactionType.EXPENSE
        }

        _uiState.update {
            it.copy(
                amount = finalAmount,
                transactionType = type,
                dateTimestamp = finalDate,
                bankName = finalBankName?.ifBlank { null },
                accountIdent = finalAccountIdent?.ifBlank { null },
                smsHash = finalSmsHash,
                note = finalNote ?: ""
            )
        }

        viewModelScope.launch {
            val allAccs = accountRepository.allAccounts.first()
            var matchedAccount: AccountEntity? = null

            if (!finalAccountIdent.isNullOrBlank()) {
                matchedAccount = allAccs.find { acc ->
                    (acc.cardNumber != null && acc.cardNumber.contains(finalAccountIdent)) ||
                    (acc.shabaNumber != null && acc.shabaNumber.contains(finalAccountIdent)) ||
                    acc.name.contains(finalAccountIdent)
                }
            }

            if (matchedAccount == null && !finalBankName.isNullOrBlank()) {
                matchedAccount = allAccs.find { acc ->
                    acc.name.contains(finalBankName, ignoreCase = true) ||
                    (finalBankName.contains("ملی") && acc.name.contains("ملی")) ||
                    (finalBankName.contains("رسالت") && acc.name.contains("رسالت")) ||
                    (finalBankName.contains("تجارت") && acc.name.contains("تجارت")) ||
                    (finalBankName.contains("ملت") && acc.name.contains("ملت")) ||
                    (finalBankName.contains("دی") && acc.name.contains("دی"))
                }
            }

            val defaultAccId = matchedAccount?.id ?: allAccs.firstOrNull()?.id

            val allCats = categoryRepository.allCategories.first()
            val filteredCats = allCats.filter {
                when (type) {
                    TransactionType.EXPENSE -> it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH
                    TransactionType.INCOME -> it.type == CategoryType.INCOME || it.type == CategoryType.BOTH
                    else -> true
                }
            }

            _uiState.update {
                it.copy(
                    accounts = allAccs,
                    selectedAccountId = defaultAccId,
                    categories = filteredCats,
                    selectedCategoryId = filteredCats.firstOrNull()?.id
                )
            }
        }
    }

    fun setSelectedAccount(accountId: Long) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun setSelectedCategory(categoryId: Long) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun setTransactionType(type: TransactionType) {
        viewModelScope.launch {
            val allCats = categoryRepository.allCategories.first()
            val filteredCats = allCats.filter {
                when (type) {
                    TransactionType.EXPENSE -> it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH
                    TransactionType.INCOME -> it.type == CategoryType.INCOME || it.type == CategoryType.BOTH
                    else -> true
                }
            }
            _uiState.update {
                it.copy(
                    transactionType = type,
                    categories = filteredCats,
                    selectedCategoryId = if (filteredCats.any { c -> c.id == it.selectedCategoryId }) {
                        it.selectedCategoryId
                    } else {
                        filteredCats.firstOrNull()?.id
                    }
                )
            }
        }
    }

    fun setDateTimestamp(timestamp: Long) {
        _uiState.update { it.copy(dateTimestamp = timestamp) }
    }

    fun saveTransaction() {
        val currentState = _uiState.value
        val amt = currentState.amount
        val accId = currentState.selectedAccountId
        val catId = currentState.selectedCategoryId

        if (amt <= 0L) {
            _uiState.update { it.copy(errorMessage = "مبلغ معتبر نیست") }
            return
        }
        if (accId == null) {
            _uiState.update { it.copy(errorMessage = "لطفاً حساب را انتخاب کنید") }
            return
        }
        if (catId == null) {
            _uiState.update { it.copy(errorMessage = "لطفاً دسته را انتخاب کنید") }
            return
        }

        viewModelScope.launch {
            val transaction = TransactionEntity(
                type = currentState.transactionType,
                amount = amt,
                accountId = accId,
                categoryId = catId,
                date = currentState.dateTimestamp,
                note = currentState.note.ifBlank { null },
                smsHash = currentState.smsHash.ifBlank { null }
            )

            val res = addTransactionUseCase(transaction)
            if (res.isSuccess) {
                _uiState.update { it.copy(isSavedSuccess = true) }
            } else {
                _uiState.update {
                    it.copy(errorMessage = res.exceptionOrNull()?.message ?: "خطا در ثبت تراکنش")
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val addTransactionUseCase: AddTransactionUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PreConfirmViewModel(accountRepository, categoryRepository, addTransactionUseCase) as T
        }
    }
}
