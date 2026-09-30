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
    val isExecuting: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class RecurringTransactionsViewModel(
    private val recurringRepository: RecurringRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _activeTab = MutableStateFlow(RecurringFilterTab.ALL)
    private val _successMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isExecuting = MutableStateFlow(false)

    val uiState: StateFlow<RecurringUiState> = combine(
        recurringRepository.getAll(),
        _activeTab,
        _successMessage,
        _errorMessage,
        _isExecuting
    ) { allItems, tab, message, errorMsg, executing ->
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
            isExecuting = executing,
            successMessage = message,
            errorMessage = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringUiState(isLoading = true)
    )

    fun selectTab(tab: RecurringFilterTab) {
        _activeTab.value = tab
    }

    fun executeNow(
        item: RecurringWithDetails,
        onSuccess: () -> Unit = {},
        onError: () -> Unit = {}
    ) {
        if (_isExecuting.value) return
        _isExecuting.value = true
        viewModelScope.launch {
            try {
                val title = item.recurring.title
                val result = recurringRepository.executeRecurring(item.recurring, transactionRepository)
                if (result > 0) {
                    _successMessage.value = if (item.recurring.isInstallment) {
                        "قسط «$title» با موفقیت در تراکنش‌ها ثبت شد"
                    } else {
                        "تراکنش دوره‌ای «$title» با موفقیت ثبت شد"
                    }
                    onSuccess()
                } else if (result == -1L) {
                    _errorMessage.value = "این مورد قبلاً برای سررسید جاری ثبت شده است"
                    onSuccess()
                } else {
                    _errorMessage.value = "خطا در ثبت تراکنش"
                    onError()
                }
            } catch (e: Exception) {
                android.util.Log.e("RecurringVM", "Failed to execute recurring item ${item.recurring.id}: ${item.recurring.title}", e)
                _errorMessage.value = "خطا در ثبت تراکنش"
                onError()
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun toggleActive(item: RecurringWithDetails) {
        viewModelScope.launch {
            try {
                val updated = item.recurring.copy(isActive = !item.recurring.isActive)
                recurringRepository.update(updated)
            } catch (e: Exception) {
                android.util.Log.e("RecurringVM", "Failed to toggle active for ${item.recurring.id}", e)
            }
        }
    }

    fun delete(item: RecurringWithDetails) {
        viewModelScope.launch {
            try {
                recurringRepository.delete(item.recurring)
                _successMessage.value = "مورد انتخابی با موفقیت حذف شد"
            } catch (e: Exception) {
                android.util.Log.e("RecurringVM", "Failed to delete item ${item.recurring.id}", e)
                _errorMessage.value = "خطا در حذف مورد"
            }
        }
    }

    fun clearMessage() {
        _successMessage.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
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
