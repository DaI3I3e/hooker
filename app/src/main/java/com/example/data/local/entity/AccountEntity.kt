package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType {
    CASH, BANK, CREDIT_CARD
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: Long,
    val color: Int,
    val icon: String,
    val cardNumber: String? = null,
    val shabaNumber: String? = null,
    val cardExpiry: String? = null,
    val logoResName: String? = null,
    val logoImage: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
