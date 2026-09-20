package com.example.ui.screens.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.repository.AccountRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.usecase.AddTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class TransferUiState(
    val amount: Long = 0L,
    val selectedFromAccount: AccountEntity? = null,
    val selectedToAccount: AccountEntity? = null,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val selectedHour: Int = java.time.LocalTime.now().hour,
    val selectedMinute: Int = java.time.LocalTime.now().minute,
    val note: String = "",
    val accounts: List<AccountEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class TransferViewModel(
    private val accountRepository: AccountRepository,
    private val settingsRepository: SettingsRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            val accounts = accountRepository.allAccounts.firstOrNull() ?: emptyList()
            val lastAccId = settingsRepository.lastAccountId

            val fromAcc = accounts.find { it.id == lastAccId } ?: accounts.firstOrNull()
            val toAcc = accounts.find { it.id != fromAcc?.id } ?: accounts.getOrNull(1)

            _uiState.value = _uiState.value.copy(
                accounts = accounts,
                selectedFromAccount = fromAcc,
                selectedToAccount = toAcc
            )
        }
    }

    fun setAmount(amount: Long) {
        _uiState.value = _uiState.value.copy(amount = amount, errorMessage = null)
    }

    fun setSelectedFromAccount(account: AccountEntity) {
        val currentTo = _uiState.value.selectedToAccount
        val newTo = if (currentTo?.id == account.id) {
            _uiState.value.accounts.find { it.id != account.id }
        } else {
            currentTo
        }
        _uiState.value = _uiState.value.copy(
            selectedFromAccount = account,
            selectedToAccount = newTo,
            errorMessage = null
        )
    }

    fun setSelectedToAccount(account: AccountEntity) {
        _uiState.value = _uiState.value.copy(selectedToAccount = account, errorMessage = null)
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

    fun executeTransfer() {
        val state = _uiState.value
        if (state.amount <= 0L) {
            _uiState.value = state.copy(errorMessage = "لطفاً مبلغ معتبری وارد کنید")
            return
        }
        if (state.selectedFromAccount == null) {
            _uiState.value = state.copy(errorMessage = "لطفاً حساب مبدا را انتخاب کنید")
            return
        }
        if (state.selectedToAccount == null) {
            _uiState.value = state.copy(errorMessage = "لطفاً حساب مقصد را انتخاب کنید")
            return
        }
        if (state.selectedFromAccount.id == state.selectedToAccount.id) {
            _uiState.value = state.copy(errorMessage = "حساب مبدا و مقصد نمی‌توانند یکسان باشند")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            val finalTimestamp = com.example.util.DateFormatter.combineDateAndTime(
                state.dateTimestamp,
                state.selectedHour,
                state.selectedMinute
            )

            val transferTransaction = TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = state.amount,
                accountId = state.selectedFromAccount.id,
                toAccountId = state.selectedToAccount.id,
                categoryId = null,
                date = finalTimestamp,
                note = state.note.ifBlank { "انتقال بین حساب‌ها" }
            )

            val result = addTransactionUseCase(transferTransaction)
            result.onSuccess {
                settingsRepository.lastAccountId = state.selectedFromAccount.id
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = ex.message ?: "خطا در ثبت انتقال"
                )
            }
        }
    }

    class Factory(
        private val accountRepository: AccountRepository,
        private val settingsRepository: SettingsRepository,
        private val addTransactionUseCase: AddTransactionUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransferViewModel(
                accountRepository,
                settingsRepository,
                addTransactionUseCase
            ) as T
        }
    }
}
