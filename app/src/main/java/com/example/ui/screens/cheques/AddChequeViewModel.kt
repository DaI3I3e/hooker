package com.example.ui.screens.cheques

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.ChequeEntity
import com.example.data.local.entity.ChequeStatus
import com.example.data.local.entity.ChequeType
import com.example.data.repository.AccountRepository
import com.example.data.repository.ChequeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddChequeUiState(
    val sayadNumber: String = "",
    val type: ChequeType = ChequeType.PAYABLE,
    val amount: Long = 0L,
    val dueDate: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L),
    val partyName: String = "",
    val bankName: String = "ملی",
    val selectedAccountId: Long? = null,
    val note: String = "",
    val accounts: List<AccountEntity> = emptyList(),
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class AddChequeViewModel(
    private val chequeRepository: ChequeRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddChequeUiState())
    val uiState: StateFlow<AddChequeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val accountsList = accountRepository.getAll().firstOrNull() ?: emptyList()
            _uiState.update {
                it.copy(
                    accounts = accountsList,
                    selectedAccountId = accountsList.firstOrNull()?.id
                )
            }
        }
    }

    fun setSayadNumber(number: String) = _uiState.update { it.copy(sayadNumber = number, errorMessage = null) }
    fun setType(type: ChequeType) = _uiState.update { it.copy(type = type) }
    fun setAmount(amount: Long) = _uiState.update { it.copy(amount = amount, errorMessage = null) }
    fun setDueDate(date: Long) = _uiState.update { it.copy(dueDate = date) }
    fun setPartyName(name: String) = _uiState.update { it.copy(partyName = name, errorMessage = null) }
    fun setBankName(bank: String) = _uiState.update { it.copy(bankName = bank) }
    fun setAccount(accountId: Long?) = _uiState.update { it.copy(selectedAccountId = accountId) }
    fun setNote(note: String) = _uiState.update { it.copy(note = note) }

    fun save() {
        val state = _uiState.value
        if (state.sayadNumber.isBlank()) {
            _uiState.update { it.copy(errorMessage = "لطفاً شناسه ۱۶ رقمی صیاد یا شماره چک را وارد کنید") }
            return
        }
        if (state.amount <= 0) {
            _uiState.update { it.copy(errorMessage = "مبلغ چک باید بیشتر از صفر باشد") }
            return
        }
        if (state.partyName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "لطفاً نام طرف حساب (در وجه یا صادرکننده) را وارد کنید") }
            return
        }

        viewModelScope.launch {
            val cheque = ChequeEntity(
                sayadNumber = state.sayadNumber.trim(),
                type = state.type,
                amount = state.amount,
                dueDate = state.dueDate,
                issueDate = System.currentTimeMillis(),
                partyName = state.partyName.trim(),
                bankName = state.bankName.trim(),
                accountId = state.selectedAccountId,
                status = ChequeStatus.PENDING,
                note = state.note.ifBlank { null }
            )
            chequeRepository.insert(cheque)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    class Factory(
        private val chequeRepository: ChequeRepository,
        private val accountRepository: AccountRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddChequeViewModel(chequeRepository, accountRepository) as T
        }
    }
}
