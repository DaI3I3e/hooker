package com.example.data.local.relation

import androidx.room.Embedded
import com.example.data.local.entity.AccountEntity

data class AccountWithBalance(
    @Embedded val account: AccountEntity,
    val currentBalance: Long,
    val transactionCount: Int
)
