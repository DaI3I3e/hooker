package com.example.ui.screens.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.relation.RecurringWithDetails
import com.example.data.repository.RecurringRepository
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RecurringFilterTab(val title: String) {
    ALL("همه"),
    INSTALLMENTS("اقساط وام"),
    RECURRING("دوره‌ای"),
    DUE("سررسید شده")
}

data class RecurringUiState(
    val items: List<RecurringWithDetails> = emptyList(),
    val filteredItems: List<RecurringWithDetails> = emptyList(),
    val activeTab: RecurringFilterTab = RecurringFilterTab.ALL,
    val totalActiveCount: Int = 0,
    val dueCount: Int = 0,
    val totalMonthlyCommitment: Long = 0L,
    val isLoading: Boolean = false,
    val successMessage: String? = null
)

class RecurringTransactionsViewModel(
    private val recurringRepository: RecurringRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _activeTab = MutableStateFlow(RecurringFilterTab.ALL)
    private val _successMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RecurringUiState> = combine(
        recurringRepository.getAll(),
        _activeTab,
        _successMessage
    ) { allItems, tab, message ->
        val now = System.currentTimeMillis()
        val activeItems = allItems.filter { it.recurring.isActive }
        val dueItems = activeItems.filter { it.recurring.nextDueDate <= now }

        val monthlyCommitment = activeItems
            .filter { it.recurring.type == com.example.data.local.entity.TransactionType.EXPENSE }
            .sumOf { item ->
                when (item.recurring.period) {
                    com.example.data.local.entity.RecurrencePeriod.DAILY -> item.recurring.amount * 30
                    com.example.data.local.entity.RecurrencePeriod.WEEKLY -> item.recurring.amount * 4
                    com.example.data.local.entity.RecurrencePeriod.MONTHLY -> item.recurring.amount
                    com.example.data.local.entity.RecurrencePeriod.YEARLY -> item.recurring.amount / 12
                }
            }

        val filtered = when (tab) {
            RecurringFilterTab.ALL -> allItems
            RecurringFilterTab.INSTALLMENTS -> allItems.filter { it.recurring.isInstallment }
            RecurringFilterTab.RECURRING -> allItems.filter { !it.recurring.isInstallment }
            RecurringFilterTab.DUE -> dueItems
        }

        RecurringUiState(
            items = allItems,
            filteredItems = filtered,
            activeTab = tab,
            totalActiveCount = activeItems.size,
            dueCount = dueItems.size,
            totalMonthlyCommitment = monthlyCommitment,
            successMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringUiState(isLoading = true)
    )

    fun selectTab(tab: RecurringFilterTab) {
        _activeTab.value = tab
    }

    fun executeNow(item: RecurringWithDetails) {
        viewModelScope.launch {
            val title = item.recurring.title
            recurringRepository.executeRecurring(item.recurring, transactionRepository)
            _successMessage.value = if (item.recurring.isInstallment) {
                "قسط «$title» با موفقیت در تراکنش‌ها ثبت شد"
            } else {
                "تراکنش دوره‌ای «$title» با موفقیت ثبت شد"
            }
        }
    }

    fun toggleActive(item: RecurringWithDetails) {
        viewModelScope.launch {
            val updated = item.recurring.copy(isActive = !item.recurring.isActive)
            recurringRepository.update(updated)
        }
    }

    fun delete(item: RecurringWithDetails) {
        viewModelScope.launch {
            recurringRepository.delete(item.recurring)
            _successMessage.value = "مورد انتخابی با موفقیت حذف شد"
        }
    }

    fun clearMessage() {
        _successMessage.value = null
    }

    class Factory(
        private val recurringRepository: RecurringRepository,
        private val transactionRepository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecurringTransactionsViewModel(recurringRepository, transactionRepository) as T
        }
    }
}
