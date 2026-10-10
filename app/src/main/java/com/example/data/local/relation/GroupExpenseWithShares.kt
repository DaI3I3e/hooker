package com.example.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.GroupExpenseEntity
import com.example.data.local.entity.GroupExpenseShareEntity
import com.example.data.local.entity.TransactionEntity

data class GroupExpenseWithShares(
    @Embedded
    val groupExpense: GroupExpenseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "groupExpenseId"
    )
    val shares: List<GroupExpenseShareEntity>,
    @Relation(
        parentColumn = "transactionId",
        entityColumn = "id"
    )
    val transaction: TransactionEntity?
)
