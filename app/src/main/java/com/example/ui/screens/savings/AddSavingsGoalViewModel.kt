package com.example.ui.screens.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddSavingsGoalUiState(
    val title: String = "",
    val targetAmount: Long = 0L,
    val initialAmount: Long = 0L,
    val targetDate: Long? = null,
    val selectedColor: Int = -16738680, // #00897B
    val note: String = "",
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class AddSavingsGoalViewModel(
    private val savingsGoalRepository: SavingsGoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddSavingsGoalUiState())
    val uiState: StateFlow<AddSavingsGoalUiState> = _uiState.asStateFlow()

    fun setTitle(title: String) = _uiState.update { it.copy(title = title, errorMessage = null) }
    fun setTargetAmount(amount: Long) = _uiState.update { it.copy(targetAmount = amount, errorMessage = null) }
    fun setInitialAmount(amount: Long) = _uiState.update { it.copy(initialAmount = amount) }
    fun setTargetDate(date: Long?) = _uiState.update { it.copy(targetDate = date) }
    fun setColor(color: Int) = _uiState.update { it.copy(selectedColor = color) }
    fun setNote(note: String) = _uiState.update { it.copy(note = note) }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "لطفاً عنوان هدف را وارد کنید") }
            return
        }
        if (state.targetAmount <= 0) {
            _uiState.update { it.copy(errorMessage = "مبلغ هدف باید بیشتر از صفر باشد") }
            return
        }

        viewModelScope.launch {
            val goal = SavingsGoalEntity(
                title = state.title.trim(),
                targetAmount = state.targetAmount,
                currentAmount = state.initialAmount,
                targetDate = state.targetDate,
                color = state.selectedColor,
                icon = "Savings",
                isCompleted = state.initialAmount >= state.targetAmount,
                note = state.note.ifBlank { null }
            )
            savingsGoalRepository.insert(goal)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    class Factory(
        private val savingsGoalRepository: SavingsGoalRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddSavingsGoalViewModel(savingsGoalRepository) as T
        }
    }
}
