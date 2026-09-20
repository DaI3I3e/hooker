package com.example.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.SmsPatternEntity
import com.example.data.local.relation.AccountWithBalance
import com.example.data.repository.AccountRepository
import com.example.data.repository.SmsPatternRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountsViewModel(
    private val accountRepository: AccountRepository,
    private val smsPatternRepository: SmsPatternRepository
) : ViewModel() {

    val accountsWithBalance: StateFlow<List<AccountWithBalance>> = accountRepository.allAccountsWithBalance
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allPatterns: StateFlow<List<SmsPatternEntity>> = smsPatternRepository.allPatterns
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            accountRepository.deleteAccount(account)
        }
    }

    class Factory(
        private val accountRepository: AccountRepository,
        private val smsPatternRepository: SmsPatternRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AccountsViewModel(accountRepository, smsPatternRepository) as T
        }
    }
}
