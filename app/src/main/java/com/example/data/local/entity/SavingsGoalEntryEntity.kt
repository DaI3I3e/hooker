package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SavingsEntryType {
    DEPOSIT, WITHDRAW
}

@Entity(
    tableName = "savings_goal_entries",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("goalId"),
        Index("transactionId")
    ]
)
data class SavingsGoalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long,
    val transactionId: Long? = null,
    val amount: Long,
    val type: SavingsEntryType,
    val date: Long = System.currentTimeMillis()
)
