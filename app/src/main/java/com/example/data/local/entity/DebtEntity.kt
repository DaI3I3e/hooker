package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DebtType {
    OWED_TO_ME, // طلب‌های من
    I_OWE       // بدهی‌های من
}

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personName: String,
    val type: DebtType,
    val amount: Long,
    val dueDate: Long? = null,
    val note: String? = null,
    val isSettled: Boolean = false,
    val settledDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
