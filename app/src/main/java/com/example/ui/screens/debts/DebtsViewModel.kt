package com.example.ui.screens.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtType
import com.example.data.repository.DebtRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DebtsUiState(
    val selectedTab: DebtType = DebtType.OWED_TO_ME,
    val debts: List<DebtEntity> = emptyList(),
    val totalOwedToMe: Long = 0L,
    val totalIOwe: Long = 0L,
    val showAddDialog: Boolean = false,
    val personName: String = "",
    val amount: Long = 0L,
    val note: String = "",
    val dueDate: Long? = null,
    val dialogType: DebtType = DebtType.OWED_TO_ME,
    val isLoading: Boolean = false
)

class DebtsViewModel(
    private val debtRepository: DebtRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(DebtType.OWED_TO_ME)
    private val _showAddDialog = MutableStateFlow(false)
    private val _personName = MutableStateFlow("")
    private val _amount = MutableStateFlow(0L)
    private val _note = MutableStateFlow("")
    private val _dueDate = MutableStateFlow<Long?>(null)
    private val _dialogType = MutableStateFlow(DebtType.OWED_TO_ME)

    private val _dialogState = combine(
        _selectedTab,
        _showAddDialog,
        _personName,
        _amount,
        _note
    ) { tab, showDialog, name, amt, nt ->
        Triple(tab, showDialog, Triple(name, amt, nt))
    }

    private val _dialogState2 = combine(
        _dueDate,
        _dialogType
    ) { date, dType ->
        Pair(date, dType)
    }

    val uiState: StateFlow<DebtsUiState> = combine(
        _dialogState,
        _dialogState2,
        debtRepository.allDebts,
        debtRepository.totalOwedToMe,
        debtRepository.totalIOwe
    ) { (tab, showDialog, nameAmtNt), (date, dType), allDebts, totalOwed, totalOwe ->
        val (name, amt, nt) = nameAmtNt
        val filtered = allDebts.filter { it.type == tab }
        DebtsUiState(
            selectedTab = tab,
            debts = filtered,
            totalOwedToMe = totalOwed,
            totalIOwe = totalOwe,
            showAddDialog = showDialog,
            personName = name,
            amount = amt,
            note = nt,
            dueDate = date,
            dialogType = dType,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtsUiState(isLoading = true)
    )

    fun setSelectedTab(tab: DebtType) {
        _selectedTab.value = tab
    }

    fun openAddDialog(defaultType: DebtType = _selectedTab.value) {
        _dialogType.value = defaultType
        _personName.value = ""
        _amount.value = 0L
        _note.value = ""
        _dueDate.value = null
        _showAddDialog.value = true
    }

    fun closeAddDialog() {
        _showAddDialog.value = false
    }

    fun setDialogType(type: DebtType) {
        _dialogType.value = type
    }

    fun setPersonName(name: String) {
        _personName.value = name
    }

    fun setAmount(amount: Long) {
        _amount.value = amount
    }

    fun setNote(note: String) {
        _note.value = note
    }

    fun setDueDate(timestamp: Long?) {
        _dueDate.value = timestamp
    }

    fun saveDebt() {
        val name = _personName.value.trim()
        val amt = _amount.value
        if (name.isBlank() || amt <= 0L) return

        viewModelScope.launch {
            val debt = DebtEntity(
                personName = name,
                type = _dialogType.value,
                amount = amt,
                dueDate = _dueDate.value,
                note = _note.value.ifBlank { null }
            )
            debtRepository.insertDebt(debt)
            _showAddDialog.value = false
        }
    }

    fun toggleSettled(debt: DebtEntity) {
        viewModelScope.launch {
            debtRepository.markAsSettled(debt, !debt.isSettled)
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            debtRepository.deleteDebt(debt)
        }
    }

    class Factory(private val debtRepository: DebtRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DebtsViewModel(debtRepository) as T
        }
    }
}
