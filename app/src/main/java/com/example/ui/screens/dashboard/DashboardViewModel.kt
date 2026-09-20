package com.example.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.DashboardSummary
import com.example.domain.usecase.GetDashboardDataUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    getDashboardDataUseCase: GetDashboardDataUseCase
) : ViewModel() {

    val uiState: StateFlow<DashboardSummary> = getDashboardDataUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardSummary(
                totalBalance = 0L,
                todayIncome = 0L,
                todayExpense = 0L,
                recentTransactions = emptyList(),
                accounts = emptyList()
            )
        )

    class Factory(
        private val getDashboardDataUseCase: GetDashboardDataUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(getDashboardDataUseCase) as T
        }
    }
}
