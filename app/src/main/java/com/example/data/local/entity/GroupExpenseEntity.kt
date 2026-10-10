package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class GroupExpenseShareMode {
    EQUAL,
    CUSTOM
}

@Entity(
    tableName = "group_expenses",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("transactionId")
    ]
)
data class GroupExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val shareMode: GroupExpenseShareMode = GroupExpenseShareMode.EQUAL,
    val createdAt: Long = System.currentTimeMillis()
)
