package com.example.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.repository.AccountRepository
import com.example.util.BankLogos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class AddAccountUiState(
    val accountId: Long? = null,
    val name: String = "",
    val type: AccountType = AccountType.BANK,
    val initialBalance: Long = 0L,
    val color: Int = 0xFF1B5E20.toInt(),
    val icon: String = "account_balance",
    val cardNumber: String = "",
    val shabaNumber: String = "",
    val cardExpiry: String = "",
    val logoResName: String? = null,
    val logoImage: String? = null,
    val isNoLogo: Boolean = false,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class AddAccountViewModel(
    private val editAccountId: Long?,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAccountUiState(accountId = editAccountId))
    val uiState: StateFlow<AddAccountUiState> = _uiState.asStateFlow()

    init {
        if (editAccountId != null && editAccountId > 0) {
            loadAccount(editAccountId)
        }
    }

    private fun loadAccount(id: Long) {
        viewModelScope.launch {
            val account = accountRepository.getAccountById(id).firstOrNull()
            account?.let { acc ->
                val hasAnyLogo = acc.logoResName != null || acc.logoImage != null
                _uiState.value = _uiState.value.copy(
                    accountId = acc.id,
                    name = acc.name,
                    type = acc.type,
                    initialBalance = acc.initialBalance,
                    color = acc.color,
                    icon = acc.icon,
                    cardNumber = acc.cardNumber ?: "",
                    shabaNumber = acc.shabaNumber ?: "",
                    cardExpiry = acc.cardExpiry ?: "",
                    logoResName = acc.logoResName,
                    logoImage = acc.logoImage,
                    isNoLogo = !hasAnyLogo && acc.logoResName == null,
                    isEditing = true
                )
            }
        }
    }

    fun setName(name: String) {
        val detected = if (_uiState.value.logoResName == null && _uiState.value.logoImage == null && !_uiState.value.isNoLogo) {
            BankLogos.detectBank(_uiState.value.cardNumber, name)
        } else null
        _uiState.value = _uiState.value.copy(
            name = name,
            errorMessage = null,
            logoResName = detected?.id ?: _uiState.value.logoResName
        )
    }

    fun setType(type: AccountType) {
        val defaultIcon = when (type) {
            AccountType.CASH -> "payments"
            AccountType.BANK -> "account_balance"
            AccountType.CREDIT_CARD -> "credit_card"
        }
        _uiState.value = _uiState.value.copy(type = type, icon = defaultIcon)
    }

    fun setInitialBalance(balance: Long) {
        _uiState.value = _uiState.value.copy(initialBalance = balance)
    }

    fun setColor(color: Int) {
        _uiState.value = _uiState.value.copy(color = color)
    }

    fun setIcon(icon: String) {
        _uiState.value = _uiState.value.copy(icon = icon)
    }

    fun setLogoResName(logoResName: String?) {
        _uiState.value = _uiState.value.copy(
            logoResName = logoResName,
            logoImage = null,
            isNoLogo = false
        )
    }

    fun setLogoImage(base64: String?) {
        _uiState.value = _uiState.value.copy(
            logoImage = base64,
            logoResName = null,
            isNoLogo = false
        )
    }

    fun setNoLogo(noLogo: Boolean) {
        _uiState.value = _uiState.value.copy(
            isNoLogo = noLogo,
            logoResName = if (noLogo) null else _uiState.value.logoResName,
            logoImage = if (noLogo) null else _uiState.value.logoImage
        )
    }

    fun setCardNumber(cardNumber: String) {
        val detected = if (_uiState.value.logoResName == null && _uiState.value.logoImage == null && !_uiState.value.isNoLogo) {
            BankLogos.detectBank(cardNumber, _uiState.value.name)
        } else null
        _uiState.value = _uiState.value.copy(
            cardNumber = cardNumber,
            logoResName = detected?.id ?: _uiState.value.logoResName
        )
    }

    fun setShabaNumber(shabaNumber: String) {
        _uiState.value = _uiState.value.copy(shabaNumber = shabaNumber)
    }

    fun setCardExpiry(cardExpiry: String) {
        _uiState.value = _uiState.value.copy(cardExpiry = cardExpiry)
    }

    fun saveAccount() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "نام حساب نمی‌تواند خالی باشد")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            val now = System.currentTimeMillis()
            val finalLogo = if (state.isNoLogo) {
                null
            } else if (state.logoImage != null) {
                null
            } else {
                state.logoResName ?: BankLogos.detectBank(state.cardNumber, state.name)?.id
            }

            val finalImage = if (state.isNoLogo) null else state.logoImage

            val account = AccountEntity(
                id = state.accountId ?: 0L,
                name = state.name.trim(),
                type = state.type,
                initialBalance = state.initialBalance,
                color = state.color,
                icon = state.icon,
                cardNumber = state.cardNumber.ifBlank { null },
                shabaNumber = state.shabaNumber.ifBlank { null },
                cardExpiry = state.cardExpiry.ifBlank { null },
                logoResName = finalLogo,
                logoImage = finalImage,
                createdAt = now,
                updatedAt = now
            )

            if (state.isEditing && state.accountId != null) {
                accountRepository.updateAccount(account)
            } else {
                accountRepository.insertAccount(account)
            }

            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
        }
    }

    class Factory(
        private val editAccountId: Long?,
        private val accountRepository: AccountRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddAccountViewModel(editAccountId, accountRepository) as T
        }
    }
}
