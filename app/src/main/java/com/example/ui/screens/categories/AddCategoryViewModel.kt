package com.example.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryType
import com.example.data.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class AddCategoryUiState(
    val categoryId: Long? = null,
    val name: String = "",
    val type: CategoryType = CategoryType.EXPENSE,
    val color: Int = 0xFFE53935.toInt(),
    val icon: String = "more_horiz",
    val isDefault: Boolean = false,
    val monthlyBudget: Long = 0L,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class AddCategoryViewModel(
    private val editCategoryId: Long?,
    initialTypeString: String?,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddCategoryUiState(
            categoryId = editCategoryId,
            type = if (initialTypeString == "INCOME") CategoryType.INCOME else CategoryType.EXPENSE,
            color = if (initialTypeString == "INCOME") 0xFF43A047.toInt() else 0xFFE53935.toInt()
        )
    )
    val uiState: StateFlow<AddCategoryUiState> = _uiState.asStateFlow()

    init {
        if (editCategoryId != null && editCategoryId > 0) {
            loadCategory(editCategoryId)
        }
    }

    private fun loadCategory(id: Long) {
        viewModelScope.launch {
            val category = categoryRepository.getCategoryById(id).firstOrNull()
            category?.let { cat ->
                _uiState.value = _uiState.value.copy(
                    categoryId = cat.id,
                    name = cat.name,
                    type = cat.type,
                    color = cat.color,
                    icon = cat.icon,
                    isDefault = cat.isDefault,
                    monthlyBudget = cat.monthlyBudget,
                    isEditing = true
                )
            }
        }
    }

    fun setName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, errorMessage = null)
    }

    fun setType(type: CategoryType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun setColor(color: Int) {
        _uiState.value = _uiState.value.copy(color = color)
    }

    fun setIcon(icon: String) {
        _uiState.value = _uiState.value.copy(icon = icon)
    }

    fun setMonthlyBudget(budget: Long) {
        _uiState.value = _uiState.value.copy(monthlyBudget = budget)
    }

    fun saveCategory() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "نام دسته‌بندی نمی‌تواند خالی باشد")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            val category = CategoryEntity(
                id = state.categoryId ?: 0L,
                name = state.name.trim(),
                type = state.type,
                color = state.color,
                icon = state.icon,
                isDefault = state.isDefault,
                monthlyBudget = if (state.type != CategoryType.INCOME) state.monthlyBudget else 0L,
                createdAt = System.currentTimeMillis()
            )

            if (state.isEditing && state.categoryId != null) {
                categoryRepository.updateCategory(category)
            } else {
                categoryRepository.insertCategory(category)
            }

            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
        }
    }

    class Factory(
        private val editCategoryId: Long?,
        private val initialTypeString: String?,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddCategoryViewModel(editCategoryId, initialTypeString, categoryRepository) as T
        }
    }
}
