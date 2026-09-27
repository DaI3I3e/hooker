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
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChequeFilterTab(val title: String) {
    ALL("همه"),
    RECEIVABLE("دریافتی"),
    PAYABLE("پرداختی"),
    PENDING("در انتظار"),
    CLEARED("پاس شده")
}

data class ChequesUiState(
    val cheques: List<ChequeEntity> = emptyList(),
    val filteredCheques: List<ChequeEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val activeTab: ChequeFilterTab = ChequeFilterTab.ALL,
    val pendingPayableTotal: Long = 0L,
    val pendingReceivableTotal: Long = 0L,
    val upcomingDueCount: Int = 0,
    val isLoading: Boolean = false,
    val successMessage: String? = null
)

class ChequesViewModel(
    private val chequeRepository: ChequeRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _activeTab = MutableStateFlow(ChequeFilterTab.ALL)
    private val _successMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ChequesUiState> = combine(
        chequeRepository.getAll(),
        accountRepository.getAll(),
        _activeTab,
        _successMessage
    ) { allCheques, accountsList, tab, message ->
        val now = System.currentTimeMillis()
        val pending = allCheques.filter { it.status == ChequeStatus.PENDING }
        val pendingPayable = pending.filter { it.type == ChequeType.PAYABLE }.sumOf { it.amount }
        val pendingReceivable = pending.filter { it.type == ChequeType.RECEIVABLE }.sumOf { it.amount }
        val upcomingDue = pending.count { it.dueDate <= now + (3 * 24 * 60 * 60 * 1000L) }

        val filtered = when (tab) {
            ChequeFilterTab.ALL -> allCheques
            ChequeFilterTab.RECEIVABLE -> allCheques.filter { it.type == ChequeType.RECEIVABLE }
            ChequeFilterTab.PAYABLE -> allCheques.filter { it.type == ChequeType.PAYABLE }
            ChequeFilterTab.PENDING -> pending
            ChequeFilterTab.CLEARED -> allCheques.filter { it.status == ChequeStatus.CLEARED }
        }

        ChequesUiState(
            cheques = allCheques,
            filteredCheques = filtered,
            accounts = accountsList,
            activeTab = tab,
            pendingPayableTotal = pendingPayable,
            pendingReceivableTotal = pendingReceivable,
            upcomingDueCount = upcomingDue,
            successMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChequesUiState(isLoading = true)
    )

    fun selectTab(tab: ChequeFilterTab) {
        _activeTab.value = tab
    }

    fun clearChequeWithAccount(cheque: ChequeEntity, accountId: Long) {
        viewModelScope.launch {
            chequeRepository.clearChequeAndRegisterTransaction(cheque.id, accountId, transactionRepository)
            val actionName = if (cheque.type == ChequeType.PAYABLE) "پاس شدن چک پرداختی" else "وصول چک دریافتی"
            _successMessage.value = "$actionName با موفقیت در تراکنش‌های حساب ثبت شد"
        }
    }

    fun updateStatus(cheque: ChequeEntity, status: ChequeStatus) {
        viewModelScope.launch {
            chequeRepository.updateStatus(cheque.id, status)
            _successMessage.value = "وضعیت چک به «${status.title}» تغییر یافت"
        }
    }

    fun delete(cheque: ChequeEntity) {
        viewModelScope.launch {
            chequeRepository.delete(cheque)
            _successMessage.value = "چک با موفقیت حذف شد"
        }
    }

    fun clearMessage() {
        _successMessage.value = null
    }

    class Factory(
        private val chequeRepository: ChequeRepository,
        private val accountRepository: AccountRepository,
        private val transactionRepository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChequesViewModel(chequeRepository, accountRepository, transactionRepository) as T
        }
    }
}
