package com.example.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryType
import com.example.data.local.entity.TransactionType
import com.example.data.local.relation.TransactionWithDetails
import com.example.data.repository.AccountRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.TransactionRepository
import com.example.domain.model.CategorySummary
import com.example.ui.screens.transactions.DateFilterPeriod
import com.example.util.JalaliDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

data class MonthlyTrend(
    val monthName: String,
    val year: Int,
    val month: Int,
    val income: Long,
    val expense: Long
)

data class CategoryBudgetProgress(
    val category: CategoryEntity,
    val spentThisMonth: Long,
    val budget: Long,
    val percentage: Float,
    val remaining: Long,
    val isOverBudget: Boolean
)

data class FinancialInsight(
    val currentMonthExpense: Long,
    val lastMonthExpense: Long,
    val expenseDiffPercent: Double?,
    val isExpenseIncreased: Boolean,
    val dailyAverageExpenseThisMonth: Long,
    val topSpendingCategoryName: String?,
    val topSpendingCategoryPercent: Float
)

data class ReportsUiState(
    val selectedPeriod: DateFilterPeriod = DateFilterPeriod.ALL,
    val customStartTimestamp: Long? = null,
    val customEndTimestamp: Long? = null,
    val selectedAccountId: Long? = null,
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedType: TransactionType? = null,
    val isFullscreen: Boolean = false,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netBalance: Long = 0L,
    val categorySummaries: List<CategorySummary> = emptyList(),
    val monthlyTrends: List<MonthlyTrend> = emptyList(),
    val budgetProgressList: List<CategoryBudgetProgress> = emptyList(),
    val financialInsight: FinancialInsight? = null,
    val filteredTransactions: List<TransactionWithDetails> = emptyList(),
    val currentStartTimestamp: Long = 0L,
    val currentEndTimestamp: Long = Long.MAX_VALUE,
    val isLoading: Boolean = false
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (selectedPeriod != DateFilterPeriod.ALL) count++
            if (selectedAccountId != null) count++
            if (selectedCategoryIds.isNotEmpty()) count++
            if (selectedType != null) count++
            return count
        }
}

class ReportsViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(DateFilterPeriod.ALL)
    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    private val _selectedCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    private val _customStartTimestamp = MutableStateFlow<Long?>(null)
    private val _customEndTimestamp = MutableStateFlow<Long?>(null)
    private val _isFullscreen = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ReportsUiState> = combine(
        _selectedPeriod,
        _selectedAccountId,
        _selectedCategoryIds,
        _selectedType,
        _customStartTimestamp,
        _customEndTimestamp,
        _isFullscreen,
        accountRepository.allAccounts,
        categoryRepository.allCategories,
        transactionRepository.allTransactionsWithDetails
    ) { flows: Array<Any?> ->
        val period = flows[0] as DateFilterPeriod
        val accountId = flows[1] as Long?
        @Suppress("UNCHECKED_CAST")
        val categoryIds = flows[2] as Set<Long>
        @Suppress("UNCHECKED_CAST")
        val filterType = flows[3] as TransactionType?
        val customStart = flows[4] as Long?
        val customEnd = flows[5] as Long?
        val isFullscreen = flows[6] as Boolean
        @Suppress("UNCHECKED_CAST")
        val accountsList = flows[7] as List<AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val categoriesList = flows[8] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val allTxWithDetails = flows[9] as List<TransactionWithDetails>

        val zoneId = ZoneId.systemDefault()
        val todayJalali = JalaliDate.today(zoneId)
        val (start, end) = when (period) {
            DateFilterPeriod.TODAY -> Pair(
                todayJalali.toStartOfDayTimestamp(zoneId),
                todayJalali.toEndOfDayTimestamp(zoneId)
            )
            DateFilterPeriod.WEEK -> Pair(
                todayJalali.toStartOfDayTimestamp(zoneId) - (6 * 24 * 60 * 60 * 1000L),
                todayJalali.toEndOfDayTimestamp(zoneId)
            )
            DateFilterPeriod.MONTH -> Pair(
                JalaliDate.getStartOfCurrentMonth(zoneId),
                JalaliDate.getEndOfCurrentMonth(zoneId)
            )
            DateFilterPeriod.ALL -> Pair(
                0L,
                Long.MAX_VALUE
            )
            DateFilterPeriod.CUSTOM -> Pair(
                customStart ?: 0L,
                customEnd ?: Long.MAX_VALUE
            )
        }

        val inRange = allTxWithDetails.filter { item ->
            val d = item.transaction.date
            val inDateRange = d in start..end
            val inAccount = (accountId == null || item.transaction.accountId == accountId || item.transaction.toAccountId == accountId)
            val inCategory = (categoryIds.isEmpty() || (item.transaction.categoryId != null && categoryIds.contains(item.transaction.categoryId)))
            val inType = (filterType == null || item.transaction.type == filterType)
            inDateRange && inAccount && inCategory && inType
        }

        var incomeSum = 0L
        var expenseSum = 0L
        val categoryAmountMap = mutableMapOf<Long, Long>()
        val categoryColorMap = mutableMapOf<Long, Int>()
        val categoryIconMap = mutableMapOf<Long, String>()
        val categoryNameMap = mutableMapOf<Long, String>()
        val categoryCountMap = mutableMapOf<Long, Int>()

        // Decide which transaction type to break down in the chart:
        // If filterType is INCOME, break down income; otherwise break down expense
        val chartType = if (filterType == TransactionType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE

        inRange.forEach { item ->
            val tx = item.transaction
            if (tx.type == TransactionType.INCOME) {
                incomeSum += tx.amount
            } else if (tx.type == TransactionType.EXPENSE) {
                expenseSum += tx.amount
            }

            if (tx.type == chartType) {
                val catId = tx.categoryId ?: 0L
                categoryAmountMap[catId] = (categoryAmountMap[catId] ?: 0L) + tx.amount
                categoryCountMap[catId] = (categoryCountMap[catId] ?: 0) + 1
                if (!categoryNameMap.containsKey(catId)) {
                    categoryNameMap[catId] = item.categoryName ?: "سایر"
                    categoryColorMap[catId] = item.categoryColor ?: 0xFF757575.toInt()
                    categoryIconMap[catId] = item.categoryIcon ?: "more_horiz"
                }
            }
        }

        val totalForType = if (chartType == TransactionType.EXPENSE) expenseSum else incomeSum

        val summaries = categoryAmountMap.map { (catId, totalAmt) ->
            val percentage = if (totalForType > 0) (totalAmt.toDouble() / totalForType * 100).toFloat() else 0f
            CategorySummary(
                category = CategoryEntity(
                    id = catId,
                    name = categoryNameMap[catId] ?: "سایر",
                    type = if (chartType == TransactionType.EXPENSE) CategoryType.EXPENSE else CategoryType.INCOME,
                    color = categoryColorMap[catId] ?: 0xFF757575.toInt(),
                    icon = categoryIconMap[catId] ?: "more_horiz"
                ),
                totalAmount = totalAmt,
                transactionCount = categoryCountMap[catId] ?: 0,
                percentage = percentage
            )
        }.sortedByDescending { it.totalAmount }

        // Calculate 6-month trends
        val sixMonthsTrends = mutableListOf<MonthlyTrend>()
        var curY = todayJalali.year
        var curM = todayJalali.month

        val monthsToInspect = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until 6) {
            monthsToInspect.add(0, Pair(curY, curM))
            if (curM == 1) {
                curY -= 1
                curM = 12
            } else {
                curM -= 1
            }
        }

        for ((y, m) in monthsToInspect) {
            val monthStart = JalaliDate(y, m, 1).toStartOfDayTimestamp(zoneId)
            val monthEnd = JalaliDate(y, m, JalaliDate.getJalaliMonthLength(y, m)).toEndOfDayTimestamp(zoneId)

            var mIncome = 0L
            var mExpense = 0L

            for (txItem in allTxWithDetails) {
                val tx = txItem.transaction
                if (accountId != null && tx.accountId != accountId) continue
                if (tx.date in monthStart..monthEnd) {
                    when (tx.type) {
                        TransactionType.INCOME -> mIncome += tx.amount
                        TransactionType.EXPENSE -> mExpense += tx.amount
                        else -> {}
                    }
                }
            }

            val mName = JalaliDate.MONTH_NAMES.getOrNull(m - 1) ?: "$m"
            sixMonthsTrends.add(
                MonthlyTrend(
                    monthName = mName,
                    year = y,
                    month = m,
                    income = mIncome,
                    expense = mExpense
                )
            )
        }

        // 1. Month-over-Month & Financial Insights
        val curMonthStart = JalaliDate(todayJalali.year, todayJalali.month, 1).toStartOfDayTimestamp(zoneId)
        val curMonthEnd = JalaliDate(todayJalali.year, todayJalali.month, JalaliDate.getJalaliMonthLength(todayJalali.year, todayJalali.month)).toEndOfDayTimestamp(zoneId)

        val prevY = if (todayJalali.month == 1) todayJalali.year - 1 else todayJalali.year
        val prevM = if (todayJalali.month == 1) 12 else todayJalali.month - 1
        val prevMonthStart = JalaliDate(prevY, prevM, 1).toStartOfDayTimestamp(zoneId)
        val prevMonthEnd = JalaliDate(prevY, prevM, JalaliDate.getJalaliMonthLength(prevY, prevM)).toEndOfDayTimestamp(zoneId)

        var curMonthExpense = 0L
        var prevMonthExpense = 0L
        val curMonthCategoryExpenses = mutableMapOf<Long, Long>()

        for (txItem in allTxWithDetails) {
            val tx = txItem.transaction
            if (accountId != null && tx.accountId != accountId) continue
            if (tx.type == TransactionType.EXPENSE) {
                if (tx.date in curMonthStart..curMonthEnd) {
                    curMonthExpense += tx.amount
                    val catId = tx.categoryId ?: 0L
                    curMonthCategoryExpenses[catId] = (curMonthCategoryExpenses[catId] ?: 0L) + tx.amount
                } else if (tx.date in prevMonthStart..prevMonthEnd) {
                    prevMonthExpense += tx.amount
                }
            }
        }

        val diffPercent: Double? = if (prevMonthExpense > 0) {
            ((curMonthExpense - prevMonthExpense).toDouble() / prevMonthExpense.toDouble()) * 100.0
        } else null

        val topCategoryEntry = curMonthCategoryExpenses.maxByOrNull { it.value }
        val topCategoryName = topCategoryEntry?.let { entry ->
            categoriesList.find { it.id == entry.key }?.name ?: "نامشخص"
        }
        val topCategoryPercent = if (curMonthExpense > 0 && topCategoryEntry != null) {
            (topCategoryEntry.value.toFloat() / curMonthExpense.toFloat()) * 100f
        } else 0f

        val insight = FinancialInsight(
            currentMonthExpense = curMonthExpense,
            lastMonthExpense = prevMonthExpense,
            expenseDiffPercent = diffPercent,
            isExpenseIncreased = curMonthExpense > prevMonthExpense,
            dailyAverageExpenseThisMonth = curMonthExpense / maxOf(1, todayJalali.day),
            topSpendingCategoryName = topCategoryName,
            topSpendingCategoryPercent = topCategoryPercent
        )

        // 2. Category Budget Progress
        val budgetProgressList = categoriesList.filter { it.monthlyBudget > 0 }.map { cat ->
            val spent = curMonthCategoryExpenses[cat.id] ?: 0L
            val pct = (spent.toFloat() / cat.monthlyBudget.toFloat()) * 100f
            CategoryBudgetProgress(
                category = cat,
                spentThisMonth = spent,
                budget = cat.monthlyBudget,
                percentage = pct,
                remaining = maxOf(0L, cat.monthlyBudget - spent),
                isOverBudget = spent > cat.monthlyBudget
            )
        }

        ReportsUiState(
            selectedPeriod = period,
            customStartTimestamp = customStart,
            customEndTimestamp = customEnd,
            selectedAccountId = accountId,
            selectedCategoryIds = categoryIds,
            selectedType = filterType,
            isFullscreen = isFullscreen,
            accounts = accountsList,
            categories = categoriesList,
            totalIncome = incomeSum,
            totalExpense = expenseSum,
            netBalance = incomeSum - expenseSum,
            categorySummaries = summaries,
            monthlyTrends = sixMonthsTrends,
            budgetProgressList = budgetProgressList,
            financialInsight = insight,
            filteredTransactions = inRange,
            currentStartTimestamp = start,
            currentEndTimestamp = end,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState(isLoading = true)
    )

    fun exportReportCsv(): String {
        return com.example.util.CsvExporter.generateTransactionsCsv(uiState.value.filteredTransactions)
    }

    fun updateCategoryBudget(categoryId: Long, budget: Long) {
        viewModelScope.launch {
            categoryRepository.updateBudget(categoryId, budget)
        }
    }

    fun applyFilters(
        period: DateFilterPeriod,
        customStart: Long?,
        customEnd: Long?,
        accountId: Long?,
        categoryIds: Set<Long>,
        type: TransactionType?
    ) {
        _selectedPeriod.value = period
        _customStartTimestamp.value = customStart
        _customEndTimestamp.value = customEnd
        _selectedAccountId.value = accountId
        _selectedCategoryIds.value = categoryIds
        _selectedType.value = type
    }

    fun resetFilters() {
        _selectedPeriod.value = DateFilterPeriod.ALL
        _customStartTimestamp.value = null
        _customEndTimestamp.value = null
        _selectedAccountId.value = null
        _selectedCategoryIds.value = emptySet()
        _selectedType.value = null
    }

    fun setPeriod(period: DateFilterPeriod) {
        _selectedPeriod.value = period
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customStartTimestamp.value = start
        _customEndTimestamp.value = end
        _selectedPeriod.value = DateFilterPeriod.CUSTOM
    }

    fun setAccountFilter(accountId: Long?) {
        _selectedAccountId.value = accountId
    }

    fun setCategoryFilter(categoryIds: Set<Long>) {
        _selectedCategoryIds.value = categoryIds
    }

    fun setReportType(type: TransactionType?) {
        _selectedType.value = type
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportsViewModel(transactionRepository, accountRepository, categoryRepository) as T
        }
    }
}
