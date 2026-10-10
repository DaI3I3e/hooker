package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.GroupExpenseEntity
import com.example.data.local.entity.GroupExpenseShareEntity
import com.example.data.local.relation.GroupExpenseWithShares
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupExpense(groupExpense: GroupExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShares(shares: List<GroupExpenseShareEntity>): List<Long>

    @Update
    suspend fun updateShare(share: GroupExpenseShareEntity)

    @Delete
    suspend fun deleteGroupExpense(groupExpense: GroupExpenseEntity)

    @Query("DELETE FROM group_expenses WHERE transactionId = :transactionId")
    suspend fun deleteByTransactionId(transactionId: Long)

    @Transaction
    @Query("SELECT * FROM group_expenses ORDER BY createdAt DESC")
    fun getAllWithShares(): Flow<List<GroupExpenseWithShares>>

    @Transaction
    @Query("SELECT * FROM group_expenses WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getByTransactionId(transactionId: Long): GroupExpenseWithShares?

    @Transaction
    @Query("SELECT * FROM group_expenses WHERE transactionId = :transactionId LIMIT 1")
    fun observeByTransactionId(transactionId: Long): Flow<GroupExpenseWithShares?>

    @Transaction
    @Query("SELECT * FROM group_expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): GroupExpenseWithShares?

    @Query("SELECT * FROM group_expense_shares WHERE id = :shareId LIMIT 1")
    suspend fun getShareById(shareId: Long): GroupExpenseShareEntity?

    @Query("SELECT DISTINCT personName FROM group_expense_shares WHERE personName IS NOT NULL AND personName != '' ORDER BY id DESC LIMIT 25")
    fun getRecentPersonNames(): Flow<List<String>>

    @Query("SELECT * FROM group_expenses")
    suspend fun getAllEntities(): List<GroupExpenseEntity>

    @Query("SELECT * FROM group_expense_shares")
    suspend fun getAllShares(): List<GroupExpenseShareEntity>

    @Query("DELETE FROM group_expense_shares")
    suspend fun deleteAllShares()

    @Query("DELETE FROM group_expenses")
    suspend fun deleteAllGroupExpenses()
}
