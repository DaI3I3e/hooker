package com.example.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.repository.CategoryRepository
import com.example.domain.model.DashboardSummary
import com.example.domain.usecase.GetDashboardDataUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    getDashboardDataUseCase: GetDashboardDataUseCase,
    categoryRepository: CategoryRepository? = null
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = (categoryRepository?.allCategories
        ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
        private val getDashboardDataUseCase: GetDashboardDataUseCase,
        private val categoryRepository: CategoryRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(getDashboardDataUseCase, categoryRepository) as T
        }
    }
}
