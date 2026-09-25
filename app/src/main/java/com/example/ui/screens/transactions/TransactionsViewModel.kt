package com.example.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.local.relation.TransactionWithDetails
import com.example.data.repository.AccountRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.TransactionRepository
import com.example.util.JalaliDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

enum class DateFilterPeriod(val label: String) {
    TODAY("امروز"),
    WEEK("هفته"),
    MONTH("ماه"),
    ALL("همه"),
    CUSTOM("دلخواه")
}

enum class TransactionSortOrder(val label: String) {
    DATE_DESC("جدیدترین"),
    DATE_ASC("قدیمی‌ترین"),
    AMOUNT_DESC("بیشترین مبلغ"),
    AMOUNT_ASC("کمترین مبلغ")
}

data class TransactionsUiState(
    val selectedPeriod: DateFilterPeriod = DateFilterPeriod.ALL,
    val selectedSortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
    val customStartTimestamp: Long? = null,
    val customEndTimestamp: Long? = null,
    val selectedType: TransactionType? = null, // null means ALL
    val selectedAccountId: Long? = null, // null means ALL ACCOUNTS
    val selectedCategoryIds: Set<Long> = emptySet(), // empty means ALL CATEGORIES
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isFullscreen: Boolean = false,
    val zoomLevel: Float = 1.0f,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val rawFilteredTransactions: List<TransactionWithDetails> = emptyList(),
    val groupedTransactions: Map<String, List<TransactionWithDetails>> = emptyMap(),
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
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

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(DateFilterPeriod.ALL)
    private val _selectedSortOrder = MutableStateFlow(TransactionSortOrder.DATE_DESC)
    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    private val _selectedCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _customStartTimestamp = MutableStateFlow<Long?>(null)
    private val _customEndTimestamp = MutableStateFlow<Long?>(null)
    private val _isFullscreen = MutableStateFlow(false)
    private val _zoomLevel = MutableStateFlow(1.0f)

    private var recentlyDeletedTransaction: TransactionEntity? = null

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.allTransactionsWithDetails,
        accountRepository.allAccounts,
        categoryRepository.allCategories,
        _selectedPeriod,
        _selectedSortOrder,
        _selectedType,
        _selectedAccountId,
        _selectedCategoryIds,
        _searchQuery,
        _isSearchActive,
        _customStartTimestamp,
        _customEndTimestamp,
        _isFullscreen,
        _zoomLevel
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val transactions = flows[0] as List<TransactionWithDetails>
        @Suppress("UNCHECKED_CAST")
        val accountsList = flows[1] as List<AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val categoriesList = flows[2] as List<CategoryEntity>
        val period = flows[3] as DateFilterPeriod
        val sortOrder = flows[4] as TransactionSortOrder
        @Suppress("UNCHECKED_CAST")
        val type = flows[5] as TransactionType?
        val accountId = flows[6] as Long?
        @Suppress("UNCHECKED_CAST")
        val categoryIds = flows[7] as Set<Long>
        val query = flows[8] as String
        val isSearch = flows[9] as Boolean
        val customStart = flows[10] as Long?
        val customEnd = flows[11] as Long?
        val isFullscreen = flows[12] as Boolean
        val zoom = flows[13] as Float

        val zoneId = ZoneId.systemDefault()
        val todayJalali = JalaliDate.today(zoneId)

        val filtered = transactions.filter { tx ->
            val date = tx.transaction.date

            // Date filter
            val matchesPeriod = when (period) {
                DateFilterPeriod.TODAY -> {
                    date >= todayJalali.toStartOfDayTimestamp(zoneId) && date <= todayJalali.toEndOfDayTimestamp(zoneId)
                }
                DateFilterPeriod.WEEK -> {
                    val startOfWeek = todayJalali.toStartOfDayTimestamp(zoneId) - (6 * 24 * 60 * 60 * 1000L)
                    date >= startOfWeek
                }
                DateFilterPeriod.MONTH -> {
                    val startOfMonth = JalaliDate.getStartOfCurrentMonth(zoneId)
                    val endOfMonth = JalaliDate.getEndOfCurrentMonth(zoneId)
                    date >= startOfMonth && date <= endOfMonth
                }
                DateFilterPeriod.CUSTOM -> {
                    val s = customStart ?: 0L
                    val e = customEnd ?: Long.MAX_VALUE
                    date in s..e
                }
                DateFilterPeriod.ALL -> true
            }

            // Type filter
            val matchesType = type == null || tx.transaction.type == type

            // Account filter
            val matchesAccount = accountId == null || tx.transaction.accountId == accountId || tx.transaction.toAccountId == accountId

            // Category filter (multi-select)
            val matchesCategory = categoryIds.isEmpty() || (tx.transaction.categoryId != null && categoryIds.contains(tx.transaction.categoryId))

            // Search query filter (support note, category, account, and exact or partial amount in Rial/Toman)
            val cleanQuery = query.trim()
            val numericQuery = cleanQuery.replace(",", "")
                .replace("،", "")
                .replace("۰", "0").replace("۱", "1").replace("۲", "2").replace("۳", "3").replace("۴", "4")
                .replace("۵", "5").replace("۶", "6").replace("۷", "7").replace("۸", "8").replace("۹", "9")

            val matchesAmount = numericQuery.isNotEmpty() && numericQuery.all { it.isDigit() } && (
                tx.transaction.amount.toString().contains(numericQuery) ||
                (tx.transaction.amount / 10).toString().contains(numericQuery)
            )

            val matchesQuery = cleanQuery.isBlank() ||
                (tx.transaction.note?.contains(cleanQuery, ignoreCase = true) == true) ||
                (tx.categoryName?.contains(cleanQuery, ignoreCase = true) == true) ||
                (tx.accountName?.contains(cleanQuery, ignoreCase = true) == true) ||
                (tx.toAccountName?.contains(cleanQuery, ignoreCase = true) == true) ||
                matchesAmount

            matchesPeriod && matchesType && matchesAccount && matchesCategory && matchesQuery
        }

        // Compute total income and expense for filtered list
        var incomeSum = 0L
        var expenseSum = 0L
        filtered.forEach { item ->
            when (item.transaction.type) {
                TransactionType.INCOME -> incomeSum += item.transaction.amount
                TransactionType.EXPENSE -> expenseSum += item.transaction.amount
                TransactionType.TRANSFER -> {}
            }
        }

        // Sort transactions according to selectedSortOrder
        val sorted = when (sortOrder) {
            TransactionSortOrder.DATE_DESC -> filtered.sortedByDescending { it.transaction.date }
            TransactionSortOrder.DATE_ASC -> filtered.sortedBy { it.transaction.date }
            TransactionSortOrder.AMOUNT_DESC -> filtered.sortedByDescending { it.transaction.amount }
            TransactionSortOrder.AMOUNT_ASC -> filtered.sortedBy { it.transaction.amount }
        }

        // Group by Jalali Date String or Sort criteria
        val grouped = if (sortOrder == TransactionSortOrder.AMOUNT_DESC || sortOrder == TransactionSortOrder.AMOUNT_ASC) {
            sorted.groupBy { "مرتب‌سازی: ${sortOrder.label}" }
        } else {
            sorted.groupBy { item ->
                val jalali = JalaliDate.fromTimestamp(item.transaction.date, zoneId)
                if (jalali == todayJalali) {
                    "امروز - ${jalali.format()}"
                } else {
                    jalali.format(includeDayName = true)
                }
            }
        }

        TransactionsUiState(
            selectedPeriod = period,
            selectedSortOrder = sortOrder,
            customStartTimestamp = customStart,
            customEndTimestamp = customEnd,
            selectedType = type,
            selectedAccountId = accountId,
            selectedCategoryIds = categoryIds,
            searchQuery = query,
            isSearchActive = isSearch,
            isFullscreen = isFullscreen,
            zoomLevel = zoom,
            accounts = accountsList,
            categories = categoriesList,
            rawFilteredTransactions = sorted,
            groupedTransactions = grouped,
            totalIncome = incomeSum,
            totalExpense = expenseSum,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState(isLoading = true)
    )

    fun setSortOrder(order: TransactionSortOrder) {
        _selectedSortOrder.value = order
    }

    fun exportTransactionsCsv(): String {
        return com.example.util.CsvExporter.generateTransactionsCsv(uiState.value.rawFilteredTransactions)
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

    fun setType(type: TransactionType?) {
        _selectedType.value = type
    }

    fun setAccountFilter(accountId: Long?) {
        _selectedAccountId.value = accountId
    }

    fun setCategoryFilter(categoryIds: Set<Long>) {
        _selectedCategoryIds.value = categoryIds
    }

    fun setZoomLevel(zoom: Float) {
        _zoomLevel.value = zoom.coerceIn(0.7f, 1.5f)
    }

    fun zoomIn() {
        setZoomLevel(_zoomLevel.value + 0.1f)
    }

    fun zoomOut() {
        setZoomLevel(_zoomLevel.value - 0.1f)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        recentlyDeletedTransaction = transaction
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun undoDelete() {
        recentlyDeletedTransaction?.let { deleted ->
            viewModelScope.launch {
                transactionRepository.insertTransaction(deleted)
                recentlyDeletedTransaction = null
            }
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionsViewModel(transactionRepository, accountRepository, categoryRepository) as T
        }
    }
}
