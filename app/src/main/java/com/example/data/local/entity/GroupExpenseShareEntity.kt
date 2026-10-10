package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_expense_shares",
    foreignKeys = [
        ForeignKey(
            entity = GroupExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupExpenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["settledTransactionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("groupExpenseId"),
        Index("settledTransactionId")
    ]
)
data class GroupExpenseShareEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupExpenseId: Long,
    val personName: String,
    val amount: Long, // Amount in Rial
    val isSettled: Boolean = false,
    val settledTransactionId: Long? = null
)
