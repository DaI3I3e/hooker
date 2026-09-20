package com.example.domain.model

import com.example.data.local.relation.AccountWithBalance
import com.example.data.local.relation.TransactionWithDetails

data class DashboardSummary(
    val totalBalance: Long,
    val todayIncome: Long,
    val todayExpense: Long,
    val recentTransactions: List<TransactionWithDetails>,
    val accounts: List<AccountWithBalance>
)
