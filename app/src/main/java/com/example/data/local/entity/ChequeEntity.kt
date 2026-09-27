package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ChequeType(val title: String) {
    PAYABLE("پرداختی (صادره توسط من)"),
    RECEIVABLE("دریافتی (واگذار شده به من)")
}

enum class ChequeStatus(val title: String) {
    PENDING("در انتظار سررسید"),
    CLEARED("پاس شده"),
    BOUNCED("برگشت خورده"),
    CANCELLED("باطل شده")
}

@Entity(tableName = "cheques")
data class ChequeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sayadNumber: String,
    val type: ChequeType,
    val amount: Long,
    val dueDate: Long,
    val issueDate: Long? = null,
    val partyName: String, // Recipient or Issuer name
    val bankName: String,
    val accountId: Long? = null,
    val status: ChequeStatus = ChequeStatus.PENDING,
    val note: String? = null,
    val clearedDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
