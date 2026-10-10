package com.example.ui.screens.group_expenses

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.GroupExpenseShareEntity
import com.example.data.local.relation.GroupExpenseWithShares
import com.example.data.repository.AccountRepository
import com.example.data.repository.GroupExpenseRepository
import com.example.util.AmountFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupExpensesUiState(
    val groupExpenses: List<GroupExpenseWithShares> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val filterOnlyUnsettled: Boolean = true,
    val totalPendingAmount: Long = 0L,
    val totalSettledAmount: Long = 0L,
    val pendingPeopleCount: Int = 0,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    // Dialog state for settling
    val settlingShare: GroupExpenseShareEntity? = null,
    val settlingExpense: GroupExpenseWithShares? = null,
    val selectedIncomeAccountId: Long? = null,
    val createIncomeTransaction: Boolean = true,
    // Dialog state for un-settling
    val unsettlingShare: GroupExpenseShareEntity? = null
)

class GroupExpensesViewModel(
    private val groupExpenseRepository: GroupExpenseRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _filterOnlyUnsettled = MutableStateFlow(true)
    private val _dialogState = MutableStateFlow(DialogState())
    private val _userMessage = MutableStateFlow<String?>(null)

    private data class DialogState(
        val settlingShare: GroupExpenseShareEntity? = null,
        val settlingExpense: GroupExpenseWithShares? = null,
        val selectedIncomeAccountId: Long? = null,
        val createIncomeTransaction: Boolean = true,
        val unsettlingShare: GroupExpenseShareEntity? = null
    )

    val uiState: StateFlow<GroupExpensesUiState> = combine(
        groupExpenseRepository.allGroupExpenses,
        accountRepository.allAccounts,
        _filterOnlyUnsettled,
        _dialogState,
        _userMessage
    ) { expenses, accounts, onlyUnsettled, dialog, msg ->
        val allShares = expenses.flatMap { it.shares }
        val pendingShares = allShares.filter { !it.isSettled }
        val settledShares = allShares.filter { it.isSettled }

        val totalPending = pendingShares.sumOf { it.amount }
        val totalSettled = settledShares.sumOf { it.amount }
        val pendingCount = pendingShares.size

        val filteredExpenses = if (onlyUnsettled) {
            expenses.filter { exp -> exp.shares.any { !it.isSettled } }
        } else {
            expenses
        }

        GroupExpensesUiState(
            groupExpenses = filteredExpenses,
            accounts = accounts,
            filterOnlyUnsettled = onlyUnsettled,
            totalPendingAmount = totalPending,
            totalSettledAmount = totalSettled,
            pendingPeopleCount = pendingCount,
            userMessage = msg,
            settlingShare = dialog.settlingShare,
            settlingExpense = dialog.settlingExpense,
            selectedIncomeAccountId = dialog.selectedIncomeAccountId ?: accounts.firstOrNull()?.id,
            createIncomeTransaction = dialog.createIncomeTransaction,
            unsettlingShare = dialog.unsettlingShare
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GroupExpensesUiState()
    )

    fun setFilterOnlyUnsettled(onlyUnsettled: Boolean) {
        _filterOnlyUnsettled.value = onlyUnsettled
    }

    fun openSettleDialog(share: GroupExpenseShareEntity, expense: GroupExpenseWithShares) {
        _dialogState.update {
            it.copy(
                settlingShare = share,
                settlingExpense = expense,
                createIncomeTransaction = true
            )
        }
    }

    fun closeSettleDialog() {
        _dialogState.update {
            it.copy(
                settlingShare = null,
                settlingExpense = null
            )
        }
    }

    fun setSelectedIncomeAccountId(accountId: Long) {
        _dialogState.update { it.copy(selectedIncomeAccountId = accountId) }
    }

    fun setCreateIncomeTransaction(create: Boolean) {
        _dialogState.update { it.copy(createIncomeTransaction = create) }
    }

    fun confirmSettleShare() {
        val share = _dialogState.value.settlingShare ?: return
        val createTx = _dialogState.value.createIncomeTransaction
        val accountId = if (createTx) _dialogState.value.selectedIncomeAccountId else null

        viewModelScope.launch {
            try {
                groupExpenseRepository.settleShare(
                    shareId = share.id,
                    incomeAccountId = accountId,
                    categoryId = null,
                    customNote = "تسویه دنگ ${share.personName}"
                )
                _dialogState.update { it.copy(settlingShare = null, settlingExpense = null) }
                _userMessage.value = "دنگ ${share.personName} با موفقیت تسویه شد"
            } catch (e: Exception) {
                _userMessage.value = "خطا در تسویه دنگ: ${e.message}"
            }
        }
    }

    fun openUnsettleDialog(share: GroupExpenseShareEntity) {
        _dialogState.update { it.copy(unsettlingShare = share) }
    }

    fun closeUnsettleDialog() {
        _dialogState.update { it.copy(unsettlingShare = null) }
    }

    fun confirmUnsettleShare() {
        val share = _dialogState.value.unsettlingShare ?: return
        viewModelScope.launch {
            try {
                groupExpenseRepository.unsettleShare(share.id)
                _dialogState.update { it.copy(unsettlingShare = null) }
                _userMessage.value = "وضعیت دنگ ${share.personName} به تسویه‌نشده بازگردانده شد"
            } catch (e: Exception) {
                _userMessage.value = "خطا در بازگردانی وضعیت: ${e.message}"
            }
        }
    }

    fun shareSingleDong(context: Context, expense: GroupExpenseWithShares, share: GroupExpenseShareEntity) {
        val txTitle = expense.transaction?.note?.ifBlank { "هزینه مشترک" } ?: "هزینه مشترک"
        val amountStr = AmountFormatter.formatAmountWithCurrency(share.amount)

        val account = uiState.value.accounts.find { it.id == expense.transaction?.accountId }
            ?: uiState.value.accounts.firstOrNull()

        val cardInfo = if (!account?.cardNumber.isNullOrBlank()) {
            "\nشماره کارت جهت واریز:\n${account?.cardNumber}"
        } else ""

        val text = """
            سلام ${share.personName} عزیز،
            دنگ شما از بابت «$txTitle» مبلغ $amountStr می‌باشد.$cardInfo

            با تشکر
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "ارسال دنگ به ${share.personName}")
        context.startActivity(shareIntent)
    }

    fun shareAllDongs(context: Context, expense: GroupExpenseWithShares) {
        val txTitle = expense.transaction?.note?.ifBlank { "هزینه مشترک" } ?: "هزینه مشترک"
        val totalAmountStr = AmountFormatter.formatAmountWithCurrency(expense.transaction?.amount ?: 0L)

        val sharesList = expense.shares.joinToString("\n") {
            val status = if (it.isSettled) " (تسویه شده ✓)" else ""
            "- ${it.personName}: ${AmountFormatter.formatAmountWithCurrency(it.amount)}$status"
        }

        val text = """
            📋 صورت‌حساب هزینه مشترک: «$txTitle»
            مبلغ کل: $totalAmountStr

            سهم افراد:
            $sharesList
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "اشتراک‌گذاری صورت‌حساب دنگ‌ها")
        context.startActivity(shareIntent)
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    class Factory(
        private val groupExpenseRepository: GroupExpenseRepository,
        private val accountRepository: AccountRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GroupExpensesViewModel(groupExpenseRepository, accountRepository) as T
        }
    }
}
