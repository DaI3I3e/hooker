package com.example.ui.screens.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.relation.SavingsGoalWithDetails
import com.example.data.repository.AccountRepository
import com.example.data.repository.SavingsGoalRepository
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavingsGoalsUiState(
    val goals: List<SavingsGoalWithDetails> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val totalTargetAmount: Long = 0L,
    val totalCurrentSaved: Long = 0L,
    val overallProgress: Float = 0f,
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val isLoading: Boolean = false,
    val successMessage: String? = null
)

class SavingsGoalsViewModel(
    private val savingsGoalRepository: SavingsGoalRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _successMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SavingsGoalsUiState> = combine(
        savingsGoalRepository.getAll(),
        accountRepository.getAll(),
        _successMessage
    ) { goalsList, accountsList, message ->
        val totalTarget = goalsList.sumOf { it.goal.targetAmount }
        val totalSaved = goalsList.sumOf { it.currentAmount }
        val progress = if (totalTarget > 0) (totalSaved.toFloat() / totalTarget).coerceIn(0f, 1f) else 0f
        val active = goalsList.count { !it.isCompleted }
        val completed = goalsList.count { it.isCompleted }

        SavingsGoalsUiState(
            goals = goalsList,
            accounts = accountsList,
            totalTargetAmount = totalTarget,
            totalCurrentSaved = totalSaved,
            overallProgress = progress,
            activeCount = active,
            completedCount = completed,
            successMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SavingsGoalsUiState(isLoading = true)
    )

    fun deposit(goalId: Long, amount: Long, accountId: Long) {
        viewModelScope.launch {
            savingsGoalRepository.deposit(goalId, amount, accountId, transactionRepository)
            _successMessage.value = "واریز به هدف پس‌انداز با موفقیت انجام و از حساب کسر شد"
        }
    }

    fun withdraw(goalId: Long, amount: Long, accountId: Long) {
        viewModelScope.launch {
            savingsGoalRepository.withdraw(goalId, amount, accountId, transactionRepository)
            _successMessage.value = "برداشت از پس‌انداز با موفقیت به حساب بازگردانده شد"
        }
    }

    fun delete(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            savingsGoalRepository.delete(goal)
            _successMessage.value = "هدف پس‌انداز با موفقیت حذف شد"
        }
    }

    fun clearMessage() {
        _successMessage.value = null
    }

    class Factory(
        private val savingsGoalRepository: SavingsGoalRepository,
        private val accountRepository: AccountRepository,
        private val transactionRepository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SavingsGoalsViewModel(savingsGoalRepository, accountRepository, transactionRepository) as T
        }
    }
}
