package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetAmount: Long,
    val currentAmount: Long = 0L,
    val targetDate: Long? = null,
    val color: Int = -16738680, // Default green #00897B
    val icon: String = "Savings",
    val isCompleted: Boolean = false,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
