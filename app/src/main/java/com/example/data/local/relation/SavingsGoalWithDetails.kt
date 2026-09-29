package com.example.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.SavingsEntryType
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.SavingsGoalEntryEntity

data class SavingsGoalWithDetails(
    @Embedded
    val goal: SavingsGoalEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "goalId"
    )
    val entries: List<SavingsGoalEntryEntity> = emptyList()
) {
    val currentAmount: Long
        get() = entries.sumOf {
            if (it.type == SavingsEntryType.DEPOSIT) it.amount else -it.amount
        }.coerceAtLeast(0L)

    val isCompleted: Boolean
        get() = goal.targetAmount > 0 && currentAmount >= goal.targetAmount
}
