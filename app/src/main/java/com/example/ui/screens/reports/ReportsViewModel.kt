package com.example.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.TransactionType
import com.example.data.repository.TransactionRepository
import com.example.domain.model.CategorySummary
import com.example.util.JalaliDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

enum class ReportPeriod(val label: String) {
    TODAY("امروز"),
    WEEK("این هفته"),
    MONTH("این ماه"),
    ALL("همه زمان‌ها"),
    CUSTOM("بازه دلخواه")
}

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.MONTH,
    val selectedAccountId: Long? = null,
    val selectedReportType: TransactionType = TransactionType.EXPENSE,
    val isFullscreen: Boolean = false,
    val accounts: List<com.example.data.local.entity.AccountEntity> = emptyList(),
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netBalance: Long = 0L,
    val categorySummaries: List<CategorySummary> = emptyList(),
    val isLoading: Boolean = false
)

class ReportsViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: com.example.data.repository.AccountRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.MONTH)
    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    private val _selectedReportType = MutableStateFlow(TransactionType.EXPENSE)
    private val _customStartTimestamp = MutableStateFlow<Long?>(null)
    private val _customEndTimestamp = MutableStateFlow<Long?>(null)
    private val _isFullscreen = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ReportsUiState> = combine(
        _selectedPeriod,
        _selectedAccountId,
        _selectedReportType,
        _customStartTimestamp,
        _customEndTimestamp,
        _isFullscreen,
        accountRepository.allAccounts,
        transactionRepository.allTransactionsWithDetails
    ) { flows: Array<Any?> ->
        val period = flows[0] as ReportPeriod
        val accountId = flows[1] as Long?
        val reportType = flows[2] as TransactionType
        val customStart = flows[3] as Long?
        val customEnd = flows[4] as Long?
        val isFullscreen = flows[5] as Boolean
        @Suppress("UNCHECKED_CAST")
        val accountsList = flows[6] as List<com.example.data.local.entity.AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val allTxWithDetails = flows[7] as List<com.example.data.local.relation.TransactionWithDetails>

        val zoneId = ZoneId.systemDefault()
        val todayJalali = JalaliDate.today(zoneId)
        val (start, end) = when (period) {
            ReportPeriod.TODAY -> Pair(
                todayJalali.toStartOfDayTimestamp(zoneId),
                todayJalali.toEndOfDayTimestamp(zoneId)
            )
            ReportPeriod.WEEK -> Pair(
                todayJalali.toStartOfDayTimestamp(zoneId) - (6 * 24 * 60 * 60 * 1000L),
                todayJalali.toEndOfDayTimestamp(zoneId)
            )
            ReportPeriod.MONTH -> Pair(
                JalaliDate.getStartOfCurrentMonth(zoneId),
                JalaliDate.getEndOfCurrentMonth(zoneId)
            )
            ReportPeriod.ALL -> Pair(
                0L,
                Long.MAX_VALUE
            )
            ReportPeriod.CUSTOM -> Pair(
                customStart ?: 0L,
                customEnd ?: Long.MAX_VALUE
            )
        }

        val inRange = allTxWithDetails.filter { item ->
            val d = item.transaction.date
            val inDateRange = d in start..end
            val inAccount = (accountId == null || item.transaction.accountId == accountId || item.transaction.toAccountId == accountId)
            inDateRange && inAccount
        }

        var incomeSum = 0L
        var expenseSum = 0L
        val categoryAmountMap = mutableMapOf<Long, Long>()
        val categoryColorMap = mutableMapOf<Long, Int>()
        val categoryIconMap = mutableMapOf<Long, String>()
        val categoryNameMap = mutableMapOf<Long, String>()
        val categoryCountMap = mutableMapOf<Long, Int>()

        inRange.forEach { item ->
            val tx = item.transaction
            if (tx.type == TransactionType.INCOME) {
                incomeSum += tx.amount
            } else if (tx.type == TransactionType.EXPENSE) {
                expenseSum += tx.amount
            }

            if (tx.type == reportType) {
                tx.categoryId?.let { catId ->
                    categoryAmountMap[catId] = (categoryAmountMap[catId] ?: 0L) + tx.amount
                    categoryCountMap[catId] = (categoryCountMap[catId] ?: 0) + 1
                    if (!categoryNameMap.containsKey(catId)) {
                        categoryNameMap[catId] = item.categoryName ?: "سایر"
                        categoryColorMap[catId] = item.categoryColor ?: 0xFF757575.toInt()
                        categoryIconMap[catId] = item.categoryIcon ?: "more_horiz"
                    }
                }
            }
        }

        val totalForType = if (reportType == TransactionType.EXPENSE) expenseSum else incomeSum

        val summaries = categoryAmountMap.map { (catId, totalAmt) ->
            val percentage = if (totalForType > 0) (totalAmt.toDouble() / totalForType * 100).toFloat() else 0f
            CategorySummary(
                category = com.example.data.local.entity.CategoryEntity(
                    id = catId,
                    name = categoryNameMap[catId] ?: "سایر",
                    type = if (reportType == TransactionType.EXPENSE) com.example.data.local.entity.CategoryType.EXPENSE else com.example.data.local.entity.CategoryType.INCOME,
                    color = categoryColorMap[catId] ?: 0xFF757575.toInt(),
                    icon = categoryIconMap[catId] ?: "more_horiz"
                ),
                totalAmount = totalAmt,
                transactionCount = categoryCountMap[catId] ?: 0,
                percentage = percentage
            )
        }.sortedByDescending { it.totalAmount }

        ReportsUiState(
            selectedPeriod = period,
            selectedAccountId = accountId,
            selectedReportType = reportType,
            isFullscreen = isFullscreen,
            accounts = accountsList,
            totalIncome = incomeSum,
            totalExpense = expenseSum,
            netBalance = incomeSum - expenseSum,
            categorySummaries = summaries,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState(isLoading = true)
    )

    fun setPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customStartTimestamp.value = start
        _customEndTimestamp.value = end
        _selectedPeriod.value = ReportPeriod.CUSTOM
    }

    fun setAccountFilter(accountId: Long?) {
        _selectedAccountId.value = accountId
    }

    fun setReportType(type: TransactionType) {
        _selectedReportType.value = type
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val accountRepository: com.example.data.repository.AccountRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportsViewModel(transactionRepository, accountRepository) as T
        }
    }
}
